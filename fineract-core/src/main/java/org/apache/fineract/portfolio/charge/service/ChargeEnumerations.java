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
package org.apache.fineract.portfolio.charge.service;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.charge.domain.*;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;

public final class ChargeEnumerations {

    private ChargeEnumerations() {

    }

    public static EnumOptionData chargeTimeType(final int id) {
        return chargeTimeType(ChargeTimeType.fromInt(id));
    }

    public static EnumOptionData chargeTimeType(final ChargeTimeType type) {
        EnumOptionData optionData = null;
        switch (type) {
            case DISBURSEMENT:
                optionData = new EnumOptionData(ChargeTimeType.DISBURSEMENT.getValue().longValue(), ChargeTimeType.DISBURSEMENT.getCode(),
                        "Disbursement");
            break;
            case SPECIFIED_DUE_DATE:
                optionData = new EnumOptionData(ChargeTimeType.SPECIFIED_DUE_DATE.getValue().longValue(),
                        ChargeTimeType.SPECIFIED_DUE_DATE.getCode(), "Specified due date");
            break;
            case SAVINGS_ACTIVATION:
                optionData = new EnumOptionData(ChargeTimeType.SAVINGS_ACTIVATION.getValue().longValue(),
                        ChargeTimeType.SAVINGS_ACTIVATION.getCode(), "Savings Activation");
            break;
            case SAVINGS_CLOSURE:
                optionData = new EnumOptionData(ChargeTimeType.SAVINGS_CLOSURE.getValue().longValue(),
                        ChargeTimeType.SAVINGS_CLOSURE.getCode(), "Savings Closure");
            break;
            case WITHDRAWAL_FEE:
                optionData = new EnumOptionData(ChargeTimeType.WITHDRAWAL_FEE.getValue().longValue(),
                        ChargeTimeType.WITHDRAWAL_FEE.getCode(), "Withdrawal Fee");
            break;
            case ANNUAL_FEE:
                optionData = new EnumOptionData(ChargeTimeType.ANNUAL_FEE.getValue().longValue(), ChargeTimeType.ANNUAL_FEE.getCode(),
                        "Annual Fee");
            break;
            case MONTHLY_FEE:
                optionData = new EnumOptionData(ChargeTimeType.MONTHLY_FEE.getValue().longValue(), ChargeTimeType.MONTHLY_FEE.getCode(),
                        "Monthly Fee");
            break;
            case WEEKLY_FEE:
                optionData = new EnumOptionData(ChargeTimeType.WEEKLY_FEE.getValue().longValue(), ChargeTimeType.WEEKLY_FEE.getCode(),
                        "Weekly Fee");
            break;
            case INSTALMENT_FEE:
                optionData = new EnumOptionData(ChargeTimeType.INSTALMENT_FEE.getValue().longValue(),
                        ChargeTimeType.INSTALMENT_FEE.getCode(), "Installment Fee");
            break;
            case OVERDUE_INSTALLMENT:
                optionData = new EnumOptionData(ChargeTimeType.OVERDUE_INSTALLMENT.getValue().longValue(),
                        ChargeTimeType.OVERDUE_INSTALLMENT.getCode(), "Overdue Fees");
            break;
            case OVERDRAFT_FEE:
                optionData = new EnumOptionData(ChargeTimeType.OVERDRAFT_FEE.getValue().longValue(), ChargeTimeType.OVERDRAFT_FEE.getCode(),
                        "Overdraft Fee");
            break;
            case TRANCHE_DISBURSEMENT:
                optionData = new EnumOptionData(ChargeTimeType.TRANCHE_DISBURSEMENT.getValue().longValue(),
                        ChargeTimeType.TRANCHE_DISBURSEMENT.getCode(), "Tranche Disbursement");
            break;
            case SHAREACCOUNT_ACTIVATION:
                optionData = new EnumOptionData(ChargeTimeType.SHAREACCOUNT_ACTIVATION.getValue().longValue(),
                        ChargeTimeType.SHAREACCOUNT_ACTIVATION.getCode(), "Share Account Activate");
            break;

            case SHARE_PURCHASE:
                optionData = new EnumOptionData(ChargeTimeType.SHARE_PURCHASE.getValue().longValue(),
                        ChargeTimeType.SHARE_PURCHASE.getCode(), "Share Purchase");
            break;
            case SHARE_REDEEM:
                optionData = new EnumOptionData(ChargeTimeType.SHARE_REDEEM.getValue().longValue(), ChargeTimeType.SHARE_REDEEM.getCode(),
                        "Share Redeem");
            break;
            case SAVINGS_NOACTIVITY_FEE:
                optionData = new EnumOptionData(ChargeTimeType.SAVINGS_NOACTIVITY_FEE.getValue().longValue(),
                        ChargeTimeType.SAVINGS_NOACTIVITY_FEE.getCode(), "Saving No Activity Fee");
            break;
            default:
                optionData = new EnumOptionData(ChargeTimeType.INVALID.getValue().longValue(), ChargeTimeType.INVALID.getCode(), "Invalid");
            break;
        }
        return optionData;
    }

