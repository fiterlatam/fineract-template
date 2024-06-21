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
package org.apache.fineract.custom.infrastructure.channel.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChannelRepository extends JpaRepository<Channel, Long>, JpaSpecificationExecutor<Channel> {

    String FIND_NON_CORE_ACTIVE_CHANNELS = "select c from Channel c where c.active = :active and c.id > 1 order by c.name";
    String FIND_HASH_CHANNEL = "select distinct c from Channel c where c.hash = :hash";

    @Query(FIND_HASH_CHANNEL)
    Optional<Channel> findByHash(@Param("hash") String hash);

    Optional<Channel> findByName(String name);

    @Query(FIND_NON_CORE_ACTIVE_CHANNELS)
    List<Channel> findAllActive(@Param("active") Boolean active);
}
