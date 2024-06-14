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
package org.apache.fineract.infrastructure.clientblockingreasons.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_blocking_reason_setting")
@Getter
@Setter
@NoArgsConstructor
@Accessors(chain = true)
public class BlockingReasonSetting extends AbstractPersistableCustom {

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "description")
    private String description;

    @Column(name = "name_of_reason")
    private String nameOfReason;

    @Column(name = "level")
    private String level;

    @Column(name = "is_enabled")
    private int isEnabled;

    @Column(name = "disabled_on_date")
    private LocalDate disabledOnDate;

    @ManyToOne
    @JoinColumn(name = "disabled_by", nullable = false)
    private AppUser disabledBy;

    @Column(name = "enabled_on_date")
    private LocalDate enabledOnDate;

    @ManyToOne
    @JoinColumn(name = "enabled_by", nullable = false)
    private AppUser enabledBy;

    @Column(name = "affects_client_level")
    private Integer affectsClientLevel;

    @Column(name = "affects_credit_level")
    private Integer affectsCreditLevel;

    public boolean isEnabled() {
        return this.isEnabled == 1;
    }

    public boolean isAffectsClientLevel() {
        Integer affected = 0;
        if (this.affectsClientLevel != null) {
            affected = 1;
        }
        return this.affectsClientLevel == affected;
    }

    public boolean isAffectsCreditLevel() {
        Integer affected = 0;
        if (this.affectsCreditLevel != null) {
            affected = 1;
        }
        return this.affectsCreditLevel == affected;
    }
}
