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
package org.apache.fineract.custom.portfolio.buyprocess.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class ClientBuyProcessApiConstants {

    private ClientBuyProcessApiConstants() {

    }

    public static final String RESOURCE_NAME = "clientbuyprocess";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String channelIdParamName = "channelId";
    public static final String clientIdParamName = "clientId";
    public static final String clientDocumentIdParamName = "clientDocumentId";
    public static final String pointOfSalesIdParamName = "pointOfSalesId";
    public static final String pointOfSalesCodeParamName = "pointOfSalesCode";
    public static final String productIdParamName = "productId";
    public static final String creditIdParamName = "creditId";
    public static final String requestedDateParamName = "requestedDate";
    public static final String amountParamName = "amount";
    public static final String termParamName = "term";
    public static final String createdAtParamName = "createdAt";
    public static final String createdByParamName = "createdBy";
    public static final String ipDetailsParamName = "ipDetails";

    // request parameters Set
    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, idParamName,
            channelIdParamName, clientIdParamName, pointOfSalesIdParamName, productIdParamName, creditIdParamName, requestedDateParamName,
            amountParamName, termParamName, createdAtParamName, createdByParamName, ipDetailsParamName, pointOfSalesCodeParamName,
            clientDocumentIdParamName));
}
