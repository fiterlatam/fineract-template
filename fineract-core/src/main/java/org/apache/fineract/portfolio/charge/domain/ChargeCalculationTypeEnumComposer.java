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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChargeCalculationTypeEnumComposer {
    public static void main(String[] args) {
        List<List<String>> combinations = generateCombinations(
                Arrays.asList("1AMOUNT", "2INTEREST", "3OUTSTANDING_AMOUNT", "4INSURANCE", "5AVAL", "6HONORARIOS"));

        System.out.println("Combinações possíveis:");
        for (List<String> combination : combinations) {

            String idComposer = "";
            if(combination.size() > 0 && combination.get(0).length() > 0) {
                idComposer = combination.get(0).substring(0, 1);
            }

            String lineConcatenated = "";
            String lineConcatenatedUpperCase = "";
            System.out.print("PERCENT_OF");
            for (String line : combination) {
                line = line.substring(1);
                System.out.print("_" + line);
                lineConcatenated = lineConcatenated.concat(line.toLowerCase()).concat(".");
                lineConcatenatedUpperCase = lineConcatenatedUpperCase.concat(line).concat(".");
            }

            // "AMOUNT", "INTEREST", "OUTSTANDING_AMOUNT", "INSURANCE", "AVAL", "HONORARIOS"
            if (lineConcatenatedUpperCase.contains("AMOUNT") && !lineConcatenatedUpperCase.contains("OUTSTANDING_AMOUNT")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenatedUpperCase.contains("INTEREST")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenatedUpperCase.contains("OUTSTANDING_AMOUNT")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenatedUpperCase.contains("INSURANCE")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenatedUpperCase.contains("AVAL")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenatedUpperCase.contains("HONORARIOS")) {
                idComposer = idComposer.concat("1");
            } else {
                idComposer = idComposer.concat("0");
            }

            if (lineConcatenated.length() > 0) {
                System.out.print("(" + idComposer + ", \"chargeCalculationType.percent.of." + lineConcatenated.substring(0, lineConcatenated.length() - 1) + "\"");
                System.out.println("), //");
            }
        }
    }

    public static List<List<String>> generateCombinations(List<String> variables) {
        List<List<String>> combinations = new ArrayList<>();
        generateCombinationsHelper(variables, 0, new ArrayList<>(), combinations);
        return combinations;
    }

    private static void generateCombinationsHelper(List<String> variables, int index, List<String> currentCombination, List<List<String>> combinations) {
        if (index == variables.size()) {
            combinations.add(new ArrayList<>(currentCombination));
            return;
        }

        // Exclude the current variable
        generateCombinationsHelper(variables, index + 1, currentCombination, combinations);

        // Include the current variable
        currentCombination.add(variables.get(index));
        generateCombinationsHelper(variables, index + 1, currentCombination, combinations);
        currentCombination.remove(currentCombination.size() - 1);
    }
}
