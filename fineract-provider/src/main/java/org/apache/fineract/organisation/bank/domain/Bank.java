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
package org.apache.fineract.organisation.bank.domain;

import lombok.Getter;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.bank.service.BankConstants;
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Entity
@Getter
@Table(name = "m_bank")
public class Bank extends AbstractPersistableCustom {

    @Column(name = "code", nullable = false, length = 2)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JoinColumn(name = "added_by", nullable = false)
    private AppUser addedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    protected Bank() {
        //
    }

    public Bank(String code, String name, AppUser currentUser) {
        this.addedBy = currentUser;
        this.code = code;
        this.name = name;
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();

    }

    public static Bank fromJson(AppUser currentUser, JsonCommand command) {
        final String code = command.stringValueOfParameterNamed(BankConstants.BankSupportedParameters.CODE.getValue());
        final String name = command.stringValueOfParameterNamed(BankConstants.BankSupportedParameters.NAME.getValue());

        return new Bank(code, name, currentUser);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInStringParameterNamed(BankConstants.BankSupportedParameters.NAME.getValue(), this.name)) {
            final String newValue = command.stringValueOfParameterNamed(BankConstants.BankSupportedParameters.NAME.getValue());
            actualChanges.put(BankConstants.BankSupportedParameters.NAME.getValue(), newValue);
            this.name = newValue;
        }

        if (command.isChangeInStringParameterNamed(BankConstants.BankSupportedParameters.CODE.getValue(), this.code)) {
            final String newValue = command.stringValueOfParameterNamed(BankConstants.BankSupportedParameters.CODE.getValue());
            actualChanges.put(BankConstants.BankSupportedParameters.CODE.getValue(), newValue);
            this.code = newValue;
        }

        return actualChanges;
    }
}
