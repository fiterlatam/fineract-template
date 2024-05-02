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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

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

    @Column(name = "rent_fee", scale = 6, precision = 19, nullable = true)
    private BigDecimal rentFee;

    @Column(name = "mortgage_fee", scale = 6, precision = 19, nullable = true)
    private BigDecimal mortgageFee;

    @Column(name = "monthly_income", scale = 6, precision = 19, nullable = true)
    private BigDecimal monthlyIncome;

    @Column(name = "family_expenses", scale = 6, precision = 19, nullable = true)
    private BigDecimal familyExpenses;

    @Column(name = "total_external_loan_amount", scale = 6, precision = 19, nullable = true)
    private BigDecimal totalExternalLoanAmount;

    @Column(name = "total_installments")
    private Integer totalInstallments;

    @Column(name = "client_type")
    private Long clientType;

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

    @Column(name = "other_income", scale = 6, precision = 19, nullable = true)
    private BigDecimal otherIncome;

    @OneToMany(mappedBy = "groupLoanAdditionals", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<AdditionalsExtraLoans> extraLoans;

    protected GroupLoanAdditionals() {}

    public GroupLoanAdditionals(Integer loanCycleCompleted, BigDecimal rentFee, BigDecimal mortgageFee, BigDecimal monthlyIncome,
            BigDecimal familyExpenses, BigDecimal totalExternalLoanAmount, Integer totalInstallments, Long clientType,
            String houseHoldGoods, String businessActivities, Long businessLocation, Integer businessExperience, BigDecimal salesValue,
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
            String relativeNumber, Integer yearsInCommunity, Loan loan, LocalDate dateOfBirth, BigDecimal otherIncome) {

        this.loan = loan;
        this.facilitator = facilitator;
        this.loanCycleCompleted = loanCycleCompleted;
        this.earlyCancellationReason = earlyCancellationReason;
        this.sourceOfFunds = sourceOfFunds;
        this.clientLoanRequestNumber = clientLoanRequestNumber;
        this.dateRequested = dateRequested;
        this.dateOfBirth = dateOfBirth;
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
        this.rentFee = rentFee;
        this.mortgageFee = mortgageFee;
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
        this.otherIncome = otherIncome;
    }

    public static GroupLoanAdditionals assembleFromJson(JsonCommand command, Loan loan, AppUser facilitator) {

        Integer loanCycleCompleted = command.integerValueOfParameterNamed("loanCycleCompleted");
        BigDecimal rentFee = command.bigDecimalValueOfParameterNamed("rentFee");
        BigDecimal mortgageFee = command.bigDecimalValueOfParameterNamed("mortgageFee");
        BigDecimal monthlyIncome = command.bigDecimalValueOfParameterNamed("monthlyIncome");
        BigDecimal familyExpenses = command.bigDecimalValueOfParameterNamed("familyExpenses");
        BigDecimal totalExternalLoanAmount = command.bigDecimalValueOfParameterNamed("totalExternalLoanAmount");
        Integer totalInstallments = command.integerValueOfParameterNamed("totalInstallments");
        Long clientType = command.longValueOfParameterNamed("clientType");
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
        LocalDate dateOfBirth = command.localDateValueOfParameterNamed("dateOfBirth");
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
        // Long actsOwnBehalf = command.longValueOfParameterNamed("actsOwnBehalf");
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
        BigDecimal otherIncome = command.bigDecimalValueOfParameterNamed("otherIncome");

        return new GroupLoanAdditionals(loanCycleCompleted, rentFee, mortgageFee, monthlyIncome, familyExpenses, totalExternalLoanAmount,
                totalInstallments, clientType, houseHoldGoods, businessActivities, businessLocation, businessExperience, salesValue,
                businessPurchases, businessProfit, clientProfit, inventories, visitBusiness, familySupport, businessEvolution,
                numberOfApprovals, recommenderName, monthlyPaymentCapacity, loanPurpose, currentCreditValue, requestedValue,
                groupAuthorizedValue, facilitatorProposedValue, proposedFee, agencyAuthorizedAmount, authorizedFee, totalIncome,
                totalExpenditures, availableMonthly, facValue, debtLevel, facilitator, earlyCancellationReason, sourceOfFunds,
                clientLoanRequestNumber, dateRequested, position, fullName, lastName, maritalStatus, educationLevel, schoolingYears,
                noOfChildren, nationality, language, dpi, nit, jobType, occupancyClassification, null, onBehalfOf, politicalPosition,
                politicalOffice, housingType, address, populatedPlace, referencePoint, phoneNumber, relativeNumber, yearsInCommunity, loan,
                dateOfBirth, otherIncome);
    }

    public Loan getLoan() {
        return this.loan;
    }

    public void setExtraLoans(List<AdditionalsExtraLoans> extraLoans) {
        this.extraLoans = extraLoans;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>();

        if (command.isChangeInIntegerParameterNamed("loanCycleCompleted", this.loanCycleCompleted)) {
            final Integer newValue = command.integerValueOfParameterNamed("loanCycleCompleted");
            actualChanges.put("loanCycleCompleted", newValue);
            this.loanCycleCompleted = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("rentFee", this.rentFee)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("rentFee");
            actualChanges.put("rentFee", newValue);
            this.rentFee = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("mortgageFee", this.mortgageFee)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("mortgageFee");
            actualChanges.put("mortgageFee", newValue);
            this.mortgageFee = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("monthlyIncome", this.monthlyIncome)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("monthlyIncome");
            actualChanges.put("monthlyIncome", newValue);
            this.monthlyIncome = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("familyExpenses", this.familyExpenses)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("familyExpenses");
            actualChanges.put("familyExpenses", newValue);
            this.familyExpenses = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("totalExternalLoanAmount", this.totalExternalLoanAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("totalExternalLoanAmount");
            actualChanges.put("totalExternalLoanAmount", newValue);
            this.totalExternalLoanAmount = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("totalInstallments", this.totalInstallments)) {
            final Integer newValue = command.integerValueOfParameterNamed("totalInstallments");
            actualChanges.put("totalInstallments", newValue);
            this.totalInstallments = newValue;
        }
        if (command.isChangeInLongParameterNamed("clientType", this.clientType)) {
            final Long newValue = command.longValueOfParameterNamed("clientType");
            actualChanges.put("clientType", newValue);
            this.clientType = newValue;
        }
        if (command.isChangeInStringParameterNamed("houseHoldGoods", this.houseHoldGoods)) {
            final String newValue = command.stringValueOfParameterNamed("houseHoldGoods");
            actualChanges.put("houseHoldGoods", newValue);
            this.houseHoldGoods = newValue;
        }
        if (command.isChangeInStringParameterNamed("businessActivities", this.businessActivities)) {
            final String newValue = command.stringValueOfParameterNamed("businessActivities");
            actualChanges.put("businessActivities", newValue);
            this.businessActivities = newValue;
        }
        if (command.isChangeInLongParameterNamed("businessLocation", this.businessLocation)) {
            final Long newValue = command.longValueOfParameterNamed("businessLocation");
            actualChanges.put("businessLocation", newValue);
            this.businessLocation = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("businessExperience", this.businessExperience)) {
            final Integer newValue = command.integerValueOfParameterNamed("businessExperience");
            actualChanges.put("businessExperience", newValue);
            this.businessExperience = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("salesValue", this.salesValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("salesValue");
            actualChanges.put("salesValue", newValue);
            this.salesValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("businessPurchases", this.businessPurchases)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("businessPurchases");
            actualChanges.put("businessPurchases", newValue);
            this.businessPurchases = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("businessProfit", this.businessProfit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("businessProfit");
            actualChanges.put("businessProfit", newValue);
            this.businessProfit = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("clientProfit", this.clientProfit)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("clientProfit");
            actualChanges.put("clientProfit", newValue);
            this.clientProfit = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("inventories", this.inventories)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("inventories");
            actualChanges.put("inventories", newValue);
            this.inventories = newValue;
        }
        if (command.isChangeInLongParameterNamed("visitBusiness", this.visitBusiness)) {
            final Long newValue = command.longValueOfParameterNamed("visitBusiness");
            actualChanges.put("visitBusiness", newValue);
            this.visitBusiness = newValue;
        }
        if (command.isChangeInLongParameterNamed("familySupport", this.familySupport)) {
            final Long newValue = command.longValueOfParameterNamed("familySupport");
            actualChanges.put("familySupport", newValue);
            this.familySupport = newValue;
        }
        if (command.isChangeInLongParameterNamed("businessEvolution", this.businessEvolution)) {
            final Long newValue = command.longValueOfParameterNamed("businessEvolution");
            actualChanges.put("businessEvolution", newValue);
            this.businessEvolution = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("numberOfApprovals", this.numberOfApprovals)) {
            final Integer newValue = command.integerValueOfParameterNamed("numberOfApprovals");
            actualChanges.put("numberOfApprovals", newValue);
            this.numberOfApprovals = newValue;
        }
        if (command.isChangeInStringParameterNamed("recommenderName", this.recommenderName)) {
            final String newValue = command.stringValueOfParameterNamed("recommenderName");
            actualChanges.put("recommenderName", newValue);
            this.recommenderName = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("monthlyPaymentCapacity", this.monthlyPaymentCapacity)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("monthlyPaymentCapacity");
            actualChanges.put("monthlyPaymentCapacity", newValue);
            this.monthlyPaymentCapacity = newValue;
        }
        if (command.isChangeInLongParameterNamed("loanPurpose", this.loanPurpose)) {
            final Long newValue = command.longValueOfParameterNamed("loanPurpose");
            actualChanges.put("loanPurpose", newValue);
            this.loanPurpose = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("currentCreditValue", this.currentCreditValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("currentCreditValue");
            actualChanges.put("currentCreditValue", newValue);
            this.currentCreditValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("requestedValue", this.requestedValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("requestedValue");
            actualChanges.put("requestedValue", newValue);
            this.requestedValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("groupAuthorizedValue", this.groupAuthorizedValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("groupAuthorizedValue");
            actualChanges.put("groupAuthorizedValue", newValue);
            this.groupAuthorizedValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("facilitatorProposedValue", this.facilitatorProposedValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("facilitatorProposedValue");
            actualChanges.put("facilitatorProposedValue", newValue);
            this.facilitatorProposedValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("proposedFee", this.proposedFee)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("proposedFee");
            actualChanges.put("proposedFee", newValue);
            this.proposedFee = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("agencyAuthorizedAmount", this.agencyAuthorizedAmount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("agencyAuthorizedAmount");
            actualChanges.put("agencyAuthorizedAmount", newValue);
            this.agencyAuthorizedAmount = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("authorizedFee", this.authorizedFee)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("authorizedFee");
            actualChanges.put("authorizedFee", newValue);
            this.authorizedFee = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("totalIncome", this.totalIncome)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("totalIncome");
            actualChanges.put("totalIncome", newValue);
            this.totalIncome = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("totalExpenditures", this.totalExpenditures)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("totalExpenditures");
            actualChanges.put("totalExpenditures", newValue);
            this.totalExpenditures = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("availableMonthly", this.availableMonthly)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("availableMonthly");
            actualChanges.put("availableMonthly", newValue);
            this.availableMonthly = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("facValue", this.facValue)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("facValue");
            actualChanges.put("facValue", newValue);
            this.facValue = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("debtLevel", this.debtLevel)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("debtLevel");
            actualChanges.put("debtLevel", newValue);
            this.debtLevel = newValue;
        }
        if (command.isChangeInLongParameterNamed("earlyCancellationReason", this.earlyCancellationReason)) {
            final Long newValue = command.longValueOfParameterNamed("earlyCancellationReason");
            actualChanges.put("earlyCancellationReason", newValue);
            this.earlyCancellationReason = newValue;
        }
        if (command.isChangeInLongParameterNamed("sourceOfFunds", this.sourceOfFunds)) {
            final Long newValue = command.longValueOfParameterNamed("sourceOfFunds");
            actualChanges.put("sourceOfFunds", newValue);
            this.sourceOfFunds = newValue;
        }
        if (command.isChangeInStringParameterNamed("clientLoanRequestNumber", this.clientLoanRequestNumber)) {
            final String newValue = command.stringValueOfParameterNamed("clientLoanRequestNumber");
            actualChanges.put("clientLoanRequestNumber", newValue);
            this.clientLoanRequestNumber = newValue;
        }
        if (command.isChangeInLocalDateParameterNamed("dateRequested", this.dateRequested)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed("dateRequested");
            actualChanges.put("dateRequested", newValue);
            this.dateRequested = newValue;
        }
        if (command.isChangeInLocalDateParameterNamed("dateOfBirth", this.dateOfBirth)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed("dateOfBirth");
            actualChanges.put("dateOfBirth", newValue);
            this.dateOfBirth = newValue;
        }
        if (command.isChangeInLongParameterNamed("position", this.position)) {
            final Long newValue = command.longValueOfParameterNamed("position");
            actualChanges.put("position", newValue);
            this.position = newValue;
        }
        if (command.isChangeInStringParameterNamed("fullName", this.fullName)) {
            final String newValue = command.stringValueOfParameterNamed("fullName");
            actualChanges.put("fullName", newValue);
            this.fullName = newValue;
        }
        if (command.isChangeInStringParameterNamed("lastName", this.lastName)) {
            final String newValue = command.stringValueOfParameterNamed("lastName");
            actualChanges.put("lastName", newValue);
            this.lastName = newValue;
        }
        if (command.isChangeInLongParameterNamed("maritalStatus", this.maritalStatus)) {
            final Long newValue = command.longValueOfParameterNamed("maritalStatus");
            actualChanges.put("maritalStatus", newValue);
            this.maritalStatus = newValue;
        }
        if (command.isChangeInLongParameterNamed("educationLevel", this.educationLevel)) {
            final Long newValue = command.longValueOfParameterNamed("educationLevel");
            actualChanges.put("educationLevel", newValue);
            this.educationLevel = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("schoolingYears", this.schoolingYears)) {
            final Integer newValue = command.integerValueOfParameterNamed("schoolingYears");
            actualChanges.put("schoolingYears", newValue);
            this.schoolingYears = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("noOfChildren", this.noOfChildren)) {
            final Integer newValue = command.integerValueOfParameterNamed("noOfChildren");
            actualChanges.put("noOfChildren", newValue);
            this.noOfChildren = newValue;
        }
        if (command.isChangeInStringParameterNamed("nationality", this.nationality)) {
            final String newValue = command.stringValueOfParameterNamed("nationality");
            actualChanges.put("nationality", newValue);
            this.nationality = newValue;
        }
        if (command.isChangeInStringParameterNamed("language", this.language)) {
            final String newValue = command.stringValueOfParameterNamed("language");
            actualChanges.put("language", newValue);
            this.language = newValue;
        }
        if (command.isChangeInStringParameterNamed("dpi", this.dpi)) {
            final String newValue = command.stringValueOfParameterNamed("dpi");
            actualChanges.put("dpi", newValue);
            this.dpi = newValue;
        }
        if (command.isChangeInStringParameterNamed("nit", this.nit)) {
            final String newValue = command.stringValueOfParameterNamed("nit");
            actualChanges.put("nit", newValue);
            this.nit = newValue;
        }
        if (command.isChangeInLongParameterNamed("jobType", this.jobType)) {
            final Long newValue = command.longValueOfParameterNamed("jobType");
            actualChanges.put("jobType", newValue);
            this.jobType = newValue;
        }
        if (command.isChangeInLongParameterNamed("occupancyClassification", this.occupancyClassification)) {
            final Long newValue = command.longValueOfParameterNamed("occupancyClassification");
            actualChanges.put("occupancyClassification", newValue);
            this.occupancyClassification = newValue;
        }
        if (command.isChangeInStringParameterNamed("onBehalfOf", this.onBehalfOf)) {
            final String newValue = command.stringValueOfParameterNamed("onBehalfOf");
            actualChanges.put("onBehalfOf", newValue);
            this.onBehalfOf = newValue;
        }
        if (command.isChangeInStringParameterNamed("politicalPosition", this.politicalPosition)) {
            final String newValue = command.stringValueOfParameterNamed("politicalPosition");
            actualChanges.put("politicalPosition", newValue);
            this.politicalPosition = newValue;
        }
        if (command.isChangeInStringParameterNamed("politicalOffice", this.politicalOffice)) {
            final String newValue = command.stringValueOfParameterNamed("politicalOffice");
            actualChanges.put("politicalOffice", newValue);
            this.politicalOffice = newValue;
        }
        if (command.isChangeInLongParameterNamed("housingType", this.housingType)) {
            final Long newValue = command.longValueOfParameterNamed("housingType");
            actualChanges.put("housingType", newValue);
            this.housingType = newValue;
        }
        if (command.isChangeInStringParameterNamed("address", this.address)) {
            final String newValue = command.stringValueOfParameterNamed("address");
            actualChanges.put("address", newValue);
            this.address = newValue;
        }
        if (command.isChangeInStringParameterNamed("populatedPlace", this.populatedPlace)) {
            final String newValue = command.stringValueOfParameterNamed("populatedPlace");
            actualChanges.put("populatedPlace", newValue);
            this.populatedPlace = newValue;
        }
        if (command.isChangeInStringParameterNamed("referencePoint", this.referencePoint)) {
            final String newValue = command.stringValueOfParameterNamed("referencePoint");
            actualChanges.put("referencePoint", newValue);
            this.referencePoint = newValue;
        }
        if (command.isChangeInStringParameterNamed("phoneNumber", this.phoneNumber)) {
            final String newValue = command.stringValueOfParameterNamed("phoneNumber");
            actualChanges.put("phoneNumber", newValue);
            this.phoneNumber = newValue;
        }
        if (command.isChangeInStringParameterNamed("relativeNumber", this.relativeNumber)) {
            final String newValue = command.stringValueOfParameterNamed("relativeNumber");
            actualChanges.put("relativeNumber", newValue);
            this.relativeNumber = newValue;
        }
        if (command.isChangeInIntegerParameterNamed("yearsInCommunity", this.yearsInCommunity)) {
            final Integer newValue = command.integerValueOfParameterNamed("yearsInCommunity");
            actualChanges.put("yearsInCommunity", newValue);
            this.yearsInCommunity = newValue;
        }
        if (command.isChangeInBigDecimalParameterNamed("otherIncome", this.otherIncome)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed("otherIncome");
            actualChanges.put("otherIncome", newValue);
            this.otherIncome = newValue;
        }

        return actualChanges;
    }

    public void updateClientType(Long clientType) {
        this.clientType = clientType;
    }
}
