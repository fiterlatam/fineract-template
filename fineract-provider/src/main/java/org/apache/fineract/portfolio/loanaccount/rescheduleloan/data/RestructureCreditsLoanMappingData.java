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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Immutable data object representing restructure credits request data.
 **/
public final class RestructureCreditsLoanMappingData {

    private final Long id;
    private final String loanProduct;
    private final BigDecimal outstandingBalance;
    private final LocalDate disbursementDate;
    private final LocalDate maturityDate;



    private RestructureCreditsLoanMappingData(
            final Long id,String loanProduct, BigDecimal outstandingBalance, LocalDate disbursementDate, LocalDate maturityDate) {

        this.id = id;
        this.loanProduct = loanProduct;
        this.maturityDate=maturityDate;
        this.outstandingBalance=outstandingBalance;
        this.disbursementDate=disbursementDate;
    }


    public static RestructureCreditsLoanMappingData instance(final Long id,String loanProduct, BigDecimal outstandingBalance, LocalDate disbursementDate, LocalDate maturityDate) {

        return new RestructureCreditsLoanMappingData(id, loanProduct,outstandingBalance,disbursementDate,maturityDate);
    }
}