    public static EnumOptionData chargeAppliesTo(final int id) {
        return chargeAppliesTo(ChargeAppliesTo.fromInt(id));
    }

    public static EnumOptionData chargeAppliesTo(final ChargeAppliesTo type) {
        EnumOptionData optionData = null;
        switch (type) {
            case LOAN:
                optionData = new EnumOptionData(ChargeAppliesTo.LOAN.getValue().longValue(), ChargeAppliesTo.LOAN.getCode(), "Loan");
            break;
            case SAVINGS:
                optionData = new EnumOptionData(ChargeAppliesTo.SAVINGS.getValue().longValue(), ChargeAppliesTo.SAVINGS.getCode(),
                        "Savings");
            break;
            case CLIENT:
                optionData = new EnumOptionData(ChargeAppliesTo.CLIENT.getValue().longValue(), ChargeAppliesTo.CLIENT.getCode(), "Client");
            break;
            case SHARES:
                optionData = new EnumOptionData(ChargeAppliesTo.SHARES.getValue().longValue(), ChargeAppliesTo.SHARES.getCode(), "Shares");
            break;
            default:
                optionData = new EnumOptionData(ChargeAppliesTo.INVALID.getValue().longValue(), ChargeAppliesTo.INVALID.getCode(),
                        "Invalid");
            break;
        }
        return optionData;
    }

    public static EnumOptionData chargeCalculationType(final int id) {
        return chargeCalculationType(ChargeCalculationType.fromInt(id));
    }

    public static EnumOptionData loanChargeCalculationType(final int id) {
        return loanChargeCalculationType(ChargeCalculationType.fromInt(id));
    }

