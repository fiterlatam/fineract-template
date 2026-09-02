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
package org.apache.fineract.portfolio.exchange.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ExchangeApiConstants {

    private ExchangeApiConstants() {}

    public static final String EXCHANGE_RESOURCE_NAME = "exchange";

    // request attributes
    public static final String destinationCurrencyParamName = "destinationCurrency";
    public static final String originCurrencyParamName = "originCurrency";
    public static final String exchangeRateParamName = "exchangeRate";
    public static final String validFromParamName = "validFrom";

    public static final Set<String> CREATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(destinationCurrencyParamName,
            originCurrencyParamName, exchangeRateParamName, validFromParamName, "locale", "dateFormat"));

    public static final Set<String> UPDATE_REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(destinationCurrencyParamName,
            originCurrencyParamName, exchangeRateParamName, validFromParamName, "locale", "dateFormat"));

    // response attributes
    public static final String idDataParamName = "id";
    public static final String destinationCurrencyDataParamName = "destinationCurrency";
    public static final String originCurrencyDataParamName = "originCurrency";
    public static final String exchangeRateDataParamName = "exchangeRate";
    public static final String validFromDataParamName = "validFrom";
    public static final String createdOnDataParamName = "createdOn";
    public static final String createdByDataParamName = "createdBy";

    public static final Set<String> EXCHANGE_RESPONSE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(idDataParamName, destinationCurrencyDataParamName, originCurrencyDataParamName, exchangeRateDataParamName,
                    validFromDataParamName, createdOnDataParamName, createdByDataParamName));
}
