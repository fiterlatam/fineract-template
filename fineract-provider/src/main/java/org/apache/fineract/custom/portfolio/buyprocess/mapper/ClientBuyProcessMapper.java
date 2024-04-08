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

package org.apache.fineract.custom.portfolio.buyprocess.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.buyprocess.data.ClientBuyProcessData;
import org.apache.fineract.custom.portfolio.buyprocess.domain.ClientBuyProcess;

public class ClientBuyProcessMapper {

    public static ClientBuyProcess toModel(ClientBuyProcessData dto) {
        return ClientBuyProcess.builder().id(dto.getId()).channelId(dto.getChannelId()).clientId(dto.getClientId())
                .pointOfSalesId(dto.getPointOfSalesId()).productId(dto.getProductId()).creditId(dto.getCreditId())
                .requestedDate(dto.getRequestedDate()).amount(dto.getAmount()).term(dto.getTerm()).createdAt(dto.getCreatedAt())
                .createdBy(dto.getCreatedBy()).ipDetails(dto.getIpDetails()).status(dto.getStatus()).errorMessage(dto.getErrorMessage())
                .build();
    }

    public static ClientBuyProcessData toDTO(ClientBuyProcess model) {
        return ClientBuyProcessData.builder().id(model.getId()).channelId(model.getChannelId()).clientId(model.getClientId())
                .pointOfSalesId(model.getPointOfSalesId()).productId(model.getProductId()).creditId(model.getCreditId())
                .requestedDate(model.getRequestedDate()).amount(model.getAmount()).term(model.getTerm()).createdAt(model.getCreatedAt())
                .createdBy(model.getCreatedBy()).ipDetails(model.getIpDetails()).status(model.getStatus())
                .errorMessage(model.getErrorMessage()).build();
    }

    public static List<ClientBuyProcessData> toDTO(List<ClientBuyProcess> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
