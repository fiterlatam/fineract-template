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

import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeTypeData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeType;

import java.util.List;
import java.util.stream.Collectors;

public class CustomChargeTypeMapper {

    public static CustomChargeType toModel(CustomChargeTypeData dto) {
        CustomChargeType ret = new CustomChargeType();

		ret.setId(dto.getId());
		ret.setCustomChargeEntityId(dto.getCustomChargeEntityId());
		ret.setName(dto.getName());
		ret.setCode(dto.getCode());


        return ret;
    }

    public static CustomChargeTypeData toDTO(CustomChargeType model) {
        CustomChargeTypeData ret = new CustomChargeTypeData();

		ret.setId(model.getId());
		ret.setCustomChargeEntityId(model.getCustomChargeEntityId());
		ret.setName(model.getName());
		ret.setCode(model.getCode());


        return ret;
    }
    
    public static List<CustomChargeTypeData> toDTO(List<CustomChargeType> model) {
		return model.stream().map(obj ->toDTO(obj)).collect(Collectors.toList());
	}
}

