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
package org.apache.fineract.portfolio.client.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.data.CodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;
import org.apache.fineract.infrastructure.configuration.data.GlobalConfigurationPropertyData;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.domain.JdbcSupport;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.dataqueries.data.DatatableData;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksReadService;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.utils.ColumnValidator;
import org.apache.fineract.organisation.office.data.OfficeData;
import org.apache.fineract.organisation.office.service.OfficeReadPlatformService;
import org.apache.fineract.organisation.staff.data.StaffData;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.address.service.AddressReadPlatformService;
import org.apache.fineract.portfolio.client.api.ClientApiConstants;
import org.apache.fineract.portfolio.client.data.ClientCollateralManagementData;
import org.apache.fineract.portfolio.client.data.ClientContactInformationData;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.data.ClientFamilyMembersData;
import org.apache.fineract.portfolio.client.data.ClientInfoRelatedDetailData;
import org.apache.fineract.portfolio.client.data.ClientNonPersonData;
import org.apache.fineract.portfolio.client.data.ClientTimelineData;
import org.apache.fineract.portfolio.client.data.EconomicActivityData;
import org.apache.fineract.portfolio.client.data.EconomicSectorData;
import org.apache.fineract.portfolio.client.domain.ClientContactInformationRepository;
import org.apache.fineract.portfolio.client.domain.ClientEnumerations;
import org.apache.fineract.portfolio.client.domain.ClientStatus;
import org.apache.fineract.portfolio.client.domain.LegalForm;
import org.apache.fineract.portfolio.client.exception.ClientNotFoundException;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagementRepositoryWrapper;
import org.apache.fineract.portfolio.group.data.GroupGeneralData;
import org.apache.fineract.portfolio.savings.data.SavingsProductData;
import org.apache.fineract.portfolio.savings.service.SavingsProductReadPlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

