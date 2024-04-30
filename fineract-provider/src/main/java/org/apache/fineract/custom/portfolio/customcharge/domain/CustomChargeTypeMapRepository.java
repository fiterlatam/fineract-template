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
package org.apache.fineract.custom.portfolio.customcharge.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface CustomChargeTypeMapRepository
        extends JpaRepository<CustomChargeTypeMap, Long>, JpaSpecificationExecutor<CustomChargeTypeMap> {

    List<CustomChargeTypeMap> findByCustomChargeTypeIdAndActive(Long customChargeTypeId, boolean active);

    List<CustomChargeTypeMap> findByCustomChargeTypeIdAndTermAndActive(Long customChargeTypeId, Long term, boolean active);

    @Modifying
    @Query("update CustomChargeTypeMap cctm set cctm.active = false, cctm.validTo = :validTo, cctm.updatedAt = :updatedAt, cctm.updatedBy = :updatedBy " +
            "where cctm.customChargeTypeId = :customChargeTypeId and cctm.term = :term")
    int deactivatePreviousTermData(@Param("customChargeTypeId") Long customChargeTypeId, //
                                   @Param("term") Long term, //
                                   @Param("validTo") LocalDate validTo, //
                                   @Param("updatedAt") LocalDateTime updatedAt, //
                                   @Param("updatedBy") Long updatedBy //
    );

}
