package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.util.List;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;

public interface LoanArchiveHistoryReadWritePlatformService {

    List<LoanArchiveHistoryData> getLoanArchiveCollectionData();
}
