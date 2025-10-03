package org.apache.fineract.custom.portfolio.blockaccounts.service.impl;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockComponentEnum;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockDTO;
import org.apache.fineract.custom.portfolio.blockaccounts.data.LoanAccountBlockData;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlock;
import org.apache.fineract.custom.portfolio.blockaccounts.domain.LoanAccountBlockRepository;
import org.apache.fineract.custom.portfolio.blockaccounts.mapper.LoanAccountBlockMapper;
import org.apache.fineract.custom.portfolio.blockaccounts.service.LoanAccountBlockReadPlatformService;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class LoanAccountBlockReadPlatformServiceImpl implements LoanAccountBlockReadPlatformService {

    public static final String STR_CASTIGO = "CASTIGO";
    private final LoanAccountBlockRepository loanAccountBlockRepository;
    private final LoanAccountBlockMapper loanAccountBlockMapper;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Override
    public LoanAccountBlockDTO retrieveByLoanId(Long loanId) {
        Optional<LoanAccountBlock> loanAccountBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);
        return loanAccountBlock.map(loanAccountBlockMapper::toDto).orElse(null);
    }

    @Override
    public List<LoanAccountBlockDTO> retrieveHistoryByLoanId(Long loanId) {
        final List<LoanAccountBlock> loanAccountBlocks = loanAccountBlockRepository.retrieveHistoryByLoanId(loanId);
        List<LoanAccountBlockDTO> dtoList = loanAccountBlockMapper.toDto(loanAccountBlocks);

        for (int i = 0; i < loanAccountBlocks.size(); i++) {
            LoanAccountBlock entity = loanAccountBlocks.get(i);
            LoanAccountBlockDTO dto = dtoList.get(i);

            entity.getCreatedBy().ifPresent(userId -> {
                var appUser = appUserReadPlatformService.retrieveUser(userId);
                dto.setCreatedByName(appUser.getUsername());
            });
        }
        return dtoList;
    }

    @Override
    public LoanAccountBlockDTO retrieveByLoanIdWithoutException(Long loanId) {
        Optional<LoanAccountBlock> loanAccountBlock = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);
        return loanAccountBlock.map(loanAccountBlockMapper::toDto).orElse(null);

    }

    @Override
    public LoanAccountBlockData checkBlockAccountComponents(Long loanId, LocalDate givenDate) {
        List<LoanAccountBlockComponentEnum> blockedComponents = new ArrayList<>();

        Optional<LoanAccountBlock> loanAccountBlockOpt = loanAccountBlockRepository.retrieveByLoanIdAndStatusActive(loanId);

        if (loanAccountBlockOpt.isPresent()) {
            LoanAccountBlock loanAccountBlock = loanAccountBlockOpt.get();
            return loanAccountBlock.getLoan().checkBlockAccountComponents(givenDate);
        }

        return LoanAccountBlockData.builder().loanId(loanId).providedDate(givenDate).loanAccountBlockComponentEnumList(blockedComponents)
                .build();
    }

    @Override
    public boolean containsBlockAccountDisbursal(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.BLOCK_DISBURSAL);
    }

    @Override
    public boolean containsBlockAccountAccelerate(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.ACCELERATE);
    }

    @Override
    public boolean containsBlockAccountFreezeInterest(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.FREEZE_INTEREST);
    }

    @Override
    public boolean containsBlockAccountFreezeMora(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.FREEZE_MORA);
    }

    @Override
    public boolean containsBlockAccountFreezeLifeInsurance(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.FREEZE_LIFE_INSURANCE);
    }

    @Override
    public boolean containsBlockAccountFreezeMipyme(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.FREEZE_MIPYME);
    }

    @Override
    public boolean containsBlockAccountFreezeGAC(Long loanId, LocalDate givenDate) {
        return containsBlockAccount(loanId, givenDate, LoanAccountBlockComponentEnum.FREEZE_GAC);
    }

    private boolean containsBlockAccount(Long loanId, LocalDate givenDate, LoanAccountBlockComponentEnum blockComponentEnum) {
        return checkBlockAccountComponents(loanId, givenDate).getLoanAccountBlockComponentEnumList().stream()
                .anyMatch(disb -> disb.equals(blockComponentEnum));
    }
}
