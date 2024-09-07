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
package org.apache.fineract.portfolio.insurance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.portfolio.insurance.data.InsuranceIncidentData;

@Entity
@Table(name = "m_insurance_incidents", uniqueConstraints = { @UniqueConstraint(columnNames = { "name" }, name = "unq_name") })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(staticName = "instance")
public class InsuranceIncident extends AbstractAuditableWithUTCDateTimeCustom {

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "is_mandatory")
    private boolean isMandatory;

    @Column(name = "is_voluntary")
    private boolean isVoluntary;

    @Column(name = "incident_type")
    private InsuranceIncidentType incidentType;

    public InsuranceIncidentData toData() {
        String code = "";
        Integer value = null;
        if (this.incidentType != null) {
            code = this.incidentType.getCode();
            value = this.incidentType.getValue();
        }

        return InsuranceIncidentData.instance(this.getId(), this.name, this.isMandatory, this.isVoluntary, code, value, null);
    }

}
