package org.apache.fineract.custom.portfolio.buyprocess.data;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

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
    private List<String> charges;
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
}
