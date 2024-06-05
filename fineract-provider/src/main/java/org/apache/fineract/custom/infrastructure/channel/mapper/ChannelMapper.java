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
import org.apache.fineract.custom.infrastructure.channel.data.ChannelData;
import org.apache.fineract.custom.infrastructure.channel.domain.Channel;
import org.apache.fineract.custom.infrastructure.channel.domain.ChannelType;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public class ChannelMapper {

    public static Channel toModel(ChannelData dto) {
        return Channel.builder().id(dto.getId()) //
                .hash(dto.getHash()) //
                .name(dto.getName()) //
                .description(dto.getDescription()) //
                .active(dto.getActive()) //
                .build();
    }

    public static ChannelData toDTO(Channel model) {
        final EnumOptionData channelTypeEnumOptionData = ChannelType.fromInt(model.getChannelType()).asEnumOptionData();
        return ChannelData.builder().id(model.getId()) //
                .hash(model.getHash()) //
                .name(model.getName()) //
                .channelType(channelTypeEnumOptionData) //
                .description(model.getDescription()) //
                .active(model.getActive()) //
                .build();
    }

    public static List<ChannelData> toDTO(List<Channel> model) {
        return model.stream().map(obj -> toDTO(obj)).collect(Collectors.toList());
    }
}
