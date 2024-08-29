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
package org.apache.fineract.custom.portfolio.buyprocess.validator;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.ClientAdditionalInformation;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.ClientAdditionalInformationRepository;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.IndividualAdditionalInformation;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.IndividualAdditionalInformationRepository;
import org.apache.fineract.custom.portfolio.ally.api.ClientAllyPointOfSalesApiConstants;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSalesRepository;
import org.apache.fineract.custom.portfolio.buyprocess.constants.ClientBuyProcessApiConstants;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcessRepository;
import org.apache.fineract.custom.portfolio.buyprocess.validator.chain.BuyProcessValidationLayerProcessor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.charge.data.ChargeData;
import org.apache.fineract.portfolio.charge.data.ChargeInsuranceDetailData;
import org.apache.fineract.portfolio.charge.domain.ChargeInsuranceType;
import org.apache.fineract.portfolio.charge.exception.ChargeNotFoundException;
import org.apache.fineract.portfolio.charge.service.ChargeReadPlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ClientBuyProcessDataValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final PlatformSecurityContext platformSecurityContext;
    private final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository;
    private final ClientAdditionalInformationRepository camposClienteEmpresaRepository;
    private final IndividualAdditionalInformationRepository individualAdditionalInformationRepository;
    private final ClientRepository clientRepository;
    private final LoanProductRepository loanProductRepository;
    private final ChargeReadPlatformService chargeReadPlatformService;

    @Autowired
    public ClientBuyProcessDataValidator(final FromJsonHelper fromApiJsonHelper, final PlatformSecurityContext platformSecurityContext,
            final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository,
            final ClientAdditionalInformationRepository camposClienteEmpresaRepository,
            final IndividualAdditionalInformationRepository individualAdditionalInformationRepository,
            final ClientRepository clientRepository, final LoanProductRepository loanProductRepository,
            final ChargeReadPlatformService chargeReadPlatformService) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
        this.camposClienteEmpresaRepository = camposClienteEmpresaRepository;
        this.individualAdditionalInformationRepository = individualAdditionalInformationRepository;
        this.clientRepository = clientRepository;
        this.loanProductRepository = loanProductRepository;
        this.chargeReadPlatformService = chargeReadPlatformService;
    }

    @Autowired
    private List<BuyProcessValidationLayerProcessor> buyProcessValidationLayerProcessors;

    public ClientBuyProcess validateForCreate(final String json, ClientBuyProcessRepository repository) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientBuyProcessApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientBuyProcessApiConstants.RESOURCE_NAME);

        boolean isSaleOfInsuranceOrAssistance;
        if (this.fromApiJsonHelper.parameterExists(ClientBuyProcessApiConstants.isSaleOfInsruanceOrAssistanceParamName, element)) {
            isSaleOfInsuranceOrAssistance = this.fromApiJsonHelper
                    .extractBooleanNamed(ClientBuyProcessApiConstants.isSaleOfInsruanceOrAssistanceParamName, element);
        } else {
            isSaleOfInsuranceOrAssistance = false;
        }

        Long clientId = 0L;
        String clientDocumentId;
        Client client = null;
        if (this.fromApiJsonHelper.parameterExists(ClientBuyProcessApiConstants.clientDocumentIdParamName, element)) {
            clientDocumentId = this.fromApiJsonHelper.extractStringNamed(ClientBuyProcessApiConstants.clientDocumentIdParamName, element);
            Optional<IndividualAdditionalInformation> camposClientePersona = individualAdditionalInformationRepository
                    .findByCedula(clientDocumentId);
            if (camposClientePersona.isPresent()) {
                clientId = camposClientePersona.get().getClientId();
            } else {
                Optional<ClientAdditionalInformation> camposClienteEmpresa = camposClienteEmpresaRepository.findByNit(clientDocumentId);
                if (camposClienteEmpresa.isPresent()) {
                    clientId = camposClienteEmpresa.get().getClientId();
                } else {
                    clientId = 0L;
                }
            }

            if (clientId > 0L) {
                Optional<Client> clientObject = clientRepository.findById(clientId);

                if (clientObject.isPresent()) {
                    client = clientObject.get();
                }
            }

        } else {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.clientDocumentIdParamName).value(null).notNull();
        }
        if (client == null) {
            baseDataValidator.reset().parameter("Cédula del cliente").failWithCode("Cédula del cliente no válido",
                    "Cédula del cliente no válido.");
        }
        Long pointOfSalesId = 0L;
        String pointOfSalesCode;
        if (this.fromApiJsonHelper.parameterExists(ClientBuyProcessApiConstants.pointOfSalesCodeParamName, element)) {
            pointOfSalesCode = this.fromApiJsonHelper.extractStringNamed(ClientBuyProcessApiConstants.pointOfSalesCodeParamName, element);
            Optional<ClientAllyPointOfSales> clientAllyPointOfSales = clientAllyPointOfSalesRepository.findByCode(pointOfSalesCode);
            if (clientAllyPointOfSales.isPresent()) {
                if (clientAllyPointOfSales.get().getStateCodeValueId() == ClientAllyPointOfSalesApiConstants.stateCodeValueInavtiveParamName
                        .longValue()) {
                    throw new ClientBuyProsessPoinOfSalesInactive("No se ha podido proceder debido a que el Punto de venta esta inactivo",
                            "");
                }
                pointOfSalesId = clientAllyPointOfSales.get().getId();
            } else {
                pointOfSalesId = 0L;
            }

        } else {
            if (isSaleOfInsuranceOrAssistance) {
                pointOfSalesId = null;
            } else {
                baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.pointOfSalesCodeParamName).value(null).notNull();
            }
        }

        final Long productId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.productIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.productIdParamName).value(productId).notNull();

        final Long creditId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.creditIdParamName, element);
        if (!isSaleOfInsuranceOrAssistance) {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.creditIdParamName).value(creditId).notNull();
        }

        final LocalDate requestedDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientBuyProcessApiConstants.requestedDateParamName,
                element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.requestedDateParamName).value(requestedDate).notNull();

        final Long term = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.termParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.termParamName).value(term).notNull();

        final LocalDateTime createdAt = DateUtils.getLocalDateTimeOfTenant();

        final Long createdBy = this.platformSecurityContext.authenticatedUser().getId();

        final String ipDetails = platformSecurityContext.getApiRequestClientIP();

        final Long codigoSeguro = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.codigoSeguroParamName, element);
        if (!isSaleOfInsuranceOrAssistance) {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.codigoSeguroParamName).value(codigoSeguro).ignoreIfNull()
                    .longZeroOrGreater();
        } else {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.codigoSeguroParamName).value(codigoSeguro).longZeroOrGreater();
        }

        final Long cedulaSeguroVoluntario = this.fromApiJsonHelper
                .extractLongNamed(ClientBuyProcessApiConstants.cedulaSeguroVoluntarioParamName, element);
        if (!isSaleOfInsuranceOrAssistance) {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.cedulaSeguroVoluntarioParamName).value(cedulaSeguroVoluntario)
                    .ignoreIfNull().longZeroOrGreater();
        } else {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.cedulaSeguroVoluntarioParamName).value(cedulaSeguroVoluntario)
                    .longZeroOrGreater();
        }

        final Integer interestRatePoints = this.fromApiJsonHelper.extractIntegerWithLocaleNamed(LoanApiConstants.INTEREST_RATE_POINTS,
                element);

        String channelHash = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.CHANNEL_HASH, element);
        if (channelHash == null) {
            channelHash = this.platformSecurityContext.getApiRequestChannel();
        }

        BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(ClientBuyProcessApiConstants.amountParamName, element);
        if (!isSaleOfInsuranceOrAssistance) {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.amountParamName).value(amount).notNull();
        } else {
            Optional<LoanProduct> entityOpt = loanProductRepository.findById(productId);
            if (entityOpt.isPresent()) {
                LoanProduct productEntity = entityOpt.get();
                if ((codigoSeguro != null && codigoSeguro > 0) && (cedulaSeguroVoluntario != null && cedulaSeguroVoluntario > 0)) {
                    final Collection<ChargeData> insuranceCharges = this.chargeReadPlatformService
                            .retrieveChargesByInsuranceCode(codigoSeguro);
                    if (CollectionUtils.isNotEmpty(insuranceCharges)) {
                        final ChargeData chargeData = insuranceCharges.iterator().next();
                        final ChargeInsuranceDetailData chargeInsuranceDetailData = chargeData.getChargeInsuranceDetailData();
                        if (chargeInsuranceDetailData != null) {
                            final ChargeInsuranceType chargeInsuranceType = ChargeInsuranceType
                                    .fromInt(chargeInsuranceDetailData.getInsuranceChargedAs() != null
                                            ? chargeInsuranceDetailData.getInsuranceChargedAs().intValue()
                                            : 0);
                            if (chargeInsuranceType.isCargo() || !productEntity.isPurChaseCharge()) {
                                baseDataValidator.reset().parameter("loanProductCharge").failWithCode("El tipo de cargo del producto de préstamo es carga.",
                                        "El tipo de cargo del producto de préstamo es carga.");
                            } else if (!productEntity.isPurChaseCharge()) {
                                baseDataValidator.reset().parameter("loanProduct").failWithCode("El producto de préstamo no es un cargo de compra",
                                        "El producto de préstamo no es un cargo de compra");
                            } else if (chargeInsuranceType.isCompra() && productEntity.isPurChaseCharge()) {
                                amount = chargeInsuranceDetailData.getTotalValue();
                            }
                        }
                    } else {
                        throw new ChargeNotFoundException("error.msg.charge.codigo.invalid", "Cobro con código seguro "+codigoSeguro +" no existe", codigoSeguro);
                    }
                }
            }
        }

        ClientBuyProcess ret = new ClientBuyProcess(null, clientId, pointOfSalesId, productId, creditId, requestedDate, amount, term,
                createdAt, createdBy, ipDetails, codigoSeguro, cedulaSeguroVoluntario, channelHash);
        ret.setClient(client);
        ret.setInterestRatePoints(interestRatePoints);
        ret.setSaleOfInsuranceOrAssistance(isSaleOfInsuranceOrAssistance);

        // If there is no primary errors, then execute second level validation chain
        if (dataValidationErrors.isEmpty()) {
            buyProcessValidationLayerProcessors.stream().sorted(Comparator.comparing(BuyProcessValidationLayerProcessor::getPriority))
                    .forEach(processor -> processor.validateStepChain(ret));

            if (Boolean.FALSE.equals(ret.getErrorMessageHM().isEmpty())) {
                // Add validation messages to the API Requester
                LinkedHashMap<String, String> errorMessageHM = ret.getErrorMessageHM();
                errorMessageHM.forEach((key, value) -> {
                    if (!isSaleOfInsuranceOrAssistance) {
                        baseDataValidator.reset().parameter(key).failWithCode("second.level.validation", value);
                    } else {
                        baseDataValidator.reset().parameter(key).failWithCode(value, value);
                    }
                });

                // Concatenate errorMessage to persist in the database and set status to 403
                String errorMessage = String.join(" | ", errorMessageHM.values());
                ret.setErrorMessage(errorMessage);
                ret.setStatus(HttpStatus.SC_FORBIDDEN);

                // repository.saveAndFlush(ret);

            } else {
                ret.setStatus(HttpStatus.SC_OK);
            }
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return ret;
    }

    public ClientBuyProcess validateForUpdate(final String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ClientBuyProcessApiConstants.REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientBuyProcessApiConstants.RESOURCE_NAME);

        final Long id = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.idParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.idParamName).value(id).notNull();

        final Long channelId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.channelIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.channelIdParamName).value(channelId).notNull();

        final Long clientId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.clientIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.clientIdParamName).value(clientId).notNull();

        final Long pointOfSalesId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.pointOfSalesIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.pointOfSalesIdParamName).value(pointOfSalesId).notNull();

        final Long creditId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.creditIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.creditIdParamName).value(creditId).notNull();

        final LocalDate requestedDate = this.fromApiJsonHelper.extractLocalDateNamed(ClientBuyProcessApiConstants.requestedDateParamName,
                element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.requestedDateParamName).value(requestedDate).notNull();

        final BigDecimal amount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(ClientBuyProcessApiConstants.amountParamName,
                element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.amountParamName).value(amount).notNull();

        final Long term = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.termParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.termParamName).value(term).notNull();

        final LocalDateTime createdAt = this.fromApiJsonHelper.extractLocalDateTimeNamed(ClientBuyProcessApiConstants.createdAtParamName,
                element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.createdAtParamName).value(createdAt).notNull();

        final Long createdBy = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.createdByParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.createdByParamName).value(createdBy).notNull();

        final String ipDetails = this.fromApiJsonHelper.extractStringNamed(ClientBuyProcessApiConstants.ipDetailsParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.ipDetailsParamName).value(ipDetails).notExceedingLengthOf(5000);

        final Long codigoSeguro = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.codigoSeguroParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.codigoSeguroParamName).value(codigoSeguro).ignoreIfNull()
                .longZeroOrGreater();

        final Long cedulaSeguroVoluntario = this.fromApiJsonHelper
                .extractLongNamed(ClientBuyProcessApiConstants.cedulaSeguroVoluntarioParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.cedulaSeguroVoluntarioParamName).value(cedulaSeguroVoluntario)
                .ignoreIfNull().longZeroOrGreater();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return new ClientBuyProcess(channelId, null, clientId, pointOfSalesId, null, creditId, requestedDate, amount, term, createdAt,
                createdBy, ipDetails, codigoSeguro, cedulaSeguroVoluntario);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
