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
package org.apache.fineract.custom.ally.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ClientAllyApiConstants {

    private ClientAllyApiConstants() {

    }

    public static final String RESOURCE_NAME = "clientally";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String companyNameParamName = "companyName";
    public static final String nitParamName = "nit";
    public static final String nitDigitParamName = "nitDigit";
    public static final String addressParamName = "address";
    public static final String cityCodeValueIdParamName = "cityCodeValueId";
    public static final String departmentCodeValueIdParamName = "departmentCodeValueId";
    public static final String liquidationFrequencyCodeValueIdParamName = "liquidationFrequencyCodeValueId";
    public static final String applyCupoMaxSellParamName = "applyCupoMaxSell";
    public static final String cupoMaxSellParamName = "cupoMaxSell";
    public static final String settledComissionParamName = "settledComission";
    public static final String buyEnabledParamName = "buyEnabled";
    public static final String collectionEnabledParamName = "collectionEnabled";
    public static final String bankEntityCodeValueIdParamName = "bankEntityCodeValueId";
    public static final String accountTypeCodeValueIdParamName = "accountTypeCodeValueId";
    public static final String accountNumberParamName = "accountNumber";
    public static final String taxProfileCodeValueIdParamName = "taxProfileCodeValueId";
    public static final String stateCodeValueIdParamName = "stateCodeValueId";

    // request parameters Set
    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, idParamName,
            companyNameParamName, nitParamName, nitDigitParamName, addressParamName, cityCodeValueIdParamName,
            departmentCodeValueIdParamName, liquidationFrequencyCodeValueIdParamName, applyCupoMaxSellParamName, cupoMaxSellParamName,
            settledComissionParamName, buyEnabledParamName, collectionEnabledParamName, bankEntityCodeValueIdParamName,
            accountTypeCodeValueIdParamName, accountNumberParamName, taxProfileCodeValueIdParamName, stateCodeValueIdParamName));
}
