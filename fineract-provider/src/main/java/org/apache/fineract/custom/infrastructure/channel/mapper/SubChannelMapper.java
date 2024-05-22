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

package org.apache.fineract.custom.infrastructure.channel.mapper;

import java.util.List;
import java.util.stream.Collectors;
import org.apache.fineract.custom.infrastructure.channel.data.SubChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.SubChannel;

public class SubChannelMapper {

    public static SubChannel toModel(SubChannelData dto) {
        return SubChannel.builder().id(dto.getId()) //
                .channelId(dto.getChannelId()) //
                .name(dto.getName()) //
                .description(dto.getDescription()) //
                .active(dto.getActive()) //
                .build();

    }

    public static SubChannelData toDTO(SubChannel model) {
        return SubChannelData.builder().id(model.getId()) //
                .channelId(model.getChannelId()) //
                .name(model.getName()) //
                .description(model.getDescription()) //
                .active(model.getActive()) //
                .build();

    }

    public static List<SubChannelData> toDTO(List<SubChannel> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
