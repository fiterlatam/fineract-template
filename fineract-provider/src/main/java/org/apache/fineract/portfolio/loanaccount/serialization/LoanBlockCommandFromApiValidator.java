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

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.client.data.ClientApiCollectionConstants;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class LoanBlockCommandFromApiValidator {

    public static final String BLOCKING_COMMENT = "undoBlockingComment";
    public static final String BLOCKING_DATE = "undoBlockedOnDate";
    public static final String LOAN_BLOCK_IDS = "loanBlockIds";

    private static final String[] SUPPORTED_PARAMETERS = new String[] { "dateFormat", "loanBlockIds", "locale", BLOCKING_DATE,
            BLOCKING_COMMENT };
    public static final String BLOCKING_REASON_ID = "blockingReasonId";
    public static final String BLOCK_COMMENT = "blockComment";
    public static final String BLOCK_DATE = "blockDate";
    public static final String DATE_FORMAT = "dateFormat";
    private final FromJsonHelper fromApiJsonHelper;

    public void validateForDelete(String json) {

        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();

        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, Arrays.stream(SUPPORTED_PARAMETERS).toList());

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final String[] loanBlockIds = this.fromApiJsonHelper.extractArrayNamed(LOAN_BLOCK_IDS, element);
        baseDataValidator.reset().parameter(LOAN_BLOCK_IDS).value(loanBlockIds).notNull().arrayNotEmpty();

        final String comment = this.fromApiJsonHelper.extractStringNamed(BLOCKING_COMMENT, element);
        baseDataValidator.reset().parameter(BLOCKING_COMMENT).value(comment).notNull().notExceedingLengthOf(500);

        final String blockedOnDate = this.fromApiJsonHelper.extractStringNamed(BLOCKING_DATE, element);
        baseDataValidator.reset().parameter(BLOCKING_DATE).value(blockedOnDate).notNull();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateForBlockGuarantee(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json,
                Arrays.stream(new String[] { BLOCKING_REASON_ID, BLOCK_COMMENT, BLOCK_DATE, DATE_FORMAT, "locale" }).toList());

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Long blockId = this.fromApiJsonHelper.extractLongNamed(BLOCKING_REASON_ID, element);
        baseDataValidator.reset().parameter(BLOCKING_REASON_ID).value(blockId).notNull().integerGreaterThanZero();

        final String comment = this.fromApiJsonHelper.extractStringNamed(BLOCK_COMMENT, element);
        baseDataValidator.reset().parameter(BLOCK_COMMENT).value(comment).notNull().notExceedingLengthOf(500);

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateUnblockLoanMassively(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, Arrays.stream(new String[] { "unblockDate",
                "blockingReasonLevel", "blockingReasonId", "unblockComment", "loanId", "dateFormat", "locale" }).toList());

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ClientApiCollectionConstants.CLIENT_RESOURCE_NAME);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final LocalDate unblockDate = this.fromApiJsonHelper.extractLocalDateNamed("unblockDate", element);
        baseDataValidator.reset().parameter("unblockDate").value(unblockDate).notBlank()
                .validateDateBeforeOrEqual(DateUtils.getBusinessLocalDate());

        final Long blockingReasonId = this.fromApiJsonHelper.extractLongNamed("blockingReasonId", element);
        baseDataValidator.reset().parameter("blockingReasonId").value(blockingReasonId).notBlank();

        final String unblockComment = this.fromApiJsonHelper.extractStringNamed("unblockComment", element);
        baseDataValidator.reset().parameter("unblockComment").value(unblockComment).notBlank();

        final String[] loanId = this.fromApiJsonHelper.extractArrayNamed("loanId", element);
        baseDataValidator.reset().parameter("loanId").value(loanId).notBlank().arrayNotEmpty();

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
