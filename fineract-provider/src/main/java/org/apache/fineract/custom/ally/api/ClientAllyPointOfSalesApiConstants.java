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

public final class ClientAllyPointOfSalesApiConstants {

    private ClientAllyPointOfSalesApiConstants() {

    }

    public static final String RESOURCE_NAME = "clientallypointofsales";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
    public static final String idParamName = "id";
    public static final String clientAllyIdParamName = "clientAllyId";
    public static final String codeParamName = "code";
    public static final String nameParamName = "name";
    public static final String brandParamName = "brandCodeValueId";
    public static final String cityCodeValueIdParamName = "cityCodeValueId";
    public static final String departmentCodeValueIdParamName = "departmentCodeValueId";
    public static final String categoryCodeValueIdParamName = "categoryCodeValueId";
    public static final String segmentCodeValueIdParamName = "segmentCodeValueId";
    public static final String typeCodeValueIdParamName = "typeCodeValueId";
    public static final String settledComissionParamName = "settledComission";
    public static final String buyEnabledParamName = "buyEnabled";
    public static final String collectionEnabledParamName = "collectionEnabled";
    public static final String stateCodeValueIdParamName = "stateCodeValueId";

    // request parameters Set
    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, idParamName,
            clientAllyIdParamName, codeParamName, nameParamName, brandParamName, cityCodeValueIdParamName, departmentCodeValueIdParamName,
            categoryCodeValueIdParamName, segmentCodeValueIdParamName, typeCodeValueIdParamName, settledComissionParamName,
            buyEnabledParamName, collectionEnabledParamName, stateCodeValueIdParamName));
}
