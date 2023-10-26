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

import java.math.BigDecimal;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Getter
@Table(name = "m_prequalification_status_range")
public class PrequalificationStatusRange extends AbstractPersistableCustom {

    @Column(name = "prequalification_type_enum")
    private Integer prequalificationType;

    @Column(name = "min_amount", nullable = false)
    private BigDecimal minAmount;

    @Column(name = "max_amount")
    private BigDecimal maxAmount;

    @Column(name = "min_error", nullable = false)
    private Integer minError;

    @Column(name = "max_error", nullable = false)
    private Integer maxError;

    @Column(name = "status", nullable = false)
    private Integer status;

    public Integer getPrequalificationType() {
        return prequalificationType;
    }

    public void setPrequalificationType(Integer prequalificationType) {
        this.prequalificationType = prequalificationType;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getMinError() {
        return minError;
    }

    public void setMinError(Integer minError) {
        this.minError = minError;
    }

    public Integer getMaxError() {
        return maxError;
    }

    public void setMaxError(Integer maxError) {
        this.maxError = maxError;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
