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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.organisation.prequalification.data.ChecklistValidationResult;
import org.apache.fineract.organisation.prequalification.data.GenericValidationResultSet;
import org.apache.fineract.organisation.prequalification.data.HardPolicyCategoryData;
import org.apache.fineract.organisation.prequalification.data.PrequalificationChecklistData;
import org.apache.fineract.organisation.prequalification.domain.CheckValidationColor;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMember;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupMemberRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class PrequalificationChecklistReadPlatformServiceImpl implements PrequalificationChecklistReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    private final PrequalificationGroupMemberRepositoryWrapper memberRepositoryWrapper;
    final ValidationChecklistMapper validationChecklistMapper = new ValidationChecklistMapper();

    @Autowired
    public PrequalificationChecklistReadPlatformServiceImpl(JdbcTemplate jdbcTemplate,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final PrequalificationGroupMemberRepositoryWrapper memberRepositoryWrapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.memberRepositoryWrapper = memberRepositoryWrapper;
    }

    @Override
    public PrequalificationChecklistData retrieveHardPolicyValidationResults(final Long prequalificationId) {
        PrequalificationGroup prequalificationGroup = this.prequalificationGroupRepositoryWrapper
                .findOneWithNotFoundDetection(prequalificationId);
        final Long productId = prequalificationGroup.getLoanProduct().getId();
        final PrequalificationChecklistWritePlatformServiceImpl.CheckCategoryMapper checkCategoryMapper = new PrequalificationChecklistWritePlatformServiceImpl.CheckCategoryMapper();
        final List<HardPolicyCategoryData> groupPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper,
                productId, PrequalificationType.GROUP.getValue());
        final List<String> prequalificationColumnHeaders = new ArrayList<>(List.of("label.heading.prequalification.name"));
        final List<List<String>> prequalificationRows = new ArrayList<>();
        final String sql = "SELECT " + validationChecklistMapper.schema() + " WHERE mcvr.prequalification_id = ? ";
        final List<ChecklistValidationResult> validationResults = this.jdbcTemplate.query(sql, validationChecklistMapper,
                prequalificationId);
        final List<String> prequalificationRow = new ArrayList<>(List.of(prequalificationGroup.getGroupName()));
        for (HardPolicyCategoryData policy : groupPolicies) {
            prequalificationColumnHeaders.add(policy.getDescription());
            for (ChecklistValidationResult validationResult : validationResults) {
                if (policy.getId().equals(validationResult.getPolicyId())
                        && PrequalificationType.GROUP.getValue().equals(validationResult.getPrequalificationTypeEnum())) {
                    final String validationColor = CheckValidationColor.fromInt(validationResult.getColorEnum()).name();
                    prequalificationRow.add(validationColor);
                    break;
                }
            }
        }
        prequalificationRows.add(prequalificationRow);
        final GenericValidationResultSet groupValidationResultSet = new GenericValidationResultSet(prequalificationColumnHeaders,
                prequalificationRows);
        final List<HardPolicyCategoryData> individualPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper,
                productId, PrequalificationType.INDIVIDUAL.getValue());
        final List<String> memberColumnHeaders = new ArrayList<>(
                List.of("label.heading.clientid", "label.heading.clientname", "label.heading.dpi"));
        individualPolicies.forEach(policy -> memberColumnHeaders.add(policy.getDescription()));
        final List<List<String>> memberRows = new ArrayList<>();
        for (PrequalificationGroupMember member : prequalificationGroup.getMembers()) {
            final List<String> memberRow = new ArrayList<>();
            String memberId = null;
            String clientName;
            String dpi;
            for (HardPolicyCategoryData policy : individualPolicies) {
                for (ChecklistValidationResult validationResult : validationResults) {
                    if (policy.getId().equals(validationResult.getPolicyId())
                            && PrequalificationType.INDIVIDUAL.getValue().equals(validationResult.getPrequalificationTypeEnum())
                            && member.getId().equals(validationResult.getMemberId().longValue())) {
                        final String validationColor = CheckValidationColor.fromInt(validationResult.getColorEnum()).name();
                        if (memberId == null) {
                            memberId = validationResult.getMemberId().toString();
                            clientName = validationResult.getClientName();
                            dpi = validationResult.getDpi();
                            memberRow.addAll(List.of(memberId, clientName, dpi));
                        }
                        memberRow.add(validationColor);
                        break;

                    }
                }
            }
            memberRows.add(memberRow);
        }
        final GenericValidationResultSet memberValidationResultSet = new GenericValidationResultSet(memberColumnHeaders, memberRows);
        return new PrequalificationChecklistData(groupValidationResultSet, memberValidationResultSet);
    }

    @Override
    public GenericValidationResultSet retrieveClientHardPolicyDetails(Long clientId) {
        PrequalificationGroupMember groupMember = this.memberRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        PrequalificationGroup prequalificationGroup = groupMember.getPrequalificationGroup();
        LoanProduct loanProduct = prequalificationGroup.getLoanProduct();

        final PrequalificationChecklistWritePlatformServiceImpl.CheckCategoryMapper checkCategoryMapper = new PrequalificationChecklistWritePlatformServiceImpl.CheckCategoryMapper();
        final List<List<String>> memberRows = new ArrayList<>();
        final String sql = "SELECT " + validationChecklistMapper.schema() + " WHERE mcvr.prequalification_member_id = ?";
        final List<ChecklistValidationResult> validationResults = this.jdbcTemplate.query(sql, validationChecklistMapper, clientId);
        final List<HardPolicyCategoryData> individualPolicies = this.jdbcTemplate.query(checkCategoryMapper.schema(), checkCategoryMapper,
                loanProduct.getId(), PrequalificationType.INDIVIDUAL.getValue());

        final List<String> memberColumnHeaders = new ArrayList<>(
                List.of("label.heading.clientid", "label.heading.clientname", "label.heading.dpi"));
        individualPolicies.forEach(policy -> memberColumnHeaders.add(policy.getDescription()));

        final List<String> memberRow = new ArrayList<>();
        String memberId = null;
        String clientName;
        String dpi;
        for (HardPolicyCategoryData policy : individualPolicies) {
            for (ChecklistValidationResult validationResult : validationResults) {
                if (policy.getId().equals(validationResult.getPolicyId())
                        && PrequalificationType.INDIVIDUAL.getValue().equals(validationResult.getPrequalificationTypeEnum())) {
                    final String validationColor = CheckValidationColor.fromInt(validationResult.getColorEnum()).name();
                    if (memberId == null) {
                        memberId = validationResult.getMemberId().toString();
                        clientName = validationResult.getClientName();
                        dpi = validationResult.getDpi();
                        memberRow.addAll(List.of(memberId, clientName, dpi));
                    }
                    memberRow.add(validationColor);
                    break;
                }
            }
        }
        memberRows.add(memberRow);
        final GenericValidationResultSet memberValidationResultSet = new GenericValidationResultSet(memberColumnHeaders, memberRows);
        return memberValidationResultSet;
    }

    private static final class ValidationChecklistMapper implements RowMapper<ChecklistValidationResult> {

        public String schema() {
            return """
                    mc.id AS clientId, IFNULL(mc.dpi, mpgm.dpi) AS dpi, mpgm.id AS memberId, IFNULL(mc.display_name, mpgm.name) AS clientName, cc.id AS policyId,\s
                    cc.name AS policyName, mcvr.validation_color_enum AS colorEnum, mpg.id AS prequalificationId, mpg.group_name AS prequalificationName, mcvr.prequalification_type AS prequalificationTypeEnum
                    FROM m_checklist_validation_result mcvr\s
                    INNER JOIN checklist_categories cc ON cc.id =  mcvr.checklist_category_id\s
                    LEFT JOIN m_prequalification_group mpg ON mpg.id = mcvr.prequalification_id\s
                    LEFT JOIN m_group mg ON mg.id = mpg.group_id\s
                    LEFT JOIN m_prequalification_group_members mpgm ON mpgm.id = mcvr.prequalification_member_id\s
                    LEFT JOIN m_client mc ON mc.id = mcvr.client_id\s
                    """;
        }

        @Override
        public ChecklistValidationResult mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Integer clientId = JdbcSupport.getInteger(rs, "clientId");
            final String clientName = rs.getString("clientName");
            final String dpi = rs.getString("dpi");
            final Integer memberId = JdbcSupport.getInteger(rs, "memberId");
            final Integer policyId = JdbcSupport.getInteger(rs, "policyId");
            final String policyName = rs.getString("policyName");
            final Integer colorEnum = JdbcSupport.getInteger(rs, "colorEnum");
            final Integer prequalificationId = JdbcSupport.getInteger(rs, "prequalificationId");
            final String prequalificationName = rs.getString("prequalificationName");
            final Integer prequalificationTypeEnum = JdbcSupport.getInteger(rs, "prequalificationTypeEnum");
            return new ChecklistValidationResult(clientId, clientName, dpi, memberId, policyId, policyName, colorEnum, prequalificationId,
                    prequalificationName, prequalificationTypeEnum);
        }
    }
}
