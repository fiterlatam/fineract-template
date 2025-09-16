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
package org.apache.fineract.custom.portfolio.blockaccounts.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LoanAccountBlockConstants {

    public LoanAccountBlockConstants() {}

    public static final String loanIdParamName = "loanId";
    public static final String applicationDateParamName = "applicationDate";
    public static final String blockingReasonIdParamName = "blockingReasonId";
    public static final String accelerateParamName = "accelerate";
    public static final String freezeCurrentInterestParamName = "freezeCurrentInterest";
    public static final String freezeInterestArrearsParamName = "freezeInterestArrears";
    public static final String freezeLifeInsuranceParamName = "freezeLifeInsurance";
    public static final String freezeMypimeParamName = "freezeMypime";
    public static final String activeParamName = "active";
    public static final String dateFormatParamName = "dateFormat";
    public static final String localeParamName = "locale";
    public static final String noteParamName = "note";

    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(loanIdParamName, applicationDateParamName,
            blockingReasonIdParamName, accelerateParamName, freezeCurrentInterestParamName, freezeInterestArrearsParamName,
            freezeLifeInsuranceParamName, freezeMypimeParamName, activeParamName, localeParamName, dateFormatParamName, noteParamName));

}
