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
package org.apache.fineract.custom.portfolio.buyprocess.data;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;

@Builder
@Getter
@Setter
public class CreateLoanPayloadData {

    private Long productId;
    private String loanOfficerId;
    private String loanPurposeId;
    private String fundId;
    private String submittedOnDate;
    private String expectedDisbursementDate;
    private String externalId;
    private String linkAccountId;
    private String createStandingInstructionAtDisbursement;
    private Long loanTermFrequency;
    private Integer loanTermFrequencyType;
    private Long numberOfRepayments;
    private Integer repaymentEvery;
    private Integer repaymentFrequencyType;
    private String repaymentFrequencyNthDayType;
    private String repaymentFrequencyDayOfWeekType;
    private String repaymentsStartingFromDate;
    private String interestChargedFromDate;
    private BigDecimal interestRatePerPeriod;
    private Integer interestType;
    private Boolean isEqualAmortization;
    private Integer amortizationType;
    private Integer interestCalculationPeriodType;
    private String loanIdToClose;
    private String isTopup;
    private String transactionProcessingStrategyCode;
    private List<LoanChargeData> charges;
    private List<String> collateral;
    private String dateFormat;
    private String locale;
    private Long clientId;
    private String loanType;
    private BigDecimal principal;
    private Integer graceOnPrincipalPayment;
    private Integer graceOnInterestPayment;
    private Integer graceOnInterestCharged;
    private Integer interestRatePoints;
    private String clientIdNumber;
    private String pointOfSaleCode;
    private boolean isWriteoffPunish;
}
