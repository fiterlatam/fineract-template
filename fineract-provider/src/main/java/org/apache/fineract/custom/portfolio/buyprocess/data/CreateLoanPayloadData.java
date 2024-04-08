package org.apache.fineract.custom.portfolio.buyprocess.data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.tika.utils.StringUtils;

@Builder
@Getter
@Setter
public class CreateLoanPayloadData {

    private Long productId;
    private String loanOfficerId = StringUtils.EMPTY;
    private String loanPurposeId = StringUtils.EMPTY;
    private String fundId = StringUtils.EMPTY;
    private String submittedOnDate;
    private String expectedDisbursementDate;
    private String externalId = StringUtils.EMPTY;
    private String linkAccountId = StringUtils.EMPTY;
    private String createStandingInstructionAtDisbursement = StringUtils.EMPTY;
    private Long loanTermFrequency;
    private Integer loanTermFrequencyType;
    private Long numberOfRepayments;
    private Integer repaymentEvery;
    private Integer repaymentFrequencyType;
    private String repaymentFrequencyNthDayType = StringUtils.EMPTY;
    private String repaymentFrequencyDayOfWeekType = StringUtils.EMPTY;
    private String repaymentsStartingFromDate;
    private String interestChargedFromDate;
    private BigDecimal interestRatePerPeriod;
    private Integer interestType;
    private Boolean isEqualAmortization = Boolean.FALSE;
    private Integer amortizationType;
    private Integer interestCalculationPeriodType;
    private String loanIdToClose = StringUtils.EMPTY;
    private String isTopup = StringUtils.EMPTY;
    private String transactionProcessingStrategyCode;
    private List<String> charges = new ArrayList<>();
    private List<String> collateral = new ArrayList<>();;
    private String dateFormat;
    private String locale;
    private Long clientId;
    private String loanType;
    private BigDecimal principal;
}
