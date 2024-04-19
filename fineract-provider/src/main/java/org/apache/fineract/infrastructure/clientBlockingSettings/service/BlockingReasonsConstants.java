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
package org.apache.fineract.infrastructure.clientBlockingSettings.service;

public class BlockingReasonsConstants {

    // request parameters
    public static final String ID_PARAM = "id";
    public static final String ENTITY_NAME = "BLOCK_SETTINGS";
    public static final String RESOURCE_URL = "/v1/blockSettings";

    public static final String CUSTOMER_LEVEL_PARAM = "customerLevel";
    public static final String CUSTOMER_CODE = "Nivel Cliente";
    public static final String CREDIT_LEVEL_PARAM = "creditLevel";
    public static final String CREDIT_CODE = "Nivel Crédito";
    public static final String CUSTOMER_LEVEL_OPTIONS_PARAM = "customerLevelOptions";
    public static final String CREDIT_LEVEL_OPTIONS_PARAM = "creditLevelOptions";
    public static final String PRIORITY_PARAM = "priority";
    public static final String NAME_OF_REASON_PARAM = "nameOfReason";
    public static final String LEVEL_PARAM = "level";
    public static final String DESCRIPTION_PARAM = "description";
}
