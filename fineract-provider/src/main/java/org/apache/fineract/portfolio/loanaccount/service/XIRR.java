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
package org.apache.fineract.portfolio.loanaccount.service;

import java.util.List;
import java.util.function.Function;

/*
    To calculate the XIRR (Extended Internal Rate of Return) in Java, it is needed a suitable implementation for this financial calculation.
    The XIRR algorithm is an optimization problem, and it can be solved using numerical methods, such as the Newton-Raphson method.
    This class implements the Newton-Raphson method in order to calculate the XIRR.
 */
public class XIRR {

    private static final double PRECISION = 0.001;
    private static final int MAX_ITERATIONS = 100;

    public static double calculateXIRR(List<CashFlowData> cashFlows) {
        Function<Double, Double> npvFunction = rate -> {
            double npv = 0;
            for (CashFlowData cashFlow : cashFlows) {
                npv += cashFlow.getAmount() / Math.pow(1 + rate, cashFlow.getDaysSinceFirstCashFlow() / 365.0);
            }
            return npv;
        };

        double guess = 0.1; // Initial guess for the XIRR
        int iterations = 0;

        while (iterations < MAX_ITERATIONS) {
            double npv = npvFunction.apply(guess);
            double derivative = (npvFunction.apply(guess + PRECISION) - npv) / PRECISION;
            double newGuess = guess - npv / derivative;

            if (Math.abs(newGuess - guess) <= PRECISION) {
                return newGuess;
            }

            guess = newGuess;
            iterations++;
        }

        throw new RuntimeException("XIRR calculation did not converge after " + MAX_ITERATIONS + " iterations.");
    }

}
