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
package org.apache.fineract.portfolio.charge.domain;

public enum ChargeCalculationType {

    INVALID(0, "chargeCalculationType.invalid"), //
    FLAT(1, "chargeCalculationType.flat"), //
    PERCENT_OF_AMOUNT(2, "chargeCalculationType.percent.of.amount"), //
    PERCENT_OF_AMOUNT_AND_INTEREST(3, "chargeCalculationType.percent.of.amount.and.interest"), //
    PERCENT_OF_INTEREST(4, "chargeCalculationType.percent.of.interest"), //
    PERCENT_OF_DISBURSEMENT_AMOUNT(5,"chargeCalculationType.percent.of.disbursement.amount"), //

    PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT(6, "chargeCalculationType.percent.of.outstanding.principal.amount"), //
    PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT(7, "chargeCalculationType.percent.of.outstanding.interest.amount"), //
    PERCENT_OF_OUTSTANDING_PRINCIPAL_AND_INTEREST_AMOUNT(8, "chargeCalculationType.percent.of.outstanding.principal.and.interest.amount"), //
    PERCENT_OF_PRINCIPAL_TERM(9, "chargeCalculationType.percent.of.principal.term"), //
    PERCENT_OF_GUARANTEE_TERM(10, "chargeCalculationType.percent.of.guarantee.term"), //

