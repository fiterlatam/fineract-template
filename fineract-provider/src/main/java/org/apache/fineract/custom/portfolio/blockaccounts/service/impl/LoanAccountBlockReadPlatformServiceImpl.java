package org.apache.fineract.custom.portfolio.blockaccounts.service.impl;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlockRepository;
import org.apache.fineract.custom.portfolio.blockaccounts.mapper.LoanAccountBlockMapper;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanAccountBlockReadPlatformServiceImpl implements LoanAccountBlockReadPlatformService {

    private final LoanAccountBlockRepository loanAccountBlockRepository;
    private final LoanAccountBlockMapper loanAccountBlockMapper;

    @Override
    public LoanAccountBlockDTO retrieveByLoanId(Long loanId) {
        Optional<LoanAccountBlock> loanAccountBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);
        if (loanAccountBlock.isEmpty()) {
            throw new NotFoundException(String.valueOf(loanId));
        }
        return loanAccountBlockMapper.toDto(loanAccountBlock.get());
    }
}
