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
package org.apache.fineract.portfolio.loanaccount.loanschedule.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInterestRecalcualtionAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.FeeDetails;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;

public interface LoanScheduleModelPeriod {

    LoanSchedulePeriodData toData();

    boolean isRepaymentPeriod();

    boolean isDownPaymentPeriod();

    boolean isDisbursementPeriod();

    boolean isTotalGracePeriod();

    Integer periodNumber();

    LocalDate periodFromDate();

    LocalDate periodDueDate();

    BigDecimal principalDue();

    BigDecimal interestDue();

    BigDecimal feeChargesDue();

    BigDecimal penaltyChargesDue();

    void addLoanCharges(BigDecimal feeCharge, BigDecimal penaltyCharge);

    boolean isRecalculatedInterestComponent();

    void addPrincipalAmount(Money principalDue);

    void addInterestAmount(Money interestDue);

    Set<LoanInterestRecalcualtionAdditionalDetails> getLoanCompoundingDetails();

    void setEMIFixedSpecificToInstallmentTrue();

    boolean isEMIFixedSpecificToInstallment();

    BigDecimal rescheduleInterestPortion();

    void setRescheduleInterestPortion(BigDecimal rescheduleInterestPortion);

    BigDecimal getTotalHonorariosCharged();

    void setTotalHonorariosCharged(BigDecimal totalHonorariosCharged);

    BigDecimal getTotalAvalCharged();

    void setTotalAvalCharged(BigDecimal totalAvalCharged);

    BigDecimal getTotalVoluntaryInsuranceCharged();

    void setTotalVoluntaryInsuranceCharged(BigDecimal totalVoluntaryInsuranceCharged);

    BigDecimal getTotalMandatoryInsuranceCharged();

    void setTotalMandatoryInsuranceCharged(BigDecimal totalMandatoryInsuranceCharged);

    BigDecimal getTotalLifeInsuranceCharged();

    void setTotalLifeInsuranceCharged(BigDecimal totalLifeInsuranceCharged);

    boolean recalculateEMIForInstallment();

    public void setRecalculateEMIForInstallment(boolean recalculateEMIForInstallment);

    public boolean reduceTermForInstallment();

    public void setReduceTermForInstallment(boolean reduceTermForInstallment);

    public BigDecimal advancePrincipalAmountForInstallment();

    public void setAdvancePrincipalAmountForInstallment(BigDecimal advancePrincipalAmountForInstallment);

    public void setFeeDetails(Collection<FeeDetails> feeDetails);

    public Collection<FeeDetails> getFeeDetails();

}
