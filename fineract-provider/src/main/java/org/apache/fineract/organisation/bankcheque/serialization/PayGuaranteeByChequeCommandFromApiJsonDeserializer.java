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
package org.apache.fineract.organisation.bankcheque.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.google.gson.JsonObject;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.organisation.bankcheque.api.BankChequeApiConstants;
import org.apache.fineract.organisation.bankcheque.command.PayGuaranteeByChequeCommand;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PayGuaranteeByChequeCommandFromApiJsonDeserializer extends AbstractFromApiJsonDeserializer<List<PayGuaranteeByChequeCommand>> {

    private final FromJsonHelper fromApiJsonHelper;

    @Override
    public List<PayGuaranteeByChequeCommand> commandFromApiJson(String json) {
        final JsonElement jsonElement = this.fromApiJsonHelper.parse(json);
        final Locale locale = this.fromApiJsonHelper.extractLocaleParameter(jsonElement.getAsJsonObject());
        JsonArray jsonArray = jsonElement.getAsJsonObject().getAsJsonArray("guarantees");
        List<PayGuaranteeByChequeCommand> payGuaranteeByChequeCommands = new ArrayList<>();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonElement originalElement = jsonArray.get(i);
            JsonObject asJsonObject = originalElement.getAsJsonObject();
            asJsonObject.addProperty("locale", locale.toLanguageTag());
            JsonElement element = this.fromApiJsonHelper.parse(originalElement.toString());
            final Long chequeId = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.CHEQUE_ID, element);
            baseDataValidator.reset().parameter(LoanApiConstants.CHEQUE_ID).value(chequeId).notBlank();
            final String caseId = this.fromApiJsonHelper.extractStringNamed(BankChequeApiConstants.GUARANTEE_CASE_ID, element);
            final String clientNo = this.fromApiJsonHelper.extractStringNamed(BankChequeApiConstants.CLIENT_NUMBER, element);
            baseDataValidator.reset().parameter(BankChequeApiConstants.GUARANTEE_CASE_ID).value(caseId).notBlank();
            baseDataValidator.reset().parameter(BankChequeApiConstants.CLIENT_NUMBER).value(clientNo).notBlank();
            final Long guaranteeId = this.fromApiJsonHelper.extractLongNamed(BankChequeApiConstants.GUARANTEE_ID, element);
            baseDataValidator.reset().parameter(BankChequeApiConstants.GUARANTEE_ID).value(guaranteeId).notBlank();
            final BigDecimal guaranteeAmount = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed(BankChequeApiConstants.GUARANTEE_AMOUNT,
                    element);
            baseDataValidator.reset().parameter(BankChequeApiConstants.GUARANTEE_AMOUNT).value(guaranteeAmount).notNull()
                    .notLessThanMin(BigDecimal.ZERO);
            final String guaranteeName = this.fromApiJsonHelper.extractStringNamed(BankChequeApiConstants.GUARANTEE_NAME, element);
            final PayGuaranteeByChequeCommand payGuaranteeByChequeCommand = PayGuaranteeByChequeCommand.builder().chequeId(chequeId)
                    .caseId(caseId).clientExternalId(clientNo).guaranteeId(guaranteeId).guaranteeAmount(guaranteeAmount)
                    .guaranteeName(guaranteeName).build();
            payGuaranteeByChequeCommands.add(payGuaranteeByChequeCommand);
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        return payGuaranteeByChequeCommands;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