    PERCENT_OF_HONORARIOS(6000001, "chargeCalculationType.percent.of.honorarios"), //
    PERCENT_OF_AVAL(5000010, "chargeCalculationType.percent.of.aval"), //
    PERCENT_OF_AVAL_HONORARIOS(5000011, "chargeCalculationType.percent.of.aval.honorarios"), //
    PERCENT_OF_INSURANCE(4000100, "chargeCalculationType.percent.of.insurance"), //
    PERCENT_OF_INSURANCE_HONORARIOS(4000101, "chargeCalculationType.percent.of.insurance.honorarios"), //
    PERCENT_OF_INSURANCE_AVAL(4000110, "chargeCalculationType.percent.of.insurance.aval"), //
    PERCENT_OF_INSURANCE_AVAL_HONORARIOS(4000111, "chargeCalculationType.percent.of.insurance.aval.honorarios"), //
//    PERCENT_OF_OUTSTANDING_AMOUNT(3001000, "chargeCalculationType.percent.of.outstanding_amount"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_HONORARIOS(3001001, "chargeCalculationType.percent.of.outstanding_amount.honorarios"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_AVAL(3001010, "chargeCalculationType.percent.of.outstanding_amount.aval"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_AVAL_HONORARIOS(3001011, "chargeCalculationType.percent.of.outstanding_amount.aval.honorarios"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_INSURANCE(3001100, "chargeCalculationType.percent.of.outstanding_amount.insurance"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_INSURANCE_HONORARIOS(3001101, "chargeCalculationType.percent.of.outstanding_amount.insurance.honorarios"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_INSURANCE_AVAL(3001110, "chargeCalculationType.percent.of.outstanding_amount.insurance.aval"), //
    PERCENT_OF_OUTSTANDING_AMOUNT_INSURANCE_AVAL_HONORARIOS(3001111, "chargeCalculationType.percent.of.outstanding_amount.insurance.aval.honorarios"), //
//    PERCENT_OF_INTEREST(2010000, "chargeCalculationType.percent.of.interest"), //
    PERCENT_OF_INTEREST_HONORARIOS(2010001, "chargeCalculationType.percent.of.interest.honorarios"), //
    PERCENT_OF_INTEREST_AVAL(2010010, "chargeCalculationType.percent.of.interest.aval"), //
    PERCENT_OF_INTEREST_AVAL_HONORARIOS(2010011, "chargeCalculationType.percent.of.interest.aval.honorarios"), //
    PERCENT_OF_INTEREST_INSURANCE(2010100, "chargeCalculationType.percent.of.interest.insurance"), //
    PERCENT_OF_INTEREST_INSURANCE_HONORARIOS(2010101, "chargeCalculationType.percent.of.interest.insurance.honorarios"), //
    PERCENT_OF_INTEREST_INSURANCE_AVAL(2010110, "chargeCalculationType.percent.of.interest.insurance.aval"), //
    PERCENT_OF_INTEREST_INSURANCE_AVAL_HONORARIOS(2010111, "chargeCalculationType.percent.of.interest.insurance.aval.honorarios"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT(2011000, "chargeCalculationType.percent.of.interest.outstanding_amount"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_HONORARIOS(2011001, "chargeCalculationType.percent.of.interest.outstanding_amount.honorarios"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_AVAL(2011010, "chargeCalculationType.percent.of.interest.outstanding_amount.aval"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_AVAL_HONORARIOS(2011011, "chargeCalculationType.percent.of.interest.outstanding_amount.aval.honorarios"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_INSURANCE(2011100, "chargeCalculationType.percent.of.interest.outstanding_amount.insurance"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_HONORARIOS(2011101, "chargeCalculationType.percent.of.interest.outstanding_amount.insurance.honorarios"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_AVAL(2011110, "chargeCalculationType.percent.of.interest.outstanding_amount.insurance.aval"), //
    PERCENT_OF_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_AVAL_HONORARIOS(2011111, "chargeCalculationType.percent.of.interest.outstanding_amount.insurance.aval.honorarios"), //
//    PERCENT_OF_AMOUNT(1100000, "chargeCalculationType.percent.of.amount"), //
    PERCENT_OF_AMOUNT_HONORARIOS(1100001, "chargeCalculationType.percent.of.amount.honorarios"), //
    PERCENT_OF_AMOUNT_AVAL(1100010, "chargeCalculationType.percent.of.amount.aval"), //
    PERCENT_OF_AMOUNT_AVAL_HONORARIOS(1100011, "chargeCalculationType.percent.of.amount.aval.honorarios"), //
    PERCENT_OF_AMOUNT_INSURANCE(1100100, "chargeCalculationType.percent.of.amount.insurance"), //
    PERCENT_OF_AMOUNT_INSURANCE_HONORARIOS(1100101, "chargeCalculationType.percent.of.amount.insurance.honorarios"), //
    PERCENT_OF_AMOUNT_INSURANCE_AVAL(1100110, "chargeCalculationType.percent.of.amount.insurance.aval"), //
    PERCENT_OF_AMOUNT_INSURANCE_AVAL_HONORARIOS(1100111, "chargeCalculationType.percent.of.amount.insurance.aval.honorarios"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT(1001000, "chargeCalculationType.percent.of.amount.outstanding_amount"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_HONORARIOS(1001001, "chargeCalculationType.percent.of.amount.outstanding_amount.honorarios"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_AVAL(1001010, "chargeCalculationType.percent.of.amount.outstanding_amount.aval"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_AVAL_HONORARIOS(1001011, "chargeCalculationType.percent.of.amount.outstanding_amount.aval.honorarios"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_INSURANCE(1001100, "chargeCalculationType.percent.of.amount.outstanding_amount.insurance"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_INSURANCE_HONORARIOS(1001101, "chargeCalculationType.percent.of.amount.outstanding_amount.insurance.honorarios"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_INSURANCE_AVAL(1001110, "chargeCalculationType.percent.of.amount.outstanding_amount.insurance.aval"), //
    PERCENT_OF_AMOUNT_OUTSTANDING_AMOUNT_INSURANCE_AVAL_HONORARIOS(1001111, "chargeCalculationType.percent.of.amount.outstanding_amount.insurance.aval.honorarios"), //
//    PERCENT_OF_AMOUNT_INTEREST(1110000, "chargeCalculationType.percent.of.amount.interest"), //
    PERCENT_OF_AMOUNT_INTEREST_HONORARIOS(1110001, "chargeCalculationType.percent.of.amount.interest.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_AVAL(1110010, "chargeCalculationType.percent.of.amount.interest.aval"), //
    PERCENT_OF_AMOUNT_INTEREST_AVAL_HONORARIOS(1110011, "chargeCalculationType.percent.of.amount.interest.aval.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_INSURANCE(1110100, "chargeCalculationType.percent.of.amount.interest.insurance"), //
    PERCENT_OF_AMOUNT_INTEREST_INSURANCE_HONORARIOS(1110101, "chargeCalculationType.percent.of.amount.interest.insurance.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_INSURANCE_AVAL(1110110, "chargeCalculationType.percent.of.amount.interest.insurance.aval"), //
    PERCENT_OF_AMOUNT_INTEREST_INSURANCE_AVAL_HONORARIOS(1110111, "chargeCalculationType.percent.of.amount.interest.insurance.aval.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT(1011000, "chargeCalculationType.percent.of.amount.interest.outstanding_amount"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_HONORARIOS(1011001, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_AVAL(1011010, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.aval"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_AVAL_HONORARIOS(1011011, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.aval.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_INSURANCE(1011100, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.insurance"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_HONORARIOS(1011101, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.insurance.honorarios"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_AVAL(1011110, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.insurance.aval"), //
    PERCENT_OF_AMOUNT_INTEREST_OUTSTANDING_AMOUNT_INSURANCE_AVAL_HONORARIOS(1011111, "chargeCalculationType.percent.of.amount.interest.outstanding_amount.insurance.aval.honorarios"), //
    ;

    private final Integer value;
    private final String code;

    ChargeCalculationType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static Object[] validValuesForLoan() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getValue(), ChargeCalculationType.PERCENT_OF_INTEREST.getValue(),
                ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_OUTSTANDING_PRINCIPAL_AND_INTEREST_AMOUNT.getValue(),
                ChargeCalculationType.PERCENT_OF_PRINCIPAL_TERM.getValue(), ChargeCalculationType.PERCENT_OF_GUARANTEE_TERM.getValue() };
    }

    public static Object[] validValuesForSavings() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue() };
    }

    public static Object[] validValuesForShares() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue(), ChargeCalculationType.PERCENT_OF_AMOUNT.getValue() };
    }

    public static Object[] validValuesForClients() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue() };
    }

    public static Object[] validValuesForShareAccountActivation() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue() };
    }

    public static Object[] validValuesForTrancheDisbursement() {
        return new Integer[] { ChargeCalculationType.FLAT.getValue(), ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue() };
    }

    public static ChargeCalculationType fromInt(final Integer chargeCalculation) {
        ChargeCalculationType chargeCalculationType = ChargeCalculationType.INVALID;
        switch (chargeCalculation) {
            case 1:
                chargeCalculationType = FLAT;
            break;
            case 2:
                chargeCalculationType = PERCENT_OF_AMOUNT;
            break;
            case 3:
                chargeCalculationType = PERCENT_OF_AMOUNT_AND_INTEREST;
            break;
            case 4:
                chargeCalculationType = PERCENT_OF_INTEREST;
            break;
            case 5:
                chargeCalculationType = PERCENT_OF_DISBURSEMENT_AMOUNT;
            break;
            case 6:
                chargeCalculationType = PERCENT_OF_OUTSTANDING_PRINCIPAL_AMOUNT;
            break;
            case 7:
                chargeCalculationType = PERCENT_OF_OUTSTANDING_INTEREST_AMOUNT;
            break;
            case 8:
                chargeCalculationType = PERCENT_OF_OUTSTANDING_PRINCIPAL_AND_INTEREST_AMOUNT;
            break;
            case 9:
                chargeCalculationType = PERCENT_OF_PRINCIPAL_TERM;
            break;
            case 10:
                chargeCalculationType = PERCENT_OF_GUARANTEE_TERM;
            break;
        }
        return chargeCalculationType;
    }

    public boolean isPercentageOfAmount() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_AMOUNT.getValue());
    }

    public boolean isPercentageOfAmountAndInterest() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_AMOUNT_AND_INTEREST.getValue());
    }

    public boolean isPercentageOfInterest() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_INTEREST.getValue());
    }

    public boolean isFlat() {
        return this.value.equals(ChargeCalculationType.FLAT.getValue());
    }

    public boolean isAllowedSavingsChargeCalculationType() {
        return isFlat() || isPercentageOfAmount();
    }

    public boolean isAllowedClientChargeCalculationType() {
        return isFlat();
    }

    public boolean isPercentageBased() {
        return isPercentageOfAmount() || isPercentageOfAmountAndInterest() || isPercentageOfInterest()
                || isPercentageOfDisbursementAmount();
    }

    public boolean isPercentageOfDisbursementAmount() {
        return this.value.equals(ChargeCalculationType.PERCENT_OF_DISBURSEMENT_AMOUNT.getValue());
    }
}
