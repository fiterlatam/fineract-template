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

import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;

import java.util.Collection;

/**
 * Immutable data object representing restructure credits request data.
 **/
public final class RestructureCreditsTemplateData {

    private final Long id;
    private final Long clientId;
    private final ClientData clientData;
    private final RestructureCreditsRequestData requestData;
    private final Collection<LoanAccountData> activeLoans;
    private final Collection<LoanProductData> loanProductData;


    private RestructureCreditsTemplateData(Long id, Long clientId, ClientData clientData, Collection<LoanAccountData> activeLoans,
                                           RestructureCreditsRequestData requestData, Collection<LoanProductData> loanProductData) {

        this.id = id;
        this.clientId = clientId;
        this.clientData = clientData;
        this.activeLoans = activeLoans;
        this.requestData = requestData;
        this.loanProductData = loanProductData;
    }

    /**
     * template of the restructure credits params
     * @param clientData
     * @param activeLoans
     * @param requestData
     * @param loanProductData
     * @return
     */
    public static RestructureCreditsTemplateData instance(ClientData clientData, Collection<LoanAccountData> activeLoans, RestructureCreditsRequestData requestData, Collection<LoanProductData> loanProductData) {

        return new RestructureCreditsTemplateData(null, null, clientData, activeLoans, requestData,loanProductData);
    }
}
