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

package org.apache.fineract.custom.portfolio.loanproduct.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.portfolio.loanproduct.data.SubChannelLoanProductData;
import org.apache.fineract.custom.portfolio.loanproduct.domain.SubChannelLoanProduct;

public class SubChannelLoanProductMapper {

    public static SubChannelLoanProduct toModel(SubChannelLoanProductData dto) {
        return SubChannelLoanProduct.builder().id(dto.getId()) //
                .subChannelId(dto.getSubChannelId()) //
                .loanProductId(dto.getLoanProductId()) //
                .build();
    }

    public static SubChannelLoanProductData toDTO(SubChannelLoanProduct model) {
        return SubChannelLoanProductData.builder().id(model.getId()) //
                .channelId(model.getSubChannel().getChannelId()) //
                .channelName(model.getSubChannel().getChannel().getName()) //
                .subChannelId(model.getSubChannelId()) //
                .subChannelName(model.getSubChannel().getName()) //
                .loanProductId(model.getLoanProductId()) //
                .build();
    }

    public static List<SubChannelLoanProductData> toDTO(List<SubChannelLoanProduct> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
