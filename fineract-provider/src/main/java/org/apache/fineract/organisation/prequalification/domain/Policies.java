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

import java.util.Arrays;
import java.util.Objects;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Policies {

    ZERO(0, "Invalid"), ONE(1, "New client categorization"), TWO(2, "Recurring customer categorization"), THREE(3,
            "Increase percentage"), FOUR(4, "Mandatory to attach photographs and investment plan"), FIVE(5, "Client age"), SIX(6,
                    "Number of members according to policy"), SEVEN(7, "Minimum and maximum amount"), EIGHT(8, "Value disparity"), NINE(9,
                            "Percentage of members starting business"), TEN(10, "Percentage of members with their own home"), ELEVEN(11,
                                    "President of the Board of Directors of the BC"), TWELVE(12, "General condition"), THIRTEEN(13,
                                            "Categories of clients to accept"), FOURTEEN(14,
                                                    "Amount requested in relation to the current amount of main products"), FIFTEEN(15,
                                                            "Add endorsement"), SIXTEEN(16,
                                                                    "Payments outside the current term of the main product"), SEVENTEEN(17,
                                                                            "Percentage of members of the same group who they can have parallel product"), EIGHTEEN(
                                                                                    18, "Gender"), NINETEEN(19, "Nationality"), TWENTY(20,
                                                                                            "Internal Credit History"), TWENTY_ONE(21,
                                                                                                    "External Credit History"), TWENTY_TWO(
                                                                                                            22,
                                                                                                            "Do you register any lawsuit?"), TWENTY_THREE(
                                                                                                                    23,
                                                                                                                    "Housing Type"), TWENTY_FOUR(
                                                                                                                            24,
                                                                                                                            "Rental Age"), TWENTY_FIVE(
                                                                                                                                    25,
                                                                                                                                    "Age Of Business"), TWENTY_SIX(
                                                                                                                                            26,
                                                                                                                                            "Credits"), TWENTY_SEVEN(
                                                                                                                                                    27,
                                                                                                                                                    "Cancelled Cycles Count"), TWENTY_EIGHT(
                                                                                                                                                            28,
                                                                                                                                                            "Acceptance of new clients"), TWENTY_NINE(
                                                                                                                                                                    29,
                                                                                                                                                                    "Present agricultural technical diagnosis (Commcare)"), THIRTY(
                                                                                                                                                                            30,
                                                                                                                                                                            "Age"), THIRTY_ONE(
                                                                                                                                                                                    31,
                                                                                                                                                                                    "Amount"), THIRTY_TWO(
                                                                                                                                                                                            32,
                                                                                                                                                                                            "Percentage of members with agricultural business"), THIRTY_THREE(
                                                                                                                                                                                                    33,
                                                                                                                                                                                                    "Percentage of members with their own business");

    private final Integer id;
    private final String name;

    public static Policies fromInt(final Integer id) {
        return Arrays.stream(values()).filter(policy -> Objects.equals(policy.id, id)).findFirst().orElse(ZERO);
    }
}
