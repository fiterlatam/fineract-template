package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;
import java.util.List;

@lombok.Builder
@lombok.Data
public class LoanApplicationData {

    private Long productId;
    private String loanOfficerId;
    private String loanPurposeId;
    private String fundId;
    private String submittedOnDate;
    private String expectedDisbursementDate;
    private String externalId;
    private String linkAccountId;
    private String createStandingInstructionAtDisbursement;
    private Integer loanTermFrequency;
    private Integer loanTermFrequencyType;
    private Integer numberOfRepayments;
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
    private Long interestRatePoints;
    private String clientIdNumber;
    private String pointOfSaleCode;
    private Boolean isWriteoffPunish;
    private Long allyId;
    private BigDecimal valorDescuento;
    private BigDecimal valorGiro;
    private Boolean isCloneLoan;

    // Approval fields
    private String approvedOnDate;
    private BigDecimal approvedLoanAmount;
    private String note;

    // Disbursement fields
    private String actualDisbursementDate;
    private BigDecimal transactionAmount;
    private String channelName;
    private String paymentTypeId;

    // Transaction fields
    private String transactionDate;
    private Boolean reduceInstallmentAmount;
    private String pointOfSalesCode;

}
