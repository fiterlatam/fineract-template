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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.useradministration.domain.AppUser;

/**
 * All monetary transactions against a loan are modelled through this entity. Disbursements, Repayments, Waivers,
 * Write-off etc
 */
@Entity
@Table(name = "m_loan_additionals_group")
public class GroupLoanAdditionals extends AbstractPersistableCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facilitator", nullable = false)
    private AppUser facilitator;

    @Column(name = "loan_cycle_completed", nullable = false)
    private Integer loanCycleCompleted;

    @Column(name = "early_cancellation_reason")
    private Long earlyCancellationReason;

    @Column(name = "source_of_funds")
    private Long sourceOfFunds;

    @Column(name = "client_loan_request_number", nullable = false)
    private String clientLoanRequestNumber;

    @Column(name = "date_requested", nullable = false)
    private LocalDate dateRequested;

    @Column(name = "position", nullable = false)
    private Long position;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "marital_status")
    private Long maritalStatus;

    @Column(name = "education_level")
    private Long educationLevel;

    @Column(name = "years_of_schooling")
    private Integer schoolingYears;

    @Column(name = "number_of_children")
    private Integer noOfChildren;

    @Column(name = "nationality")
    private String nationality;

    @Column(name = "language")
    private String language;

    @Column(name = "dpi")
    private String dpi;

    @Column(name = "nit")
    private String nit;

    @Column(name = "job_type", nullable = false)
    private Long jobType;

    @Column(name = "occupancy_classification", nullable = false)
    private Long occupancyClassification;

    @Column(name = "acts_own_behalf")
    private Long actsOwnBehalf;

    @Column(name = "on_behalf_of")
    private String onBehalfOf;

    @Column(name = "political_position")
    private String politicalPosition;

    @Column(name = "political_office")
    private String politicalOffice;

    @Column(name = "housing_type")
    private Long housingType;

    @Column(name = "address")
    private String address;

    @Column(name = "populated_place")
    private String populatedPlace;

    @Column(name = "reference_point")
    private String referencePoint;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "relative_number")
    private String relativeNumber;

    @Column(name = "years_in_community")
    private Integer yearsInCommunity;

    @Column(name = "rent_mortgage_fee", scale = 6, precision = 19, nullable = true)
    private BigDecimal rentMortgageFee;

    @Column(name = "monthly_income", scale = 6, precision = 19, nullable = true)
    private BigDecimal monthlyIncome;

    @Column(name = "family_expenses", scale = 6, precision = 19, nullable = true)
    private BigDecimal familyExpenses;

    @Column(name = "total_external_loan_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalExternalLoanAmount;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "client_type")
    private Integer clientType;

    @Column(name = "house_hold_goods")
    private String houseHoldGoods;

    @Column(name = "business_activities")
    private String businessActivities;

    @Column(name = "business_location")
    private Long businessLocation;

    @Column(name = "business_experience")
    private Integer businessExperience;

    @Column(name = "sales_value", scale = 6, precision = 19, nullable = true)
    private BigDecimal salesValue;

    @Column(name = "business_purchases", scale = 6, precision = 19, nullable = true)
    private BigDecimal businessPurchases;

    @Column(name = "business_profit", scale = 6, precision = 19, nullable = true)
    private BigDecimal businessProfit;

    @Column(name = "client_profit", scale = 6, precision = 19, nullable = true)
    private BigDecimal clientProfit;

    @Column(name = "inventories", scale = 6, precision = 19, nullable = true)
    private BigDecimal inventories;

    @Column(name = "visit_business")
    private Long visitBusiness;

    @Column(name = "family_support")
    private Long familySupport;

    @Column(name = "business_evolution")
    private Long businessEvolution;

    @Column(name = "number_of_approvals")
    private Integer numberOfApprovals;

    @Column(name = "recommender_name")
    private String recommenderName;

    @Column(name = "monthly_payment_capacity", scale = 6, precision = 19, nullable = true)
    private BigDecimal monthlyPaymentCapacity;

    @Column(name = "loan_purpose")
    private Long loanPurpose;

    @Column(name = "current_credit_value", scale = 6, precision = 19, nullable = true)
    private BigDecimal currentCreditValue;

    @Column(name = "requested_value", scale = 6, precision = 19, nullable = true)
    private BigDecimal requestedValue;

    @Column(name = "group_authorized_value", scale = 6, precision = 19, nullable = true)
    private BigDecimal groupAuthorizedValue;

    @Column(name = "facilitator_proposed_value", scale = 6, precision = 19, nullable = true)
    private BigDecimal facilitatorProposedValue;

    @Column(name = "proposed_fee", scale = 6, precision = 19, nullable = true)
    private BigDecimal proposedFee;

    @Column(name = "agency_authorized_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal agencyAuthorizedAmount;

    @Column(name = "authorized_fee", scale = 6, precision = 19, nullable = true)
    private BigDecimal authorizedFee;

    @Column(name = "total_income", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalIncome;

    @Column(name = "total_expenditures", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalExpenditures;

    @Column(name = "available_monthly", scale = 6, precision = 19, nullable = true)
    private BigDecimal availableMonthly;

    @Column(name = "f_a_c", scale = 6, precision = 19, nullable = true)
    private BigDecimal facValue;

    @Column(name = "debt_level", scale = 6, precision = 19, nullable = true)
    private BigDecimal debtLevel;

    @OneToMany(mappedBy = "groupLoanAdditionals", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AdditionalsExtraLoans> extraLoans;

    protected GroupLoanAdditionals() {}

    public GroupLoanAdditionals(Integer loanCycleCompleted, BigDecimal rentMortgageFee, BigDecimal monthlyIncome, BigDecimal familyExpenses,
                                BigDecimal totalExternalLoanAmount, Integer totalInstallments, Integer clientType, String houseHoldGoods,
                                String businessActivities, Long businessLocation, Integer businessExperience, BigDecimal salesValue,
                                BigDecimal businessPurchases, BigDecimal businessProfit, BigDecimal clientProfit, BigDecimal inventories, Long visitBusiness,
                                Long familySupport, Long businessEvolution, Integer numberOfApprovals, String recommenderName,
                                BigDecimal monthlyPaymentCapacity, Long loanPurpose, BigDecimal currentCreditValue, BigDecimal requestedValue,
                                BigDecimal groupAuthorizedValue, BigDecimal facilitatorProposedValue, BigDecimal proposedFee, BigDecimal agencyAuthorizedAmount,
                                BigDecimal authorizedFee, BigDecimal totalIncome, BigDecimal totalExpenditures, BigDecimal availableMonthly,
                                BigDecimal facValue, BigDecimal debtLevel, AppUser facilitator, Long earlyCancellationReason, Long sourceOfFunds,
                                String clientLoanRequestNumber, LocalDate dateRequested, Long position, String fullName, String lastName, Long maritalStatus,
                                Long educationLevel, Integer schoolingYears, Integer noOfChildren, String nationality, String language, String dpi, String nit,
                                Long jobType, Long occupancyClassification, Long actsOwnBehalf, String onBehalfOf, String politicalPosition,
                                String politicalOffice, Long housingType, String address, String populatedPlace, String referencePoint, String phoneNumber,
                                String relativeNumber, Integer yearsInCommunity, Loan loan) {

        this.loan = loan;
        this.facilitator = facilitator;
        this.loanCycleCompleted = loanCycleCompleted;
        this.earlyCancellationReason = earlyCancellationReason;
        this.sourceOfFunds = sourceOfFunds;
        this.clientLoanRequestNumber = clientLoanRequestNumber;
        this.dateRequested = dateRequested;
        this.position = position;
        this.fullName = fullName;
        this.lastName = lastName;
        this.maritalStatus = maritalStatus;
        this.educationLevel = educationLevel;
        this.schoolingYears = schoolingYears;
        this.noOfChildren = noOfChildren;
        this.nationality = nationality;
        this.language = language;
        this.dpi = dpi;
        this.nit = nit;
        this.jobType = jobType;
        this.occupancyClassification = occupancyClassification;
        this.actsOwnBehalf = actsOwnBehalf;
        this.onBehalfOf = onBehalfOf;
        this.politicalPosition = politicalPosition;
        this.politicalOffice = politicalOffice;
        this.housingType = housingType;
        this.address = address;
        this.populatedPlace = populatedPlace;
        this.referencePoint = referencePoint;
        this.phoneNumber = phoneNumber;
        this.relativeNumber = relativeNumber;
        this.yearsInCommunity = yearsInCommunity;
        this.rentMortgageFee = rentMortgageFee;
        this.monthlyIncome = monthlyIncome;
        this.familyExpenses = familyExpenses;
        this.totalExternalLoanAmount = totalExternalLoanAmount;
        this.totalInstallments = totalInstallments;
        this.clientType = clientType;
        this.houseHoldGoods = houseHoldGoods;
        this.businessActivities = businessActivities;
        this.businessLocation = businessLocation;
        this.businessExperience = businessExperience;
        this.salesValue = salesValue;
        this.businessPurchases = businessPurchases;
        this.businessProfit = businessProfit;
        this.clientProfit = clientProfit;
        this.inventories = inventories;
        this.visitBusiness = visitBusiness;
        this.familySupport = familySupport;
        this.businessEvolution = businessEvolution;
        this.numberOfApprovals = numberOfApprovals;
        this.recommenderName = recommenderName;
        this.monthlyPaymentCapacity = monthlyPaymentCapacity;
        this.loanPurpose = loanPurpose;
        this.currentCreditValue = currentCreditValue;
        this.requestedValue = requestedValue;
        this.groupAuthorizedValue = groupAuthorizedValue;
        this.facilitatorProposedValue = facilitatorProposedValue;
        this.proposedFee = proposedFee;
        this.agencyAuthorizedAmount = agencyAuthorizedAmount;
        this.authorizedFee = authorizedFee;
        this.totalIncome = totalIncome;
        this.totalExpenditures = totalExpenditures;
        this.availableMonthly = availableMonthly;
        this.facValue = facValue;
        this.debtLevel = debtLevel;

    }

    public static GroupLoanAdditionals assembleFromJson(JsonCommand command, Loan loan, AppUser facilitator) {

        Integer loanCycleCompleted = command.integerValueOfParameterNamed("loanCycleCompleted");
        BigDecimal rentMortgageFee = command.bigDecimalValueOfParameterNamed("rentMortgageFee");
        BigDecimal monthlyIncome = command.bigDecimalValueOfParameterNamed("monthlyIncome");
        BigDecimal familyExpenses = command.bigDecimalValueOfParameterNamed("familyExpenses");
        BigDecimal totalExternalLoanAmount = command.bigDecimalValueOfParameterNamed("totalExternalLoanAmount");
        Integer totalInstallments = command.integerValueOfParameterNamed("totalInstallments");
        Integer clientType = command.integerValueOfParameterNamed("clientType");
        String houseHoldGoods = command.stringValueOfParameterNamed("houseHoldGoods");
        String businessActivities = command.stringValueOfParameterNamed("businessActivities");
        Long businessLocation = command.longValueOfParameterNamed("businessLocation");
        Integer businessExperience = command.integerValueOfParameterNamed("businessExperience");
        BigDecimal salesValue = command.bigDecimalValueOfParameterNamed("salesValue");
        BigDecimal businessPurchases = command.bigDecimalValueOfParameterNamed("businessPurchases");
        BigDecimal businessProfit = command.bigDecimalValueOfParameterNamed("businessProfit");
        BigDecimal clientProfit = command.bigDecimalValueOfParameterNamed("clientProfit");
        BigDecimal inventories = command.bigDecimalValueOfParameterNamed("inventories");
        Long visitBusiness = command.longValueOfParameterNamed("visitBusiness");
        Long familySupport = command.longValueOfParameterNamed("familySupport");
        Long businessEvolution = command.longValueOfParameterNamed("businessEvolution");
        Integer numberOfApprovals = command.integerValueOfParameterNamed("numberOfApprovals");
        String recommenderName = command.stringValueOfParameterNamed("recommenderName");
        BigDecimal monthlyPaymentCapacity = command.bigDecimalValueOfParameterNamed("monthlyPaymentCapacity");
        Long loanPurpose = command.longValueOfParameterNamed("loanPurpose");
        BigDecimal currentCreditValue = command.bigDecimalValueOfParameterNamed("currentCreditValue");
        BigDecimal requestedValue = command.bigDecimalValueOfParameterNamed("requestedValue");
        BigDecimal groupAuthorizedValue = command.bigDecimalValueOfParameterNamed("groupAuthorizedValue");
        BigDecimal facilitatorProposedValue = command.bigDecimalValueOfParameterNamed("facilitatorProposedValue");
        BigDecimal proposedFee = command.bigDecimalValueOfParameterNamed("proposedFee");
        BigDecimal agencyAuthorizedAmount = command.bigDecimalValueOfParameterNamed("agencyAuthorizedAmount");
        BigDecimal authorizedFee = command.bigDecimalValueOfParameterNamed("authorizedFee");
        BigDecimal totalIncome = command.bigDecimalValueOfParameterNamed("totalIncome");
        BigDecimal totalExpenditures = command.bigDecimalValueOfParameterNamed("totalExpenditures");
        BigDecimal availableMonthly = command.bigDecimalValueOfParameterNamed("availableMonthly");
        BigDecimal facValue = command.bigDecimalValueOfParameterNamed("facValue");
        BigDecimal debtLevel = command.bigDecimalValueOfParameterNamed("debtLevel");
        Long earlyCancellationReason = command.longValueOfParameterNamed("earlyCancellationReason");
        Long sourceOfFunds = command.longValueOfParameterNamed("sourceOfFunds");
        String clientLoanRequestNumber = command.stringValueOfParameterNamed("clientLoanRequestNumber");
        LocalDate dateRequested = command.localDateValueOfParameterNamed("dateRequested");
        Long position = command.longValueOfParameterNamed("position");
        String fullName = command.stringValueOfParameterNamed("fullName");
        String lastName = command.stringValueOfParameterNamed("lastName");
        Long maritalStatus = command.longValueOfParameterNamed("maritalStatus");
        Long educationLevel = command.longValueOfParameterNamed("educationLevel");
        Integer schoolingYears = command.integerValueOfParameterNamed("schoolingYears");
        Integer noOfChildren = command.integerValueOfParameterNamed("noOfChildren");
        String nationality = command.stringValueOfParameterNamed("nationality");
        String language = command.stringValueOfParameterNamed("language");
        String dpi = command.stringValueOfParameterNamed("dpi");
        String nit = command.stringValueOfParameterNamed("nit");
        Long jobType = command.longValueOfParameterNamed("jobType");
        Long occupancyClassification = command.longValueOfParameterNamed("occupancyClassification");
//        Long actsOwnBehalf = command.longValueOfParameterNamed("actsOwnBehalf");
        String onBehalfOf = command.stringValueOfParameterNamed("onBehalfOf");
        String politicalPosition = command.stringValueOfParameterNamed("politicalPosition");
        String politicalOffice = command.stringValueOfParameterNamed("politicalOffice");
        Long housingType = command.longValueOfParameterNamed("housingType");
        String address = command.stringValueOfParameterNamed("address");
        String populatedPlace = command.stringValueOfParameterNamed("populatedPlace");
        String referencePoint = command.stringValueOfParameterNamed("referencePoint");
        String phoneNumber = command.stringValueOfParameterNamed("phoneNumber");
        String relativeNumber = command.stringValueOfParameterNamed("relativeNumber");
        Integer yearsInCommunity = command.integerValueOfParameterNamed("yearsInCommunity");

        return new GroupLoanAdditionals(loanCycleCompleted, rentMortgageFee, monthlyIncome, familyExpenses, totalExternalLoanAmount,
                totalInstallments, clientType, houseHoldGoods, businessActivities, businessLocation, businessExperience, salesValue,
                businessPurchases, businessProfit, clientProfit, inventories, visitBusiness, familySupport, businessEvolution,
                numberOfApprovals, recommenderName, monthlyPaymentCapacity, loanPurpose, currentCreditValue, requestedValue,
                groupAuthorizedValue, facilitatorProposedValue, proposedFee, agencyAuthorizedAmount, authorizedFee, totalIncome,
                totalExpenditures, availableMonthly, facValue, debtLevel, facilitator, earlyCancellationReason, sourceOfFunds,
                clientLoanRequestNumber, dateRequested, position, fullName, lastName, maritalStatus, educationLevel, schoolingYears,
                noOfChildren, nationality, language, dpi, nit, jobType, occupancyClassification, null, onBehalfOf,
                politicalPosition, politicalOffice, housingType, address, populatedPlace, referencePoint, phoneNumber, relativeNumber,
                yearsInCommunity, loan);
    }

    public Loan getLoan() {
        return this.loan;
    }

    public void setExtraLoans(List<AdditionalsExtraLoans> extraLoans) {
        this.extraLoans = extraLoans;
    }
}
