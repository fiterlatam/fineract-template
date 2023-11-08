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
package org.apache.fineract.portfolio.loanaccount.serialization;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.AbstractFromApiJsonDeserializer;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.command.DisburseByChequesCommand;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DisburseByChequesCommandFromApiJsonDeserializer extends AbstractFromApiJsonDeserializer<List<DisburseByChequesCommand>> {

    private final FromJsonHelper fromApiJsonHelper;

    @Override
    public List<DisburseByChequesCommand> commandFromApiJson(String json) {
        final JsonElement jsonElement = this.fromApiJsonHelper.parse(json);
        JsonArray jsonArray = jsonElement.getAsJsonArray();
        List<DisburseByChequesCommand> disburseByChequesCommands = new ArrayList<>();
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        for (int i = 0; i < jsonArray.size(); i++) {
            final JsonElement element = jsonArray.get(i);
            final Long chequeId = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.CHEQUE_ID, element);
            baseDataValidator.reset().parameter(LoanApiConstants.CHEQUE_ID).value(chequeId).notBlank();
            final Long loanId = this.fromApiJsonHelper.extractLongNamed(LoanApiConstants.LOAN_ID, element);
            baseDataValidator.reset().parameter(LoanApiConstants.LOAN_ID).value(loanId).notBlank();
            final BigDecimal actualGuaranteeAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApiConstants.ACTUAL_GUARANTEE_AMOUNT, element);
            baseDataValidator.reset().parameter(LoanApiConstants.ACTUAL_GUARANTEE_AMOUNT).value(actualGuaranteeAmount).notBlank()
                    .positiveAmount();
            final BigDecimal requiredGuaranteeAmount = this.fromApiJsonHelper
                    .extractBigDecimalWithLocaleNamed(LoanApiConstants.REQUIRED_GUARANTEE_AMOUNT, element);
            baseDataValidator.reset().parameter(LoanApiConstants.REQUIRED_GUARANTEE_AMOUNT).value(requiredGuaranteeAmount).notBlank()
                    .positiveAmount();
            final String description = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.CHEQUE_DESCRIPTION, element);
            baseDataValidator.reset().parameter(LoanApiConstants.CHEQUE_DESCRIPTION).value(description).notBlank()
                    .notExceedingLengthOf(1000);
            final String depositGuaranteeNo = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.DEPOSIT_GUARANTEE_NUMBER, element);
            baseDataValidator.reset().parameter(LoanApiConstants.DEPOSIT_GUARANTEE_NUMBER).value(depositGuaranteeNo).notBlank()
                    .notExceedingLengthOf(1000);
            final DisburseByChequesCommand disburseByChequesCommand = DisburseByChequesCommand.builder().chequeId(chequeId).loanId(loanId)
                    .actualGuaranteeAmount(actualGuaranteeAmount).requiredGuaranteeAmount(requiredGuaranteeAmount).description(description)
                    .depositGuaranteeNo(depositGuaranteeNo).build();
            disburseByChequesCommands.add(disburseByChequesCommand);
        }
        throwExceptionIfValidationWarningsExist(dataValidationErrors);
        return disburseByChequesCommands;
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
