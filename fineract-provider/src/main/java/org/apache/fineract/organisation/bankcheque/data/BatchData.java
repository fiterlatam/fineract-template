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
package org.apache.fineract.organisation.bankcheque.data;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.organisation.agency.data.AgencyData;
import org.apache.fineract.portfolio.group.data.CenterData;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.useradministration.data.AppUserData;

@Builder
@Data
public class BatchData {

    private Long id;
    private Long batchNo;
    private Long bankAccNo;
    private Long bankAccId;
    private Long agencyId;
    private String bankName;
    private String bankCode;
    private String agencyName;
    private Collection<ChequeData> cheques;
    private Long from;
    private Long to;
    private LocalDate createdDate;
    private Long createdByUserId;
    private String createdByUsername;
    private String description;
    private List<EnumOptionData> statusOptions;
    private Collection<AgencyData> agencyOptions;
    private final Collection<CenterData> centerOptions;
    private final Collection<GroupGeneralData> groupOptions;
    private final Collection<AppUserData> facilitatorOptions;
}
