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

package org.apache.fineract.portfolio.blacklist.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_client_blacklist", uniqueConstraints = { @UniqueConstraint(columnNames = { "dpi" }, name = "unique_dpi_number") })
public class BlacklistClients extends AbstractPersistableCustom {

    @Column(name = "dpi", nullable = false)
    private String dpi;

    @Column(name = "client_name", nullable = false)
    private String clientName;

    @Column(name = "nit", nullable = false)
    private String nit;

    @Column(name = "type_enum", nullable = false)
    private Integer typeEnum;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "agency_id", nullable = false)
    private String agencyId;

    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private LoanProduct loanProduct;

    @ManyToOne
    @JoinColumn(name = "added_by", nullable = false)
    private AppUser addedBy;

    @Column(name = "product_code", length = 1000)
    private String productCode;

    @Column(name = "status", nullable = false)
    private Integer status;

    @Column(name = "balance", nullable = false)
    private BigDecimal balance;

    @Column(name = "disbursement_amount", nullable = false)
    private BigDecimal disbursementAmount;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public static BlacklistClients fromJson(final AppUser appUser, final LoanProduct loanProduct, final CodeValueData typification,
                                            final JsonCommand command) {
        final String dpi = command.stringValueOfParameterNamed("dpiNumber").trim();
        final String clientName = command.stringValueOfParameterNamed("clientName").trim();
        final String nit = command.stringValueOfParameterNamed("nit");
        final String description = command.stringValueOfParameterNamed("description");
        final String agencyId = command.stringValueOfParameterNamed("agencyId");
        final Integer year = command.integerValueOfParameterNamed("year");
        final BigDecimal balance = command.bigDecimalValueOfParameterNamed("balance");
        final BigDecimal disbursementAmount = command.bigDecimalValueOfParameterNamed("disbursementAmount");
        return new BlacklistClients(appUser, typification, clientName, dpi, nit, description, agencyId, loanProduct, balance,
                disbursementAmount, year);
    }

    protected BlacklistClients() {
        //
    }

    private BlacklistClients(final AppUser appUser, final CodeValueData typification, final String clientName, final String dpi,
                             final String nit, final String description, final String agencyId, final LoanProduct loanProduct, final BigDecimal balance,
                             final BigDecimal disbursementAmount, final Integer year) {
        this.addedBy = appUser;
        this.typeEnum = typification.getId().intValue();
        this.agencyId = agencyId;
        this.dpi = dpi;
        this.nit = nit;
        this.description = StringUtils.defaultIfEmpty(description, null);
        this.status = BlacklistStatus.ACTIVE.getValue();
        this.loanProduct = loanProduct;
        this.productCode = loanProduct.getShortName();
        this.balance = balance;
        this.disbursementAmount = disbursementAmount;
        this.year = year;
        this.clientName = clientName;
        this.createdAt = DateUtils.getLocalDateTimeOfTenant();
    }

    public void updateStatus(final BlacklistStatus blacklistStatus) {
        ;
        this.status = blacklistStatus.getValue();
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(7);

        // final String documentTypeIdParamName = "documentTypeId";
        // if (command.isChangeInLongParameterNamed(documentTypeIdParamName, this.documentType.getId())) {
        // final Long newValue = command.longValueOfParameterNamed(documentTypeIdParamName);
        // actualChanges.put(documentTypeIdParamName, newValue);
        // }
        //
        // final String documentKeyParamName = "documentKey";
        // if (command.isChangeInStringParameterNamed(documentKeyParamName, this.documentKey)) {
        // final String newValue = command.stringValueOfParameterNamed(documentKeyParamName);
        // actualChanges.put(documentKeyParamName, newValue);
        // this.documentKey = StringUtils.defaultIfEmpty(newValue, null);
        // }
        //
        // final String descriptionParamName = "description";
        // if (command.isChangeInStringParameterNamed(descriptionParamName, this.description)) {
        // final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
        // actualChanges.put(descriptionParamName, newValue);
        // this.description = StringUtils.defaultIfEmpty(newValue, null);
        // }
        //
        // final String statusParamName = "status";
        // if (command.isChangeInStringParameterNamed(statusParamName,
        // ClientIdentifierStatus.fromInt(this.status).getCode())) {
        // final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
        // actualChanges.put(descriptionParamName, ClientIdentifierStatus.valueOf(newValue));
        // this.status = ClientIdentifierStatus.valueOf(newValue).getValue();
        // }

        return actualChanges;
    }
}
