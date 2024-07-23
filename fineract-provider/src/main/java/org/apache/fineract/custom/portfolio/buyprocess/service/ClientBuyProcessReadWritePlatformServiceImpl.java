/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.custom.portfolio.buyprocess.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import jakarta.persistence.PersistenceException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.data.ApproveLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.ClientBuyProcessData;
import org.apache.fineract.custom.portfolio.buyprocess.data.CreateLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.DisburseLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcessRepository;
import org.apache.fineract.custom.portfolio.buyprocess.exception.ClientBuyProcessNotCompletedException;
import org.apache.fineract.custom.portfolio.buyprocess.exception.ClientBuyProcessNotFoundException;
import org.apache.fineract.custom.portfolio.buyprocess.mapper.ClientBuyProcessMapper;
import org.apache.fineract.custom.portfolio.buyprocess.validator.ClientBuyProcessDataValidator;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.data.ChargeInsuranceDetailData;
import org.apache.fineract.portfolio.charge.domain.ChargeInsuranceType;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.service.LoanApplicationWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanWritePlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClientBuyProcessReadWritePlatformServiceImpl implements ClientBuyProcessReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientBuyProcessDataValidator validatorClass;
    private final PlatformSecurityContext context;
    private final LoanWritePlatformService loanWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanApplicationWritePlatformService loanApplicationWritePlatformService;
    private final ClientBuyProcessRepository clientBuyProcessRepository;
    private final LoanProductRepository loanProductRepository;
    private final ChargeReadPlatformService chargeReadPlatformService;
    private final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;

    @Autowired
    public ClientBuyProcessReadWritePlatformServiceImpl(JdbcTemplate jdbcTemplate, final ClientBuyProcessDataValidator validatorClass,
            final PlatformSecurityContext context, LoanWritePlatformService loanWritePlatformService, FromJsonHelper fromApiJsonHelper,
            LoanApplicationWritePlatformService loanApplicationWritePlatformService, ClientBuyProcessRepository clientBuyProcessRepository,
            LoanProductRepository loanProductRepository, ChargeReadPlatformService chargeReadPlatformService,
            ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.validatorClass = validatorClass;
        this.context = context;
        this.loanWritePlatformService = loanWritePlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.loanApplicationWritePlatformService = loanApplicationWritePlatformService;
        this.clientBuyProcessRepository = clientBuyProcessRepository;
        this.loanProductRepository = loanProductRepository;
        this.chargeReadPlatformService = chargeReadPlatformService;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
    }

    @Override
    public List<ClientBuyProcessData> findAllActive() {
        return ClientBuyProcessMapper.toDTO(clientBuyProcessRepository.findAll());
    }

    @Override
    public ClientBuyProcessData findById(Long id) {
        Optional<ClientBuyProcess> entity = clientBuyProcessRepository.findById(id);
        if (entity.isEmpty()) {
            throw new ClientBuyProcessNotFoundException();
        }
        return ClientBuyProcessMapper.toDTO(entity.get());
    }

    @Override
    public CommandProcessingResult create(final JsonCommand command) {

        try {
            this.context.authenticatedUser();
            final ClientBuyProcess entity = this.validatorClass.validateForCreate(command.json(), clientBuyProcessRepository);
            // Create Loan and disburse
            createApproveAndDisburseLoan(entity);
            clientBuyProcessRepository.saveAndFlush(entity);
            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).withLoanId(entity.getLoanId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException | PersistenceException dve) {
            handleDataIntegrityIssues(dve);
            return CommandProcessingResult.empty();
        }
    }

    private void createApproveAndDisburseLoan(final ClientBuyProcess entity) {

        // Get data from product
        Optional<LoanProduct> entityOpt = loanProductRepository.findById(entity.getProductId());
        if (entityOpt.isPresent()) {

            LoanProduct productEntity = entityOpt.get();

            // Build create loan dto
            createLoanApplication(entity, productEntity);

            // Approve loan
            approveLoanApplication(entity);

            // disburse loan
            disburseLoanApplication(entity, productEntity);

            log.info("Loan created and disbursed");
        } else {
            throw new ClientBuyProcessNotCompletedException();
        }
    }

    private void disburseLoanApplication(ClientBuyProcess entity, LoanProduct prodiuctEntity) {

        DisburseLoanPayloadData payloadData = DisburseLoanPayloadData.builder()
                .actualDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .transactionAmount(entity.getAmount()).locale("es").dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT)
                .channelName(entity.getChannelName()).build();

        // Execute create loan command
        log.info("Client Buy Process Disburse {} ", entity.getCreditId());

        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        loanWritePlatformService.disburseLoan(entity.getLoanId(), jsonCommand, false);

    }

    private void approveLoanApplication(ClientBuyProcess entity) {

        ApproveLoanPayloadData payloadData = ApproveLoanPayloadData.builder()
                .approvedOnDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .approvedLoanAmount(entity.getAmount()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("en").build();

        // Execute create loan command
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        loanApplicationWritePlatformService.approveApplication(entity.getLoanId(), jsonCommand);
    }

    private void createLoanApplication(final ClientBuyProcess entity, final LoanProduct productEntity) {
        BigDecimal loanPrincipalAmount = entity.getAmount();
        Long numberOfRepayments = entity.getTerm();
        final Long codigoSeguro = entity.getCodigoSeguro();
        final Long cedulaSeguroVoluntario = entity.getCedulaSeguroVoluntario();
        final List<LoanChargeData> loanCharges = new ArrayList<>();
        if ((codigoSeguro != null && codigoSeguro > 0) && (cedulaSeguroVoluntario != null && cedulaSeguroVoluntario > 0)) {
            final Collection<ChargeData> insuranceCharges = this.chargeReadPlatformService.retrieveChargesByInsuranceCode(codigoSeguro);
            if (CollectionUtils.isNotEmpty(insuranceCharges)) {
                final ChargeData chargeData = insuranceCharges.iterator().next();
                final ChargeInsuranceDetailData chargeInsuranceDetailData = chargeData.getChargeInsuranceDetailData();
                if (chargeInsuranceDetailData != null) {
                    final ChargeInsuranceType chargeInsuranceType = ChargeInsuranceType
                            .fromInt(chargeInsuranceDetailData.getInsuranceChargedAs() != null
                                    ? chargeInsuranceDetailData.getInsuranceChargedAs().intValue()
                                    : 0);
                    if (chargeInsuranceType.isCargo()) {
                        final Long loanChargeId = chargeData.getId();
                        final BigDecimal loanChargeAmount = chargeData.getAmount();
                        final LoanChargeData loanChargeData = LoanChargeData.builder().chargeId(loanChargeId).amount(loanChargeAmount)
                                .build();
                        loanCharges.add(loanChargeData);
                    } else if (chargeInsuranceType.isCompra()) {
                        loanPrincipalAmount = chargeInsuranceDetailData.getTotalValue();
                        numberOfRepayments = chargeInsuranceDetailData.getDeadline();
                    }
                }
            }
        }

        final ClientData clientData = getClientExtras(entity.getClientId());
        final ClientAllyPointOfSales clientAllyPointOfSales = this.clientAllyPointOfSalesRepository.findById(entity.getPointOfSalesId())
                .orElse(null);
        String pointOfSaleCode = null;
        String clientIdNumber = null;
        if (clientData != null) {
            clientIdNumber = clientData.getIdNumber();
        }
        if (clientAllyPointOfSales != null) {
            pointOfSaleCode = clientAllyPointOfSales.getCode();
        }

        final CreateLoanPayloadData payloadData = CreateLoanPayloadData.builder().productId(entity.getProductId())
                .interestRatePoints(entity.getInterestRatePoints())
                .submittedOnDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(entity.getRequestedDate(), CustomDateUtils.SPANISH_DATE_FORMAT))
                .loanTermFrequency(numberOfRepayments)
                .loanTermFrequencyType(productEntity.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue())
                .numberOfRepayments(numberOfRepayments).repaymentEvery(productEntity.getLoanProductRelatedDetail().getRepayEvery())
                .repaymentFrequencyType(productEntity.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue())
                .interestRatePerPeriod(productEntity.getLoanProductRelatedDetail().getNominalInterestRatePerPeriod())
                .interestType(productEntity.getLoanProductRelatedDetail().getInterestMethod().getValue())
                .amortizationType(productEntity.getLoanProductRelatedDetail().getAmortizationMethod().getValue())
                .interestCalculationPeriodType(productEntity.getLoanProductRelatedDetail().getInterestCalculationPeriodMethod().getValue())
                .transactionProcessingStrategyCode(productEntity.getTransactionProcessingStrategyCode()).charges(loanCharges)
                .collateral(Collections.emptyList()).dateFormat(CustomDateUtils.SPANISH_DATE_FORMAT).locale("es")
                .clientId(entity.getClientId()).loanType("individual").principal(loanPrincipalAmount)
                .graceOnPrincipalPayment(productEntity.getLoanProductRelatedDetail().getGraceOnPrincipalPayment())
                .graceOnInterestPayment(productEntity.getLoanProductRelatedDetail().getGraceOnInterestPayment())
                .graceOnInterestCharged(productEntity.getLoanProductRelatedDetail().graceOnInterestCharged()).clientIdNumber(clientIdNumber)
                .pointOfSaleCode(pointOfSaleCode).build();
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));
        final String payload = gsonBuilder.create().toJson(payloadData);
        final JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        final JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        final CommandProcessingResult result = loanApplicationWritePlatformService.submitApplication(jsonCommand);
        entity.setLoanId(result.getLoanId());
        entity.setAmount(loanPrincipalAmount);
        entity.setTerm(numberOfRepayments);
    }

    private void handleDataIntegrityIssues(final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.clientbuyprocess.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource." + dve.getMessage());
    }

    private ClientData getClientExtras(final Long clientId) {
        final String loanSQL = """
                      SELECT
                        mc.id AS "clientId",
                        COALESCE(cce."NIT", ccp."Cedula") AS "clientIdNumber"
                     FROM m_client mc
                     LEFT JOIN campos_cliente_empresas cce ON cce.client_id = mc.id
                     LEFT JOIN campos_cliente_persona ccp ON ccp.client_id = mc.id
                     WHERE mc.id = ?
                """;
        final List<ClientData> clients = jdbcTemplate.query(loanSQL, resultSet -> {
            final List<ClientData> clientDataList = new ArrayList<>();
            while (resultSet.next()) {
                final Long id = resultSet.getLong("clientId");
                final String clientIdNumber = resultSet.getString("clientIdNumber");
                final ClientData clientData = new ClientData();
                clientData.setId(id);
                clientData.setIdNumber(clientIdNumber);
                clientDataList.add(clientData);
            }
            return clientDataList;
        }, clientId);
        return CollectionUtils.isNotEmpty(clients) ? clients.get(0) : null;
    }

}
