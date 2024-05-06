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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomChargeHonorarioMapRepository
        extends JpaRepository<CustomChargeHonorarioMap, Long>, JpaSpecificationExecutor<CustomChargeHonorarioMap> {

    // Optional<CustomChargeHonorarioMap> findByClientIdClientAllyIdLoanIdLoanInstallmentNr2(Long clientId, Long
    // clientAllyId, Long loanId, Integer loanInstallmentNr);

    String FIND_BY_CLIENT_ALLY_LOAN_INSTALLMENT_ACTIVE = "select cchm from CustomChargeHonorarioMap cchm where "
            + "cchm.clientId = :clientId and cchm.clientAllyId = :clientAllyId and cchm.loanId = :loanId "
            + "and cchm.loanInstallmentNr = :loanInstallmentNr and cchm.disabledBy = null";

    @Query(FIND_BY_CLIENT_ALLY_LOAN_INSTALLMENT_ACTIVE)
    Optional<CustomChargeHonorarioMap> findByClientIdClientAllyIdLoanIdLoanInstallmentNr(@Param("clientId") Long clientId,
            @Param("clientAllyId") Long clientAllyId, @Param("loanId") Long loanId, @Param("loanInstallmentNr") Integer loanInstallmentNr);
}
