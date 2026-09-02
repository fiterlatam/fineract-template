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
package org.apache.fineract.portfolio.cupo.api;

public final class CupoApiConstants {

    private CupoApiConstants() {}

    public static final String CUPO_RESOURCE_NAME = "CUPO";
    public static final String CUPO_TRANSACTION_RESOURCE_NAME = "CUPOTRANSACTION";

    public static String amountParamName = "amount";
    public static String expirationDateParamName = "expirationDate";
    public static String clientIdParamName = "clientId";
    public static String groupIdParamName = "groupId";
    public static String approvalDateParamName = "approvalDate";
    public static String rejectDateParamName = "rejectDate";
    public static String currencyCodeParamName = "currencyCode";

}
