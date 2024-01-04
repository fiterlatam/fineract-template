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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class GroupLoanAdditionalData {

    private Long id;
    private String facilitatorName;
    private Long facilitatorId;
    private Integer loanCycleCompleted;
    private String loanCycleCompletedValue;
    private Long earlyCancellationReason;
    private String earlyCancellationReasonValue;
    private Long sourceOfFunds;
    private String sourceOfFundsValue;
    private String clientLoanRequestNumber;
    private LocalDate dateRequested;
    private LocalDate dateOfBirth;
    private Long position;
    private String positionValue;
    private String fullName;
    private String lastName;
    private Long maritalStatus;
    private String maritalStatusValue;
    private Long educationLevel;
    private String educationLevelValue;
    private Integer schoolingYears;
    private Integer noOfChildren;
    private String nationality;
    private String language;
    private String dpi;
    private String nit;
    private Long jobType;
    private String jobTypeValue;
    private Long occupancyClassification;
    private String occupancyClassificationValue;
    private Long actsOwnBehalf;
    private String actsOwnBehalfValue;
    private String onBehalfOf;
    private String politicalPosition;
    private String politicalOffice;
    private Long housingType;
    private String housingTypeValue;
    private String address;
    private String populatedPlace;
    private String referencePoint;
    private String phoneNumber;
    private String relativeNumber;
    private Integer yearsInCommunity;
    private BigDecimal rentMortgageFee;
    private BigDecimal monthlyIncome;
    private BigDecimal familyExpenses;
    private BigDecimal totalExternalLoanAmount;
    private Integer totalInstallments;
    private Integer clientType;
    private String clientTypeValue;
    private String houseHoldGoods;
    private String businessActivities;
    private Long businessLocation;
    private String businessLocationValue;
    private Integer businessExperience;
    private String businessExperienceValue;
    private BigDecimal salesValue;
    private BigDecimal businessPurchases;
    private BigDecimal businessProfit;
    private BigDecimal clientProfit;
    private BigDecimal inventories;
    private Long visitBusiness;
    private String visitBusinessValue;
    private Long familySupport;
    private String familySupportValue;
    private Long businessEvolution;
    private String businessEvolutionValue;
    private Integer numberOfApprovals;
    private String recommenderName;
    private BigDecimal monthlyPaymentCapacity;
    private Long loanPurpose;
    private String loanPurposeValue;
    private BigDecimal currentCreditValue;
    private BigDecimal requestedValue;
    private BigDecimal groupAuthorizedValue;
    private BigDecimal facilitatorProposedValue;
    private BigDecimal proposedFee;
    private BigDecimal agencyAuthorizedAmount;
    private BigDecimal authorizedFee;
    private BigDecimal totalIncome;
    private BigDecimal totalExpenditures;
    private BigDecimal availableMonthly;
    private BigDecimal facValue;
    private BigDecimal debtLevel;
    private Collection<AdditionalsExtraLoansData> extraLoansData;
}
