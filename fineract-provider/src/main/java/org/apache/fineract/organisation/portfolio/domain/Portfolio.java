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
package org.apache.fineract.organisation.portfolio.domain;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.portfolio.service.PortfolioConstants;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_portfolio")
public class Portfolio extends AbstractAuditableCustom {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_office_id")
    private Office parentOffice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsible_user_id")
    private AppUser responsibleUser;

    protected Portfolio() {

    }

    public Portfolio(Office parentOffice, String name, AppUser responsibleUser) {
        this.parentOffice = parentOffice;
        this.name = StringUtils.defaultIfEmpty(name, null);
        this.responsibleUser = responsibleUser;
    }

    public static Portfolio fromJson(Office parentOffice, AppUser responsibleUser, JsonCommand command) {
        final String name = command.stringValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue());

        return new Portfolio(parentOffice, name, responsibleUser);
    }

    public Map<String, Object> update(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInStringParameterNamed(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue(), this.name)) {
            final String newValue = command.stringValueOfParameterNamed(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue());
            actualChanges.put(PortfolioConstants.PortfolioSupportedParameters.NAME.getValue(), newValue);
            this.name = newValue;
        }

        return actualChanges;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Office getParentOffice() {
        return parentOffice;
    }

    public void setParentOffice(Office parentOffice) {
        this.parentOffice = parentOffice;
    }

    public AppUser getResponsibleUser() {
        return responsibleUser;
    }

    public void setResponsibleUser(AppUser responsibleUser) {
        this.responsibleUser = responsibleUser;
    }
}
