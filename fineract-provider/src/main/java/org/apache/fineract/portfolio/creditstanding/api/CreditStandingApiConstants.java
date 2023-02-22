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
package org.apache.fineract.portfolio.creditstanding.api;

public class CreditStandingApiConstants {

    public static final String CREDIT_STANDING_RESOURCE_NAME = "creditstanding";

    // Credit standing parameters
    public static String mraParamName = "mra";
    public static String mraAvailableParamName = "mraAvailable";
    public static String rciMaxParamName = "rciMax";
    public static String monthlyCommitmentParamName = "monthlyCommitment";
    public static String totalDebtParamName = "totalDebt";
    public static String currentDebtParamName = "currentDebt";
    public static String expiredDebtParamName = "expiredDebt";
    public static String delayInDaysParamName = "delayInDays";

    // Parameter for client associated with credit standing
    public static String clientIdParamName = "clientId";

}
