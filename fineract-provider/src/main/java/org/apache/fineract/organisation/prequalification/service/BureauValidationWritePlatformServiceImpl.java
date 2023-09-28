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
package org.apache.fineract.organisation.prequalification.service;

import java.util.List;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationMemberRepository;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationMemberIndication;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.domain.ValidationChecklistResultRepository;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class BureauValidationWritePlatformServiceImpl implements BureauValidationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(BureauValidationWritePlatformServiceImpl.class);
    private final PlatformSecurityContext context;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final PreQualificationMemberRepository preQualificationMemberRepository;
    private final PreQualificationStatusLogRepository preQualificationStatusLogRepository;
    private final ValidationChecklistResultRepository validationChecklistResultRepository;
    private final PlatformSecurityContext platformSecurityContext;
    private final JdbcTemplate jdbcTemplate;

    public BureauValidationWritePlatformServiceImpl(PlatformSecurityContext context,
            final PreQualificationMemberRepository preQualificationMemberRepository,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final PreQualificationStatusLogRepository preQualificationStatusLogRepository,
            ValidationChecklistResultRepository validationChecklistResultRepository, PlatformSecurityContext platformSecurityContext,
            JdbcTemplate jdbcTemplate) {
        this.context = context;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.validationChecklistResultRepository = validationChecklistResultRepository;
        this.platformSecurityContext = platformSecurityContext;
        this.preQualificationMemberRepository = preQualificationMemberRepository;
        this.preQualificationStatusLogRepository = preQualificationStatusLogRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CommandProcessingResult validatePrequalificationWithBureau(Long prequalificationId, JsonCommand command) {

        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);

        Integer fromStatus = prequalificationGroup.getStatus();
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        // TODO --PROCESS THE PREQUALIFICATION GROUP WITH THE BUREAU AND UPDATE MEMBERS WITH THE RESULTS
        List<PrequalificationGroupMember> members = this.preQualificationMemberRepository
                .findAllByPrequalificationGroup(prequalificationGroup);
        for (PrequalificationGroupMember member : members) {
            // TODO --PROCESS THE MEMBER WITH THE BUREAU AND UPDATE THE RESULTS
            // TODO --UPDATE THE MEMBER WITH THE RESULTS
            member.updateBuroCheckStatus(PrequalificationMemberIndication.BUREAU_AVAILABLE.getValue());
            this.preQualificationMemberRepository.save(member);
        }
        prequalificationGroup.updateStatus(PrequalificationStatus.BURO_CHECKED);
        this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus,
                prequalificationGroup.getStatus(), null, prequalificationGroup);

        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }
}
