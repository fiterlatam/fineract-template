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
package org.apache.fineract.custom.portfolio.ally.data;

import java.math.BigDecimal;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAllyPointOfSalesCollectionData {

    private String collectionDate;
    private String nit;
    private String name;
    private Long clientAllyId;
    private Long liquidationFrequencyId;
    private Long pointOfSalesId;
    private String pointOfSalesName;
    private Long cityId;
    private String cityName;
    private BigDecimal amount;
    private Integer settledComission;
    private Integer taxId;
    private Long channelId;
    private Long loanId;
    private Long clientId;
    private String lastJobsRun;
    private Integer loanStatusId;
    private Long transId;
}
