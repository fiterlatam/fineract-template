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
package org.apache.fineract.organisation.bankAccount.api;

import org.apache.fineract.organisation.bankAccount.data.BankAccountData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings({ "HideUtilityClassConstructor" })
public class BankAccountApiConstants {

    // request parameters
    public static final String idParamName = "id";
    public static final String bankAccountParamName = "accountNumber";
    public static final String agencyParamName = "agency";
    public static final String bankParamName = "bank";
    public static final String glAccountParamName = "glAccount";
    public static final String descriptionParamName = "description";

    /**
     * These parameters will match the class level parameters of {@link BankAccountData}. Where possible, we try to get
     * response parameters to match those of request parameters.
     */
    protected static final Set<String> BANK_ACCOUNT_RESPONSE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(idParamName, bankAccountParamName, agencyParamName, bankParamName, glAccountParamName, descriptionParamName));
}
