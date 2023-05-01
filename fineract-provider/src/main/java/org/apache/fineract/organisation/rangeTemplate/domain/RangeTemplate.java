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
package org.apache.fineract.organisation.rangeTemplate.domain;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;

@Entity
@Table(name = "m_range_template")
public class RangeTemplate extends AbstractPersistableCustom {

    @Column(name = "name", nullable = false, length = 60)
    private String name;

    @Column(name = "code", nullable = false, length = 3)
    private String code;

    @JoinColumn(name = "month_start_day")
    private Integer startDay;

    @JoinColumn(name = "month_end_day")
    private Integer endDay;

    protected RangeTemplate() {}

    public RangeTemplate(String name, String code, Integer startDay, Integer endDay) {
        this.name = name;
        this.code = code;
        this.startDay = startDay;
        this.endDay = endDay;
    }

}
