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
package org.apache.fineract.organisation.bankAccount.data;

import org.apache.fineract.accounting.glaccount.data.GLAccountData;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.organisation.bank.data.BankData;

import java.util.Collection;

/**
 * Immutable data object for bank data.
 */
public class BankAccountData {

    private final Long id;

    private final Long accountNumber;

    private final AgencyData agency;
    private final BankData bank;
    private final GLAccountData glAccount;

    private final Collection<AgencyData> agencyOptions;

    private final Collection<BankData> bankOptions;

    private final Collection<GLAccountData> glAccountOptions;

    private final String description;


    // template

    public static BankAccountData instance(Long id, Long accountNumber, AgencyData agencyData, BankData bankData, GLAccountData glAccountData, String description) {
        return new BankAccountData(id, accountNumber, agencyData, bankData, glAccountData, description);
    }

    public BankAccountData(Long id, Long accountNumber, AgencyData agencyData, BankData bankData, GLAccountData glAccountData, String description) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.agency = agencyData;
        this.bank = bankData;
        this.glAccount = glAccountData;
        this.description = description;
        this.agencyOptions = null;
        this.bankOptions = null;
        this.glAccountOptions = null;
    }

    public BankAccountData(Collection<AgencyData> agencyOptions, Collection<BankData> bankOptions,
                           Collection<GLAccountData> glAccountOptions) {
        this.id = null;
        this.accountNumber = null;
        this.agencyOptions = agencyOptions;
        this.agency = null;
        this.bank = null;
        this.glAccount = null;
        this.description = null;
        this.bankOptions = bankOptions;
        this.glAccountOptions = glAccountOptions;
    }

    // TODO: complete
    public static BankAccountData template(Collection<AgencyData> agencyData, Collection<BankData> bankData,
                                           Collection<GLAccountData> glAccountData) {
        return new BankAccountData(agencyData, bankData, glAccountData);
    }
}
