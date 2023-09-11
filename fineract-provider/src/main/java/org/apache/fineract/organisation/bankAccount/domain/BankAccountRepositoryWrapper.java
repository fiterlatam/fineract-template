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
package org.apache.fineract.organisation.bankAccount.domain;

import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.bankAccount.exception.BankAccountNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class BankAccountRepositoryWrapper {

    BankAccountRepository repository;

    @Autowired
    public BankAccountRepositoryWrapper(final BankAccountRepository repository) {
        this.repository = repository;
    }

    public BankAccount findOneWithNotFoundDetection(final Long id) {
        return this.repository.findById(id).orElseThrow(() -> new BankAccountNotFoundException(id));
    }

    public BankAccount findOneByAccountAndAgency(final Long accountNumber, Agency agency) {
        return this.repository.findBankAccountByAccountNumberAndAgency(accountNumber, agency);
    }

    public BankAccount save(final BankAccount entity) {
        return this.repository.save(entity);
    }

    public BankAccount saveAndFlush(final BankAccount entity) {
        return this.repository.saveAndFlush(entity);
    }

    public void delete(final BankAccount entity) {
        this.repository.delete(entity);
    }
}
