package org.apache.fineract.custom.portfolio.blockaccounts.service;

import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;

public interface LoanAccountBlockReadPlatformService {

    LoanAccountBlockDTO retrieveByLoanId(final Long loanId);
}
