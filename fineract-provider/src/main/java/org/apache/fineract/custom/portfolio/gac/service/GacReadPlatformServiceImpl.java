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
package org.apache.fineract.custom.portfolio.gac.service;

import jakarta.ws.rs.NotFoundException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.custom.portfolio.gac.data.GacData;
import org.apache.fineract.custom.portfolio.gac.domain.Gac;
import org.apache.fineract.custom.portfolio.gac.domain.GacRepository;
import org.apache.fineract.custom.portfolio.gac.mapper.GacMapper;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsData;
import org.apache.fineract.infrastructure.clientblockingreasons.service.ManageBlockingReasonsReadPlatformService;
import org.apache.fineract.useradministration.service.AppUserReadPlatformService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GacReadPlatformServiceImpl implements GacReadPlatformService {

    private final GacMapper gacMapper;
    private final GacRepository gacRepository;
    private final ManageBlockingReasonsReadPlatformService manageBlockingReasonsReadPlatformService;
    private final AppUserReadPlatformService appUserReadPlatformService;

    @Override
    public List<GacData> retrieveAll() {
        final List<Gac> gacs = gacRepository.findAll();
        return gacMapper.map(gacs);
    }

    @Override
    public GacData retrieveOne(Long gacId) {
        final Optional<Gac> optGac = gacRepository.findById(gacId);
        if (!optGac.isPresent()) {
            throw new NotFoundException(String.valueOf(gacId));
        }
        Gac gac = optGac.get();
        GacData gacData = gacMapper.map(gac);
        if (gac.getCreatedBy().isPresent()) {
            var appUser = appUserReadPlatformService.retrieveUser(optGac.get().getCreatedBy().get());
            gacData.setCreatedByName(appUser.getUsername());
        }
        return gacData;
    }

    @Override
    public GacData retrieveTemplate() {
        Collection<BlockingReasonsData> blockingReasonSettings = manageBlockingReasonsReadPlatformService.retrieveAllBlockingReasons(null);
        return new GacData(null, null, null, null, null, null, null, blockingReasonSettings, null);
    }
}
