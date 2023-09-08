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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RescheduleCreditsDataValidator {

    private final FromJsonHelper fromJsonHelper;
    private static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList("clientId","selectedLoanIds","disbursementDate","outstandingBalance",
                    "productId","comments","locale","dateFormat"));

    private static final Set<String> REJECT_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(RescheduleLoansApiConstants.localeParamName, RescheduleLoansApiConstants.dateFormatParamName,
                    RescheduleLoansApiConstants.rejectedOnDateParam));

    private static final Set<String> APPROVE_REQUEST_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(RescheduleLoansApiConstants.localeParamName, RescheduleLoansApiConstants.dateFormatParamName,
                    RescheduleLoansApiConstants.approvedOnDateParam));

    @Autowired
    public RescheduleCreditsDataValidator(FromJsonHelper fromJsonHelper) {
        this.fromJsonHelper = fromJsonHelper;
    }

    /**
     * Validates the request to create a new loan reschedule entry
     *
     * @param jsonCommand
     *            the JSON command object (instance of the JsonCommand class)
     *
     **/
    public void validateForCreateAction(final JsonCommand jsonCommand) {

        final String jsonString = jsonCommand.json();

        if (StringUtils.isBlank(jsonString)) {
            throw new InvalidJsonException();
        }

        final Type typeToken = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromJsonHelper.checkForUnsupportedParameters(typeToken, jsonString, CREATE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder dataValidatorBuilder = new DataValidatorBuilder(dataValidationErrors)
                .resource(StringUtils.lowerCase(RescheduleLoansApiConstants.RESTRUCTURE_CREDITS_RESOURCE));

        final JsonElement jsonElement = jsonCommand.parsedJson();

        final LocalDate disbursementDate = this.fromJsonHelper
                .extractLocalDateNamed("disbursementDate", jsonElement);
        dataValidatorBuilder.reset().parameter("disbursementDate").value(disbursementDate).notNull();

        String[] selectedLoanIds = this.fromJsonHelper
                .extractArrayNamed("selectedLoanIds", jsonElement);
        dataValidatorBuilder.reset().parameter("selectedLoanIds").value(selectedLoanIds).arrayNotEmpty();

        final Long productId = this.fromJsonHelper.extractLongNamed("productId", jsonElement);
        dataValidatorBuilder.reset().parameter("productId").value(productId).notNull();

        final String comments = this.fromJsonHelper
                .extractStringNamed("comments", jsonElement);
        dataValidatorBuilder.reset().parameter("comments").value(comments)
                .ignoreIfNull().notExceedingLengthOf(500);

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    private void validateForOverdueCharges(DataValidatorBuilder dataValidatorBuilder, final Loan loan,
            final LoanRepaymentScheduleInstallment installment) {
        if (installment != null) {
            LocalDate rescheduleFromDate = installment.getFromDate();
            Collection<LoanCharge> charges = loan.getLoanCharges();
            for (LoanCharge loanCharge : charges) {
                if (loanCharge.isOverdueInstallmentCharge() && loanCharge.getDueLocalDate().isAfter(rescheduleFromDate)) {
                    dataValidatorBuilder.failWithCodeNoParameterAddedToErrorCode("not.allowed.due.to.overdue.charges");
                    break;
                }
            }
        }
    }

    /**
     * Validates a user request to approve a loan reschedule request
     *
     * @param jsonCommand
     *            the JSON command object (instance of the JsonCommand class)
     *
     **/
    public void validateForApproveAction(final JsonCommand jsonCommand, LoanRescheduleRequest loanRescheduleRequest) {
        final String jsonString = jsonCommand.json();

        if (StringUtils.isBlank(jsonString)) {
            throw new InvalidJsonException();
        }

        final Type typeToken = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromJsonHelper.checkForUnsupportedParameters(typeToken, jsonString, APPROVE_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder dataValidatorBuilder = new DataValidatorBuilder(dataValidationErrors)
                .resource(StringUtils.lowerCase(RescheduleLoansApiConstants.ENTITY_NAME));

        final JsonElement jsonElement = jsonCommand.parsedJson();

        final LocalDate approvedOnDate = this.fromJsonHelper.extractLocalDateNamed(RescheduleLoansApiConstants.approvedOnDateParam,
                jsonElement);
        dataValidatorBuilder.reset().parameter(RescheduleLoansApiConstants.approvedOnDateParam).value(approvedOnDate).notNull();

        if (approvedOnDate != null && loanRescheduleRequest.getSubmittedOnDate().isAfter(approvedOnDate)) {
            dataValidatorBuilder.reset().parameter(RescheduleLoansApiConstants.approvedOnDateParam).failWithCode("before.submission.date",
                    "Approval date cannot be before the request submission date.");
        }

        LoanRescheduleRequestStatusEnumData loanRescheduleRequestStatusEnumData = LoanRescheduleRequestEnumerations
                .status(loanRescheduleRequest.getStatusEnum());

        if (!loanRescheduleRequestStatusEnumData.isPendingApproval()) {
            dataValidatorBuilder.reset().failWithCodeNoParameterAddedToErrorCode("request.is.not.in.submitted.and.pending.state",
                    "Loan reschedule request approval is not allowed. "
                            + "Loan reschedule request is not in submitted and pending approval state.");
        }

        LocalDate rescheduleFromDate = loanRescheduleRequest.getRescheduleFromDate();
        final Loan loan = loanRescheduleRequest.getLoan();
        LoanRepaymentScheduleInstallment installment = null;
        if (loan != null) {

            if (!loan.status().isActive()) {
                dataValidatorBuilder.reset().failWithCodeNoParameterAddedToErrorCode("loan.is.not.active", "Loan is not active");
            }

            if (rescheduleFromDate != null) {
                installment = loan.getRepaymentScheduleInstallment(rescheduleFromDate);

                if (installment == null) {
                    dataValidatorBuilder.reset().failWithCodeNoParameterAddedToErrorCode(
                            "loan.repayment.schedule.installment.does.not.exist", "Repayment schedule installment does not exist");
                }

                if (installment != null && installment.isObligationsMet()) {
                    dataValidatorBuilder.reset().failWithCodeNoParameterAddedToErrorCode(
                            "loan.repayment.schedule.installment." + "obligation.met", "Repayment schedule installment obligation met");
                }
            }
        }

        validateForOverdueCharges(dataValidatorBuilder, loan, installment);

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    /**
     * Validates a user request to reject a loan reschedule request
     *
     * @param jsonCommand
     *            the JSON command object (instance of the JsonCommand class)
     *
     **/
    public void validateForRejectAction(final JsonCommand jsonCommand, LoanRescheduleRequest loanRescheduleRequest) {
        final String jsonString = jsonCommand.json();

        if (StringUtils.isBlank(jsonString)) {
            throw new InvalidJsonException();
        }

        final Type typeToken = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromJsonHelper.checkForUnsupportedParameters(typeToken, jsonString, REJECT_REQUEST_DATA_PARAMETERS);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder dataValidatorBuilder = new DataValidatorBuilder(dataValidationErrors)
                .resource(StringUtils.lowerCase(RescheduleLoansApiConstants.ENTITY_NAME));

        final JsonElement jsonElement = jsonCommand.parsedJson();

        final LocalDate rejectedOnDate = this.fromJsonHelper.extractLocalDateNamed(RescheduleLoansApiConstants.rejectedOnDateParam,
                jsonElement);
        dataValidatorBuilder.reset().parameter(RescheduleLoansApiConstants.rejectedOnDateParam).value(rejectedOnDate).notNull();

        if (rejectedOnDate != null && loanRescheduleRequest.getSubmittedOnDate().isAfter(rejectedOnDate)) {
            dataValidatorBuilder.reset().parameter(RescheduleLoansApiConstants.rejectedOnDateParam).failWithCode("before.submission.date",
                    "Rejection date cannot be before the request submission date.");
        }

        LoanRescheduleRequestStatusEnumData loanRescheduleRequestStatusEnumData = LoanRescheduleRequestEnumerations
                .status(loanRescheduleRequest.getStatusEnum());

        if (!loanRescheduleRequestStatusEnumData.isPendingApproval()) {
            dataValidatorBuilder.reset().failWithCodeNoParameterAddedToErrorCode("request.is.not.in.submitted.and.pending.state",
                    "Loan reschedule request rejection is not allowed. "
                            + "Loan reschedule request is not in submitted and pending approval state.");
        }

        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }
}
