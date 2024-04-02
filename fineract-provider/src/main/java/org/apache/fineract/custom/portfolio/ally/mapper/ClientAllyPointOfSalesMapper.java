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

package org.apache.fineract.custom.portfolio.ally.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyPointOfSalesData;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyPointOfSales;

public class ClientAllyPointOfSalesMapper {

    public static ClientAllyPointOfSales toModel(ClientAllyPointOfSalesData dto) {
        return ClientAllyPointOfSales.builder().id(dto.getId()).clientAllyId(dto.getClientAllyId()).code(dto.getCode()).name(dto.getName())
                .brandCodeValueId(dto.getBrandCodeValueId()).cityCodeValueId(dto.getCityCodeValueId())
                .departmentCodeValueId(dto.getDepartmentCodeValueId()).categoryCodeValueId(dto.getCategoryCodeValueId())
                .segmentCodeValueId(dto.getSegmentCodeValueId()).typeCodeValueId(dto.getTypeCodeValueId())
                .settledComission(dto.getSettledComission()).buyEnabled(dto.getBuyEnabled()).collectionEnabled(dto.getCollectionEnabled())
                .stateCodeValueId(dto.getStateCodeValueId()).build();
    }

    public static ClientAllyPointOfSalesData toDTO(ClientAllyPointOfSales model) {
        return ClientAllyPointOfSalesData.builder().id(model.getId()).clientAllyId(model.getClientAllyId()).code(model.getCode())
                .name(model.getName()).brandCodeValueId(model.getBrandCodeValueId()).cityCodeValueId(model.getCityCodeValueId())
                .departmentCodeValueId(model.getDepartmentCodeValueId()).categoryCodeValueId(model.getCategoryCodeValueId())
                .segmentCodeValueId(model.getSegmentCodeValueId()).typeCodeValueId(model.getTypeCodeValueId())
                .settledComission(model.getSettledComission()).buyEnabled(model.getBuyEnabled())
                .collectionEnabled(model.getCollectionEnabled()).stateCodeValueId(model.getStateCodeValueId()).build();
    }

    public static List<ClientAllyPointOfSalesData> toDTO(List<ClientAllyPointOfSales> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
