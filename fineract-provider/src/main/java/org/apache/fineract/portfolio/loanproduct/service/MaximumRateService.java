package org.apache.fineract.portfolio.loanproduct.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.portfolio.loanproduct.data.MaximumCreditRateConfigurationData;
import org.apache.fineract.portfolio.loanproduct.domain.MaximumCreditRateConfiguration;
import org.apache.fineract.portfolio.loanproduct.domain.MaximumRateRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaximumRateService {
    private final MaximumRateRepository maximumRateRepository;

    public MaximumCreditRateConfigurationData findByProductType(Long productTypeCVid) {
        MaximumCreditRateConfiguration maximumCreditRateConfiguration = this.maximumRateRepository.findAllByProductTypeCv_Id(productTypeCVid).stream().findFirst().orElse(null);
        if (maximumCreditRateConfiguration == null) return null;
        return MaximumCreditRateConfigurationData.builder().eaRate(maximumCreditRateConfiguration.getEaRate())
                .dailyNominalRate(maximumCreditRateConfiguration.getDailyNominalRate())
                .productTypeId(maximumCreditRateConfiguration.getProductTypeCv().getId())
                .annualNominalRate(maximumCreditRateConfiguration.getAnnualNominalRate())
                .appliedOnDate(maximumCreditRateConfiguration.getAppliedOnDate())
                .monthlyNominalRate(maximumCreditRateConfiguration.getMonthlyNominalRate())
                .overdueInterestRate(maximumCreditRateConfiguration.getOverdueInterestRate())
                .currentInterestRate(maximumCreditRateConfiguration.getCurrentInterestRate()).build();
    }
}
