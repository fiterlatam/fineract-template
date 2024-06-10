package org.apache.fineract.portfolio.charge.data;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

@Getter
@AllArgsConstructor
public class ChargeInsuranceDetailData {

    private final String insuranceType;
    private final String insuranceName;
    private final Long insuranceChargedAs;
    private final String insuranceCompany;
    private final String insurerName;
    private final Long insuranceCode;
    private final String insurancePlan;
    private final BigDecimal baseValue;
    private final BigDecimal vatValue;
    private final BigDecimal totalValue;
    private final Long deadline;
    private final List<EnumOptionData> insurancesChargedAsOptions;

}
