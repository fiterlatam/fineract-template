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
package org.apache.fineract.custom.portfolio.customcharge.constants;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class CustomChargeTypeMapApiConstants {

    private CustomChargeTypeMapApiConstants() {

    }

    public static final String RESOURCE_NAME = "customchargetypemap";

    // general
    public static final String localeParamName = "locale";
    public static final String dateFormatParamName = "dateFormat";

    // request parameters
	public static final String idParamName = "id";
	public static final String customChargeEntityIdParamName = "customChargeEntityId";
	public static final String customChargeTypeIdParamName = "customChargeTypeId";
	public static final String termParamName = "term";
	public static final String percentageParamName = "percentage";
	public static final String validFromParamName = "validFrom";
	public static final String validToParamName = "validTo";
	public static final String activeParamName = "active";
	public static final String createdByParamName = "createdBy";
	public static final String createdAtParamName = "createdAt";
	public static final String updatedByParamName = "updatedBy";
	public static final String updatedAtParamName = "updatedAt";

    
    // request parameters Set
    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(localeParamName, dateFormatParamName, idParamName,customChargeTypeIdParamName,termParamName,percentageParamName,validFromParamName,validToParamName,activeParamName,createdByParamName,createdAtParamName,updatedByParamName,updatedAtParamName, customChargeEntityIdParamName));
}
