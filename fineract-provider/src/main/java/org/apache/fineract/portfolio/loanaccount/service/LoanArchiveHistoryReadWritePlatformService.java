package org.apache.fineract.portfolio.loanaccount.service;

import java.util.List;
import org.apache.fineract.portfolio.loanaccount.data.LoanArchiveHistoryData;

public interface LoanArchiveHistoryReadWritePlatformService {

    List<LoanArchiveHistoryData> getLoanArchiveCollectionData();
}
