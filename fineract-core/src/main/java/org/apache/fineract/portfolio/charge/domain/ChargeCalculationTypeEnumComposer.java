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

import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.portfolio.charge.enumerator.ChargeCalculationTypeBaseItemsEnum;

public class ChargeCalculationTypeEnumComposer {

    private static Map<String, String> combinationsForDropDown = new HashMap<>();

    private static Map<String, String> combinationsForAllowed = new HashMap<>();

    public static Map<String, String> generateCombinations() {
        Map<String, String> combinations = new HashMap<>();

        Map<String, String> combinationsForDropDown = new HashMap<>();

        ChargeCalculationTypeBaseItemsEnum[] values = ChargeCalculationTypeBaseItemsEnum.values();
        int totalCombinations = (int) Math.pow(2, values.length);

        for (int i = 1; i < totalCombinations; i++) {
            StringBuilder binary = new StringBuilder(Integer.toBinaryString(i));
            while (binary.length() < values.length) {
                binary.insert(0, "0");
            }

            StringBuilder combinationAcronym = new StringBuilder();
            StringBuilder combinationText = new StringBuilder();

            StringBuilder locanCalculationTypes = new StringBuilder();

            for (int j = 0; j < binary.length(); j++) {
                if (binary.charAt(j) == '1') {
                    if (combinationText.length() > 0) {
                        combinationText.append(".");

                        combinationAcronym.append("_");
                    }
                    combinationText.append(values[j].getCode());
                    combinationAcronym.append(values[j].getAcronym());
                }
            }

            System.out.println("chargeCalculationType(ChargeCalculationType." + combinationAcronym.toString() + "), //");

            combinationAcronym.append("(@@code@@, \"@@ID@@\", ");
            combinationAcronym.append("\"");
            combinationAcronym.append(combinationText);
            combinationAcronym.append("\"), //");

            combinations.put(binary.toString(), combinationAcronym.toString());
        }

        return combinations;
    }

    // Método principal para testar
    public static void main(String[] args) {

        System.out.println("####################################################################################################");
        System.out.println("Combination DropDown");
        System.out.println("####################################################################################################");

        Map<String, String> combinations = generateCombinations();
        Long codeGenerator = 10L;

        System.out.println("####################################################################################################");
        System.out.println("Charge Claculation Type Enum");
        System.out.println("####################################################################################################");
        // Imprime todas as combinações possíveis
        for (Map.Entry<String, String> entry : combinations.entrySet()) {
            codeGenerator++;
            System.out.println(entry.getValue().replaceAll("@@code@@", String.valueOf(codeGenerator)).replaceAll("@@ID@@", entry.getKey()));
        }

        // Imprime todas as combinações possíveis
        for (Map.Entry<String, String> entry : combinationsForDropDown.entrySet()) {
            codeGenerator++;
            System.out.println("");
        }
    }
}
