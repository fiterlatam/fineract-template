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
package org.apache.fineract.infrastructure.core.service;

import org.apache.commons.lang3.StringUtils;

public final class SearchParameters {

    private final String sqlSearch;
    private final Long officeId;
    private final String externalId;
    private final String name;
    private final String hierarchy;
    private final String firstname;
    private final String lastname;
    private final String status;
    private final Integer offset;
    private final Integer limit;
    private final String orderBy;
    private final String sortOrder;
    private final String accountNo;
    private final String currencyCode;

    private final Long staffId;

    private final Long loanId;

    private final Long savingsId;
    private final Boolean orphansOnly;

    // Provisning Entries Search Params
    private final Long provisioningEntryId;
    private final Long productId;
    private final Long categoryId;
    private final boolean isSelfUser;

    // Prequalification Search Params
    private final String dpiNumber;
    private final String type;
    private final String groupName;
    private final String centerName;

    private final String accountNumber;
    private final String bankName;
    private final String bankCode;
    private final String chequeNo;
    private final Long batchId;
    private final Long chequeId;

    public static SearchParameters from(final String sqlSearch, final Long officeId, final String externalId, final String name,
            final String hierarchy) {
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;
        return new SearchParameters(sqlSearch, officeId, externalId, name, hierarchy, null, null, null, null, null, null, staffId,
                accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forClients(final String sqlSearch, final Long officeId, final String externalId,
            final String displayName, final String firstname, final String lastname, final String status, final String hierarchy,
            final Integer offset, final Integer limit, final String orderBy, final String sortOrder, final Boolean orphansOnly,
            final boolean isSelfUser) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;

        return new SearchParameters(sqlSearch, officeId, externalId, displayName, hierarchy, firstname, lastname, status, offset,
                maxLimitAllowed, orderBy, sortOrder, staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser, null);
    }

    public static SearchParameters forBlacklist(final String displayName, final String status, final Integer offset, final Integer limit,
            final String orderBy, final String sortOrder, final String dpiNumber, String searchText) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;

        return new SearchParameters(searchText, null, null, displayName, null, null, null, status, offset, maxLimitAllowed, orderBy,
                sortOrder, staffId, accountNo, loanId, savingsId, null, false, dpiNumber);
    }

    public static SearchParameters forPrequalification(final String displayName, final String status, final Integer offset,
            final Integer limit, final String orderBy, final String sortOrder, final String type, String searchText) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;

