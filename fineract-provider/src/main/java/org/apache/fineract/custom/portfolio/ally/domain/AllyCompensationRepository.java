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
package org.apache.fineract.custom.portfolio.ally.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AllyCompensationRepository extends JpaRepository<AllyCompensation, Long>, JpaSpecificationExecutor<AllyCompensation> {

    Optional<AllyCompensation> findById(Long id);

    Optional<AllyCompensation> findFirst1ByNit(String nit);

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.settlementStatus=null")
    List<AllyCompensation> findBySettlementStatus();

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.nit = :nit and allyCompensation.startDate = :startDate and allyCompensation.endDate = :endDate")
    Optional<AllyCompensation> findBynitAndDate(@Param("nit") String nit, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("select allyCompensation from AllyCompensation allyCompensation where allyCompensation.netOutstandingAmount < 0")
    List<AllyCompensation> findNegativeCompensations();

    @Query(value = "select * from m_ally_compensation allyCompensation where allyCompensation.client_ally_id= ? order by id DESC limit 1", nativeQuery = true)
    Optional<AllyCompensation> findCompensationByClientId(@Param("clientAllyId") Long clientAllyId);

    @Query("SELECT allyCompensation.id FROM AllyCompensation allyCompensation WHERE allyCompensation.id NOT IN (SELECT MAX(allyCompensation2.id) FROM AllyCompensation allyCompensation2 GROUP BY allyCompensation2.nit,allyCompensation2.startDate,allyCompensation2.endDate)")
    List<Long> findListDuplicateIdsToDeleteByNitDate();
}