    public static EnumOptionData chargeCalculationType(final ChargeCalculationType type) {
        EnumOptionData optionData = null;
        switch (type) {
            case FLAT_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.FLAT_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.FLAT_AMOUNT.getCode(), "Flat");
            break;
            case PERCENT_OF_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_AMOUNT.getCode(), "% Principal");
            break;
            case PERCENT_OF_AMOUNT_AND_INTEREST:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getCode(), "% Principal + Interest");
            break;
            case PERCENT_OF_INTEREST:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_INTEREST.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_INTEREST.getCode(), "% Interest");
            break;
            case PERCENT_OF_DISBURSEMENT_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getCode(), "% Disbursement Amount");
            break;
            case PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getCode(), "% Outstanding Principal Amount");
            break;
            case PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getCode(), "% Outstanding Interest Amount");
            break;
            case PERCENT_OF_ANOTHER_CHARGE:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getCode(), "% of Another Charge Amount");
            break;
            case AMOUNT_FROM_EXTERNAL_CALCULATION:
                optionData = new EnumOptionData(ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getValue().longValue(),
                        ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getCode(), "Amount from External System");
            break;

            default:
                /*
                 * FLAT("Flat", "FLAT", "flat"), DISBURSED_AMOUNT("Disbursed Amount", "DISB", "disbursedamount"),
                 * PRINCIPAL_INSTALLMENT("Installment Principal", "IPRIN", "installmentprincipal"),
                 * INTEREST_INSTALLMENT("Installment Interest", "IINT", "installmentinterest"),
                 * OUTSTANDING_PRINCIPAL("Outstanding principal", "OPRIN", "outstandingprincipal"),
                 * OUTSTANDING_INTEREST("Outstanding Interest", "OINT", "outstandinginterest"),
                 * SEGURO_OBRIGATORIO("Seguro Obrigatorio", "SEGO", "seguroobrigatorio"), AVAL("Aval", "AVAL", "aval"),
                 * HOORARIOS("Hoorarios","HONO","hoorarios"), PERCENT_OF_ANOTHER_CHARGE("% Of another charge", "ACHG",
                 * "percentofanothercharge");
                 */
                StringBuilder label = new StringBuilder();
                if (type.getCode().contains(".flat")) {
                    label.append("Flat + ");
                }

                if (type.getCode().contains(".disbursedamount")) {
                    label.append("Disbursed Amount + ");
                }

                if (type.getCode().contains(".installmentprincipal")) {
                    label.append("Installment´s Principal + ");
                }

                if (type.getCode().contains(".installmentinterest")) {
                    label.append("Installment´s Interest + ");
                }

                if (type.getCode().contains(".outstandingprincipal")) {
                    label.append("Outstanding Principal + ");
                }

                if (type.getCode().contains(".outstandinginterest")) {
                    label.append("Outstanding Interest + ");
                }

                if (type.getCode().contains(".seguroobrigatorio")) {
                    label.append("Seguro Obrigatorio + ");
                }

                if (type.getCode().contains(".aval")) {
                    label.append("Aval + ");
                }

                if (type.getCode().contains(".honorarios")) {
                    label.append("Fees + ");
                }

                if (type.getCode().contains(".percentofanothercharge")) {
                    label.append("Pct of another Charge + ");
                }

                String val = label.toString();
                if (val.endsWith(" + ")) {
                    label = new StringBuilder(val.substring(0, val.length() - 3));
                }

                optionData = new EnumOptionData(type.getValue().longValue(), type.getCode(), "% " + val);
            break;
        }
        return optionData;
    }

    public static EnumOptionData loanChargeCalculationType(final ChargeCalculationType type) {
        EnumOptionData optionData;
        switch (type) {
            case FLAT_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.FLAT_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.FLAT_AMOUNT.getCode(), "Flat");
            break;
            case PERCENT_OF_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_AMOUNT.getValue().longValue(),
                        "chargeCalculationType.percent.of.principal", "% Principal Amount");
            break;
            case PERCENT_OF_AMOUNT_AND_INTEREST:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getValue().longValue(),
                        "chargeCalculationType.percent.of.principal.and.interest", "% Loan Principal + Interest");
            break;
            case PERCENT_OF_INTEREST:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_INTEREST.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_INTEREST.getCode(), "% Interest");
            break;
            case PERCENT_OF_DISBURSEMENT_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getCode(), "% Disbursement Amount");
            break;
            case PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getCode(), "% Outstanding Principal Amount");
            break;
            case PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getCode(), "% Outstanding Interest Amount");
            break;
            case PERCENT_OF_ANOTHER_CHARGE:
                optionData = new EnumOptionData(ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getValue().longValue(),
                        ChargeCalculationType.PERCENT_OF_ANOTHER_CHARGE.getCode(), "% of Another Charge Amount");
            break;
            case AMOUNT_FROM_EXTERNAL_CALCULATION:
                optionData = new EnumOptionData(ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getValue().longValue(),
                        ChargeCalculationType.AMOUNT_FROM_EXTERNAL_CALCULATION.getCode(), "Amount from External System");
            break;
            case INVALID:
                optionData = new EnumOptionData(ChargeCalculationType.INVALID.getValue().longValue(),
                        ChargeCalculationType.INVALID.getCode(), "Invalid");
            break;

            default:
                String code = type.getCode();
                if (Boolean.FALSE.equals("flat.".equalsIgnoreCase(type.getCode()))) {
                    code = "percent.of." + type.getCode();
                }

                optionData = new EnumOptionData(type.getValue().longValue(), "chargeCalculationType." + code, type.getCode());
            break;
        }
        return optionData;
    }

    public static EnumOptionData chargePaymentMode(final int id) {
        return chargePaymentMode(ChargePaymentMode.fromInt(id));
    }

    public static EnumOptionData chargePaymentMode(final ChargePaymentMode type) {
        EnumOptionData optionData = null;
        switch (type) {
            case ACCOUNT_TRANSFER:
                optionData = new EnumOptionData(ChargePaymentMode.ACCOUNT_TRANSFER.getValue().longValue(),
                        ChargePaymentMode.ACCOUNT_TRANSFER.getCode(), "Account transfer");
            break;
            default:
                optionData = new EnumOptionData(ChargePaymentMode.REGULAR.getValue().longValue(), ChargePaymentMode.REGULAR.getCode(),
                        "Regular");
            break;
        }
        return optionData;
    }

    public static EnumOptionData feeFrequencyType(final int id) {
        return feeFrequencyType(PeriodFrequencyType.fromInt(id));
    }

    public static EnumOptionData feeFrequencyType(final PeriodFrequencyType frequencyType) {
        EnumOptionData optionData;
        switch (frequencyType) {
            case DAYS -> optionData = new EnumOptionData(PeriodFrequencyType.DAYS.getValue().longValue(),
                    PeriodFrequencyType.DAYS.getCode(), "Daily");
            case WEEKS -> optionData = new EnumOptionData(PeriodFrequencyType.WEEKS.getValue().longValue(),
                    PeriodFrequencyType.WEEKS.getCode(), "Weekly");
            case MONTHS -> optionData = new EnumOptionData(PeriodFrequencyType.MONTHS.getValue().longValue(),
                    PeriodFrequencyType.MONTHS.getCode(), "Monthly");
            case YEARS -> optionData = new EnumOptionData(PeriodFrequencyType.YEARS.getValue().longValue(),
                    PeriodFrequencyType.YEARS.getCode(), "Yearly");
            case WHOLE_TERM -> optionData = new EnumOptionData(PeriodFrequencyType.WHOLE_TERM.getValue().longValue(),
                    PeriodFrequencyType.WHOLE_TERM.getCode(), "Whole term");
            default -> throw new UnsupportedOperationException(frequencyType + " is not supported");
        }
        return optionData;
    }

    public static EnumOptionData chargeInsuranceType(final int id) {
        return chargeInsuranceType(ChargeInsuranceType.fromInt(id));
    }

    public static EnumOptionData chargeInsuranceType(final ChargeInsuranceType type) {
        EnumOptionData optionData = null;
        switch (type) {
            case CARGO:
                optionData = new EnumOptionData(ChargeInsuranceType.CARGO.getValue().longValue(), ChargeInsuranceType.CARGO.getCode(),
                        "Cargo");
            break;
            case COMPRA:
                optionData = new EnumOptionData(ChargeInsuranceType.COMPRA.getValue().longValue(), ChargeInsuranceType.COMPRA.getCode(),
                        "Compra");
            break;
            default:
                optionData = new EnumOptionData(ChargeTimeType.INVALID.getValue().longValue(), ChargeTimeType.INVALID.getCode(), "Invalid");
            break;
        }
        return optionData;
    }

}
