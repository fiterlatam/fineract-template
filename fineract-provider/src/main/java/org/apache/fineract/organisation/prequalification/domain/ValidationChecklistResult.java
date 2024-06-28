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
package org.apache.fineract.organisation.prequalification.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@EqualsAndHashCode(callSuper = true)
@Entity
@Data
@RequiredArgsConstructor
@Table(name = "m_checklist_validation_result")
public class ValidationChecklistResult extends AbstractAuditableCustom {

    @Column(name = "policy_id")
    private Integer policyId;

    @Column(name = "prequalification_id")
    private Long prequalificationId;

    @Column(name = "prequalification_member_id")
    private Integer prequalificationMemberId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "prequalification_type")
    private Integer prequalificationType;

    @Column(name = "custom_value")
    private String customValue;

    @Column(name = "validation_color_enum")
    private Integer validationColor;
}