        return new SearchParameters(searchText, null, null, displayName, null, null, null, status, offset, maxLimitAllowed, orderBy,
                sortOrder, staffId, accountNo, loanId, savingsId, null, false, null, type, null, null, null);
    }

    public static SearchParameters forBankCheques(final String chequeNo, final Long batchId, final Long chequeId, final String status,
            final Integer offset, final Integer limit, final String orderBy, final String sortOrder) {
        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final String accountNo = null;
        final String name = null;
        final String hierarchy = null;
        final String externalId = null;
        final Long staffId = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Long officeId = null;
        final boolean isSelfUser = false;
        final boolean orphansOnly = false;
        return new SearchParameters(officeId, externalId, name, hierarchy, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                accountNo, loanId, savingsId, orphansOnly, isSelfUser, chequeNo, status, batchId, chequeId);
    }

    public static SearchParameters forGroups(final Long officeId, final Long staffId, final String externalId, final String name,
            final String hierarchy, final Integer offset, final Integer limit, final String orderBy, final String sortOrder,
            final Boolean orphansOnly) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final boolean isSelfUser = false;
        final String chequeNo = null;
        final Long batchId = null;
        final Long chequeId = null;
        final String status = null;

        return new SearchParameters(officeId, externalId, name, hierarchy, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                accountNo, loanId, savingsId, orphansOnly, isSelfUser, chequeNo, status, batchId, chequeId);
    }

    public static SearchParameters forOffices(final String orderBy, final String sortOrder) {
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;
        return new SearchParameters(null, null, null, null, null, null, null, null, null, orderBy, sortOrder, null, null, null, null,
                orphansOnly, isSelfUser);
    }

    public static SearchParameters forLoans(final String sqlSearch, final String externalId, final Integer offset, final Integer limit,
            final String orderBy, final String sortOrder, final String accountNo) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(sqlSearch, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forJournalEntries(final Long officeId, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder, final Long loanId, final Long savingsId) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(null, officeId, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                null, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forJournalEntries(final Long officeId, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder, final Long loanId, final Long savingsId, final String currencyCode) {
        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Boolean orphansOnly = false;

        return new SearchParameters(null, officeId, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                null, loanId, savingsId, orphansOnly, currencyCode);
    }

    public static SearchParameters forPagination(final Integer offset, final Integer limit, final String orderBy, final String sortOrder) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId, null,
                loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forPaginationAndAccountNumberSearch(final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder, final String accountNumber) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                accountNumber, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forPagination(final Integer offset, final Integer limit) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final String orderBy = null;
        final String sortOrder = null;
        final boolean isSelfUser = false;

        return new SearchParameters(null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId, null,
                loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forProvisioningEntries(final Long provisioningEntryId, final Long officeId, final Long productId,
            final Long categoryId, final Integer offset, final Integer limit) {
        return new SearchParameters(provisioningEntryId, officeId, productId, categoryId, offset, limit);
    }

    public static SearchParameters forSavings(final String sqlSearch, final String externalId, final Integer offset, final Integer limit,
            final String orderBy, final String sortOrder) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(sqlSearch, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forAccountTransfer(final String sqlSearch, final String externalId, final Integer offset,
            final Integer limit, final String orderBy, final String sortOrder) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(sqlSearch, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forSMSCampaign(final String sqlSearch, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder) {

        final String externalId = null;
        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(sqlSearch, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forEmailCampaign(final String sqlSearch, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder) {

        final String externalId = null;
        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;

        return new SearchParameters(sqlSearch, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }

    public static SearchParameters forBanks(final Integer offset, final Integer limit, final String orderBy, final String sortOrder,
            String searchText) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;

        return new SearchParameters(searchText, null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, null, false, null);
    }

    public static SearchParameters forBankAccounts(final Integer offset, final Integer limit, final String orderBy, final String sortOrder,
            String searchText, final String accountNumber, final String bankName, final String bankCode) {

        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;

        return new SearchParameters(searchText, null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder,
                staffId, accountNo, loanId, savingsId, null, false, null, accountNumber, bankName, bankCode);
    }

    private SearchParameters(final String sqlSearch, final Long officeId, final String externalId, final String name,
            final String hierarchy, final String firstname, final String lastname, final Integer offset, final Integer limit,
            final String orderBy, final String sortOrder, final Long staffId, final String accountNo, final Long loanId,
            final Long savingsId, final Boolean orphansOnly, boolean isSelfUser) {
        this.sqlSearch = sqlSearch;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = null;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = isSelfUser;
        this.status = null;
        this.dpiNumber = null;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    private SearchParameters(final String sqlSearch, final Long officeId, final String externalId, final String name,
            final String hierarchy, final String firstname, final String lastname, final String status, final Integer offset,
            final Integer limit, final String orderBy, final String sortOrder, final Long staffId, final String accountNo,
            final Long loanId, final Long savingsId, final Boolean orphansOnly, boolean isSelfUser, final String dpiNumber) {
        this.sqlSearch = sqlSearch;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = null;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = isSelfUser;
        this.status = status;
        this.dpiNumber = dpiNumber;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    private SearchParameters(final String sqlSearch, final Long officeId, final String externalId, final String name,
            final String hierarchy, final String firstname, final String lastname, final String status, final Integer offset,
            final Integer limit, final String orderBy, final String sortOrder, final Long staffId, final String accountNo,
            final Long loanId, final Long savingsId, final Boolean orphansOnly, boolean isSelfUser, final String dpiNumber,
            final String accountNumber, final String bankName, final String bankCode) {
        this.sqlSearch = sqlSearch;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = null;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = isSelfUser;
        this.status = status;
        this.dpiNumber = dpiNumber;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = accountNumber;
        this.bankName = bankName;
        this.bankCode = bankCode;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    private SearchParameters(final String sqlSearch, final Long officeId, final String externalId, final String name,
            final String hierarchy, final String firstname, final String lastname, final String status, final Integer offset,
            final Integer limit, final String orderBy, final String sortOrder, final Long staffId, final String accountNo,
            final Long loanId, final Long savingsId, final Boolean orphansOnly, boolean isSelfUser, final String dpiNumber,
            final String type, final String groupName, final String groupNumber, final String centerName) {
        this.sqlSearch = sqlSearch;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = null;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = isSelfUser;
        this.status = status;
        this.dpiNumber = dpiNumber;
        this.type = type;
        this.groupName = groupName;
        this.centerName = centerName;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    private SearchParameters(final Long officeId, final String externalId, final String name, final String hierarchy,
            final String firstname, final String lastname, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder, final Long staffId, final String accountNo, final Long loanId, final Long savingsId,
            final Boolean orphansOnly, boolean isSelfUser, final String chequeNo, final String status, final Long batchId,
            final Long chequeId) {
        this.sqlSearch = null;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = null;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = isSelfUser;
        this.status = status;
        this.dpiNumber = null;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = chequeNo;
        this.batchId = batchId;
        this.chequeId = chequeId;
    }

    private SearchParameters(final Long provisioningEntryId, final Long officeId, final Long productId, final Long categoryId,
            final Integer offset, final Integer limit) {
        this.sqlSearch = null;
        this.externalId = null;
        this.name = null;
        this.hierarchy = null;
        this.firstname = null;
        this.lastname = null;
        this.orderBy = null;
        this.sortOrder = null;
        this.staffId = null;
        this.accountNo = null;
        this.loanId = null;
        this.savingsId = null;
        this.orphansOnly = null;
        this.currencyCode = null;
        this.officeId = officeId;
        this.offset = offset;
        this.limit = limit;
        this.provisioningEntryId = provisioningEntryId;
        this.productId = productId;
        this.categoryId = categoryId;
        this.isSelfUser = false;
        this.status = null;
        this.dpiNumber = null;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    public SearchParameters(final String sqlSearch, final Long officeId, final String externalId, final String name, final String hierarchy,
            final String firstname, final String lastname, final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder, final Long staffId, final String accountNo, final Long loanId, final Long savingsId,
            final Boolean orphansOnly, final String currencyCode) {
        this.sqlSearch = sqlSearch;
        this.officeId = officeId;
        this.externalId = externalId;
        this.name = name;
        this.hierarchy = hierarchy;
        this.firstname = firstname;
        this.lastname = lastname;
        this.offset = offset;
        this.limit = limit;
        this.orderBy = orderBy;
        this.sortOrder = sortOrder;
        this.staffId = staffId;
        this.accountNo = accountNo;
        this.loanId = loanId;
        this.savingsId = savingsId;
        this.orphansOnly = orphansOnly;
        this.currencyCode = currencyCode;
        this.provisioningEntryId = null;
        this.productId = null;
        this.categoryId = null;
        this.isSelfUser = false;
        this.status = null;
        this.dpiNumber = null;
        this.type = null;
        this.groupName = null;
        this.centerName = null;
        this.accountNumber = null;
        this.bankName = null;
        this.bankCode = null;
        this.chequeNo = null;
        this.batchId = null;
        this.chequeId = null;
    }

    public boolean isOrderByRequested() {
        return StringUtils.isNotBlank(this.orderBy);
    }

    public boolean isSortOrderProvided() {
        return StringUtils.isNotBlank(this.sortOrder);
    }

    public static Integer getCheckedLimit(final Integer limit) {

        final Integer maxLimitAllowed = 200;
        // default to max limit first off
        Integer checkedLimit = maxLimitAllowed;

        if (limit != null && limit > 0) {
            checkedLimit = limit;
        } else if (limit != null) {
            // unlimited case: limit provided and 0 or less
            checkedLimit = null;
        }

        return checkedLimit;
    }

    public boolean isOfficeIdPassed() {
        return this.officeId != null && this.officeId != 0;
    }

    public boolean isCurrencyCodePassed() {
        return this.currencyCode != null;
    }

    public boolean isLimited() {
        return this.limit != null && this.limit.intValue() > 0;
    }

    public boolean isOffset() {
        return this.offset != null;
    }

    public boolean isScopedByOfficeHierarchy() {
        return StringUtils.isNotBlank(this.hierarchy);
    }

    public String getSqlSearch() {
        return this.sqlSearch;
    }

    public Long getOfficeId() {
        return this.officeId;
    }

    public String getCurrencyCode() {
        return this.currencyCode;
    }

    public String getExternalId() {
        return this.externalId;
    }

    public String getName() {
        return this.name;
    }

    public String getDpiNumber() {
        return this.dpiNumber;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getCenterName() {
        return centerName;
    }

    public String getHierarchy() {
        return this.hierarchy;
    }

    public String getFirstname() {
        return this.firstname;
    }

    public String getLastname() {
        return this.lastname;
    }

    public String getStatus() {
        return this.status;
    }

    public String getType() {
        return this.type;
    }

    public Integer getOffset() {
        return this.offset;
    }

    public Integer getLimit() {
        return this.limit;
    }

    public String getOrderBy() {
        return this.orderBy;
    }

    public String getSortOrder() {
        return this.sortOrder;
    }

    public boolean isStaffIdPassed() {
        return this.staffId != null && this.staffId != 0;
    }

    public Long getStaffId() {
        return this.staffId;
    }

    public String getAccountNo() {
        return this.accountNo;
    }

    public boolean isLoanIdPassed() {
        return this.loanId != null && this.loanId != 0;
    }

    public boolean isSavingsIdPassed() {
        return this.savingsId != null && this.savingsId != 0;
    }

    public Long getLoanId() {
        return this.loanId;
    }

    public Long getSavingsId() {
        return this.savingsId;
    }

    public Boolean isOrphansOnly() {
        if (this.orphansOnly != null) {
            return this.orphansOnly;
        }
        return false;
    }

    public Long getProvisioningEntryId() {
        return this.provisioningEntryId;
    }

    public boolean isProvisioningEntryIdPassed() {
        return this.provisioningEntryId != null && this.provisioningEntryId != 0;
    }

    public Long getProductId() {
        return this.productId;
    }

    public boolean isProductIdPassed() {
        return this.productId != null && this.productId != 0;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public boolean isCategoryIdPassed() {
        return this.categoryId != null && this.categoryId != 0;
    }

    public boolean isSelfUser() {
        return this.isSelfUser;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getBankName() {
        return bankName;
    }

    public String getBankCode() {
        return bankCode;
    }

    public String getChequeNo() {
        return chequeNo;
    }

    public Long getBatchId() {
        return batchId;
    }

    public Long getChequeId() {
        return chequeId;
    }

    /**
     * creates an instance of the SearchParameters from a request for the report mailing job run history
     *
     * @return SearchParameters object
     **/
    public static SearchParameters fromReportMailingJobRunHistory(final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder) {
        final Integer maxLimitAllowed = getCheckedLimit(limit);

        return new SearchParameters(null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, null, null, null,
                null, null, false);
    }

    /**
     * creates an instance of the {@link SearchParameters} from a request for the report mailing job
     *
     * @return {@link SearchParameters} object
     */
    public static SearchParameters fromReportMailingJob(final Integer offset, final Integer limit, final String orderBy,
            final String sortOrder) {
        final Integer maxLimitAllowed = getCheckedLimit(limit);

        return new SearchParameters(null, null, null, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, null, null, null,
                null, null, false);
    }

    public static SearchParameters forExchanges(Integer offset, Integer limit, String orderBy, String sortOrder) {
        final String externalId = null;
        final Integer maxLimitAllowed = getCheckedLimit(limit);
        final Long staffId = null;
        final String accountNo = null;
        final Long loanId = null;
        final Long savingsId = null;
        final Boolean orphansOnly = false;
        final boolean isSelfUser = false;
        return new SearchParameters(null, null, externalId, null, null, null, null, offset, maxLimitAllowed, orderBy, sortOrder, staffId,
                accountNo, loanId, savingsId, orphansOnly, isSelfUser);
    }
}
