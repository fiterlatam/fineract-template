package org.apache.fineract.custom.portfolio.buyprocess.data;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.tika.utils.StringUtils;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
public class DisburseLoanPayloadData {

    /*
{
    "actualDisbursementDate": "04 abril 2024",
    "transactionAmount": 100,
    "externalId": "",
    "paymentTypeId": "",
    "note": "",
    "dateFormat": "dd MMMM yyyy",
    "locale": "es"
}
     */

    private String actualDisbursementDate;
    private BigDecimal transactionAmount;
    private String externalId = StringUtils.EMPTY;
    private String paymentTypeId = StringUtils.EMPTY;
    private String note = StringUtils.EMPTY;
    private String dateFormat;
    private String locale;

}
