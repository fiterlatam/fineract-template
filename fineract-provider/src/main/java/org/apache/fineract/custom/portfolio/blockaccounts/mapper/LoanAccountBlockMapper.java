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
package org.apache.fineract.custom.portfolio.blockaccounts.mapper;

import java.util.List;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface LoanAccountBlockMapper {

    LoanAccountBlockMapper INSTANCE = Mappers.getMapper(LoanAccountBlockMapper.class);

    @Mapping(expression = "java(entity.getLoan() != null ? entity.getLoan().getId() : null)", target = "loanId")
    @Mapping(source = "blockingReasonSetting.id", target = "blockingReasonId")
    @Mapping(source = "blockingReasonSetting.nameOfReason", target = "blockingReasonName")
    LoanAccountBlockDTO toDto(LoanAccountBlock entity);

    @Mapping(expression = "java(entity.getLoan() != null ? entity.getLoan().getId() : null)", target = "loanId")
    @Mapping(source = "blockingReasonSetting.id", target = "blockingReasonId")
    @Mapping(source = "blockingReasonSetting.nameOfReason", target = "blockingReasonName")
    List<LoanAccountBlockDTO> toDto(List<LoanAccountBlock> entities);

    LoanAccountBlock toEntity(LoanAccountBlockDTO dto);
}
