package org.apache.fineract.custom.portfolio.blockaccounts.api;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class LoanAccountBlockConstants {

    public LoanAccountBlockConstants() {}

    public static final String loanIdParamName = "loanId";
    public static final String applicationDateParamName = "applicationDate";
    public static final String blockingReasonIdParamName = "blockingReasonId";
    public static final String accelerateParamName = "accelerate";
    public static final String freezeCurrentInterestParamName = "freezeCurrentInterest";
    public static final String freezeInterestArrearsParamName = "freezeInterestArrears";
    public static final String freezeLifeInsuranceParamName = "freezeLifeInsurance";
    public static final String freezeMypimeParamName = "freezeMypime";
    public static final String activeParamName = "active";
    public static final String dateFormatParamName = "dateFormat";
    public static final String localeParamName = "locale";

    public static final Set<String> REQUEST_DATA_PARAMETERS = new HashSet<>(Arrays.asList(loanIdParamName, applicationDateParamName,
            blockingReasonIdParamName, accelerateParamName, freezeCurrentInterestParamName, freezeInterestArrearsParamName,
            freezeLifeInsuranceParamName, freezeMypimeParamName, activeParamName, localeParamName, dateFormatParamName));

}
