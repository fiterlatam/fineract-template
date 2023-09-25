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
package org.apache.fineract.organisation.bankcheque.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BankChequeApiConstants {

    public static final String BANK_CHECK_RESOURCE_NAME = "BANKCHEQUE";
    public static final String CHECK_ACTION_CREATE = "CREATE";
    public static final String CHECK_ACTION_UPDATE = "UPDATE";
    public static final String CHECK_ACTION_DELETE = "DELETE";
    public static final String CHECK_ACTION_REASSIGN = "REASSIGN";
    public static final String CHECK_ACTION_AUTHORIZEREASSIGN = "AUTHORIZEREASSIGN";
    public static final String CHECK_ACTION_VOID = "VOID";
    public static final String CHECK_ACTION_AUTHORIZEVOID = "AUTHORIZEVOID";
    public static String ID_PARAM_NAME = "id";
    public static String BATCH_NO = "batchNo";
    public static String AGENCY = "agency";
    public static String AGENCY_ID = "agencyId";
    public static String BANK_ACCOUNT = "bankAccount";
    public static String BANK_ACC_NO = "bankAccNo";
    public static String BANK_ACC_ID = "bankAccId";
    public static String DESCRIPTION = "description";
    public static String CHEQUES = "cheques";
    public static String FROM = "from";
    public static String TO = "to";
    public static String ACCOUNT_NAME = "accountName";
    public static String BATCH_ID = "batchId";
    public static String OLD_CHEQUE_ID = "oldChequeId";
    public static String CHEQUE_ID = "chequeId";

    public static final Set<String> BATCH_RESPONSE_DATA_PARAMETERS = new HashSet<>(
            Arrays.asList(ID_PARAM_NAME, BATCH_NO, AGENCY, BANK_ACCOUNT, CHEQUES, FROM, TO, BANK_ACC_ID, DESCRIPTION));
    public static final Set<String> SUPPORTED_BATCH_CREATE_OR_UPDATE_PARAMETERS = new HashSet<>(
            Arrays.asList(BANK_ACC_NO, BANK_ACC_ID, AGENCY_ID, FROM, TO, DESCRIPTION, ACCOUNT_NAME, BATCH_ID));

    public static final Set<String> SUPPORTED_VOID_CHEQUE_PARAMETERS = new HashSet<>(Arrays.asList(OLD_CHEQUE_ID, CHEQUE_ID, DESCRIPTION));

}
