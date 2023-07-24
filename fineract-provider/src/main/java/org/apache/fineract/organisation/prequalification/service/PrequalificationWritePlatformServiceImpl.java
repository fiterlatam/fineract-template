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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.agency.domain.Agency;
import org.apache.fineract.organisation.agency.domain.AgencyRepositoryWrapper;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroup;
import org.apache.fineract.organisation.centerGroup.domain.CenterGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.command.PrequalificatoinApiConstants;
import org.apache.fineract.organisation.prequalification.command.PrequalificationDataValidator;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationMemberIndication;
import org.apache.fineract.organisation.prequalification.serialization.PrequalificationMemberCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class PrequalificationWritePlatformServiceImpl implements PrequalificationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final PrequalificationDataValidator dataValidator;
    private final LoanProductRepository loanProductRepository;
    private final ClientReadPlatformService clientReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final CenterGroupRepositoryWrapper centerGroupRepositoryWrapper;
    private final AppUserRepository appUserRepository;
    private final AgencyRepositoryWrapper agencyRepositoryWrapper;
    private final PrequalificationMemberCommandFromApiJsonDeserializer apiJsonDeserializer;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PrequalificationWritePlatformServiceImpl(final PlatformSecurityContext context, final PrequalificationDataValidator dataValidator,
                                                    final CenterGroupRepositoryWrapper centerGroupRepositoryWrapper,final AppUserRepository appUserRepository,
                                                    final LoanProductRepository loanProductRepository, final ClientReadPlatformService clientReadPlatformService,
                                                    final AgencyRepositoryWrapper agencyRepositoryWrapper,final PrequalificationMemberCommandFromApiJsonDeserializer apiJsonDeserializer,
                                                    final CodeValueReadPlatformService codeValueReadPlatformService, final JdbcTemplate jdbcTemplate,
                                                    final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper) {
        this.context = context;
        this.dataValidator = dataValidator;
        this.loanProductRepository = loanProductRepository;
        this.clientReadPlatformService = clientReadPlatformService;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.centerGroupRepositoryWrapper = centerGroupRepositoryWrapper;
        this.appUserRepository = appUserRepository;
        this.agencyRepositoryWrapper = agencyRepositoryWrapper;
        this.apiJsonDeserializer = apiJsonDeserializer;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public CommandProcessingResult processPrequalification(JsonCommand command) {
        this.dataValidator.validateForCreate(command.json());
        final Long productId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.productIdParamName);
        final Long centerGroupId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.groupIdParamName);
        final Long agencyId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.agencyIdParamName);

        Optional<LoanProduct> productOption = this.loanProductRepository.findById(productId);
        if (productOption.isEmpty()) throw new LoanProductNotFoundException(productId);
        LoanProduct loanProduct = productOption.get();
        String groupName = command.stringValueOfParameterNamed(PrequalificatoinApiConstants.groupNameParamName);

        CenterGroup centerGroup = null;
        if (centerGroupId != null) {
            centerGroup = this.centerGroupRepositoryWrapper.findOneWithNotFoundDetection(centerGroupId);
            groupName = centerGroup.getName();
        }

        Agency agency = this.agencyRepositoryWrapper.findOneWithNotFoundDetection(agencyId);

        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();
        Long facilitatorId = command.longValueOfParameterNamed(PrequalificatoinApiConstants.facilitatorParamName);
        AppUser facilitator = null;
        if (facilitatorId != null) {
            facilitator = this.appUserRepository.findById(facilitatorId)
                    .orElseThrow(() -> new UserNotFoundException(facilitatorId));
        }
        PrequalificationGroup prequalificationGroup = PrequalificationGroup.fromJson(addedBy, facilitator, agency, centerGroup, loanProduct, command);

        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        StringBuilder prequalSB = new StringBuilder();
        prequalSB.append("PRECAL-");
        prequalSB.append(agency.getId()).append("-");
        String prequalificationNumber = StringUtils.leftPad(prequalificationGroup.getId().toString(), 4, '0');
        prequalSB.append(prequalificationNumber);
        prequalificationGroup.updatePrequalificationNumber(prequalSB.toString());
        List<PrequalificationGroupMember> members = assembleMembers(command, prequalificationGroup, addedBy);
        prequalificationGroup.updateMembers(members);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    private List<PrequalificationGroupMember> assembleMembers(JsonCommand command, PrequalificationGroup group, AppUser addedBy) {
        final List<PrequalificationGroupMember> allMembers = new ArrayList<>();

            JsonArray groupMembers = command.arrayOfParameterNamed(PrequalificatoinApiConstants.membersParamName);
        if (!ObjectUtils.isEmpty(groupMembers)) {
            for (JsonElement members : groupMembers) {

                apiJsonDeserializer.validateForCreate(members.toString());

                JsonObject member = members.getAsJsonObject();

                String name = null;
                if (member.get("name") != null) {
                    name = member.get("name").getAsString();
                }
                String dpi = null;
                if (member.get("dpi") != null) {
                    dpi = member.get("dpi").getAsString();
                }

                BigDecimal requestedAmount = null;
                if (member.get("amount") != null) {
                    requestedAmount = new BigDecimal(member.get("amount").getAsString().replace(",", ""));
                }

                String puente = null;
                if (member.get("puente") != null) {
                    puente = member.get("puente").getAsString();
                }


                LocalDate dateOfBirth = null;
                if (member.get("dob") != null) {

                    DateTimeFormatter formatter = new DateTimeFormatterBuilder().appendPattern(member.get("dateFormat").getAsString())
                            .toFormatter();
                    LocalDate date;
                    try {
                        date = LocalDate.parse(member.get("dob").getAsString(), formatter);
                        dateOfBirth = date;
                    } catch (DateTimeParseException e) {
                        LOG.error("Problem occurred in addClientFamilyMember function", e);
                    }

                }

                //get light indicator
                String blistSql = "select count(*) from m_client_blacklist where dpi=? and status=?";
                Long activeBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.ACTIVE.getValue());
                Long inactiveBlacklisted = jdbcTemplate.queryForObject(blistSql, Long.class, dpi, BlacklistStatus.INACTIVE.getValue());
                Integer status = PrequalificationMemberIndication.NONE.getValue();
                if (activeBlacklisted <=0 && inactiveBlacklisted<=0) {
                    status = PrequalificationMemberIndication.NONE.getValue();
                }
                if (activeBlacklisted <=0 && inactiveBlacklisted>0) {
                    status = PrequalificationMemberIndication.INACTIVE.getValue();
                }

                if (activeBlacklisted >0) {
                    status = PrequalificationMemberIndication.ACTIVE.getValue();
                }


                PrequalificationGroupMember groupMember = PrequalificationGroupMember.fromJson(group, name, dpi, dateOfBirth, requestedAmount, puente, addedBy, status);
                allMembers.add(groupMember);
            }
        }

        return allMembers;
    }

    @Override
    public Long addCommentsToPrequalification(Long groupId, String comment) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(groupId);
        prequalificationGroup.updateComments(comment);
        this.prequalificationGroupRepositoryWrapper.saveAndFlush(prequalificationGroup);
        return groupId;
    }
}
