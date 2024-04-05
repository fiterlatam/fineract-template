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
package org.apache.fineract.custom.portfolio.buyprocess.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface  ChannelRepository
        extends JpaRepository< Channel, Long>, JpaSpecificationExecutor< Channel> {

    Optional<Channel> findByHash(String hash);

	/*
   String FIND_NON_CLOSED_LOAN_THAT_BELONGS_TO_CLIENT = "select loan from Loan loan where loan.id = :loanId and loan.loanStatus = 300 and loan.client.id = :clientId";

    @Query(FIND_GROUP_LOANS_DISBURSED_AFTER)
    List<Loan> getGroupLoansDisbursedAfter(@Param("disbursementDate") Date disbursementDate, @Param("groupId") Long groupId,
            @Param("loanType") Integer loanType);
	*/

}
