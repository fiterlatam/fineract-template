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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.data;

import java.util.Collection;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;

/**
 * Immutable data object representing restructure credits request data.
 **/
public final class RestructureCreditsTemplateData {

    private final Long id;
    private final Long clientId;
    private final Boolean waiveInterest;
    private final Boolean waiveChargesAndFees;
    private final ClientData clientData;
    private final RestructureCreditsRequestData requestData;
    private final Collection<LoanAccountData> activeLoans;
    private final Collection<LoanProductData> loanProductData;
    private final Collection<GroupPrequalificationData> clientPrequalificatoins;
    private final Collection<Loan> loanAccounts;

    private RestructureCreditsTemplateData(Long id, Long clientId, ClientData clientData, Collection<LoanAccountData> activeLoans,
            RestructureCreditsRequestData requestData, Collection<LoanProductData> loanProductData,
            Collection<GroupPrequalificationData> groupPrequalificationData, Boolean waiveInterest, Boolean waiveChargesAndFees,
            Collection<Loan> loanAccounts) {

        this.id = id;
        this.clientId = clientId;
        this.clientData = clientData;
        this.activeLoans = activeLoans;
        this.loanAccounts = loanAccounts;
        this.requestData = requestData;
        this.loanProductData = loanProductData;
        this.clientPrequalificatoins = groupPrequalificationData;
        this.waiveInterest = waiveInterest;
        this.waiveChargesAndFees = waiveChargesAndFees;
    }

    /**
     * template of the restructure credits params
     *
     * @param clientData
     * @param activeLoans
     * @param requestData
     * @param loanProductData
     * @param groupPrequalificationData
     * @param loans
     * @return
     */
    public static RestructureCreditsTemplateData instance(ClientData clientData, Collection<LoanAccountData> activeLoans,
            RestructureCreditsRequestData requestData, Collection<LoanProductData> loanProductData,
            Collection<GroupPrequalificationData> groupPrequalificationData, Boolean waiveInterest, Boolean waiveChargesAndFees,
            Collection<Loan> loanAccounts) {

        return new RestructureCreditsTemplateData(null, null, clientData, activeLoans, requestData, loanProductData,
                groupPrequalificationData, waiveInterest, waiveChargesAndFees, loanAccounts);
    }
}
