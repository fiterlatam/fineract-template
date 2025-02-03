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
package org.apache.fineract.infrastructure.hooks.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class HookApiConstants {

    private HookApiConstants() {}

    public static final String HOOK_RESOURCE_NAME = "HOOK";

    public static final String NAME_PARAM_NAME = "name";

    public static final String DISPLAY_NAME_PARAM_NAME = "displayName";

    public static final String IS_ACTIVE_PARAM_NAME = "isActive";

    public static final String WEB_TEMPLATE_NAME = "Web";

    public static final String ELASTIC_SEARCH_TEMPLATE_NAME = "Elastic Search";

    public static final String HTTP_SMS_TEMPLATE_NAME = "Message Gateway";

    public static final String SMS_TEMPLATE_NAME = "SMS Bridge";

    public static final String PAYLOAD_URL_NAME = "Payload URL";

    public static final String CONTENT_TYPE_NAME = "Content Type";

    public static final String SMS_PROVIDER_NAME = "SMS Provider";

    public static final String SMS_PROVIDER_ACCOUNT_ID_NAME = "SMS Provider Account Id";

    public static final String SMS_PROVIDER_TOKEN_ID_NAME = "SMS Provider Token";

    public static final String PHONE_NUMBER_NAME = "Phone Number";

    public static final String API_KEY_NAME = "API Key";

    public static final String CONFIG_PARAM_NAME = "config";

    public static final String EVENTS_PARAM_NAME = "events";

    public static final String ENTITY_NAME_PARAM_NAME = "entityName";

    public static final String ACTION_NAME_PARAM_NAME = "actionName";

    public static final String TEMPLATE_ID_PARAM_NAME = "templateId";

    public static final String TEMPLATE_NAME_PARAM_NAME = "templateName";

    public static final String SMS_PROVIDER_ID_PARAM_NAME = "SMS Provider Id";

    static final Set<String> RESPONSE_DATA_PARAMETERS = new HashSet<>(Arrays.asList(NAME_PARAM_NAME, DISPLAY_NAME_PARAM_NAME,
            TEMPLATE_ID_PARAM_NAME, IS_ACTIVE_PARAM_NAME, CONFIG_PARAM_NAME, EVENTS_PARAM_NAME, TEMPLATE_NAME_PARAM_NAME));

}
