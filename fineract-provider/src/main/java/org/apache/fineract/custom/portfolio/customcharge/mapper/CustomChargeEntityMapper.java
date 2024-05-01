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

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.customcharge.data.CustomChargeEntityData;
import org.apache.fineract.custom.portfolio.customcharge.domain.CustomChargeEntity;

public class CustomChargeEntityMapper {

    public static CustomChargeEntity toModel(CustomChargeEntityData dto) {
        CustomChargeEntity ret = new CustomChargeEntity();

        ret.setId(dto.getId());
        ret.setName(dto.getName());
        ret.setCode(dto.getCode());

        return ret;
    }

    public static CustomChargeEntityData toDTO(CustomChargeEntity model) {
        CustomChargeEntityData ret = new CustomChargeEntityData();

        ret.setId(model.getId());
        ret.setName(model.getName());
        ret.setCode(model.getCode());

        return ret;
    }

    public static List<CustomChargeEntityData> toDTO(List<CustomChargeEntity> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
