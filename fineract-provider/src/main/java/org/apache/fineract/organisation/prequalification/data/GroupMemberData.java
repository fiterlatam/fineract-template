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
package org.apache.fineract.organisation.prequalification.data;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import java.math.BigDecimal;
import java.time.LocalDate;


/**
 * Immutable data object represent client identity data.
 */
public class GroupMemberData {

    private final Long id;
    private String name;
    private String dpi;
    private LocalDate dob;
    private EnumOptionData status;
    private String workWithPuente;
    private BigDecimal requestedAmount;

    public GroupMemberData(final Long id, final String name, final LocalDate dob, final String dpi,
                           final EnumOptionData status, final String workWithPuente, final BigDecimal requestedAmount) {
        this.id = id;
        this.name = name;
        this.dpi = dpi;
        this.dob = dob;
        this.status = status;
        this.requestedAmount = requestedAmount;
        this.workWithPuente = workWithPuente;
        this.status = status;
    }



    public static GroupMemberData instance(final Long id, final String name, final LocalDate dob, final String dpi,
                                           final EnumOptionData status, final String workWithPuente, final BigDecimal requestedAmount) {
        return new GroupMemberData(id, name, dob, dpi, status, workWithPuente, requestedAmount);
    }
}
