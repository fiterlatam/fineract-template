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
import org.apache.fineract.custom.portfolio.ally.data.ClientAllyData;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAlly;

public class ClientAllyMapper {

    public static ClientAlly toModel(ClientAllyData dto) {
        return ClientAlly.builder() //
                .id(dto.getId()) //
                .companyName(dto.getCompanyName()) //
                .nit(dto.getNit()) //
                .nitDigit(dto.getNitDigit()) //
                .address(dto.getAddress()) //
                .cityCodeValueId(dto.getCityCodeValueId()) //
                .departmentCodeValueId(dto.getDepartmentCodeValueId()) //
                .liquidationFrequencyCodeValueId(dto.getLiquidationFrequencyCodeValueId()) //
                .applyCupoMaxSell(dto.getApplyCupoMaxSell()) //
                .cupoMaxSell(dto.getCupoMaxSell()) //
                .settledComission(dto.getSettledComission()) //
                .buyEnabled(dto.getBuyEnabled()) //
                .collectionEnabled(dto.getCollectionEnabled()) //
                .bankEntityCodeValueId(dto.getBankEntityCodeValueId()) //
                .accountTypeCodeValueId(dto.getAccountTypeCodeValueId()) //
                .accountNumber(dto.getAccountNumber()) //
                .taxProfileCodeValueId(dto.getTaxProfileCodeValueId()) //
                .stateCodeValueId(dto.getStateCodeValueId()) //
                .build(); //
    }

    public static ClientAllyData toDTO(ClientAlly model) {
        return ClientAllyData.builder() //
                .id(model.getId()) //
                .companyName(model.getCompanyName()) //
                .nit(model.getNit()) //
                .nitDigit(model.getNitDigit()) //
                .address(model.getAddress()) //
                .cityCodeValueId(model.getCityCodeValueId()) //
                .departmentCodeValueId(model.getDepartmentCodeValueId()) //
                .liquidationFrequencyCodeValueId(model.getLiquidationFrequencyCodeValueId()) //
                .applyCupoMaxSell(model.getApplyCupoMaxSell()) //
                .cupoMaxSell(model.getCupoMaxSell()) //
                .settledComission(model.getSettledComission()) //
                .buyEnabled(model.getBuyEnabled()) //
                .collectionEnabled(model.getCollectionEnabled()) //
                .bankEntityCodeValueId(model.getBankEntityCodeValueId()) //
                .accountTypeCodeValueId(model.getAccountTypeCodeValueId()) //
                .accountNumber(model.getAccountNumber()) //
                .taxProfileCodeValueId(model.getTaxProfileCodeValueId()) //
                .stateCodeValueId(model.getStateCodeValueId()) //
                .build();
    }

    public static List<ClientAllyData> toDTO(List<ClientAlly> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
