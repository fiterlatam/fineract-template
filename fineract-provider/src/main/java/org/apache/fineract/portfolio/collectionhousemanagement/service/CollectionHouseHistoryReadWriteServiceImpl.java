package org.apache.fineract.portfolio.collectionhousemanagement.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.portfolio.accountdetails.data.AccountSummaryCollectionData;
import org.apache.fineract.portfolio.accountdetails.data.LoanAccountSummaryData;
import org.apache.fineract.portfolio.accountdetails.service.AccountDetailsReadPlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseHistoryValidator;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdate;
import org.apache.fineract.portfolio.collectionhousemanagement.data.CollectionHouseUpdates;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.CollectionHouseHistoryRepository;
import org.apache.fineract.portfolio.collectionhousemanagement.domain.ColletionHouseHistory;
import org.apache.fineract.portfolio.loanaccount.data.LoanStatusEnumData;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

@Service
@Slf4j
@RequiredArgsConstructor
public class CollectionHouseHistoryReadWriteServiceImpl implements CollectionHouseHistoryReadWriteService {

    private final CollectionHouseHistoryValidator collectionHouseHistoryValidator;
    private final CollectionHouseHistoryRepository collectionHouseHistoryRepository;
    private final AccountDetailsReadPlatformService accountDetailsReadPlatformService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final Retrofit retrofit;
    private final CollectionHouseHistoryExternalRetrofitConfig collectionHouseHistoryExternalRetrofitConfig;

