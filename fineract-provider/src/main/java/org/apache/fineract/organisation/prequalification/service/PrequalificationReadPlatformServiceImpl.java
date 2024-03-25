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

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.service.GenericDataService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.prequalification.data.BuroData;
import org.apache.fineract.organisation.prequalification.data.GroupPrequalificationData;
import org.apache.fineract.organisation.prequalification.data.MemberPrequalificationData;
import org.apache.fineract.organisation.prequalification.domain.BuroCheckClassification;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationsEnumerations;
import org.apache.fineract.organisation.prequalification.domain.PreQualificationsMemberEnumerations;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationMemberIndication;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationSubStatus;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationType;
import org.apache.fineract.portfolio.client.service.ClientChargeWritePlatformServiceJpaRepositoryImpl;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class PrequalificationReadPlatformServiceImpl implements PrequalificationReadPlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(ClientChargeWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final JdbcTemplate jdbcTemplate;
    private final PaginationHelper paginationHelper;
    private final ColumnValidator columnValidator;
    private final PrequalificationsGroupMapper prequalificationsGroupMapper = new PrequalificationsGroupMapper();
    private final PrequalificationsGroupMappingsMapper mappingsMapper = new PrequalificationsGroupMappingsMapper();
    private final PrequalificationIndividualMappingsMapper prequalificationIndividualMappingsMapper = new PrequalificationIndividualMappingsMapper();
    private final PrequalificationsMemberMapper prequalificationsMemberMapper = new PrequalificationsMemberMapper();
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final GenericDataService genericDataService;

    @Autowired
    public PrequalificationReadPlatformServiceImpl(final PlatformSecurityContext context, final PaginationHelper paginationHelper,
            final DatabaseSpecificSQLGenerator sqlGenerator, final ColumnValidator columnValidator,
            final CodeValueReadPlatformService codeValueReadPlatformService, final JdbcTemplate jdbcTemplate,
            GenericDataService genericDataService) {
        this.context = context;
        this.codeValueReadPlatformService = codeValueReadPlatformService;
        this.jdbcTemplate = jdbcTemplate;
        this.paginationHelper = paginationHelper;
        this.sqlGenerator = sqlGenerator;
        this.columnValidator = columnValidator;
        this.genericDataService = genericDataService;
    }

    @Override
    public Page<GroupPrequalificationData> retrieveAll(SearchParameters searchParameters) {

        if (searchParameters != null && searchParameters.getStatus() != null
                && PrequalificationStatus.fromString(searchParameters.getStatus()) == PrequalificationStatus.INVALID) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final String defaultUserMessage = "The status value '" + searchParameters.getStatus() + "' is not supported.";
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.client.status.value.is.not.supported",
                    defaultUserMessage, "status", searchParameters.getStatus());
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(500);
        sqlBuilder.append("select ").append(this.prequalificationsGroupMapper.schema());
        sqlBuilder.append(" where g.prequalification_number is not null ");
        String sqlCountRows = this.genericDataService.wrapSQLCount(sqlBuilder.toString());
        if (searchParameters != null) {
            final String extraCriteria = buildSqlStringFromBlacklistCriteria(searchParameters, paramList, true);
            if (StringUtils.isNotBlank(extraCriteria)) {
                sqlBuilder.append(" and (").append(extraCriteria).append(")");
            }
            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy());
                if (searchParameters.isSortOrderProvided()) {
                    sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                    this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getSortOrder());
                }
            } else {
                sqlBuilder.append(" order by g.id desc ");
            }
            sqlCountRows = this.genericDataService.wrapSQLCount(sqlBuilder.toString());
            if (searchParameters.isLimited()) {
                sqlBuilder.append(" ");
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
                } else {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
                }
            }
        }
        List<GroupPrequalificationData> pageItems = this.jdbcTemplate.query(sqlBuilder.toString(), this.prequalificationsGroupMapper,
                paramList.toArray());
        final Integer totalFilteredRecords = ObjectUtils
                .defaultIfNull(this.jdbcTemplate.queryForObject(sqlCountRows, Integer.class, paramList.toArray()), 0);
        return new Page<>(pageItems, totalFilteredRecords);
    }

    @Override
    public GroupPrequalificationData retrieveOne(Long groupId) {

        final String sql = "select " + this.prequalificationsGroupMapper.schema() + " WHERE g.id = ? ";
        final GroupPrequalificationData clientData = this.jdbcTemplate.queryForObject(sql, this.prequalificationsGroupMapper,
                new Object[] { groupId });

        if (clientData != null) {
            final String membersql = "select " + this.prequalificationsMemberMapper.schema() + " WHERE m.group_id = ? ";

            List<MemberPrequalificationData> members = this.jdbcTemplate.query(membersql, this.prequalificationsMemberMapper,
                    new Object[] { groupId });

            for (MemberPrequalificationData memberPrequalificationData : members) {
                Integer status = PrequalificationMemberIndication.NONE.getValue();
                if (memberPrequalificationData.getActiveBlacklistCount() > 0) {
                    status = PrequalificationMemberIndication.ACTIVE.getValue();
                }
                if (memberPrequalificationData.getActiveBlacklistCount() <= 0
                        && memberPrequalificationData.getInActiveBlacklistCount() > 0) {
                    status = PrequalificationMemberIndication.INACTIVE.getValue();
                }
                if (memberPrequalificationData.getActiveBlacklistCount() <= 0
                        && memberPrequalificationData.getInActiveBlacklistCount() <= 0) {
                    status = PrequalificationMemberIndication.NONE.getValue();
                }
                final EnumOptionData enumOptionData = PreQualificationsMemberEnumerations.status(status);
                memberPrequalificationData.setStatus(enumOptionData);
            }
            clientData.updateMembers(members);
        }
        return clientData;
    }

    @Override
    public GroupPrequalificationData prequalifyExistingGroup(Long groupId) {
        return null;
    }

    @Override
    public Page<MemberPrequalificationData> retrieveAllMembers(SearchParameters searchParameters) {
        if (searchParameters != null && searchParameters.getStatus() != null
                && PrequalificationStatus.fromString(searchParameters.getStatus()) == PrequalificationStatus.INVALID) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final String defaultUserMessage = "The status value '" + searchParameters.getStatus() + "' is not supported.";
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.client.status.value.is.not.supported",
                    defaultUserMessage, "status", searchParameters.getStatus());
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.prequalificationsMemberMapper.schema());
        sqlBuilder.append(" where m.group_id is null ");

        if (searchParameters != null) {

            final String extraCriteria = buildSqlStringFromBlacklistCriteria(searchParameters, paramList, false);

            if (StringUtils.isNotBlank(extraCriteria)) {
                sqlBuilder.append(" and (").append(extraCriteria).append(")");
            }

            if (searchParameters.isOrderByRequested()) {
                sqlBuilder.append(" order by ").append(searchParameters.getOrderBy());
                this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getOrderBy());
                if (searchParameters.isSortOrderProvided()) {
                    sqlBuilder.append(' ').append(searchParameters.getSortOrder());
                    this.columnValidator.validateSqlInjection(sqlBuilder.toString(), searchParameters.getSortOrder());
                }
            } else {
                sqlBuilder.append(" order by m.id desc ");
            }

            if (searchParameters.isLimited()) {
                sqlBuilder.append(" ");
                if (searchParameters.isOffset()) {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit(), searchParameters.getOffset()));
                } else {
                    sqlBuilder.append(sqlGenerator.limit(searchParameters.getLimit()));
                }
            }
        }
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(),
                this.prequalificationsMemberMapper);
    }

    @Override
    public MemberPrequalificationData retrieveOneMember(Long clientId) {
        final String sql = "select " + this.prequalificationsMemberMapper.schema() + " where m.id = ? ";
        final MemberPrequalificationData clientData = this.jdbcTemplate.queryForObject(sql, this.prequalificationsMemberMapper,
                new Object[] { clientId });
        return clientData;
    }

    @Override
    public Collection<GroupPrequalificationData> retrievePrequalificationGroupsMappings(Long groupId) {
        final String sql = "select " + this.mappingsMapper.schema() + " where GR.group_id = ? ";
        Collection<GroupPrequalificationData> prequalificationGroups = this.jdbcTemplate.query(sql, this.mappingsMapper,
                new Object[] { groupId });
        return prequalificationGroups;
    }

    @Override
    public Collection<GroupPrequalificationData> retrieveGroupByPrequalificationId(Long prequalificationId) {
        final String sql = "select " + this.mappingsMapper.schema() + " where PG.id = ? ";
        Collection<GroupPrequalificationData> prequalificationGroups = this.jdbcTemplate.query(sql, this.mappingsMapper,
                new Object[] { prequalificationId });
        return prequalificationGroups;
    }

    @Override
    public Collection<GroupPrequalificationData> retrievePrequalificationIndividualMappings(final Long clientId) {
        final String sql = "select " + this.prequalificationIndividualMappingsMapper.schema() + " WHERE mc.id = ? AND mpg.status = 600";
        final Collection<GroupPrequalificationData> prequalificationGroups = this.jdbcTemplate.query(sql,
                this.prequalificationIndividualMappingsMapper, new Object[] { clientId });
        return prequalificationGroups;
    }

    private String buildSqlStringFromBlacklistCriteria(final SearchParameters searchParameters, List<Object> paramList, boolean isGroup) {

        AppUser appUser = this.context.authenticatedUser();
        String committeeQuery = "select committee_id from m_committee_user where user_id = ? order by committee_id asc limit 1";
        final List<Long> committeeIdList = this.jdbcTemplate.queryForList(committeeQuery, Long.class, appUser.getId());

        CodeValueData committeeValueData = null;
        if (!committeeIdList.isEmpty()) {
            committeeValueData = this.codeValueReadPlatformService.retrieveCodeValue(committeeIdList.get(0));
        }
        String sqlSearch = searchParameters.getSqlSearch();
        final Long officeId = searchParameters.getOfficeId();
        final Long centerId = searchParameters.getCenterId();
        final String dpiNumber = searchParameters.getName();
        final String status = searchParameters.getStatus();
        final String type = searchParameters.getType();
        final String groupName = searchParameters.getGroupName();
        final String centerName = searchParameters.getCenterName();
        final String groupingType = searchParameters.getGroupingType();

        String extraCriteria = "";

        //add hierrachy filter here.
        extraCriteria += " and mo.hierarchy LIKE CONCAT(?, '%') OR ? like CONCAT(mo.hierarchy, '%')";
        paramList.add(appUser.getOffice().getHierarchy());
        paramList.add(appUser.getOffice().getHierarchy());

        if (StringUtils.isNotBlank(groupingType)) {
            if (groupingType.equals("group")) {
                extraCriteria += " and g.prequalification_type_enum = ? ";
                paramList.add(PrequalificationType.GROUP.getValue());

                Set<Role> roles = appUser.getRoles();
                for (Role userRole : roles) {
                    if (StringUtils.containsIgnoreCase(userRole.getName(), "Líder de agencia")) {
                        extraCriteria += " and ma.responsible_user_id = ? ";
                        paramList.add(appUser.getId());
                    }
                }

            }

            if (StringUtils.equals(groupingType, "individual")) {
                extraCriteria += " and g.prequalification_type_enum = ? ";
                paramList.add(PrequalificationType.INDIVIDUAL.getValue());
                if (dpiNumber != null) {
                    extraCriteria += " and g.dpi = ? ";
                    paramList.add(dpiNumber);
                }
            }
        }

        if (sqlSearch != null && !isGroup) {
            extraCriteria += " and (m.name like '%" + sqlSearch + "%' OR m.dpi='" + sqlSearch + "') ";
        }

        if (sqlSearch != null && isGroup) {
            extraCriteria += " and (g.group_name like '%" + sqlSearch + "%' OR pc.display_name='%" + sqlSearch + "%') ";
        }

        if (officeId != null) {
            extraCriteria += " and c.office_id = ? ";
            paramList.add(officeId);
        }

        if (centerId != null) {
            extraCriteria += " and g.center_id = ? ";
            paramList.add(centerId);
        }

        if (dpiNumber != null) {
            paramList.add(dpiNumber);
            extraCriteria += " and g.prequalification_number like %?% ";
        }

        if (groupName != null) {
            paramList.add(groupName);
            extraCriteria += " and g.group_name like %?% ";
        }

        if (centerName != null) {
            paramList.add(centerName);
            extraCriteria += " and pc.display_name like %?% ";
        }

        if (status != null) {
            PrequalificationStatus prequalificationStatus = PrequalificationStatus.fromString(status);
            extraCriteria += " and g.status = " + prequalificationStatus.getValue() + " ";
        }

        if (type != null) {
            if (type.equals("existing")) {
                extraCriteria += " and g.group_id is not null ";
            } else if (type.equals("checked")) {
                extraCriteria += " and g.status = " + PrequalificationStatus.BURO_CHECKED.getValue().toString() + " "
                        + "and (g.id not in (select prequalification_id from m_group where prequalification_id is not null)) ";
            } else if (type.equals("analysis")) {
                extraCriteria += " and g.status IN( " + PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL.getValue().toString() + ", "
                        + PrequalificationStatus.ANALYSIS_UNIT_PENDING_APPROVAL_WITH_EXCEPTIONS.getValue().toString() + ") ";
            } else if (type.equals("agency")) {
                extraCriteria += " and g.status IN( " + PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL.getValue().toString() + ", "
                        + PrequalificationStatus.AGENCY_LEAD_PENDING_APPROVAL_WITH_EXCEPTIONS.getValue().toString() + ") ";
            } else if (type.equals("exceptionsqueue")) {
                extraCriteria += " and g.status IN( " + PrequalificationStatus.AGENCY_LEAD_APPROVED_WITH_EXCEPTIONS.getValue().toString()
                        + ") ";
            } else if (type.equals("committeeapprovals")) {

                if (committeeValueData == null) {
                    extraCriteria += " and g.status IN( " + PrequalificationStatus.INVALID.getValue().toString() + ") ";
                } else {
                    extraCriteria += " and g.status IN( " + resolveCommitteeGroupStatus(committeeValueData) + ") ";
                }
            }
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            extraCriteria = extraCriteria.substring(4);
        }
        return extraCriteria;
    }

    private String resolveCommitteeGroupStatus(CodeValueData committeeValueData) {
        String name = committeeValueData.getName();

        String statusValues = "";
        switch (name) {
            case "A" -> statusValues = PrequalificationStatus.PRE_COMMITTEE_A_PENDING_APPROVAL.getValue().toString() + ", "
                    + PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL.getValue().toString();
            case "B" -> statusValues = PrequalificationStatus.PRE_COMMITTEE_B_PENDING_APPROVAL.getValue().toString() + ", "
                    + PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL.getValue().toString();
            case "C" -> statusValues = PrequalificationStatus.PRE_COMMITTEE_C_PENDING_APPROVAL.getValue().toString() + ", "
                    + PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL.getValue().toString();
            case "D" -> statusValues = PrequalificationStatus.PRE_COMMITTEE_D_PENDING_APPROVAL.getValue().toString();
            default -> statusValues = PrequalificationStatus.INVALID.getValue().toString();
        }
        return statusValues;
    }

    private static final class PrequalificationsGroupMapper implements RowMapper<GroupPrequalificationData> {

        private final String schema;

        PrequalificationsGroupMapper() {
            this.schema = """
                    	g.id AS id,
                    	g.prequalification_number AS prequalificationNumber,
                    	g.status,linkedGroup.id as linkedGroupId,
                    	g.prequalification_duration as prequalilficationTimespan,
                    	g.comments,
                    	g.created_at,
                    	g.prequalification_type_enum as prequalificationType,
                    	sl.from_status as previousStatus,
                    	sl.sub_status as substatus,
                    	sl.comments as latestComments,
                    	assigned.username as assignedUser,
                    	concat(assigned.firstname, ' ', assigned.lastname) as assignedUserName,
                    	sl.date_created as statusChangedOn,
                        prequalification_numbers.total_requested_amount as totalRequestedAmount,
                        prequalification_numbers.total_approved_amount totalApprovedAmount,
                    	(case when g.previous_prequalification is not null THEN 'Recredito' ELSE 'Nuevo' END) as processType,
                    	(case when (select count(*) from m_prequalification_status_log where prequalification_id = g.id and to_status = g.status )>0 THEN 'Reproceso' ELSE 'Nuevo' END) as processQuality,
                    	concat(mu.firstname, ' ', mu.lastname) as statusChangedBy,
                    	ma.name AS agencyName,
                    	ma.id AS agencyId,
                    	cg.display_name AS groupName,
                    	g.group_name AS newGroupName,
                    	g.group_id AS groupId,
                    	pc.display_name AS centerName,
                    	pc.id AS centerId,
                    	lp.id AS productId,
                    	fa.id AS facilitatorId,
                    	concat(fa.firstname, ' ', fa.lastname) AS facilitatorName,
                    	lp.name AS productName,
                    	au.firstname,
                    	au.lastname,
                    	greenValidation.validCount AS greenValidationCount,
                    	yellowValidation.validCount AS yellowValidationCount,
                    	orangeValidation.validCount AS orangeValidationCount,
                    	redValidation.validCount AS redValidationCount
                    FROM
                    	m_prequalification_group g
                    INNER JOIN m_appuser au ON
                    	au.id = g.added_by
                    INNER JOIN m_product_loan lp ON
                    	g.product_id = lp.id
                    LEFT JOIN (
                        SELECT
                        mpgm.group_id AS prequalification_id,
                        SUM(mpgm.requested_amount) total_requested_amount,
                        SUM(mpgm.approved_amount) total_approved_amount
                        FROM m_prequalification_group_members mpgm
                        GROUP BY mpgm.group_id
                    ) prequalification_numbers ON prequalification_numbers.prequalification_id = g.id
                    LEFT JOIN m_agency ma ON
                    	g.agency_id = ma.id
                    LEFT JOIN m_office mo on mo.id = ma.linked_office_id
                    LEFT JOIN
                    (
                      SELECT p.id AS groupid,
                      COUNT(mcvr.id) AS validCount
                      FROM m_checklist_validation_result mcvr
                      INNER JOIN m_prequalification_group p ON mcvr.prequalification_id = p.id and mcvr.validation_color_enum = 1
                      GROUP BY p.id
                    ) greenValidation ON greenValidation.groupid = g.id

                     LEFT JOIN
                    (
                      SELECT p.id AS groupid,
                      COUNT(mcvr.id) AS validCount
                      FROM m_checklist_validation_result mcvr
                      INNER JOIN m_prequalification_group p ON mcvr.prequalification_id = p.id and mcvr.validation_color_enum = 2
                      GROUP BY p.id
                    ) yellowValidation ON yellowValidation.groupid = g.id

                     LEFT JOIN
                    (
                      SELECT p.id AS groupid,
                      COUNT(mcvr.id) AS validCount
                      FROM m_checklist_validation_result mcvr
                      INNER JOIN m_prequalification_group p ON mcvr.prequalification_id = p.id and mcvr.validation_color_enum = 3
                      GROUP BY p.id
                    ) orangeValidation ON orangeValidation.groupid = g.id

                     LEFT JOIN
                    (
                      SELECT p.id AS groupid,
                      COUNT(mcvr.id) AS validCount
                      FROM m_checklist_validation_result mcvr
                      INNER JOIN m_prequalification_group p ON mcvr.prequalification_id = p.id and mcvr.validation_color_enum = 4
                      GROUP BY p.id
                    ) redValidation ON redValidation.groupid = g.id

                    LEFT JOIN m_group cg ON
                    	cg.id = g.group_id
                    LEFT JOIN m_group linkedGroup ON
                    	linkedGroup.prequalification_id = g.id
                    LEFT JOIN m_group pc ON
                    	pc.id = g.center_id
                    LEFT JOIN m_prequalification_status_log sl ON
                    	sl.prequalification_id = g.id AND sl.to_status=g.status
                    	AND sl.id =
                    	(SELECT MAX(id)
                    	FROM m_prequalification_status_log WHERE prequalification_id = g.id AND sl.to_status=g.status )
                    LEFT JOIN m_appuser assigned ON assigned.id = sl.assigned_to
                    LEFT JOIN m_appuser mu ON
                    	mu.id = sl.updatedby_id
                    LEFT JOIN m_appuser fa ON
                    	fa.id = g.facilitator
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public GroupPrequalificationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = PreQualificationsEnumerations.status(statusEnum);

            final Long id = JdbcSupport.getLong(rs, "id");
            final String prequalificationNumber = rs.getString("prequalificationNumber");
            String groupName = rs.getString("groupName");
            final String agencyName = rs.getString("agencyName");
            final String centerName = rs.getString("centerName");
            final Long groupId = rs.getLong("groupId");
            final String newGroupName = rs.getString("newGroupName");
            // final String portfolioName = rs.getString("portfolioName");
            final String productName = rs.getString("productName");
            final String comments = rs.getString("comments");
            final String latestComments = rs.getString("latestComments");
            final LocalDateTime createdAt = JdbcSupport.getLocalDateTime(rs, "created_at");

            final String addedBy = rs.getString("firstname") + " " + rs.getString("lastname");
            final Long agencyId = JdbcSupport.getLong(rs, "agencyId");
            final Long centerId = JdbcSupport.getLong(rs, "centerId");
            final Long productId = JdbcSupport.getLong(rs, "productId");
            final Long linkedGroupId = JdbcSupport.getLong(rs, "linkedGroupId");
            final Long facilitatorId = JdbcSupport.getLong(rs, "facilitatorId");
            final String facilitatorName = rs.getString("facilitatorName");
            final Long redValidationCount = rs.getLong("redValidationCount");
            final Long orangeValidationCount = rs.getLong("orangeValidationCount");
            final Long greenValidationCount = rs.getLong("greenValidationCount");
            final Long yellowValidationCount = rs.getLong("yellowValidationCount");
            final Long prequalilficationTimespan = rs.getLong("prequalilficationTimespan");
            final Integer previousStatus = rs.getInt("previousStatus");
            final String statusChangedBy = rs.getString("statusChangedBy");
            final LocalDate statusChangedOn = JdbcSupport.getLocalDate(rs, "statusChangedOn");
            final String processType = rs.getString("processType");
            final String processQuality = rs.getString("processQuality");
            final Integer substatus = rs.getInt("substatus");
            String prequalificationSubStatus = PrequalificationSubStatus.PENDING.getCode();
            if (substatus != null) {
                prequalificationSubStatus = PrequalificationSubStatus.fromInt(substatus).getCode();
            }
            final String assignedUser = rs.getString("assignedUser");
            final String assignedUserName = rs.getString("assignedUserName");
            final BigDecimal totalRequestedAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalRequestedAmount");
            final BigDecimal totalApprovedAmount = JdbcSupport.getBigDecimalDefaultToZeroIfNull(rs, "totalApprovedAmount");

            if (StringUtils.isBlank(groupName)) {
                groupName = newGroupName;
            }
            EnumOptionData lastPrequalificationStatus = null;
            if (previousStatus != null) {
                lastPrequalificationStatus = PreQualificationsEnumerations.status(previousStatus);
            }

            final Integer prequalificationTypeEnum = JdbcSupport.getInteger(rs, "prequalificationType");
            final EnumOptionData prequalificationType = PreQualificationsEnumerations.prequalificationType(prequalificationTypeEnum);

            return GroupPrequalificationData.instance(id, prequalificationNumber, status, agencyName, null, centerName, groupName,
                    productName, addedBy, createdAt, comments, groupId, agencyId, centerId, productId, facilitatorId, facilitatorName,
                    greenValidationCount, yellowValidationCount, orangeValidationCount, redValidationCount, prequalilficationTimespan,
                    lastPrequalificationStatus, statusChangedBy, statusChangedOn, processType, processQuality, totalRequestedAmount,
                    totalApprovedAmount, prequalificationType, prequalificationSubStatus, assignedUser, assignedUserName, latestComments,
                    linkedGroupId);

        }
    }

    private static final class PrequalificationsGroupMappingsMapper implements RowMapper<GroupPrequalificationData> {

        private final String schema;

        PrequalificationsGroupMappingsMapper() {
            this.schema = " PG.id AS id, PG.prequalification_number AS prequalificationNumber, PG.group_name AS groupName, "
                    + "PG.status, LP.name AS productName, " + "PG.created_at, AU.firstname, AU.lastname, MG.id AS groupId "
                    + "from m_group_prequalification_relationship GR " + "inner join m_group MG on MG.id = GR.group_id "
                    + "inner join m_prequalification_group PG on PG.id = GR.prequalification_id "
                    + "inner join m_product_loan LP on LP.id = PG.product_id " + "INNER JOIN m_appuser AU ON AU.id = PG.added_by " + "";
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public GroupPrequalificationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = PreQualificationsEnumerations.status(statusEnum);
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final String prequalificationNumber = rs.getString("prequalificationNumber");
            String groupName = rs.getString("groupName");
            final String productName = rs.getString("productName");
            final LocalDateTime createdAt = JdbcSupport.getLocalDateTime(rs, "created_at");
            final String addedBy = rs.getString("firstname") + " " + rs.getString("lastname");
            return GroupPrequalificationData.simpeGroupData(id, prequalificationNumber, status, groupName, productName, addedBy, createdAt,
                    groupId);

        }
    }

    public static final class PrequalificationIndividualMappingsMapper implements RowMapper<GroupPrequalificationData> {

        private final String schema;

        public PrequalificationIndividualMappingsMapper() {
            this.schema = """
                    mpg.id AS id, mpg.prequalification_number AS prequalificationNumber, mpg.group_name AS groupName, mpg.status AS status,
                    mpl.name AS productName, mpg.created_at, ma.firstname, ma.lastname, mg.id as groupId, mpg.prequalification_type_enum as prequalificationType
                    FROM m_prequalification_group mpg
                    INNER JOIN m_product_loan mpl ON mpl.id = mpg.product_id
                    INNER JOIN m_prequalification_group_members mpgm ON mpgm.group_id = mpg.id
                    INNER JOIN m_client mc ON mc.dpi = mpgm.dpi
                    LEFT JOIN m_group mg ON mg.prequalification_id = mpg.id
                    LEFT JOIN m_appuser ma ON ma.id = mpg.added_by
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public GroupPrequalificationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {
            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = PreQualificationsEnumerations.status(statusEnum);
            final Long id = JdbcSupport.getLong(rs, "id");
            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final String prequalificationNumber = rs.getString("prequalificationNumber");
            String groupName = rs.getString("groupName");
            final String productName = rs.getString("productName");
            final LocalDateTime createdAt = JdbcSupport.getLocalDateTime(rs, "created_at");
            final String addedBy = rs.getString("firstname") + " " + rs.getString("lastname");
            final Integer prequalificationTypeEnum = JdbcSupport.getInteger(rs, "prequalificationType");
            final EnumOptionData prequalificationType = PreQualificationsEnumerations.prequalificationType(prequalificationTypeEnum);
            GroupPrequalificationData prequalificationData = GroupPrequalificationData.simpeGroupData(id, prequalificationNumber, status,
                    groupName, productName, addedBy, createdAt, groupId);
            prequalificationData.setPrequalificationType(prequalificationType);
            return prequalificationData;
        }
    }

    private static final class PrequalificationsMemberMapper implements RowMapper<MemberPrequalificationData> {

        private final String schema;

        PrequalificationsMemberMapper() {
            this.schema = """
                    	m.id AS id,
                    	m.name,
                    	m.status,
                    	m.status,
                    	m.is_president as groupPresident,
                    	m.dpi,
                    	mc.id AS clientId,
                    	m.dob,
                    	m.buro_check_status as buroCheckStatus,
                    	m.requested_amount AS requestedAmount,
                    	m.approved_amount AS approvedAmount,
                    	COALESCE((SELECT sum(principal_disbursed_derived) FROM m_loan WHERE client_id = mc.id), 0) AS totalLoanAmount,
                    	COALESCE((SELECT sum(total_outstanding_derived) FROM m_loan WHERE client_id = mc.id), 0) AS totalLoanBalance,
                    	COALESCE((SELECT sum(mloan.total_outstanding_derived) FROM m_loan mloan INNER JOIN m_guarantor mg ON mg.loan_id = mloan.id WHERE mg.entity_id = mc.id), 0) AS totalGuaranteedLoanBalance,
                    	COALESCE((SELECT max(loan_counter) FROM m_loan WHERE client_id = mc.id), 0) AS noOfCycles,
                    	0 AS additionalCreditsCount,
                    	0 AS additionalCreditsSum,
                    	m.buro_nombre AS nombre,
                    	m.buro_id_tipo AS tipo,
                    	m.buro_id_numero as numero,
                    	m.buro_id_estado as estado,
                    	m.buro_fecha AS fecha,
                    	m.buro_cuentas AS cuentas,
                    	m.buro_resumen AS resumen,
                    	(
                    	SELECT
                    		count(*)
                    	FROM
                    		m_client_blacklist b
                    	WHERE
                    		b.dpi = m.dpi) AS blacklistCount,
                    	(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_client_blacklist mcb
                    	WHERE
                    		mcb.dpi = m.dpi
                    		AND mcb.status = 200) AS activeBlacklistCount,
                    	(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_client_blacklist mcb
                    	WHERE
                    		mcb.dpi = m.dpi
                    		AND mcb.status = 100) AS inActiveBlacklistCount,
                    	m.work_with_puente AS puente,
                    	(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_checklist_validation_result mcvr
                    	WHERE
                    		mcvr.validation_color_enum = 1
                    		AND mcvr.prequalification_type = 1
                    		AND mcvr.prequalification_member_id = m.id ) AS greenValidationCount,
                    		(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_checklist_validation_result mcvr
                    	WHERE
                    		mcvr.validation_color_enum = 2
                    		AND mcvr.prequalification_type = 1
                    		AND mcvr.prequalification_member_id = m.id ) AS yellowValidationCount,
                    	(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_checklist_validation_result mcvr
                    	WHERE
                    		mcvr.validation_color_enum = 3
                    		AND mcvr.prequalification_type = 1
                    		AND mcvr.prequalification_member_id = m.id ) AS orangeValidationCount,
                    			(
                    	SELECT
                    		COUNT(*)
                    	FROM
                    		m_checklist_validation_result mcvr
                    	WHERE
                    		mcvr.validation_color_enum = 4
                    		AND mcvr.prequalification_type = 1
                    		AND mcvr.prequalification_member_id = m.id ) AS redValidationCount
                    FROM
                    	m_prequalification_group_members m
                    LEFT JOIN m_client mc ON
                    	mc.dpi = m.dpi
                    """;
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public MemberPrequalificationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Integer statusEnum = JdbcSupport.getInteger(rs, "status");
            final EnumOptionData status = PreQualificationsMemberEnumerations.status(statusEnum);
            final Integer bureauStatus = rs.getInt("buroCheckStatus");
            EnumOptionData bureauCheckStatus = BuroCheckClassification.status(BuroCheckClassification.fromInt(bureauStatus).getId());
            final Long id = JdbcSupport.getLong(rs, "id");
            final String name = rs.getString("name");
            final String nombre = rs.getString("nombre");
            final String tipo = rs.getString("tipo");
            final String numero = rs.getString("numero");
            final String estado = rs.getString("estado");
            final LocalDateTime fecha = JdbcSupport.getLocalDateTime(rs, "fecha");
            final String cuentas = rs.getString("cuentas");
            final String resumen = rs.getString("resumen");
            final BuroData buroData = BuroData.builder().nombre(nombre).tipo(tipo).numero(numero).estado(estado).fecha(fecha)
                    .cuentas(cuentas).resumen(resumen).classification(bureauCheckStatus).build();
            final String dpi = rs.getString("dpi");
            final BigDecimal requestedAmount = rs.getBigDecimal("requestedAmount");
            final BigDecimal approvedAmount = rs.getBigDecimal("approvedAmount");
            final String puente = rs.getString("puente");
            final Long blacklistCount = rs.getLong("blacklistCount");
            final Long activeBlacklistCount = rs.getLong("activeBlacklistCount");
            final Long inActiveBlacklistCount = rs.getLong("inActiveBlacklistCount");
            final LocalDate dob = JdbcSupport.getLocalDate(rs, "dob");
            final BigDecimal totalLoanAmount = rs.getBigDecimal("totalLoanAmount");
            final BigDecimal totalLoanBalance = rs.getBigDecimal("totalLoanBalance");
            final BigDecimal totalGuaranteedLoanBalance = rs.getBigDecimal("totalGuaranteedLoanBalance");
            final Long noOfCycles = rs.getLong("noOfCycles");
            final Long additionalCreditsCount = rs.getLong("additionalCreditsCount");
            final BigDecimal additionalCreditsSum = rs.getBigDecimal("additionalCreditsSum");
            final Long redValidationCount = rs.getLong("redValidationCount");
            final Long orangeValidationCount = rs.getLong("orangeValidationCount");
            final Long greenValidationCount = rs.getLong("greenValidationCount");
            final Long yellowValidationCount = rs.getLong("yellowValidationCount");
            final Long clientId = rs.getLong("clientId");
            final Boolean groupPresident = rs.getBoolean("groupPresident");
            MemberPrequalificationData memberPrequalificationData = MemberPrequalificationData.instance(id, name, dpi, dob, puente,
                    requestedAmount, status, blacklistCount, totalLoanAmount, totalLoanBalance, totalGuaranteedLoanBalance, noOfCycles,
                    additionalCreditsCount, additionalCreditsSum, activeBlacklistCount, inActiveBlacklistCount, greenValidationCount,
                    yellowValidationCount, orangeValidationCount, redValidationCount, bureauCheckStatus, approvedAmount, groupPresident);
            memberPrequalificationData.setBuroData(buroData);
            memberPrequalificationData.setClientId(clientId);
            return memberPrequalificationData;
        }
    }

}
