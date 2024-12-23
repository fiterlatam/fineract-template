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
package org.apache.fineract.portfolio.insurance.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface InsuranceIncidentNoveltyNewsRepository
        extends JpaRepository<InsuranceIncidentNoveltyNews, Long>, JpaSpecificationExecutor<InsuranceIncidentNoveltyNews> {

    @Query(nativeQuery = true, value = "select * from m_insurance_novelty_news where loan_id = ?1 and novelty_id in (?2, ?3) order by id desc limit 1")
    Optional<InsuranceIncidentNoveltyNews> findLastSuspensionIfPresent(Long loanId, Long suspensionId, Long suspensionExitId);

    @Query(nativeQuery = true, value = "select exists (select 1 from m_insurance_novelty_news where loan_id = ?1 and novelty_id = ?2)")
    boolean existsByLoanAndIncident(Long loanId, Long noveltyId);

}
