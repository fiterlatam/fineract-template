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

package org.apache.fineract.custom.portfolio.customcharge.mapper;

import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeMapData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeTypeMap;

import java.util.List;
import java.util.stream.Collectors;

public class CustomChargeTypeMapMapper {

    public static CustomChargeTypeMap toModel(CustomChargeTypeMapData dto) {
        CustomChargeTypeMap ret = CustomChargeTypeMap.builder()
			.id(dto.getId()) //
			.customChargeTypeId(dto.getCustomChargeTypeId()) //
			.term(dto.getTerm()) //
			.percentage(dto.getPercentage()) //
			.validFrom(dto.getValidFrom()) //
			.validTo(dto.getValidTo()) //
			.active(dto.getActive()) //
			.createdBy(dto.getCreatedBy()) //
			.createdAt(dto.getCreatedAt()) //
			.updatedBy(dto.getUpdatedBy()) //
			.updatedAt(dto.getUpdatedAt()) //
            .build();

        return ret;
    }

    public static CustomChargeTypeMapData toDTO(CustomChargeTypeMap model) {
        CustomChargeTypeMapData ret = CustomChargeTypeMapData.builder()
			.id(model.getId()) //
			.customChargeTypeId(model.getCustomChargeTypeId()) //
			.term(model.getTerm()) //
			.percentage(model.getPercentage()) //
			.validFrom(model.getValidFrom()) //
			.validTo(model.getValidTo()) //
			.active(model.getActive()) //
			.createdBy(model.getCreatedBy()) //
			.createdAt(model.getCreatedAt()) //
			.updatedBy(model.getUpdatedBy()) //
			.updatedAt(model.getUpdatedAt()) //
            .build();

        return ret;
    }
    
    public static List<CustomChargeTypeMapData> toDTO(List<CustomChargeTypeMap> model) {
		return model.stream().map(obj ->toDTO(obj)).collect(Collectors.toList());
	}
}

