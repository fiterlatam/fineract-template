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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface AllyCollectionSettlementRepository
        extends JpaRepository<AllyCollectionSettlement, Long>, JpaSpecificationExecutor<AllyCollectionSettlement> {

    @Override
    Optional<AllyCollectionSettlement> findById(Long aLong);

    @Query(value = "SELECT * FROM m_ally_collection_settlement macs WHERE macs.loan_id = ? ORDER BY macs.collection_date DESC LIMIT 1", nativeQuery = true)
    Optional<AllyCollectionSettlement> findCollectionByLoanId(@Param("loanId") Long loanId);

    List<AllyCollectionSettlement> findByLoanIdAndCollectionDate(Long loanId, LocalDate collectionDate);

    List<AllyCollectionSettlement> findByClientAllyId(Long clientAllyId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AllyCollectionSettlement macs WHERE macs.loanId = :loanId and macs.collectionDate != :collectionDate")
    void deleteByLoanIdAndNotCollectionDate(@Param("loanId") Long loanId, @Param("collectionDate") LocalDate collectionDate);

}