    @Override
    public CommandProcessingResult createCollectionHouseHistory(JsonCommand command) {
        collectionHouseHistoryValidator.validateForCreateCollectionHouse(command.json());
        JsonArray updatesArray = command.arrayOfParameterNamed("collectionHouseUpdates");
        List<Map<String, Object>> savedHistories = new ArrayList<>();

        try {
            collectionHouseHistoryRepository.deleteAll();
            collectionHouseHistoryRepository.flush();

            for (int i = 0; i < updatesArray.size(); i++) {
                JsonObject updateObject = updatesArray.get(i).getAsJsonObject();
                String clientAccountNo = updateObject.get("clientAccountNo").getAsString();
                String nit = updateObject.get("nit").getAsString();
                String collectionHouseCode = updateObject.get("collectionHouseCode").getAsString();

                try {
                    Client client = clientRepositoryWrapper.getClientByAccountNumber(clientAccountNo);
                    Boolean hasLoanAccountsInArrears = false;
                    if (client != null) {
                        AccountSummaryCollectionData clientAccount = accountDetailsReadPlatformService
                                .retrieveClientAccountDetails(client.getId());
                        Collection<LoanAccountSummaryData> loanAccounts = clientAccount.getLoanAccounts();
                        if (!loanAccounts.isEmpty()) {
                            for (LoanAccountSummaryData loanAccountSummaryData : loanAccounts) {
                                LoanStatusEnumData statusEnumData = loanAccountSummaryData.getStatus();
                                Long status = statusEnumData.getId();
                                if (loanAccountSummaryData.getInArrears() == true && LoanStatus.fromInt(status.intValue()).isActive()) {
                                    hasLoanAccountsInArrears = true;
                                }
                            }
                        } else {
                            hasLoanAccountsInArrears = false;
                        }
                    }

                    if (hasLoanAccountsInArrears) {
                        ColletionHouseHistory colletionHouseHistory = new ColletionHouseHistory();
                        colletionHouseHistory.setClientAccountNumber(clientAccountNo);
                        colletionHouseHistory.setCollectionNit(nit);
                        colletionHouseHistory.setCollectionCode(collectionHouseCode);
                        collectionHouseHistoryRepository.saveAndFlush(colletionHouseHistory);
                    }

                } catch (final JpaSystemException | DataIntegrityViolationException ex) {
                    throw new PlatformDataIntegrityException("error.msg.collectionHouseHistory.save.failed",
                            "Failed to save Collection House History for clientAccountNo: " + clientAccountNo, ex);
                }
            }
            List<ColletionHouseHistory> colletionHouseHistories = this.findAllCollectionHouseHistory();
            savedHistories = colletionHouseHistories.stream().map(colletionHouseHistory -> {
                Map<String, Object> map = new HashMap<>();
                map.put("clientAccountNumber", colletionHouseHistory.getClientAccountNumber());
                map.put("collectionHouseCode", colletionHouseHistory.getCollectionCode());
                map.put("collectionNit", colletionHouseHistory.getCollectionNit());
                return map;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during collection house history refresh", e);
            throw e;
        }

        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(command.entityId())
                .withCollectionHouse(savedHistories).build();
    }

    @Override
    public void createCollectionHouseHistory(List<CollectionHouseUpdate> list) {
        try {
            collectionHouseHistoryRepository.deleteAll();
            collectionHouseHistoryRepository.flush();
            if (list != null && !list.isEmpty()) {
                for (CollectionHouseUpdate data : list) {
                    String clientAccountNo = data.getClientAccountNo();
                    String nit = data.getNit();
                    String collectionHouseCode = data.getCollectionHouseCode();

                    try {
                        Client client = clientRepositoryWrapper.getClientByAccountNumber(clientAccountNo);
                        Boolean hasLoanAccountsInArrears = false;
                        if (client != null) {
                            ColletionHouseHistory colletionHouseHistory = new ColletionHouseHistory();
                            colletionHouseHistory.setClientAccountNumber(clientAccountNo);
                            colletionHouseHistory.setCollectionNit(nit);
                            colletionHouseHistory.setCollectionCode(collectionHouseCode);
                            collectionHouseHistoryRepository.saveAndFlush(colletionHouseHistory);
                        }
                    } catch (final JpaSystemException | DataIntegrityViolationException ex) {
                        throw new PlatformDataIntegrityException("error.msg.collectionHouseHistory.save.failed",
                                "Failed to save Collection House History for clientAccountNo: " + clientAccountNo, ex);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error during collection house history refresh", e);
            throw e;
        }
    }

    @Override
    public CommandProcessingResult updateCollectionHouseHistory(JsonCommand command) {
        this.collectionHouseHistoryRepository.updateCollectionHouseHistory();
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).withEntityId(command.entityId()).build();
    }

    @Override
    public ColletionHouseHistory findCollectionHouseHistoryByAcctountNo(String accountNo) {
        Optional<ColletionHouseHistory> getColletionHouseHistory = collectionHouseHistoryRepository
                .getCollectionHouseHistoryByAccountNo((accountNo));
        if (getColletionHouseHistory.isPresent()) {
            ColletionHouseHistory colletionHouseHistory = getColletionHouseHistory.get();
            return colletionHouseHistory;
        }
        return null;
    }

    @Override
    public List<ColletionHouseHistory> findAllCollectionHouseHistory() {
        List<ColletionHouseHistory> colletionHouseHistories = collectionHouseHistoryRepository.findAll();
        return colletionHouseHistories;
    }

    @Override
    public CollectionHouseUpdates fetchDataFromExternalProvider() throws IOException {
        collectionHouseHistoryExternalRetrofitConfig.apiRequestDetailsRenewal(retrofit);

        CollectionHouseExternalApiService service = collectionHouseHistoryExternalRetrofitConfig.getRetrofitInstance()
                .create(CollectionHouseExternalApiService.class);

        Call<CollectionHouseUpdates> call = service.getData();

        CollectionHouseUpdates ret = new CollectionHouseUpdates();
        Response<CollectionHouseUpdates> response = call.execute();

        if (response.isSuccessful()) {
            ret = response.body();
        } else {
            throw new IOException("Request Status " + response.code() + " for "
                    + collectionHouseHistoryExternalRetrofitConfig.getRetrofitInstance().baseUrl()
                    + " Check if endpoint is correct and if the service is up.");
        }

        return ret;
    }
}
