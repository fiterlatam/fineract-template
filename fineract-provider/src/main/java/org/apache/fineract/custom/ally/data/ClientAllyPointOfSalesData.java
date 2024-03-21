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
package org.apache.fineract.custom.ally.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
public class ClientAllyPointOfSalesData {

    private Long id;
    private Long clientAllyId;
    private String code;
    private String name;
    private Long brandCodeValueId;
    private String brandCodeValueDescription;
    private Long cityCodeValueId;
    private String cityCodeValueDescription;
    private Long departmentCodeValueId;
    private String departmentCodeValueDescription;
    private Long categoryCodeValueId;
    private String categoryCodeValueDescription;
    private Long segmentCodeValueId;
    private String segmentCodeValueDescription;
    private Long typeCodeValueId;
    private String typeCodeValueDescription;
    private BigDecimal settledComission;
    private Boolean buyEnabled;
    private Boolean collectionEnabled;
    private Long stateCodeValueId;
    private String stateCodeValueDescription;
}
