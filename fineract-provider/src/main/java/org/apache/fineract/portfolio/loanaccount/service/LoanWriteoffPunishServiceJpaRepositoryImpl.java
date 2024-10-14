package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.fineract.custom.infrastructure.core.service.CustomDateUtils;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.data.ApproveLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.CreateLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.data.DisburseLoanPayloadData;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcessRepository;
import org.apache.fineract.custom.portfolio.buyprocess.validator.ClientBuyProcessDataValidator;
import org.apache.fineract.infrastructure.bulkimport.importhandler.helper.DateSerializer;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.GoogleGsonSerializerHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanEventApiJsonValidator;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoanWriteoffPunishServiceJpaRepositoryImpl implements LoanWriteoffPunishService{

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final LoanWritePlatformService loanWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final LoanApplicationWritePlatformService loanApplicationWritePlatformService;
    private final LoanProductRepository loanProductRepository;
    private final LoanAssembler loanAssembler;
    private final ExternalIdFactory externalIdFactory;
    private final LoanEventApiJsonValidator loanEventApiJsonValidator;
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final LoanRepositoryWrapper loanRepository;
    private final BlockingReasonSettingsRepositoryWrapper blockingReasonSettingsRepositoryWrapper;
    private final LoanBlockingReasonRepository blockingReasonRepository;

    @Override
    public CommandProcessingResult writeOffPunishLoan(final Long loanId,JsonCommand command) {
        // Create new Loan and disburse as topup
        final String json = command.json();
        final JsonElement element = fromApiJsonHelper.parse(json);
        Loan existingLoanApplication = this.loanAssembler.assembleFrom(loanId);
        final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.transactionDateParamName, element);
        final String claimType = this.fromApiJsonHelper.extractStringNamed("claimType", element);
        final ExternalId externalId = externalIdFactory.createFromCommand(command, LoanApiConstants.externalIdParameterName);
        this.loanEventApiJsonValidator.validateLoanClaim(command.json());
        final Map<String, Object> changes = new LinkedHashMap<>();
        // Got changed to match with the rest of the APIs
        changes.put("dateFormat", command.dateFormat());
        changes.put("transactionDate", command.stringValueOfParameterNamed(LoanApiConstants.transactionDateParamName));
        changes.put("claimType", claimType);

        List<LoanProduct> castigadoProducts = this.loanProductRepository.findByProductType("SU+ Castigado");
        if (castigadoProducts.isEmpty()) {
            throw new LoanProductNotFoundException("error.msg.loanproduct.type.invalid", "Loan product with type SU+ Castigado does not exist", "SU+ Castigado");
        }
        if (castigadoProducts.size() > 1) {
            throw new LoanProductNotFoundException("error.msg.loanproduct.type.invalid", "Multiple Loan products with type SU+ Castigado exist", "SU+ Castigado");
        }

        LoanProduct castigadoProduct = castigadoProducts.get(0);

        final LoanRepaymentScheduleInstallment foreCloseDetail = existingLoanApplication.fetchLoanForeclosureDetail(transactionDate);
        BigDecimal outstandingAmount = foreCloseDetail.getTotalOutstanding(existingLoanApplication.getCurrency()).getAmount();
        existingLoanApplication.setClaimType(claimType);
        existingLoanApplication.setClaimDate(transactionDate);

        this.loanRepository.saveAndFlush(existingLoanApplication);
        // Create new Loan Application
        Long newLoanId = createLoanApplication(outstandingAmount, castigadoProduct, existingLoanApplication, transactionDate);
        Loan newLoan = this.loanAssembler.assembleFrom(newLoanId);

        // Approve loan
        approveLoanApplication(newLoan, outstandingAmount, transactionDate);

        // disburse loan
        disburseLoanApplication(newLoan, outstandingAmount, transactionDate);

        //Get updated existingLoan
        existingLoanApplication = this.loanAssembler.assembleFrom(loanId);

        existingLoanApplication.setClaimType(claimType);
        existingLoanApplication.setClaimDate(transactionDate);

        this.loanRepository.saveAndFlush(existingLoanApplication);

        newLoan = this.loanAssembler.assembleFrom(newLoanId);
        BlockingReasonSetting blockingReasonSetting = blockingReasonSettingsRepositoryWrapper
                .getSingleBlockingReasonSettingByReason("Castigado", BlockLevel.CREDIT.toString());

        newLoan.getLoanCustomizationDetail().setBlockStatus(blockingReasonSetting);
        final LoanBlockingReason loanBlockingReason = LoanBlockingReason.instance(newLoan, blockingReasonSetting,
                "Castigado", DateUtils.getLocalDateOfTenant());
        blockingReasonRepository.saveAndFlush(loanBlockingReason);
        this.loanRepository.saveAndFlush(newLoan);

        this.loanRepository.removeLoanExclusion(existingLoanApplication.claimType());

        final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
        return commandProcessingResultBuilder //
                .withLoanId(newLoanId) //
                .with(changes) //
                .build();

    }

    private Long createLoanApplication(BigDecimal principalAmount, LoanProduct loanProduct, Loan existingLoan, LocalDate transactionDate) {

        BigDecimal interestRatePerPeriod = loanProduct.getLoanProductRelatedDetail().getNominalInterestRatePerPeriod();
        final List<LoanChargeData> loanCharges = new ArrayList<>();

        for (Charge charge : loanProduct.getLoanProductCharges()) {
            if (!charge.isOverdueInstallment()) {
                final LoanChargeData loanChargeData = LoanChargeData.builder().chargeId(charge.getId()).amount(charge.getAmount()).build();
                loanCharges.add(loanChargeData);
            }
        }

        final ClientData clientData = getClientExtras(existingLoan.getClientId());
        String clientIdNumber = null;
        if (clientData != null) {
            clientIdNumber = clientData.getIdNumber();
        }

        final CreateLoanPayloadData payloadData = CreateLoanPayloadData.builder()
                .productId(loanProduct.getId())
                .submittedOnDate(DateUtils.format(transactionDate, CustomDateUtils.ENGLISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(transactionDate, CustomDateUtils.ENGLISH_DATE_FORMAT))
                .loanTermFrequency(1L)
                .loanTermFrequencyType(loanProduct.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue())
                .numberOfRepayments(1L)
                .repaymentEvery(loanProduct.getLoanProductRelatedDetail().getRepayEvery())
                .repaymentFrequencyType(loanProduct.getLoanProductRelatedDetail().getRepaymentPeriodFrequencyType().getValue())
                .interestRatePerPeriod(interestRatePerPeriod)
                .interestType(loanProduct.getLoanProductRelatedDetail().getInterestMethod().getValue())
                .amortizationType(loanProduct.getLoanProductRelatedDetail().getAmortizationMethod().getValue())
                .interestCalculationPeriodType(loanProduct.getLoanProductRelatedDetail().getInterestCalculationPeriodMethod().getValue())
                .transactionProcessingStrategyCode(loanProduct.getTransactionProcessingStrategyCode())
                .charges(loanCharges)
                .collateral(Collections.emptyList())
                .dateFormat(CustomDateUtils.ENGLISH_DATE_FORMAT)
                .locale("en")
                .clientId(existingLoan.getClientId())
                .clientIdNumber(clientIdNumber)
                .loanType("individual").principal(principalAmount)
                .graceOnPrincipalPayment(loanProduct.getLoanProductRelatedDetail().getGraceOnPrincipalPayment())
                .graceOnInterestPayment(loanProduct.getLoanProductRelatedDetail().getGraceOnInterestPayment())
                .graceOnInterestCharged(loanProduct.getLoanProductRelatedDetail().graceOnInterestCharged())
                .isTopup("true")
                .loanIdToClose(existingLoan.getId().toString())
                .isWriteoffPunish(true)
                .build();
        final GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.SPANISH_DATE_FORMAT));
        final String payload = gsonBuilder.create().toJson(payloadData);
        final JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        final JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        final CommandProcessingResult result = loanApplicationWritePlatformService.submitApplication(jsonCommand);
        return result.getLoanId();

    }

    private void approveLoanApplication(Loan loan, BigDecimal principalAmount,  LocalDate transactionDate) {

        ApproveLoanPayloadData payloadData = ApproveLoanPayloadData.builder()
                .approvedOnDate(DateUtils.format(transactionDate, CustomDateUtils.ENGLISH_DATE_FORMAT))
                .expectedDisbursementDate(DateUtils.format(transactionDate, CustomDateUtils.ENGLISH_DATE_FORMAT))
                .approvedLoanAmount(principalAmount)
                .locale("en")
                .dateFormat(CustomDateUtils.ENGLISH_DATE_FORMAT)
                .build();

        // Execute qpprove loan command
        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.ENGLISH_DATE_FORMAT));
        String payload = gsonBuilder.create().toJson(payloadData);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        loanApplicationWritePlatformService.approveApplication(loan.getId(), jsonCommand);
    }

    private void disburseLoanApplication(Loan loan, BigDecimal principalAmount, LocalDate transactionDate) {

        DisburseLoanPayloadData payloadData = DisburseLoanPayloadData.builder()
                .actualDisbursementDate(DateUtils.format(transactionDate, CustomDateUtils.ENGLISH_DATE_FORMAT))
                .transactionAmount(principalAmount)
                .locale("en")
                .dateFormat(CustomDateUtils.ENGLISH_DATE_FORMAT)
                .isWriteoffPunish(true)
                .build();

        // Execute disburse loan command

        GsonBuilder gsonBuilder = GoogleGsonSerializerHelper.createGsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDate.class, new DateSerializer(CustomDateUtils.ENGLISH_DATE_FORMAT));

        String payload = gsonBuilder.create().toJson(payloadData);
        JsonElement jsonElement = fromApiJsonHelper.parse(payload);
        JsonCommand jsonCommand = new JsonCommand(null, payload, jsonElement, fromApiJsonHelper, null, null, null, null, null, null, null,
                null, null, null, null, null, null);
        loanWritePlatformService.disburseLoan(loan.getId(), jsonCommand, false);

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
