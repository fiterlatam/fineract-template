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

import org.apache.fineract.infrastructure.core.domain.AbstractAuditableWithUTCDateTimeCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.account.data.AccountTransferData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionEnumData;
import org.apache.fineract.portfolio.loanproduct.service.LoanEnumerations;
import org.apache.fineract.portfolio.paymentdetail.data.PaymentDetailData;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.useradministration.domain.AppUser;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * All monetary transactions against a loan are modelled through this entity. Disbursements, Repayments, Waivers,
 * Write-off etc
 */
@Entity
@Table(name = "m_loan_additionals_group")
public class GroupLoanAdditionals extends AbstractAuditableWithUTCDateTimeCustom {

    @ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "facilitator", nullable = false)
    private AppUser facilitator;
//            facilitator
//    position
//            full_name
//    last_name
//            marital_status
//    education_level
//            years_of_schooling
//    years_of_schooling
//            nationality
//    language
//            number_of_children
//    dpi
//            nit_status
//    nit
//            job_type
//    occupancy_classification
//            acts_own_behalf
//    acts_own_behalf
//            on_behalf_of
//    political_position
//            political_office
//    housing_type
//            rent_mortgage_fee
//    address
//            populated_place
//    reference_point
//            phone_number
//    relative_number
//            years_in_community
//    monthly_income
//            family_expenses
//    total_external_loan_amount
//            total_installments
//    client_type
//            house_hold_goods
//    business_activities
//            business_location
//    business_experience
//            sales_value
//    business_purchases
//            business_profit
//    client_profit
//            inventories
//    visit_business
//            family_support
//    business_evolution
//            number_of_approvals
//    recommender_name
//            monthly_payment_capacity
//    loan_purpose
//            current_credit_value
//    requested_value
//            group_authorized_value
//    facilitator_proposed_value
//            proposed_fee
//    agency_authorized_amount
//            authorized_fee
//    total_income
//            total_expenditures
//    available_monthly
//            payment_capacity
//    f_a_c
//            debt_level

    @Column(name = "loan_cycle_completed", nullable = false)
    private Integer loanCycleCompleted;

    @Column(name = "early_cancellation_reason")
    private Integer earlyCancellationReason;

    @Column(name = "source_of_funds")
    private Integer sourceOfFunds;

    @Column(name = "client_loan_request_number", nullable = false)
    private String clientLoanRequestNumber;

    @Column(name = "date_requested", nullable = false)
    private LocalDate dateRequested;

    @Column(name = "position")
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

    @Column(name = "job_type")
    private Long jobType;

    @Column(name = "occupancy_classification")
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
    private Long yearsInCommunity;

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

    @Column(name = "overpayment_portion_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal overPaymentPortion;

    @Column(name = "unrecognized_income_portion", scale = 6, precision = 19, nullable = true)
    private BigDecimal unrecognizedIncomePortion;


    @Column(name = "external_id", length = 100, nullable = true, unique = true)
    private String externalId;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "loanTransaction", orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<LoanChargePaidBy> loanChargesPaid = new HashSet<>();

    @Column(name = "outstanding_loan_balance_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal outstandingLoanBalance;

    @Column(name = "manually_adjusted_or_reversed", nullable = false)
    private boolean manuallyAdjustedOrReversed;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY, mappedBy = "loanTransaction")
    private Set<LoanCollateralManagement> loanCollateralManagementSet = new HashSet<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER, mappedBy = "loanTransaction")
    private Set<LoanTransactionToRepaymentScheduleMapping> loanTransactionToRepaymentScheduleMappings = new HashSet<>();

    protected GroupLoanAdditionals() {}

    // TODO missing hashCode(), equals(Object obj), but probably OK as long as
    // this is never stored in a Collection.
}
