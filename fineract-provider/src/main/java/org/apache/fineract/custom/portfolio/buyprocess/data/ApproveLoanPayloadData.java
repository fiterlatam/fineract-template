package org.apache.fineract.custom.portfolio.buyprocess.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.tika.utils.StringUtils;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
public class ApproveLoanPayloadData {
    private String approvedOnDate;
    private String expectedDisbursementDate;
    private BigDecimal approvedLoanAmount;
    private String note = StringUtils.EMPTY;
    private String dateFormat;
    private String locale;
}
