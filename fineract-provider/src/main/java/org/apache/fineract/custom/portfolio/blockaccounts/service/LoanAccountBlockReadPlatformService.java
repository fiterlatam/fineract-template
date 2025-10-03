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
package org.apache.fineract.custom.portfolio.blockaccounts.service;

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockData;

public interface LoanAccountBlockReadPlatformService {

    LoanAccountBlockDTO retrieveByLoanId(final Long loanId);

    List<LoanAccountBlockDTO> retrieveHistoryByLoanId(final Long loanId);

    LoanAccountBlockDTO retrieveByLoanIdWithoutException(final Long loanId);

    LoanAccountBlockData checkBlockAccountComponents(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountDisbursal(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountAccelerate(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountFreezeInterest(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountFreezeMora(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountFreezeLifeInsurance(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountFreezeMipyme(Long loanId, LocalDate givenDate);

    boolean containsBlockAccountFreezeGAC(Long loanId, LocalDate givenDate);
}
