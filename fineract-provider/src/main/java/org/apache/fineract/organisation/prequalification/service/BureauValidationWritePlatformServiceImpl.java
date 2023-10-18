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

import com.google.gson.JsonElement;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import org.apache.commons.codec.binary.Base64;
import org.apache.fineract.infrastructure.configuration.data.ExternalServicesPropertiesData;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesConstants;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.prequalification.domain.BuroCheckClassification;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationMemberRepository;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationStatusLogRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatusLog;
import org.apache.fineract.organisation.prequalification.domain.ValidationChecklistResultRepository;
import org.apache.fineract.organisation.prequalification.exception.DpiBuroChequeException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

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
    private final RestTemplate restTemplate = new RestTemplate();
    private final ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;

    public BureauValidationWritePlatformServiceImpl(PlatformSecurityContext context,
            final PreQualificationMemberRepository preQualificationMemberRepository,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final PreQualificationStatusLogRepository preQualificationStatusLogRepository,
            ValidationChecklistResultRepository validationChecklistResultRepository, PlatformSecurityContext platformSecurityContext,
            JdbcTemplate jdbcTemplate, ExternalServicesPropertiesReadPlatformService externalServicePropertiesReadPlatformService,
            FromJsonHelper fromApiJsonHelper) {
        this.context = context;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.validationChecklistResultRepository = validationChecklistResultRepository;
        this.platformSecurityContext = platformSecurityContext;
        this.preQualificationMemberRepository = preQualificationMemberRepository;
        this.preQualificationStatusLogRepository = preQualificationStatusLogRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.externalServicePropertiesReadPlatformService = externalServicePropertiesReadPlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    @Override
    public CommandProcessingResult validatePrequalificationWithBureau(Long prequalificationId, JsonCommand command) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);

        Integer fromStatus = prequalificationGroup.getStatus();
        AppUser addedBy = this.context.getAuthenticatedUserIfPresent();

        List<PrequalificationGroupMember> members = this.preQualificationMemberRepository
                .findAllByPrequalificationGroup(prequalificationGroup);
        for (PrequalificationGroupMember member : members) {
            EnumOptionData enumOptionData = this.makeBureauCheckApiCall(member.getDpi());
            if (enumOptionData == null) {
                throw new DpiBuroChequeException(member.getDpi());
            }
            member.updateBuroCheckStatus(enumOptionData.getId().intValue());
            this.preQualificationMemberRepository.save(member);
        }
        prequalificationGroup.updateStatus(PrequalificationStatus.BURO_CHECKED);
        this.prequalificationGroupRepositoryWrapper.save(prequalificationGroup);

        PrequalificationStatusLog statusLog = PrequalificationStatusLog.fromJson(addedBy, fromStatus, prequalificationGroup.getStatus(),
                null, prequalificationGroup);

        this.preQualificationStatusLogRepository.saveAndFlush(statusLog);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withResourceIdAsString(prequalificationGroup.getId().toString()) //
                .withEntityId(prequalificationGroup.getId()) //
                .build();
    }

    private EnumOptionData makeBureauCheckApiCall(final String dpi) {
        EnumOptionData enumOptionData = null;
        final Collection<ExternalServicesPropertiesData> externalServicesPropertiesDatas = this.externalServicePropertiesReadPlatformService
                .retrieveOne(ExternalServicesConstants.DPI_BURO_CHECK_SERVICE_NAME);
        String dpiBuroCheckApiUsername = null;
        String dpiBuroCheckApiPassword = null;
        String dpiBuroCheckApiHost = null;
        for (final ExternalServicesPropertiesData externalServicesPropertiesData : externalServicesPropertiesDatas) {
            if ("dpiBuroCheckApiUsername".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiUsername = externalServicesPropertiesData.getValue();
            } else if ("dpiBuroCheckApiPassword".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiPassword = externalServicesPropertiesData.getValue();
            } else if ("dpiBuroCheckApiHost".equalsIgnoreCase(externalServicesPropertiesData.getName())) {
                dpiBuroCheckApiHost = externalServicesPropertiesData.getValue();
            }
        }
        final String credentials = dpiBuroCheckApiUsername + ":" + dpiBuroCheckApiPassword;
        final String basicAuth = new String(Base64.encodeBase64(credentials.getBytes(Charset.defaultCharset())), Charset.defaultCharset());
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        httpHeaders.setAccept(List.of(MediaType.ALL));
        httpHeaders.add("Authorization", "Basic " + basicAuth);
        final String url = dpiBuroCheckApiHost + "?DPI=" + dpi;
        ResponseEntity<String> responseEntity = null;
        try {
            responseEntity = restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(httpHeaders), String.class);
        } catch (ResourceAccessException ex) {
            LOG.debug("DPI Buro Check Provider {} not available", url, ex);
        }

        if (responseEntity == null || !responseEntity.getStatusCode().equals(HttpStatus.OK)) {
            throw new PlatformDataIntegrityException("error.msg.mobile.service.provider.not.available", "DPI Buro Check Provider.");
        }
        if (responseEntity.hasBody()) {
            final JsonElement jsonElement = this.fromApiJsonHelper.parse(responseEntity.getBody());
            if (jsonElement.isJsonObject()) {
                final String classificationLetter = this.fromApiJsonHelper.extractStringNamed("Clasificacion", jsonElement);
                enumOptionData = BuroCheckClassification.status(BuroCheckClassification.fromLetter(classificationLetter).getId());
            }
        }

        return enumOptionData;
    }
}
