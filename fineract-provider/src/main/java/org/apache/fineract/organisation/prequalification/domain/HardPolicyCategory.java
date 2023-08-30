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
package org.apache.fineract.organisation.prequalification.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum HardPolicyCategory {

    INVALID(0, "Invalid"), NEW_CLIENT(1, "New Client"), RECURRING_CUSTOMER(2, "Recurring Customer"), INCREASE_PERCENTAGE(3,
            "Increase Percentage"), MANDATORY_PHOTO_GRAPH(4, "Mandatory Photographs"), CLIENT_AGE(5,
                    "Client Age"), NUMBER_OF_MEMBERS_ACCORDING_TO_POLICY(6,
                            "No. Of Members according To Policy"), MINIMUM_AND_MAXIMUM_AMOUNT(7,
                                    "Minimum and Maximum Amount"), DISPARITY_OF_VALUES(8,
                                            "Disparity of Values"), PERCENTAGE_OF_MEMBERS_STARTING_BUSINESS(9,
                                                    "Percent Of Members Starting Business"), PERCENTAGE_OF_MEMBERS_WITH_THEIR_OWN_HOME(10,
                                                            "    Percent Of Members with their own home"), CHAIRMAN_OF_THE_BC_BOARD_OF_DIRECTORS(
                                                                    11, "Chairman of The BC Board Of Directors"), OVERALL_CONDITION(12,
                                                                            "Overall Condition"), CATEGORIES_OF_CLIENT_TO_ACCEPT(13,
                                                                                    "Categories of clients to accept"), REQUESTED_AMOUNT(14,
                                                                                            "Requested Amount"), ADD_ENDORSEMENT(15,
                                                                                                    "add_endorsemnet"), PAYMENTS_OUTSIDE_CURRENT_TERM(
                                                                                                            16,
                                                                                                            "Payments Outside Current Term"), PERCENTAGE_OF_MEMBERS_THAT_CAN_HAVE_PRODUCT(
                                                                                                                    17,
                                                                                                                    "Percentage Of members that can have Product"), GENDER(
                                                                                                                            18,
                                                                                                                            "Gender"), NATIONALITY(
                                                                                                                                    19,
                                                                                                                                    "Nationality"), INTERNAL_CREDIT_HISTORY(
                                                                                                                                            20,
                                                                                                                                            "Internal Credit History"), EXTERNAL_CREDIT_HISTORY(
                                                                                                                                                    21,
                                                                                                                                                    "External Credit History"), CLAIMS_REGISTERED(
                                                                                                                                                            22,
                                                                                                                                                            "Claim Registered"), HOUSING_TYPE(
                                                                                                                                                                    23,
                                                                                                                                                                    "Housing Type"), RENTAL_AGE(
                                                                                                                                                                            24,
                                                                                                                                                                            "Rental Age"), AGE_OF_BUSINESS(
                                                                                                                                                                                    25,
                                                                                                                                                                                    "Age Of Business"), CREDITS(
                                                                                                                                                                                            26,
                                                                                                                                                                                            "Credits"), CANCELLED_CYCLES_COUNT(
                                                                                                                                                                                                    27,
                                                                                                                                                                                                    "Cancelled Cycles Count"), SUBMIT_AGRICULTURAL_TECHNICAL_DIAGNOSIS(
                                                                                                                                                                                                            28,
                                                                                                                                                                                                            "Submit agricultural technical diagnosis (Commcare)"), ACCEPTANCE_OF_NEW_CLIENTS(
                                                                                                                                                                                                                    29,
                                                                                                                                                                                                                    "Acceptance of new clients");

    private final Integer id;
    private final String name;

    public static HardPolicyCategory fromInt(final Integer id) {
        return switch (id) {
            case 1 -> HardPolicyCategory.NEW_CLIENT;
            case 2 -> HardPolicyCategory.RECURRING_CUSTOMER;
            case 3 -> HardPolicyCategory.INCREASE_PERCENTAGE;
            case 4 -> HardPolicyCategory.MANDATORY_PHOTO_GRAPH;
            case 5 -> HardPolicyCategory.CLIENT_AGE;
            case 6 -> HardPolicyCategory.NUMBER_OF_MEMBERS_ACCORDING_TO_POLICY;
            case 7 -> HardPolicyCategory.MINIMUM_AND_MAXIMUM_AMOUNT;
            case 8 -> HardPolicyCategory.DISPARITY_OF_VALUES;
            case 9 -> HardPolicyCategory.PERCENTAGE_OF_MEMBERS_STARTING_BUSINESS;
            case 10 -> HardPolicyCategory.PERCENTAGE_OF_MEMBERS_WITH_THEIR_OWN_HOME;
            case 11 -> HardPolicyCategory.CHAIRMAN_OF_THE_BC_BOARD_OF_DIRECTORS;
            case 12 -> HardPolicyCategory.OVERALL_CONDITION;
            case 13 -> HardPolicyCategory.CATEGORIES_OF_CLIENT_TO_ACCEPT;
            case 14 -> HardPolicyCategory.REQUESTED_AMOUNT;
            case 15 -> HardPolicyCategory.ADD_ENDORSEMENT;
            case 16 -> HardPolicyCategory.PAYMENTS_OUTSIDE_CURRENT_TERM;
            case 17 -> HardPolicyCategory.PERCENTAGE_OF_MEMBERS_THAT_CAN_HAVE_PRODUCT;
            case 18 -> HardPolicyCategory.GENDER;
            case 19 -> HardPolicyCategory.NATIONALITY;
            case 20 -> HardPolicyCategory.INTERNAL_CREDIT_HISTORY;
            case 21 -> HardPolicyCategory.EXTERNAL_CREDIT_HISTORY;
            case 22 -> HardPolicyCategory.CLAIMS_REGISTERED;
            case 23 -> HardPolicyCategory.HOUSING_TYPE;
            case 24 -> HardPolicyCategory.RENTAL_AGE;
            case 25 -> HardPolicyCategory.AGE_OF_BUSINESS;
            case 26 -> HardPolicyCategory.CREDITS;
            case 27 -> HardPolicyCategory.CANCELLED_CYCLES_COUNT;
            case 28 -> HardPolicyCategory.SUBMIT_AGRICULTURAL_TECHNICAL_DIAGNOSIS;
            case 29 -> HardPolicyCategory.ACCEPTANCE_OF_NEW_CLIENTS;
            default -> HardPolicyCategory.INVALID;
        };
    }
}