@Service
@RequiredArgsConstructor
public class ClientReadPlatformServiceImpl implements ClientReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PlatformSecurityContext context;
    private final OfficeReadPlatformService officeReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final CodeValueReadPlatformService codeValueReadPlatformService;
    private final SavingsProductReadPlatformService savingsProductReadPlatformService;
    // data mappers
    private final PaginationHelper paginationHelper;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final ClientMapper clientMapper = new ClientMapper();
    private final ClientLookupMapper lookupMapper = new ClientLookupMapper();
    private final ClientMembersOfGroupMapper membersOfGroupMapper = new ClientMembersOfGroupMapper();
    private final ParentGroupsMapper clientGroupsMapper = new ParentGroupsMapper();
    private final ContactInformationMapper contactInformationMapper = new ContactInformationMapper();
    private final ClientPublicServiceMapper publicServiceMapper = new ClientPublicServiceMapper();

    private final AddressReadPlatformService addressReadPlatformService;
    private final ClientFamilyMembersReadPlatformService clientFamilyMembersReadPlatformService;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private final EntityDatatableChecksReadService entityDatatableChecksReadService;
    private final ColumnValidator columnValidator;
    private final ClientCollateralManagementRepositoryWrapper clientCollateralManagementRepositoryWrapper;
    private final ClientContactInformationRepository clientContactInformationRepository;
    private final EconomicActivityReadPlatformService economicActivityReadPlatformService;

    @Override
    public ClientData retrieveTemplate(final Long officeId, final boolean staffInSelectedOfficeOnly) {
        this.context.authenticatedUser();

        final Long defaultOfficeId = defaultToUsersOfficeIfNull(officeId);
        AddressData address = null;

        final Collection<OfficeData> offices = this.officeReadPlatformService.retrieveAllOfficesForDropdown();

        final Collection<SavingsProductData> savingsProductDatas = this.savingsProductReadPlatformService.retrieveAllForLookupByType(null);

        final GlobalConfigurationPropertyData configuration = this.configurationReadPlatformService
                .retrieveGlobalConfiguration("Enable-Address");

        final Boolean isAddressEnabled = configuration.isEnabled();
        if (isAddressEnabled) {
            address = this.addressReadPlatformService.retrieveTemplate();
        }

        final ClientFamilyMembersData familyMemberOptions = this.clientFamilyMembersReadPlatformService.retrieveTemplate();

        Collection<StaffData> staffOptions = null;

        final boolean loanOfficersOnly = false;
        if (staffInSelectedOfficeOnly) {
            staffOptions = this.staffReadPlatformService.retrieveAllStaffForDropdown(defaultOfficeId);
        } else {
            staffOptions = this.staffReadPlatformService.retrieveAllStaffInOfficeAndItsParentOfficeHierarchy(defaultOfficeId,
                    loanOfficersOnly);
        }
        if (CollectionUtils.isEmpty(staffOptions)) {
            staffOptions = null;
        }
        final List<CodeValueData> genderOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.GENDER));

        final List<CodeValueData> clientTypeOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_TYPE));

        final List<CodeValueData> clientClassificationOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_CLASSIFICATION));

        final List<CodeValueData> clientNonPersonConstitutionOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_NON_PERSON_CONSTITUTION));

        final List<CodeValueData> clientNonPersonMainBusinessLineOptions = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(ClientApiConstants.CLIENT_NON_PERSON_MAIN_BUSINESS_LINE));

        final List<EnumOptionData> clientLegalFormOptions = ClientEnumerations.legalForm(LegalForm.values());

        List<EconomicSectorData> economicSectorData = this.economicActivityReadPlatformService.retrieveSectorData();
        List<EconomicActivityData> economicActivityData = this.economicActivityReadPlatformService.retrieveEconomicActivityData();

        final List<DatatableData> datatableTemplates = this.entityDatatableChecksReadService
                .retrieveTemplates(StatusEnum.CREATE.getCode().longValue(), EntityTables.CLIENT.getName(), null);
        return ClientData.template(defaultOfficeId, LocalDate.now(DateUtils.getDateTimeZoneOfTenant()), offices, staffOptions, null,
                genderOptions, savingsProductDatas, clientTypeOptions, clientClassificationOptions, clientNonPersonConstitutionOptions,
                clientNonPersonMainBusinessLineOptions, clientLegalFormOptions, familyMemberOptions,
                new ArrayList<AddressData>(Arrays.asList(address)), isAddressEnabled, datatableTemplates, economicSectorData,
                economicActivityData);
    }

    @Override
    // @Transactional(readOnly=true)
    public Page<ClientData> retrieveAll(final SearchParameters searchParameters) {

        if (searchParameters != null && searchParameters.getStatus() != null
                && ClientStatus.fromString(searchParameters.getStatus()) == ClientStatus.INVALID) {
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final String defaultUserMessage = "The Status value '" + searchParameters.getStatus() + "' is not supported.";
            final ApiParameterError error = ApiParameterError.parameterError("validation.msg.client.status.value.is.not.supported",
                    defaultUserMessage, "status", searchParameters.getStatus());
            dataValidationErrors.add(error);
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }

        final String userOfficeHierarchy = this.context.officeHierarchy();
        final String underHierarchySearchString = userOfficeHierarchy + "%";
        final String appUserID = String.valueOf(context.authenticatedUser().getId());

        // if (searchParameters.isScopedByOfficeHierarchy()) {
        // this.context.validateAccessRights(searchParameters.getHierarchy());
        // underHierarchySearchString = searchParameters.getHierarchy() + "%";
        // }
        List<Object> paramList = new ArrayList<>(Arrays.asList(underHierarchySearchString, underHierarchySearchString));
        final StringBuilder sqlBuilder = new StringBuilder(200);
        sqlBuilder.append("select " + sqlGenerator.calcFoundRows() + " ");
        sqlBuilder.append(this.clientMapper.schema());
        sqlBuilder.append(" where (o.hierarchy like ? or transferToOffice.hierarchy like ?) ");

        if (searchParameters != null) {
            if (searchParameters.isSelfUser()) {
                sqlBuilder.append(
                        " and c.id in (select umap.client_id from m_selfservice_user_client_mapping as umap where umap.appuser_id = ? ) ");
                paramList.add(appUserID);
            }

            final String extraCriteria = buildSqlStringFromClientCriteria(this.clientMapper.schema(), searchParameters, paramList);

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
        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), this.clientMapper);
    }

    private String buildSqlStringFromClientCriteria(String schemaSql, final SearchParameters searchParameters, List<Object> paramList) {

        String sqlSearch = searchParameters.getSqlSearch();
        final Long officeId = searchParameters.getOfficeId();
        final String externalId = searchParameters.getExternalId();
        final String displayName = searchParameters.getName();
        final String firstname = searchParameters.getFirstname();
        final String lastname = searchParameters.getLastname();
        final String status = searchParameters.getStatus();

        String extraCriteria = "";
        if (sqlSearch != null) {
            sqlSearch = sqlSearch.replaceAll(" display_name ", " c.display_name ");
            sqlSearch = sqlSearch.replaceAll("display_name ", "c.display_name ");
            extraCriteria = " and (" + sqlSearch + ")";
            this.columnValidator.validateSqlInjection(schemaSql, sqlSearch);
        }

        if (officeId != null) {
            extraCriteria += " and c.office_id = ? ";
            paramList.add(officeId);
        }

        if (externalId != null) {
            paramList.add(externalId);
            extraCriteria += " and c.external_id like ? ";
        }

        if (displayName != null) {
            // extraCriteria += " and concatcoalesce(c.firstname, ''),
            // if(c.firstname > '',' ', '') , coalesce(c.lastname, '')) like "
            paramList.add("%" + displayName + "%");
            extraCriteria += " and c.display_name like ? ";
        }

        if (status != null) {
            ClientStatus clientStatus = ClientStatus.fromString(status);
            extraCriteria += " and c.status_enum = " + clientStatus.getValue().toString() + " ";
        }

        if (firstname != null) {
            paramList.add(firstname);
            extraCriteria += " and c.firstname like ? ";
        }

        if (lastname != null) {
            paramList.add(lastname);
            extraCriteria += " and c.lastname like ? ";
        }

        if (searchParameters.isScopedByOfficeHierarchy()) {
            paramList.add(searchParameters.getHierarchy() + "%");
            extraCriteria += " and o.hierarchy like ? ";
        }

        if (searchParameters.isOrphansOnly()) {
            extraCriteria += " and c.id NOT IN (select client_id from m_group_client) ";
        }

        if (StringUtils.isNotBlank(extraCriteria)) {
            extraCriteria = extraCriteria.substring(4);
        }
        return extraCriteria;
    }

    @Override
    public ClientData retrieveOne(final Long clientId) {
        try {
            final String hierarchy = this.context.officeHierarchy();
            final String hierarchySearchString = hierarchy + "%";

            final String sql = "select " + this.clientMapper.schema()
                    + " where ( o.hierarchy like ? or transferToOffice.hierarchy like ?) and c.id = ?";
            final ClientData clientData = this.jdbcTemplate.queryForObject(sql, this.clientMapper, // NOSONAR
                    hierarchySearchString, hierarchySearchString, clientId);

            // Get client collaterals
            final Collection<ClientCollateralManagement> clientCollateralManagements = this.clientCollateralManagementRepositoryWrapper
                    .getCollateralsPerClient(clientId);
            final Set<ClientCollateralManagementData> clientCollateralManagementDataSet = new HashSet<>();

            // Map to client collateral data class
            for (ClientCollateralManagement clientCollateralManagement : clientCollateralManagements) {
                BigDecimal total = clientCollateralManagement.getTotal();
                BigDecimal totalCollateral = clientCollateralManagement.getTotalCollateral(total);
                clientCollateralManagementDataSet
                        .add(ClientCollateralManagementData.setCollateralValues(clientCollateralManagement, total, totalCollateral));
            }

            final String clientGroupsSql = "select " + this.clientGroupsMapper.parentGroupsSchema();

            final Collection<GroupGeneralData> parentGroups = this.jdbcTemplate.query(clientGroupsSql, this.clientGroupsMapper, // NOSONAR
                    clientId);

            final String contactSql = "select " + this.contactInformationMapper.schema();
            final List<ClientContactInformationData> contactInformationDatas = this.jdbcTemplate.query(contactSql,
                    this.contactInformationMapper, clientId);
            ClientContactInformationData contactInformationData = null;
            if (!contactInformationDatas.isEmpty()) {
                contactInformationData = contactInformationDatas.get(0);
                final String publicServiceSql = "SELECT " + this.publicServiceMapper.schema() + " WHERE mc.id = ?";
                final List<CodeValueData> publicServiceTypes = this.jdbcTemplate.query(publicServiceSql, this.publicServiceMapper,
                        clientId);
                contactInformationData.setPublicServiceTypes(publicServiceTypes);
            }
            return ClientData.setParentGroups(clientData, parentGroups, clientCollateralManagementDataSet, contactInformationData);

        } catch (final EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(clientId, e);
        }
    }

    @Override
    public ClientData retrieveOneLookup(Long clientId) {
        String sql = "select " + this.lookupMapper.schema();
        sql += " where c.id = ?";
        return this.jdbcTemplate.queryForObject(sql, this.lookupMapper, new Object[] { clientId });
    }

    @Override
    public Collection<ClientData> retrieveAllForLookup(final String extraCriteria) {

        String sql = "select " + this.lookupMapper.schema();

        if (StringUtils.isNotBlank(extraCriteria)) {
            sql += " and (" + extraCriteria + ")";
            this.columnValidator.validateSqlInjection(sql, extraCriteria);
        }
        return this.jdbcTemplate.query(sql, this.lookupMapper); // NOSONAR
    }

    @Override
    public Collection<ClientData> retrieveAllForLookupByOfficeId(final Long officeId) {

        final String sql = "select " + this.lookupMapper.schema() + " where c.office_id = ? and c.status_enum != ?";

        return this.jdbcTemplate.query(sql, this.lookupMapper, officeId, ClientStatus.CLOSED.getValue()); // NOSONAR
    }

    @Override
    public Collection<ClientData> retrieveClientMembersOfGroup(final Long groupId) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final String sql = "select " + this.membersOfGroupMapper.schema() + " where o.hierarchy like ? and pgc.group_id = ?";

        return this.jdbcTemplate.query(sql, this.membersOfGroupMapper, hierarchySearchString, groupId); // NOSONAR
    }

    @Override
    public Collection<ClientData> retrieveActiveClientMembersOfGroup(final Long groupId) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final String sql = "select " + this.membersOfGroupMapper.schema()
                + " where o.hierarchy like ? and pgc.group_id = ? and c.status_enum = ? ";

        return this.jdbcTemplate.query(sql, this.membersOfGroupMapper, // NOSONAR
                hierarchySearchString, groupId, ClientStatus.ACTIVE.getValue());
    }

    private static final class ClientMembersOfGroupMapper implements RowMapper<ClientData> {

        private final String schema;

        ClientMembersOfGroupMapper() {
            final StringBuilder sqlBuilder = new StringBuilder(200);

            sqlBuilder.append(
                    "c.id as id, c.account_no as accountNo, c.external_id as externalId, c.status_enum as statusEnum,c.sub_status as subStatus, c.dpi as dpiNumber, c.old_customer_number as oldCustomerNumber, ");
            sqlBuilder.append(
                    "cvSubStatus.code_value as subStatusValue,cvSubStatus.code_description as subStatusDesc,c.office_id as officeId, o.name as officeName, ");
            sqlBuilder.append("c.transfer_to_office_id as transferToOfficeId, transferToOffice.name as transferToOfficeName, ");
            sqlBuilder.append("c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, ");
            sqlBuilder.append("c.fullname as fullname, c.display_name as displayName, ");
            sqlBuilder.append("c.mobile_no as mobileNo, ");
            sqlBuilder.append("c.is_staff as isStaff, ");
            sqlBuilder.append("c.email_address as emailAddress, ");
            sqlBuilder.append("c.date_of_birth as dateOfBirth, ");
            sqlBuilder.append("c.gender_cv_id as genderId, ");
            sqlBuilder.append("cv.code_value as genderValue, ");
            sqlBuilder.append("c.client_type_cv_id as clienttypeId, ");
            sqlBuilder.append("cvclienttype.code_value as clienttypeValue, ");
            sqlBuilder.append("c.client_classification_cv_id as classificationId, ");
            sqlBuilder.append("cvclassification.code_value as classificationValue, ");
            sqlBuilder.append("c.legal_form_enum as legalFormEnum, ");
            sqlBuilder.append("c.activation_date as activationDate, c.image_id as imageId, ");
            sqlBuilder.append("c.staff_id as staffId, s.display_name as staffName,");
            sqlBuilder.append("c.default_savings_product as savingsProductId, sp.name as savingsProductName, ");
            sqlBuilder.append("c.default_savings_account as savingsAccountId, ");

            sqlBuilder.append("c.submittedon_date as submittedOnDate, ");
            sqlBuilder.append("sbu.username as submittedByUsername, ");
            sqlBuilder.append("sbu.firstname as submittedByFirstname, ");
            sqlBuilder.append("sbu.lastname as submittedByLastname, ");

            sqlBuilder.append("c.closedon_date as closedOnDate, ");
            sqlBuilder.append("clu.username as closedByUsername, ");
            sqlBuilder.append("clu.firstname as closedByFirstname, ");
            sqlBuilder.append("clu.lastname as closedByLastname, ");

            sqlBuilder.append("acu.username as activatedByUsername, ");
            sqlBuilder.append("acu.firstname as activatedByFirstname, ");
            sqlBuilder.append("acu.lastname as activatedByLastname, ");

            sqlBuilder.append("cnp.constitution_cv_id as constitutionId, ");
            sqlBuilder.append("cvConstitution.code_value as constitutionValue, ");
            sqlBuilder.append("cnp.incorp_no as incorpNo, ");
            sqlBuilder.append("cnp.incorp_validity_till as incorpValidityTill, ");
            sqlBuilder.append("cnp.main_business_line_cv_id as mainBusinessLineId, ");
            sqlBuilder.append("cvMainBusinessLine.code_value as mainBusinessLineValue, ");
            sqlBuilder.append("cnp.remarks as remarks ");

            sqlBuilder.append("from m_client c ");
            sqlBuilder.append("join m_office o on o.id = c.office_id ");
            sqlBuilder.append("left join m_client_non_person cnp on cnp.client_id = c.id ");
            sqlBuilder.append("join m_group_client pgc on pgc.client_id = c.id ");
            sqlBuilder.append("left join m_staff s on s.id = c.staff_id ");
            sqlBuilder.append("left join m_savings_product sp on sp.id = c.default_savings_product ");
            sqlBuilder.append("left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");

            sqlBuilder.append("left join m_appuser sbu on sbu.id = c.created_by ");
            sqlBuilder.append("left join m_appuser acu on acu.id = c.activatedon_userid ");
            sqlBuilder.append("left join m_appuser clu on clu.id = c.closedon_userid ");
            sqlBuilder.append("left join m_code_value cv on cv.id = c.gender_cv_id ");
            sqlBuilder.append("left join m_code_value cvclienttype on cvclienttype.id = c.client_type_cv_id ");
            sqlBuilder.append("left join m_code_value cvclassification on cvclassification.id = c.client_classification_cv_id ");
            sqlBuilder.append("left join m_code_value cvSubStatus on cvSubStatus.id = c.sub_status ");
            sqlBuilder.append("left join m_code_value cvConstitution on cvConstitution.id = cnp.constitution_cv_id ");
            sqlBuilder.append("left join m_code_value cvMainBusinessLine on cvMainBusinessLine.id = cnp.main_business_line_cv_id ");

            this.schema = sqlBuilder.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public ClientData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final String accountNo = rs.getString("accountNo");

            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final EnumOptionData status = ClientEnumerations.status(statusEnum);

            final Long subStatusId = JdbcSupport.getLong(rs, "subStatus");
            final String subStatusValue = rs.getString("subStatusValue");
            final String subStatusDesc = rs.getString("subStatusDesc");
            final boolean isActive = false;
            final CodeValueData subStatus = CodeValueData.instance(subStatusId, subStatusValue, subStatusDesc, isActive);

            final Long officeId = JdbcSupport.getLong(rs, "officeId");
            final String officeName = rs.getString("officeName");

            final Long transferToOfficeId = JdbcSupport.getLong(rs, "transferToOfficeId");
            final String transferToOfficeName = rs.getString("transferToOfficeName");

            final Long id = JdbcSupport.getLong(rs, "id");
            final String firstname = rs.getString("firstname");
            final String middlename = rs.getString("middlename");
            final String lastname = rs.getString("lastname");
            final String fullname = rs.getString("fullname");
            final String displayName = rs.getString("displayName");
            final String externalId = rs.getString("externalId");
            final String mobileNo = rs.getString("mobileNo");
            final boolean isStaff = rs.getBoolean("isStaff");
            final String emailAddress = rs.getString("emailAddress");
            final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
            final Long genderId = JdbcSupport.getLong(rs, "genderId");
            final String genderValue = rs.getString("genderValue");
            final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

            final Long clienttypeId = JdbcSupport.getLong(rs, "clienttypeId");
            final String clienttypeValue = rs.getString("clienttypeValue");
            final CodeValueData clienttype = CodeValueData.instance(clienttypeId, clienttypeValue);

            final Long classificationId = JdbcSupport.getLong(rs, "classificationId");
            final String classificationValue = rs.getString("classificationValue");
            final CodeValueData classification = CodeValueData.instance(classificationId, classificationValue);

            final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
            final Long imageId = JdbcSupport.getLong(rs, "imageId");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffName = rs.getString("staffName");

            final Long savingsProductId = JdbcSupport.getLong(rs, "savingsProductId");
            final String savingsProductName = rs.getString("savingsProductName");

            final Long savingsAccountId = JdbcSupport.getLong(rs, "savingsAccountId");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final Integer legalFormEnum = JdbcSupport.getInteger(rs, "legalFormEnum");
            EnumOptionData legalForm = null;
            if (legalFormEnum != null) {
                legalForm = ClientEnumerations.legalForm(legalFormEnum);
            }

            final Long constitutionId = JdbcSupport.getLong(rs, "constitutionId");
            final String constitutionValue = rs.getString("constitutionValue");
            final CodeValueData constitution = CodeValueData.instance(constitutionId, constitutionValue);
            final String incorpNo = rs.getString("incorpNo");
            final LocalDate incorpValidityTill = JdbcSupport.getLocalDate(rs, "incorpValidityTill");
            final Long mainBusinessLineId = JdbcSupport.getLong(rs, "mainBusinessLineId");
            final String mainBusinessLineValue = rs.getString("mainBusinessLineValue");
            final CodeValueData mainBusinessLine = CodeValueData.instance(mainBusinessLineId, mainBusinessLineValue);
            final String remarks = rs.getString("remarks");
            final String dpiNumber = rs.getString("dpiNumber");
            final String oldCustomerNumber = rs.getString("oldCustomerNumber");

            final ClientNonPersonData clientNonPerson = new ClientNonPersonData(constitution, incorpNo, incorpValidityTill,
                    mainBusinessLine, remarks);

            final ClientTimelineData timeline = new ClientTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname,
                    submittedByLastname, activationDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate,
                    closedByUsername, closedByFirstname, closedByLastname);

            return ClientData.instance(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id,
                    firstname, middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender,
                    activationDate, imageId, staffId, staffName, timeline, savingsProductId, savingsProductName, savingsAccountId,
                    clienttype, classification, legalForm, clientNonPerson, isStaff, dpiNumber, oldCustomerNumber,
                    null,null,null,null,null,null,null);

        }
    }

    @Override
    public Collection<ClientData> retrieveActiveClientMembersOfCenter(final Long centerId) {

        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";

        final String sql = "select " + this.membersOfGroupMapper.schema()
                + " left join m_group g on pgc.group_id=g.id where o.hierarchy like ? and g.parent_id = ? and c.status_enum = ? group by c.id";

        return this.jdbcTemplate.query(sql, this.membersOfGroupMapper, // NOSONAR
                hierarchySearchString, centerId, ClientStatus.ACTIVE.getValue());
    }

    private static final class ClientMapper implements RowMapper<ClientData> {

        private final String schema;

        ClientMapper() {
            final StringBuilder builder = new StringBuilder(400);

            builder.append("c.id as id, c.account_no as accountNo, c.external_id as externalId, c.status_enum as statusEnum,c.sub_status as subStatus, c.dpi as dpiNumber, c.old_customer_number as oldCustomerNumber, ");
            builder.append("c.firstlastname, c.secondlastname, municipalityDpiCV.code_value as municipalityDpiName, ");
            builder.append("municipalityDpiCV.id as municipalityDpi, departmentDpiCV.code_value as departmentDpiName, departmentDpiCV.id as departmentDpi, ");
            builder.append("c.loan_cycle as loanCycle, c.group_member as groupMember, c.group_number as groupNumber, "
                    + "c.status_in_group as statusInGroup, c.retirement_reason as retirementReason, c.other_names as othernames, "
                    + "c.maiden_name as maidenName, c.civil_status as civilStatus, c.family_reference as familyReference, "
                    + "c.ethinicity, c.education_level as educationLevel, c.nationality, c.languages, c.economic_sector as economicSector, "
                    + " c.economic_activity as economicActivity, ");
            builder.append(
                    "cvSubStatus.code_value as subStatusValue,cvSubStatus.code_description as subStatusDesc,c.office_id as officeId, o.name as officeName, ");
            builder.append("c.transfer_to_office_id as transferToOfficeId, transferToOffice.name as transferToOfficeName, ");
            builder.append("c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, ");
            builder.append("c.fullname as fullname, c.display_name as displayName, ");
            builder.append("c.mobile_no as mobileNo, ");
            builder.append("c.is_staff as isStaff, ");
            builder.append("c.email_address as emailAddress, ");
            builder.append("c.date_of_birth as dateOfBirth, ");
            builder.append("c.gender_cv_id as genderId, ");
            builder.append("cv.code_value as genderValue, ");
            builder.append("c.client_type_cv_id as clienttypeId, ");
            builder.append("cvclienttype.code_value as clienttypeValue, ");
            builder.append("c.client_classification_cv_id as classificationId, ");
            builder.append("cvclassification.code_value as classificationValue, ");
            builder.append("c.legal_form_enum as legalFormEnum, ");

            builder.append("c.submittedon_date as submittedOnDate, ");
            builder.append("sbu.username as submittedByUsername, ");
            builder.append("sbu.firstname as submittedByFirstname, ");
            builder.append("sbu.lastname as submittedByLastname, ");

            builder.append("c.closedon_date as closedOnDate, ");
            builder.append("clu.username as closedByUsername, ");
            builder.append("clu.firstname as closedByFirstname, ");
            builder.append("clu.lastname as closedByLastname, ");

            // builder.append("c.submittedon as submittedOnDate, ");
            builder.append("acu.username as activatedByUsername, ");
            builder.append("acu.firstname as activatedByFirstname, ");
            builder.append("acu.lastname as activatedByLastname, ");

            builder.append("cnp.constitution_cv_id as constitutionId, ");
            builder.append("cvConstitution.code_value as constitutionValue, ");
            builder.append("cnp.incorp_no as incorpNo, ");
            builder.append("cnp.incorp_validity_till as incorpValidityTill, ");
            builder.append("cnp.main_business_line_cv_id as mainBusinessLineId, ");
            builder.append("cvMainBusinessLine.code_value as mainBusinessLineValue, ");
            builder.append("cnp.remarks as remarks, ");

            builder.append("c.activation_date as activationDate, c.image_id as imageId, ");
            builder.append("c.staff_id as staffId, s.display_name as staffName, ");
            builder.append("c.default_savings_product as savingsProductId, sp.name as savingsProductName, ");
            builder.append("c.default_savings_account as savingsAccountId ");
            builder.append("from m_client c ");
            builder.append("join m_office o on o.id = c.office_id ");
            builder.append("left join m_client_non_person cnp on cnp.client_id = c.id ");
            builder.append("left join m_staff s on s.id = c.staff_id ");
            builder.append("left join m_savings_product sp on sp.id = c.default_savings_product ");
            builder.append("left join m_office transferToOffice on transferToOffice.id = c.transfer_to_office_id ");
            builder.append("left join m_appuser sbu on sbu.id = c.created_by ");
            builder.append("left join m_appuser acu on acu.id = c.activatedon_userid ");
            builder.append("left join m_appuser clu on clu.id = c.closedon_userid ");
            builder.append("left join m_code_value cv on cv.id = c.gender_cv_id ");
            builder.append("left join m_code_value cvclienttype on cvclienttype.id = c.client_type_cv_id ");
            builder.append("left join m_code_value cvclassification on cvclassification.id = c.client_classification_cv_id ");
            builder.append("left join m_code_value cvSubStatus on cvSubStatus.id = c.sub_status ");
            builder.append("left join m_code_value cvConstitution on cvConstitution.id = cnp.constitution_cv_id ");
            builder.append("left join m_code_value cvMainBusinessLine on cvMainBusinessLine.id = cnp.main_business_line_cv_id ");
            builder.append("left join m_code_value departmentDpiCV on departmentDpiCV.id = c.department_dpi ");
            builder.append("left join m_code_value municipalityDpiCV on municipalityDpiCV.id = c.municipality_dpi ");

            this.schema = builder.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public ClientData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final String accountNo = rs.getString("accountNo");

            final Integer statusEnum = JdbcSupport.getInteger(rs, "statusEnum");
            final EnumOptionData status = ClientEnumerations.status(statusEnum);

            final Long subStatusId = JdbcSupport.getLong(rs, "subStatus");
            final String subStatusValue = rs.getString("subStatusValue");
            final String subStatusDesc = rs.getString("subStatusDesc");
            final boolean isActive = false;
            final CodeValueData subStatus = CodeValueData.instance(subStatusId, subStatusValue, subStatusDesc, isActive);

            final Long officeId = JdbcSupport.getLong(rs, "officeId");
            final String officeName = rs.getString("officeName");

            final Long transferToOfficeId = JdbcSupport.getLong(rs, "transferToOfficeId");
            final String transferToOfficeName = rs.getString("transferToOfficeName");

            final Long id = JdbcSupport.getLong(rs, "id");
            final String firstname = rs.getString("firstname");
            final String middlename = rs.getString("middlename");
            final String lastname = rs.getString("lastname");
            final String fullname = rs.getString("fullname");
            final String displayName = rs.getString("displayName");
            final String externalId = rs.getString("externalId");
            final String mobileNo = rs.getString("mobileNo");
            final boolean isStaff = rs.getBoolean("isStaff");
            final String emailAddress = rs.getString("emailAddress");
            final LocalDate dateOfBirth = JdbcSupport.getLocalDate(rs, "dateOfBirth");
            final Long genderId = JdbcSupport.getLong(rs, "genderId");
            final String genderValue = rs.getString("genderValue");
            final CodeValueData gender = CodeValueData.instance(genderId, genderValue);

            final Long clienttypeId = JdbcSupport.getLong(rs, "clienttypeId");
            final String clienttypeValue = rs.getString("clienttypeValue");
            final CodeValueData clienttype = CodeValueData.instance(clienttypeId, clienttypeValue);

            final Long classificationId = JdbcSupport.getLong(rs, "classificationId");
            final String classificationValue = rs.getString("classificationValue");
            final CodeValueData classification = CodeValueData.instance(classificationId, classificationValue);

            final LocalDate activationDate = JdbcSupport.getLocalDate(rs, "activationDate");
            final Long imageId = JdbcSupport.getLong(rs, "imageId");
            final Long staffId = JdbcSupport.getLong(rs, "staffId");
            final String staffName = rs.getString("staffName");

            final Long savingsProductId = JdbcSupport.getLong(rs, "savingsProductId");
            final String savingsProductName = rs.getString("savingsProductName");
            final Long savingsAccountId = JdbcSupport.getLong(rs, "savingsAccountId");

            final LocalDate closedOnDate = JdbcSupport.getLocalDate(rs, "closedOnDate");
            final String closedByUsername = rs.getString("closedByUsername");
            final String closedByFirstname = rs.getString("closedByFirstname");
            final String closedByLastname = rs.getString("closedByLastname");

            final LocalDate submittedOnDate = JdbcSupport.getLocalDate(rs, "submittedOnDate");
            final String submittedByUsername = rs.getString("submittedByUsername");
            final String submittedByFirstname = rs.getString("submittedByFirstname");
            final String submittedByLastname = rs.getString("submittedByLastname");

            final String activatedByUsername = rs.getString("activatedByUsername");
            final String activatedByFirstname = rs.getString("activatedByFirstname");
            final String activatedByLastname = rs.getString("activatedByLastname");

            final Integer legalFormEnum = JdbcSupport.getInteger(rs, "legalFormEnum");
            EnumOptionData legalForm = null;
            if (legalFormEnum != null) {
                legalForm = ClientEnumerations.legalForm(legalFormEnum);
            }

            final Long constitutionId = JdbcSupport.getLong(rs, "constitutionId");
            final String constitutionValue = rs.getString("constitutionValue");
            final CodeValueData constitution = CodeValueData.instance(constitutionId, constitutionValue);
            final String incorpNo = rs.getString("incorpNo");
            final LocalDate incorpValidityTill = JdbcSupport.getLocalDate(rs, "incorpValidityTill");
            final Long mainBusinessLineId = JdbcSupport.getLong(rs, "mainBusinessLineId");
            final String mainBusinessLineValue = rs.getString("mainBusinessLineValue");
            final CodeValueData mainBusinessLine = CodeValueData.instance(mainBusinessLineId, mainBusinessLineValue);
            final String remarks = rs.getString("remarks");
            final String dpiNumber = rs.getString("dpiNumber");
            final String oldCustomerNumber = rs.getString("oldCustomerNumber");
            final Integer loanCycle = rs.getInt("loanCycle");
            final String groupMember = rs.getString("groupMember");
            final String groupNumber = rs.getString("groupNumber");
            final String statusInGroup = rs.getString("statusInGroup");
            final String retirementReason = rs.getString("retirementReason");
            final String othernames = rs.getString("othernames");
            final String maidenName = rs.getString("maidenName");
            final String civilStatus = rs.getString("civilStatus");
            final String familyReference = rs.getString("familyReference");
            final String ethinicity = rs.getString("ethinicity");
            final String educationLevel = rs.getString("educationLevel");
            final String nationality = rs.getString("nationality");
            final String languages = rs.getString("languages");
            final String economicSector = rs.getString("economicSector");
            final String economicActivity = rs.getString("economicActivity");
            final String firstlastname = rs.getString("firstlastname");
            final String secondlastname = rs.getString("secondlastname");
            final String departmentDpiName = rs.getString("departmentDpiName");
            final Long departmentDpi = rs.getLong("departmentDpi");
            final String municipalityDpiName = rs.getString("municipalityDpiName");
            final Long municipalityDpi = rs.getLong("municipalityDpi");

            ClientInfoRelatedDetailData detailData = ClientInfoRelatedDetailData.instance(loanCycle, groupNumber, maidenName, othernames,
                    groupMember, statusInGroup, retirementReason, civilStatus, educationLevel, ethinicity, nationality, languages,
                    economicSector, economicActivity, familyReference);
            final ClientNonPersonData clientNonPerson = new ClientNonPersonData(constitution, incorpNo, incorpValidityTill,
                    mainBusinessLine, remarks);

            final ClientTimelineData timeline = new ClientTimelineData(submittedOnDate, submittedByUsername, submittedByFirstname,
                    submittedByLastname, activationDate, activatedByUsername, activatedByFirstname, activatedByLastname, closedOnDate,
                    closedByUsername, closedByFirstname, closedByLastname);

            return ClientData.instance(accountNo, status, subStatus, officeId, officeName, transferToOfficeId, transferToOfficeName, id,
                    firstname, middlename, lastname, fullname, displayName, externalId, mobileNo, emailAddress, dateOfBirth, gender,
                    activationDate, imageId, staffId, staffName, timeline, savingsProductId, savingsProductName, savingsAccountId,
                    clienttype, classification, legalForm, clientNonPerson, isStaff, dpiNumber, oldCustomerNumber, detailData,firstlastname,
                    secondlastname, departmentDpiName, departmentDpi, municipalityDpiName, municipalityDpi);

        }
    }

    private static final class ParentGroupsMapper implements RowMapper<GroupGeneralData> {

        public String parentGroupsSchema() {
            return "gp.id As groupId , gp.account_no as accountNo, gp.display_name As groupName from m_client cl JOIN m_group_client gc ON cl.id = gc.client_id "
                    + "JOIN m_group gp ON gp.id = gc.group_id WHERE cl.id  = ?";
        }

        @Override
        public GroupGeneralData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Long groupId = JdbcSupport.getLong(rs, "groupId");
            final String groupName = rs.getString("groupName");
            final String accountNo = rs.getString("accountNo");

            return GroupGeneralData.lookup(groupId, accountNo, groupName);
        }
    }

    private static final class ContactInformationMapper implements RowMapper<ClientContactInformationData> {

        public String schema() {
            return "ci.id , " + "acode.code_value as areaValue, " + "htype.code_value as housingValue, " + "ci.years_of_residence, "
                    + "dep.code_value as department, " + "mun.code_value as municipality, " + "ci.village, " + "ci.reference_housing_data, "
                    + "ci.street, " + "ci.avenue, " + "ci.home_number, " + "ci.colony, " + "ci.sector, " + "ci.batch, " + "ci.square, "
                    + "ci.zone, " + "ci.light_meter_number, " + "ci.home_phone, ci.years_of_community, ci.street_number "
                    + "from m_client_contact_info ci " + "left join m_code_value acode on acode.id = ci.area "
                    + "left join m_code_value htype on htype.id = ci.housing_type "
                    + "left join m_code_value dep on dep.id = ci.department_id "
                    + "left join m_code_value mun on mun.id = ci.municipality_id " + "WHERE ci.client_id  = ?";
        }

        @Override
        public ClientContactInformationData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final String area = rs.getString("areaValue");
            final String housing = rs.getString("housingValue");
            final String yearsOfResidence = rs.getString("years_of_residence");
            final String department = rs.getString("department");
            final String municipality = rs.getString("municipality");
            final String village = rs.getString("village");
            final String referenceHousingData = rs.getString("reference_housing_data");
            final String street = rs.getString("street");
            final String avenue = rs.getString("avenue");
            final String homeNumber = rs.getString("home_number");
            final String colony = rs.getString("colony");
            final String sector = rs.getString("sector");
            final String batch = rs.getString("batch");
            final String square = rs.getString("square");
            final String zone = rs.getString("zone");
            final String lightMeterNumber = rs.getString("light_meter_number");
            final String homePhone = rs.getString("home_phone");
            final Integer communityYears = JdbcSupport.getInteger(rs, "years_of_community");
            final String streetNumber = rs.getString("street_number");
            return ClientContactInformationData.instance(area, housing, yearsOfResidence, department, municipality, village,
                    referenceHousingData, street, avenue, homeNumber, colony, sector, batch, square, zone, lightMeterNumber, homePhone,
                    communityYears, streetNumber);
        }
    }

    private static final class ClientLookupMapper implements RowMapper<ClientData> {

        private final String schema;

        ClientLookupMapper() {
            final StringBuilder builder = new StringBuilder(200);

            builder.append("c.id as id, c.display_name as displayName, ");
            builder.append("c.office_id as officeId, o.name as officeName ");
            builder.append("from m_client c ");
            builder.append("join m_office o on o.id = c.office_id ");

            this.schema = builder.toString();
        }

        public String schema() {
            return this.schema;
        }

        @Override
        public ClientData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String displayName = rs.getString("displayName");
            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");

            return ClientData.lookup(id, displayName, officeId, officeName);
        }
    }

    @Override
    public ClientData retrieveClientByIdentifier(final Long identifierTypeId, final String identifierKey) {
        try {
            final ClientIdentifierMapper mapper = new ClientIdentifierMapper();

            final String sql = "select " + mapper.clientLookupByIdentifierSchema();

            return this.jdbcTemplate.queryForObject(sql, mapper, identifierTypeId, identifierKey); // NOSONAR
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    private static final class ClientPublicServiceMapper implements RowMapper<CodeValueData> {

        public String schema() {
            return """
                    mcv.id AS id,
                    mcv.code_value AS name
                    FROM m_code_value mcv
                    INNER JOIN m_client_public_service mcps ON mcps.service_type_cv_id = mcv.id
                    INNER JOIN m_client mc ON mc.id = mcps.client_id
                    """;
        }

        @Override
        public CodeValueData mapRow(@NotNull ResultSet rs, int rowNum) throws SQLException {
            final Long id = rs.getLong("id");
            final String name = rs.getString("name");
            return CodeValueData.instance(id, name);

        }
    }

    private static final class ClientIdentifierMapper implements RowMapper<ClientData> {

        public String clientLookupByIdentifierSchema() {
            return "c.id as id, c.account_no as accountNo, c.firstname as firstname, c.middlename as middlename, c.lastname as lastname, "
                    + "c.fullname as fullname, c.display_name as displayName," + "c.office_id as officeId, o.name as officeName "
                    + " from m_client c, m_office o, m_client_identifier ci " + "where o.id = c.office_id and c.id=ci.client_id "
                    + "and ci.document_type_id= ? and ci.document_key like ?";
        }

        @Override
        public ClientData mapRow(final ResultSet rs, final int rowNum) throws SQLException {

            final Long id = rs.getLong("id");
            final String accountNo = rs.getString("accountNo");

            final String firstname = rs.getString("firstname");
            final String middlename = rs.getString("middlename");
            final String lastname = rs.getString("lastname");
            final String fullname = rs.getString("fullname");
            final String displayName = rs.getString("displayName");

            final Long officeId = rs.getLong("officeId");
            final String officeName = rs.getString("officeName");

            return ClientData.clientIdentifier(id, accountNo, firstname, middlename, lastname, fullname, displayName, officeId, officeName);
        }
    }

    private Long defaultToUsersOfficeIfNull(final Long officeId) {
        Long defaultOfficeId = officeId;
        if (defaultOfficeId == null) {
            defaultOfficeId = this.context.authenticatedUser().getOffice().getId();
        }
        return defaultOfficeId;
    }

    @Override
    public ClientData retrieveAllNarrations(final String clientNarrations) {
        final List<CodeValueData> narrations = new ArrayList<>(
                this.codeValueReadPlatformService.retrieveCodeValuesByCode(clientNarrations));
        final Collection<CodeValueData> clientTypeOptions = null;
        final Collection<CodeValueData> clientClassificationOptions = null;
        final Collection<CodeValueData> clientNonPersonConstitutionOptions = null;
        final Collection<CodeValueData> clientNonPersonMainBusinessLineOptions = null;
        final List<EnumOptionData> clientLegalFormOptions = null;
        return ClientData.template(null, null, null, null, narrations, null, null, clientTypeOptions, clientClassificationOptions,
                clientNonPersonConstitutionOptions, clientNonPersonMainBusinessLineOptions, clientLegalFormOptions, null, null, null, null,
                null, null);
    }

    @Override
    public LocalDate retrieveClientTransferProposalDate(Long clientId) {
        validateClient(clientId);
        final String sql = "SELECT cl.proposed_transfer_date FROM m_client cl WHERE cl.id =? ";
        try {
            return this.jdbcTemplate.queryForObject(sql, LocalDate.class, clientId);
        } catch (final EmptyResultDataAccessException e) {
            return null;
        }
    }

    @Override
    public void validateClient(Long clientId) {
        try {
            final String sql = "SELECT cl.id FROM m_client cl WHERE cl.id =? ";
            this.jdbcTemplate.queryForObject(sql, Long.class, clientId);
        } catch (final EmptyResultDataAccessException e) {
            throw new ClientNotFoundException(clientId, e);
        }
    }

    @Override
    public Collection<Long> retrieveUserClients(Long aUserID) {
        String sql = "SELECT  m.client_id FROM m_selfservice_user_client_mapping m INNER JOIN m_client c ON c.id = m.client_id WHERE m.appuser_id = ?";
        return jdbcTemplate.queryForList(sql, Long.class, aUserID);
    }
}
