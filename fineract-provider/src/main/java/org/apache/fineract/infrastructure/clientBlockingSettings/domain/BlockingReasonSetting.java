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
package org.apache.fineract.infrastructure.clientBlockingSettings.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@Data
@Entity
@Table(name = "m_blocking_reason_setting")
public class BlockingReasonSetting extends AbstractAuditableCustom {

    @Column(name = "priority")
    private Integer priority;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_level")
    private CodeValue customerLevel;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_level")
    private CodeValue creditLevel;
    @Column(name = "description")
    private String description;
    @Column(name = "name_of_reason")
    private String nameOfReason;
    @Column(name = "level")
    private String level;
}
