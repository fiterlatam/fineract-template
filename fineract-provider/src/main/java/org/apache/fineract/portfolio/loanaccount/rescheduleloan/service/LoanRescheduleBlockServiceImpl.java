package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockLevel;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSetting;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingEnum;
import org.apache.fineract.infrastructure.clientblockingreasons.domain.BlockingReasonSettingsRepositoryWrapper;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.event.business.BusinessEventListener;
import org.apache.fineract.infrastructure.event.business.domain.loan.LoanRescheduledDueAdjustScheduleBusinessEvent;
import org.apache.fineract.infrastructure.event.business.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.service.LoanBlockWritePlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoanRescheduleBlockServiceImpl implements LoanRescheduleBlockService {

    private final BusinessEventNotifierService businessEventNotifierService;
    private final BlockingReasonSettingsRepositoryWrapper loanBlockingReasonRepository;
    private final LoanBlockWritePlatformService loanBlockWritePlatformService;

    @PostConstruct
    public void addEventListeners() {
        businessEventNotifierService.addPostBusinessEventListener(LoanRescheduledDueAdjustScheduleBusinessEvent.class,
                new LoanRescheduledDueAdjustScheduleBusinessEventListener());
    }

    private void updateLoanAndClientWithRestructureBlockDetail(final Loan loan) {

        final CodeValue productType = loan.loanProduct().getProductType();

        if (productType == null || !productType.getLabel().equals(LoanProductType.SUMAS_EMPRESSAS.getCode())) {
            BlockingReasonSetting setting = loanBlockingReasonRepository.getSingleBlockingReasonSettingByReason(
                    BlockingReasonSettingEnum.CREDIT_RESTRUCTURE.getDatabaseString(), BlockLevel.CREDIT.toString());
            loanBlockWritePlatformService.blockLoan(loan.getId(), setting, "Reestructurada", DateUtils.getLocalDateOfTenant());
        }
    }

    private final class LoanRescheduledDueAdjustScheduleBusinessEventListener
            implements BusinessEventListener<LoanRescheduledDueAdjustScheduleBusinessEvent> {

        @Override
        public void onBusinessEvent(LoanRescheduledDueAdjustScheduleBusinessEvent event) {
            final Loan loan = event.get();
            if (!event.getIsJobTriggered()) {
                updateLoanAndClientWithRestructureBlockDetail(loan);
            }
        }
    }
}
