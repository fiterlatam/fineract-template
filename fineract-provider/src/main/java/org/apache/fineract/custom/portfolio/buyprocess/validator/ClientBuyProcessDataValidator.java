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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.ClientAdditionalInformationRepository;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.IndividualAdditionalInformation;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.IndividualAdditionalInformationRepository;
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

    @Autowired
    public ClientBuyProcessDataValidator(final FromJsonHelper fromApiJsonHelper, final PlatformSecurityContext platformSecurityContext,
            final ClientAllyPointOfSalesRepository clientAllyPointOfSalesRepository,
            final ClientAdditionalInformationRepository camposClienteEmpresaRepository,
            final IndividualAdditionalInformationRepository individualAdditionalInformationRepository) {
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.platformSecurityContext = platformSecurityContext;
        this.clientAllyPointOfSalesRepository = clientAllyPointOfSalesRepository;
        this.camposClienteEmpresaRepository = camposClienteEmpresaRepository;
        this.individualAdditionalInformationRepository = individualAdditionalInformationRepository;
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

        final String channelAlias = this.platformSecurityContext.getApiRequestChannel();
        final Long channelId = null;
        ;
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.channelIdParamName).value(channelAlias).notNull();

        Long clientId = 0L;
        String clientDocumentId = StringUtils.EMPTY;
        if (this.fromApiJsonHelper.parameterExists(ClientBuyProcessApiConstants.clientDocumentIdParamName, element)) {
            clientDocumentId = this.fromApiJsonHelper.extractStringNamed(ClientBuyProcessApiConstants.clientDocumentIdParamName, element);

            Optional<IndividualAdditionalInformation> camposClientePersona = individualAdditionalInformationRepository
                    .findByCedula(clientDocumentId);
            if (camposClientePersona.isPresent()) {
                clientId = camposClientePersona.get().getClientId();
            } else {
                clientId = 0L;
            }

        } else {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.clientDocumentIdParamName).value(null).notNull();
        }

        Long pointOfSalesId = 0L;
        String pointOfSalesCode = StringUtils.EMPTY;
        if (this.fromApiJsonHelper.parameterExists(ClientBuyProcessApiConstants.pointOfSalesCodeParamName, element)) {
            pointOfSalesCode = this.fromApiJsonHelper.extractStringNamed(ClientBuyProcessApiConstants.pointOfSalesCodeParamName, element);

            if (clientAllyPointOfSalesRepository.findByCode(pointOfSalesCode).isPresent()) {
                pointOfSalesId = clientAllyPointOfSalesRepository.findByCode(pointOfSalesCode).get().getId();
            } else {
                pointOfSalesId = 0L;
            }

        } else {
            baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.pointOfSalesCodeParamName).value(null).notNull();
        }

        final Long productId = this.fromApiJsonHelper.extractLongNamed(ClientBuyProcessApiConstants.productIdParamName, element);
        baseDataValidator.reset().parameter(ClientBuyProcessApiConstants.productIdParamName).value(productId).notNull();

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

        final LocalDateTime createdAt = DateUtils.getLocalDateTimeOfTenant();

        final Long createdBy = this.platformSecurityContext.authenticatedUser().getId();

        final String ipDetails = platformSecurityContext.getApiRequestClientIP();

        ClientBuyProcess ret = new ClientBuyProcess(channelId, channelAlias, clientId, pointOfSalesId, productId, creditId, requestedDate,
                amount, term, createdAt, createdBy, ipDetails);

        // If there is no primary errors, then execute second level validation chain
        if (dataValidationErrors.isEmpty()) {
            buyProcessValidationLayerProcessors.stream().sorted(Comparator.comparing(BuyProcessValidationLayerProcessor::getPriority))
                    .forEach(processor -> processor.validateStepChain(ret));

            if (Boolean.FALSE.equals(ret.getErrorMessageHM().isEmpty())) {
                // Add validation messages to the API Requester
                LinkedHashMap<String, String> errorMessageHM = ret.getErrorMessageHM();
                errorMessageHM.forEach((key, value) -> {
                    baseDataValidator.reset().parameter(key).failWithCode("second.level.validation", value);
                });

                // Concatenate errorMessage to persist in the database and set status to 403
                String errorMessage = errorMessageHM.values().stream().collect(Collectors.joining(" | "));
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

        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return new ClientBuyProcess(channelId, null, clientId, pointOfSalesId, null, creditId, requestedDate, amount, term, createdAt,
                createdBy, ipDetails);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
