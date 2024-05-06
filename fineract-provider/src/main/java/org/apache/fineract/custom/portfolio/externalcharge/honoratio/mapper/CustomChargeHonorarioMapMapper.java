/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.fineract.custom.portfolio.externalcharge.honoratio.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;

public class CustomChargeHonorarioMapMapper {

    public static CustomChargeHonorarioMap toModel(CustomChargeHonorarioMapData dto) {
        return CustomChargeHonorarioMap.builder().id(dto.getId()) //
                .clientAllyId(dto.getClientAllyId()) //
                .nit(dto.getNit()) //
                .clientDocumentId(dto.getClientDocumentId()) //
                .clientId(dto.getClientId()) //
                .loanId(dto.getLoanId()) //
                .loanInstallmentNr(dto.getLoanInstallmentNr()) //
                .feeTotalAmount(dto.getFeeTotalAmount()) //
                .feeBaseAmount(dto.getFeeBaseAmount()) //
                .feeVatAmount(dto.getFeeVatAmount()) //
                .createdBy(dto.getCreatedBy()) //
                .createdAt(dto.getCreatedAt()) //
                .updatedBy(dto.getUpdatedBy()) //
                .updatedAt(dto.getUpdatedAt()) //
                .disabledBy(dto.getDisabledBy()) //
                .disabledAt(dto.getDisabledAt()) //
                .build();
    }

    public static CustomChargeHonorarioMapData toDTO(CustomChargeHonorarioMap model) {
        return CustomChargeHonorarioMapData.builder().id(model.getId()) //
                .clientAllyId(model.getClientAllyId()) //
                .nit(model.getNit()) //
                .clientId(model.getClientId()) // )
                .clientDocumentId(model.getClientDocumentId()) //
                .loanId(model.getLoanId()) //
                .loanInstallmentNr(model.getLoanInstallmentNr()) //
                .feeTotalAmount(model.getFeeTotalAmount()) //
                .feeBaseAmount(model.getFeeBaseAmount()) //
                .feeVatAmount(model.getFeeVatAmount()) //
                .createdBy(model.getCreatedBy()) //
                .createdAt(model.getCreatedAt()) //
                .updatedBy(model.getUpdatedBy()) //
                .updatedAt(model.getUpdatedAt()) //
                .disabledBy(model.getDisabledBy()) //
                .disabledAt(model.getDisabledAt()) //
                .build();
    }

    public static List<CustomChargeHonorarioMapData> toDTO(List<CustomChargeHonorarioMap> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
