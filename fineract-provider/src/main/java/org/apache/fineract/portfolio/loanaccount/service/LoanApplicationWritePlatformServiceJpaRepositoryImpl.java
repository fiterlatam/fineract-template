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
package org.apache.fineract.portfolio.loanaccount.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.persistence.PersistenceException;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.accounting.journalentry.data.LumaBitacoraTransactionTypeEnum;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMaster;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMasterRepository;
import org.apache.fineract.accounting.journalentry.service.LumaAccountingProcessorForLoan;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormat;
import org.apache.fineract.infrastructure.accountnumberformat.domain.AccountNumberFormatRepositoryWrapper;
import org.apache.fineract.infrastructure.accountnumberformat.domain.EntityAccountType;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationProperty;
import org.apache.fineract.infrastructure.configuration.domain.GlobalConfigurationRepositoryWrapper;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.api.JsonQuery;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.serialization.JsonParserHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.entityaccess.FineractEntityAccessConstants;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityAccessType;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityRelation;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityRelationRepository;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityToEntityMapping;
import org.apache.fineract.infrastructure.entityaccess.domain.FineractEntityToEntityMappingRepository;
import org.apache.fineract.infrastructure.entityaccess.exception.NotOfficeSpecificProductException;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.bankcheque.domain.BankChequeStatus;
import org.apache.fineract.organisation.bankcheque.domain.Cheque;
import org.apache.fineract.organisation.bankcheque.domain.ChequeBatchRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.data.LoanAdditionalData;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionProperties;
import org.apache.fineract.organisation.prequalification.domain.LoanAdditionalPropertiesRepository;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroup;
import org.apache.fineract.organisation.prequalification.domain.PrequalificationGroupRepositoryWrapper;
import org.apache.fineract.organisation.prequalification.exception.PrequalificationNotProvidedException;
import org.apache.fineract.organisation.prequalification.service.BureauValidationWritePlatformServiceImpl;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.blacklist.domain.BlacklistStatus;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanApprovedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanCreatedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanRejectedBusinessEvent;
import org.apache.fineract.portfolio.businessevent.domain.loan.LoanUndoApprovalBusinessEvent;
import org.apache.fineract.portfolio.businessevent.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.domain.CalendarEntityType;
import org.apache.fineract.portfolio.calendar.domain.CalendarFrequencyType;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstance;
import org.apache.fineract.portfolio.calendar.domain.CalendarInstanceRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarRepository;
import org.apache.fineract.portfolio.calendar.domain.CalendarType;
import org.apache.fineract.portfolio.calendar.exception.CalendarNotFoundException;
import org.apache.fineract.portfolio.calendar.service.CalendarReadPlatformService;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.client.domain.AccountNumberGenerator;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientBlacklistedException;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.collateralmanagement.domain.ClientCollateralManagement;
import org.apache.fineract.portfolio.collateralmanagement.service.LoanCollateralAssembler;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.cupo.domain.Cupo;
import org.apache.fineract.portfolio.cupo.domain.CupoRepositoryWrapper;
import org.apache.fineract.portfolio.fund.domain.Fund;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.group.exception.GroupMemberNotFoundInGSIMException;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.command.DisburseByChequesCommand;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.AdditionalsExtraLoans;
import org.apache.fineract.portfolio.loanaccount.domain.DefaultLoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.GLIMAccountInfoRepository;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanAdditionals;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanAdditionalsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.GroupLoanIndividualMonitoringAccount;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCollateralManagement;
import org.apache.fineract.portfolio.loanaccount.domain.LoanDisbursementDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTopupDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionType;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationDateException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationNotInClosedStateCannotBeModified;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeDeleted;
import org.apache.fineract.portfolio.loanaccount.exception.LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeModified;
import org.apache.fineract.portfolio.loanaccount.exception.LoanIndividualAdditionDataException;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.AprCalculator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleAssembler;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleCalculationPlatformService;
import org.apache.fineract.portfolio.loanaccount.serialization.DisburseByChequesCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationCommandFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationTransitionApiJsonValidator;
import org.apache.fineract.portfolio.loanproduct.LoanProductConstants;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRelatedDetail;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProductRepository;
import org.apache.fineract.portfolio.loanproduct.domain.LoanTransactionProcessingStrategy;
import org.apache.fineract.portfolio.loanproduct.domain.RecalculationFrequencyType;
import org.apache.fineract.portfolio.loanproduct.exception.LinkedAccountRequiredException;
import org.apache.fineract.portfolio.loanproduct.exception.LoanProductNotFoundException;
import org.apache.fineract.portfolio.loanproduct.serialization.LoanProductDataValidator;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.rate.service.RateAssembler;
import org.apache.fineract.portfolio.savings.data.GroupSavingsIndividualMonitoringAccountData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountAssembler;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountRepositoryWrapper;
import org.apache.fineract.portfolio.savings.service.GSIMReadPlatformService;
import org.apache.fineract.portfolio.savings.service.SavingsAccountWritePlatformService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Service
public class LoanApplicationWritePlatformServiceJpaRepositoryImpl implements LoanApplicationWritePlatformService {

    private static final Logger LOG = LoggerFactory.getLogger(LoanApplicationWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final FromJsonHelper fromJsonHelper;
    private final LoanApplicationTransitionApiJsonValidator loanApplicationTransitionApiJsonValidator;
    private final LoanProductDataValidator loanProductCommandFromApiJsonDeserializer;
    private final LoanApplicationCommandFromApiJsonHelper fromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final GroupLoanAdditionalsRepository groupLoanAdditionalsRepository;
    private final NoteRepository noteRepository;
    private final LoanScheduleCalculationPlatformService calculationPlatformService;
    private final LoanAssembler loanAssembler;
    private final ClientRepositoryWrapper clientRepository;
    private final LoanProductRepository loanProductRepository;
    private final LoanChargeAssembler loanChargeAssembler;
    private final LoanCollateralAssembler loanCollateralAssembler;
    private final AprCalculator aprCalculator;
    private final AccountNumberGenerator accountNumberGenerator;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final GroupRepositoryWrapper groupRepository;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final CalendarRepository calendarRepository;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final SavingsAccountAssembler savingsAccountAssembler;
    private final AccountAssociationsRepository accountAssociationsRepository;
    private final LoanReadPlatformService loanReadPlatformService;
    private final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final ConfigurationDomainService configurationDomainService;
    private final LoanScheduleAssembler loanScheduleAssembler;
    private final LoanUtilService loanUtilService;
    private final CalendarReadPlatformService calendarReadPlatformService;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final GlobalConfigurationRepositoryWrapper globalConfigurationRepository;
    private final FineractEntityToEntityMappingRepository repository;
    private final FineractEntityRelationRepository fineractEntityRelationRepository;
    private final LoanProductReadPlatformService loanProductReadPlatformService;

    private final RateAssembler rateAssembler;
    private final GLIMAccountInfoWritePlatformService glimAccountInfoWritePlatformService;
    private final GLIMAccountInfoRepository glimRepository;
    private final LoanRepository loanRepository;
    private final GSIMReadPlatformService gsimReadPlatformService;
    private final CupoRepositoryWrapper cupoRepositoryWrapper;
    private final LumaAccountingProcessorForLoan lumaAccountingProcessorForLoan;
    private final JdbcTemplate jdbcTemplate;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final DisburseByChequesCommandFromApiJsonDeserializer disburseByChequesCommandFromApiJsonDeserializer;
    private final ChequeBatchRepositoryWrapper chequeBatchRepositoryWrapper;
    private final PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper;
    @Autowired
    private BitaCoraMasterRepository bitaCoraMasterRepository;

    private final SavingsAccountWritePlatformService savingsAccountWritePlatformService;

    private final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper;
    private final AppUserRepository appUserRepository;
    private final BureauValidationWritePlatformServiceImpl bureauValidationWritePlatformService;
    private final LoanAdditionalPropertiesRepository loanAdditionalPropertiesRepository;

    @Autowired
    public LoanApplicationWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context, final FromJsonHelper fromJsonHelper,
            final LoanApplicationTransitionApiJsonValidator loanApplicationTransitionApiJsonValidator,
            final LoanApplicationCommandFromApiJsonHelper fromApiJsonDeserializer,
            final LoanProductDataValidator loanProductCommandFromApiJsonDeserializer, final AprCalculator aprCalculator,
            final LoanAssembler loanAssembler, final LoanChargeAssembler loanChargeAssembler,
            final LoanCollateralAssembler loanCollateralAssembler, final LoanRepositoryWrapper loanRepositoryWrapper,
            final NoteRepository noteRepository, final LoanScheduleCalculationPlatformService calculationPlatformService,
            final ClientRepositoryWrapper clientRepository, final LoanProductRepository loanProductRepository,
            final AccountNumberGenerator accountNumberGenerator, final LoanSummaryWrapper loanSummaryWrapper,
            final GroupRepositoryWrapper groupRepository, final CodeValueRepositoryWrapper codeValueRepository,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            final CalendarRepository calendarRepository, final CalendarInstanceRepository calendarInstanceRepository,
            final SavingsAccountAssembler savingsAccountAssembler, final AccountAssociationsRepository accountAssociationsRepository,
            final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
            final LoanReadPlatformService loanReadPlatformService, final AccountNumberFormatRepositoryWrapper accountNumberFormatRepository,
            final BusinessEventNotifierService businessEventNotifierService, final ConfigurationDomainService configurationDomainService,
            final LoanScheduleAssembler loanScheduleAssembler, final LoanUtilService loanUtilService,
            final CalendarReadPlatformService calendarReadPlatformService,
            final GlobalConfigurationRepositoryWrapper globalConfigurationRepository,
            final FineractEntityToEntityMappingRepository repository, final AppUserRepository appUserRepository,
            final FineractEntityRelationRepository fineractEntityRelationRepository,
            final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService,
            final GLIMAccountInfoWritePlatformService glimAccountInfoWritePlatformService, final GLIMAccountInfoRepository glimRepository,
            final LoanRepository loanRepository, final GSIMReadPlatformService gsimReadPlatformService, final RateAssembler rateAssembler,
            final LoanProductReadPlatformService loanProductReadPlatformService, final JdbcTemplate jdbcTemplate,
            final CupoRepositoryWrapper cupoRepositoryWrapper, final LumaAccountingProcessorForLoan lumaAccountingProcessorForLoan,
            DisburseByChequesCommandFromApiJsonDeserializer disburseByChequesCommandFromApiJsonDeserializer,
            ChequeBatchRepositoryWrapper chequeBatchRepositoryWrapper,
            PrequalificationGroupRepositoryWrapper prequalificationGroupRepositoryWrapper,
            final SavingsAccountWritePlatformService savingsAccountWritePlatformService,
            final SavingsAccountRepositoryWrapper savingsAccountRepositoryWrapper,
            final BureauValidationWritePlatformServiceImpl bureauValidationWritePlatformService,
            final LoanAdditionalPropertiesRepository loanAdditionalPropertiesRepository,
            final GroupLoanAdditionalsRepository groupLoanAdditionalsRepository) {
        this.context = context;
        this.fromJsonHelper = fromJsonHelper;
        this.loanApplicationTransitionApiJsonValidator = loanApplicationTransitionApiJsonValidator;
        this.fromApiJsonDeserializer = fromApiJsonDeserializer;
        this.loanProductCommandFromApiJsonDeserializer = loanProductCommandFromApiJsonDeserializer;
        this.aprCalculator = aprCalculator;
        this.loanAssembler = loanAssembler;
        this.loanChargeAssembler = loanChargeAssembler;
        this.loanCollateralAssembler = loanCollateralAssembler;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.noteRepository = noteRepository;
        this.calculationPlatformService = calculationPlatformService;
        this.clientRepository = clientRepository;
        this.loanProductRepository = loanProductRepository;
        this.accountNumberGenerator = accountNumberGenerator;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.groupRepository = groupRepository;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.calendarRepository = calendarRepository;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.savingsAccountAssembler = savingsAccountAssembler;
        this.accountAssociationsRepository = accountAssociationsRepository;
        this.loanReadPlatformService = loanReadPlatformService;
        this.accountNumberFormatRepository = accountNumberFormatRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.configurationDomainService = configurationDomainService;
        this.loanScheduleAssembler = loanScheduleAssembler;
        this.loanUtilService = loanUtilService;
        this.calendarReadPlatformService = calendarReadPlatformService;
        this.entityDatatableChecksWritePlatformService = entityDatatableChecksWritePlatformService;
        this.globalConfigurationRepository = globalConfigurationRepository;
        this.repository = repository;
        this.fineractEntityRelationRepository = fineractEntityRelationRepository;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.rateAssembler = rateAssembler;
        this.glimAccountInfoWritePlatformService = glimAccountInfoWritePlatformService;
        this.glimRepository = glimRepository;
        this.loanRepository = loanRepository;
        this.gsimReadPlatformService = gsimReadPlatformService;
        this.cupoRepositoryWrapper = cupoRepositoryWrapper;
        this.lumaAccountingProcessorForLoan = lumaAccountingProcessorForLoan;
        this.jdbcTemplate = jdbcTemplate;
        this.codeValueRepository = codeValueRepository;
        this.disburseByChequesCommandFromApiJsonDeserializer = disburseByChequesCommandFromApiJsonDeserializer;
        this.chequeBatchRepositoryWrapper = chequeBatchRepositoryWrapper;
        this.prequalificationGroupRepositoryWrapper = prequalificationGroupRepositoryWrapper;
        this.savingsAccountWritePlatformService = savingsAccountWritePlatformService;
        this.savingsAccountRepositoryWrapper = savingsAccountRepositoryWrapper;
        this.groupLoanAdditionalsRepository = groupLoanAdditionalsRepository;
        this.appUserRepository = appUserRepository;
        this.bureauValidationWritePlatformService = bureauValidationWritePlatformService;
        this.loanAdditionalPropertiesRepository = loanAdditionalPropertiesRepository;
    }

    private LoanLifecycleStateMachine defaultLoanLifecycleStateMachine() {
        final List<LoanStatus> allowedLoanStatuses = Arrays.asList(LoanStatus.values());
        return new DefaultLoanLifecycleStateMachine(allowedLoanStatuses);
    }

    @Transactional
    @Override
    public CommandProcessingResult submitApplication(final JsonCommand command) {

        try {
            boolean isMeetingMandatoryForJLGLoans = configurationDomainService.isMeetingMandatoryForJLGLoans();
            final Long productId = this.fromJsonHelper.extractLongNamed("productId", command.parsedJson());
            final LoanProduct loanProduct = this.loanProductRepository.findById(productId)
                    .orElseThrow(() -> new LoanProductNotFoundException(productId));

            final Long clientId = this.fromJsonHelper.extractLongNamed("clientId", command.parsedJson());
            if (clientId != null) {
                Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
                officeSpecificLoanProductValidation(productId, client.getOffice().getId());

                String dpiNumber = client.getDpiNumber();
                if (dpiNumber != null) {
                    String blacklistString = "select count(*) from m_client_blacklist where dpi=? and status=?";
                    Long blacklisted = jdbcTemplate.queryForObject(blacklistString, Long.class, dpiNumber,
                            BlacklistStatus.ACTIVE.getValue());
                    if (blacklisted > 0) {
                        String blacklistReason = "select type_enum from m_client_blacklist where dpi=? and status=?";
                        Integer typification = jdbcTemplate.queryForObject(blacklistReason, Integer.class, dpiNumber,
                                BlacklistStatus.ACTIVE.getValue());
                        CodeValue typificationCodeValue = this.codeValueRepository.findOneWithNotFoundDetection(typification.longValue());
                        throw new ClientBlacklistedException(typificationCodeValue.getDescription());
                    }
                }
            }
            final Long groupId = this.fromJsonHelper.extractLongNamed("groupId", command.parsedJson());
            if (groupId != null) {
                Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);
                officeSpecificLoanProductValidation(productId, group.getOffice().getId());
            }

            this.fromApiJsonDeserializer.validateForCreate(command.json(), isMeetingMandatoryForJLGLoans, loanProduct);

            // Validate If the externalId is already registered
            final String externalId = this.fromJsonHelper.extractStringNamed("externalId", command.parsedJson());
            if (StringUtils.isNotBlank(externalId)) {
                final boolean existByExternalId = this.loanRepositoryWrapper.existLoanByExternalId(externalId);
                if (existByExternalId) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.with.externalId.already.used",
                            "Loan with externalId is already registered.");
                }
            }

            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");

            if (loanProduct.useBorrowerCycle()) {
                Integer cycleNumber = null;
                cycleNumber = this.fromJsonHelper.extractIntegerWithLocaleNamed("borrowerCycle", command.parsedJson());
                if (cycleNumber == null) cycleNumber = 0;
                if (cycleNumber == 0) {
                    if (clientId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(clientId, loanProduct.getId());
                    } else if (groupId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(groupId, AccountType.GROUP.getValue(),
                                loanProduct.getId());
                    }
                }

                this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                        loanProduct, cycleNumber);
            } else {
                this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                        loanProduct);
            }
            if (!dataValidationErrors.isEmpty()) {
                StringBuffer err = new StringBuffer();
                for (ApiParameterError error : dataValidationErrors) {
                    err.append(error.getDeveloperMessage()).append("\n");
                }

                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", err.toString(),
                        dataValidationErrors);
            }

            final Loan newLoanApplication = this.loanAssembler.assembleFrom(command);

            checkForProductMixRestrictions(newLoanApplication);

            validateSubmittedOnDate(newLoanApplication);

            final LoanProductRelatedDetail productRelatedDetail = newLoanApplication.repaymentScheduleDetail();

            if (loanProduct.getLoanProductConfigurableAttributes() != null) {
                updateProductRelatedDetails(productRelatedDetail, newLoanApplication);
            }

            this.fromApiJsonDeserializer.validateLoanTermAndRepaidEveryValues(newLoanApplication.getTermFrequency(),
                    newLoanApplication.getTermPeriodFrequencyType(), productRelatedDetail.getNumberOfRepayments(),
                    productRelatedDetail.getRepayEvery(), productRelatedDetail.getRepaymentPeriodFrequencyType().getValue(),
                    newLoanApplication);

            if (loanProduct.canUseForTopup() && clientId != null) {
                final Boolean isTopup = command.booleanObjectValueOfParameterNamed(LoanApiConstants.isTopup);
                if (null == isTopup) {
                    newLoanApplication.setIsTopup(false);
                } else {
                    newLoanApplication.setIsTopup(isTopup);
                }

                if (newLoanApplication.isTopup()) {
                    final Long loanIdToClose = command.longValueOfParameterNamed(LoanApiConstants.loanIdToClose);
                    final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, clientId);
                    if (loanToClose == null) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.loanIdToClose.no.active.loan.associated.to.client.found",
                                "loanIdToClose is invalid, No Active Loan associated with the given Client ID found.");
                    }
                    if (loanToClose.isMultiDisburmentLoan() && !loanToClose.isInterestRecalculationEnabledForProduct()) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.topup.on.multi.tranche.loan.without.interest.recalculation.not.supported",
                                "Topup on loan with multi-tranche disbursal and without interest recalculation is not supported.");
                    }
                    final LocalDate disbursalDateOfLoanToClose = loanToClose.getDisbursementDate();
                    if (!newLoanApplication.getSubmittedOnDate().isAfter(disbursalDateOfLoanToClose)) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.submitted.date.should.be.after.topup.loan.disbursal.date",
                                "Submitted date of this loan application " + newLoanApplication.getSubmittedOnDate()
                                        + " should be after the disbursed date of loan to be closed " + disbursalDateOfLoanToClose);
                    }
                    if (!loanToClose.getCurrencyCode().equals(newLoanApplication.getCurrencyCode())) {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.has.different.currency",
                                "loanIdToClose is invalid, Currency code is different.");
                    }
                    final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                    if (newLoanApplication.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                        throw new GeneralPlatformDomainRuleException(
                                "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                                "Disbursal date of this loan application " + newLoanApplication.getDisbursementDate()
                                        + " should be after last transaction date of loan to be closed "
                                        + lastUserTransactionOnLoanToClose);
                    }
                    BigDecimal loanOutstanding = this.loanReadPlatformService.retrieveLoanPrePaymentTemplate(LoanTransactionType.REPAYMENT,
                            loanIdToClose, newLoanApplication.getDisbursementDate()).getAmount();
                    final BigDecimal firstDisbursalAmount = newLoanApplication.getFirstDisbursalAmount();
                    if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                        throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                                "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                    }

                    final LoanTopupDetails topupDetails = new LoanTopupDetails(newLoanApplication, loanIdToClose);
                    newLoanApplication.setTopupLoanDetails(topupDetails);
                }
            }

            // contract number generation
            StringBuilder contractBuilder = new StringBuilder();
            String countLoansSql = "select count(*) from m_loan where product_id = ? and client_id =? and approvedon_date is not null";
            Long productLoansCount = this.jdbcTemplate.queryForObject(countLoansSql, Long.class, loanProduct.getId(), clientId);
            contractBuilder.append(loanProduct.getShortName());
            if (clientId != null) {
                contractBuilder.append(StringUtils.leftPad(clientId.toString(), 8, '0'));
            } else if (groupId != null) {
                contractBuilder.append(StringUtils.leftPad(groupId.toString(), 8, '0'));
            }
            contractBuilder.append(StringUtils.leftPad(productLoansCount.toString(), 8, '0'));
            contractBuilder.append("00000000");
            newLoanApplication.updateLoanContract(contractBuilder.toString());

            this.loanRepositoryWrapper.saveAndFlush(newLoanApplication);
            final Long newLoanApplicationId = newLoanApplication.getId();
            final Boolean isBulkImport = this.fromJsonHelper.extractBooleanNamed("isBulkImport", command.parsedJson());
            if (isBulkImport == null || !isBulkImport) {
                newLoanApplication.setExternalId(String.valueOf(newLoanApplicationId));
            }

            Long facilitatorId = command.longValueOfParameterNamed("facilitator");
            AppUser facilitator = null;
            if (facilitatorId != null) {
                facilitator = this.appUserRepository.findById(facilitatorId)
                        .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.facilitator.not.found",
                                "Facilitator with identifier " + facilitatorId + " does not exist"));
            }

            final PrequalificationGroup prequalificationGroup = newLoanApplication.getPrequalificationGroup();
            if (prequalificationGroup != null && prequalificationGroup.isPrequalificationTypeGroup()) {
                GroupLoanAdditionals groupLoanAdditionals = GroupLoanAdditionals.assembleFromJson(command, newLoanApplication, facilitator);
                addExternalLoans(groupLoanAdditionals, command);
                this.groupLoanAdditionalsRepository.save(groupLoanAdditionals);
            }

            if (loanProduct.isInterestRecalculationEnabled()) {
                this.fromApiJsonDeserializer.validateLoanForInterestRecalculation(newLoanApplication);
                createAndPersistCalendarInstanceForInterestRecalculation(newLoanApplication);
            }

            // loan account number generation
            String accountNumber = "";
            GroupLoanIndividualMonitoringAccount glimAccount;
            BigDecimal applicationId = BigDecimal.ZERO;
            Boolean isLastChildApplication = false;

            if (newLoanApplication.isAccountNumberRequiresAutoGeneration()) {

                final AccountNumberFormat accountNumberFormat = this.accountNumberFormatRepository
                        .findByAccountType(EntityAccountType.LOAN);
                // if application is of GLIM type
                if (newLoanApplication.getLoanType() == 4) {
                    Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);

                    // GLIM specific parameters
                    if (command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("applicationId") != null) {
                        applicationId = command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("applicationId");
                    }

                    if (command.booleanObjectValueOfParameterNamed("lastApplication") != null) {
                        isLastChildApplication = command.booleanPrimitiveValueOfParameterNamed("lastApplication");
                    }

                    if (command.booleanObjectValueOfParameterNamed("isParentAccount") != null) {

                        // empty table check
                        if (glimRepository.count() != 0) {
                            // **************Parent-Not an empty
                            // table********************
                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId);
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        } else {
                            // ************** Parent-empty
                            // table********************

                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId);
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        }

                    } else {

                        if (glimRepository.count() != 0) {
                            // Child-Not an empty table

                            glimAccount = glimRepository.findOneByIsAcceptingChildAndApplicationId(true, applicationId);
                            accountNumber = glimAccount.getAccountNumber() + (glimAccount.getChildAccountsCount() + 1);
                            newLoanApplication.updateAccountNo(accountNumber);
                            this.glimAccountInfoWritePlatformService.incrementChildAccountCount(glimAccount);
                            newLoanApplication.setGlim(glimAccount);
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        } else {
                            // **************Child-empty
                            // table********************
                            // if the glim info is empty set the current account
                            // as parent
                            accountNumber = this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat);
                            newLoanApplication.updateAccountNo(accountNumber + "1");
                            glimAccountInfoWritePlatformService.addGLIMAccountInfo(accountNumber, group,
                                    command.bigDecimalValueOfParameterNamedDefaultToNullIfZero("totalLoan"), Long.valueOf(1), true,
                                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), applicationId);
                            newLoanApplication.setGlim(glimRepository.findOneByAccountNumber(accountNumber));
                            this.loanRepositoryWrapper.save(newLoanApplication);

                        }

                        // reset in cases of last child application of glim

                        if (isLastChildApplication) {
                            this.glimAccountInfoWritePlatformService
                                    .resetIsAcceptingChild(glimRepository.findOneByIsAcceptingChildAndApplicationId(true, applicationId));
                        }

                    }
                } else { // for applications other than GLIM
                    newLoanApplication.updateAccountNo(this.accountNumberGenerator.generate(newLoanApplication, accountNumberFormat));
                    this.loanRepositoryWrapper.saveAndFlush(newLoanApplication);
                }
            }

            final String submittedOnNote = command.stringValueOfParameterNamed("submittedOnNote");
            if (StringUtils.isNotBlank(submittedOnNote)) {
                final Note note = Note.loanNote(newLoanApplication, submittedOnNote);
                this.noteRepository.save(note);
            }

            // Save calendar instance
            final Long calendarId = command.longValueOfParameterNamed("calendarId");
            Calendar calendar = null;

            if (calendarId != null && calendarId != 0) {
                calendar = this.calendarRepository.findById(calendarId).orElseThrow(() -> new CalendarNotFoundException(calendarId));

                final CalendarInstance calendarInstance = new CalendarInstance(calendar, newLoanApplication.getId(),
                        CalendarEntityType.LOANS.getValue());
                this.calendarInstanceRepository.save(calendarInstance);
            } else {
                final LoanApplicationTerms loanApplicationTerms = this.loanScheduleAssembler.assembleLoanTerms(command.parsedJson());
                final Integer repaymentFrequencyNthDayType = command.integerValueOfParameterNamed("repaymentFrequencyNthDayType");
                if (loanApplicationTerms.getRepaymentPeriodFrequencyType() == PeriodFrequencyType.MONTHS
                        && repaymentFrequencyNthDayType != null) {
                    final String title = "loan_schedule_" + newLoanApplication.getId();
                    LocalDate calendarStartDate = loanApplicationTerms.getRepaymentsStartingFromLocalDate();
                    if (calendarStartDate == null) {
                        calendarStartDate = loanApplicationTerms.getExpectedDisbursementDate();
                    }
                    final CalendarFrequencyType calendarFrequencyType = CalendarFrequencyType.MONTHLY;
                    final Integer frequency = loanApplicationTerms.getRepaymentEvery();
                    final Integer repeatsOnDay = loanApplicationTerms.getWeekDayType().getValue();
                    final Integer repeatsOnNthDayOfMonth = loanApplicationTerms.getNthDay();
                    final Integer calendarEntityType = CalendarEntityType.LOANS.getValue();
                    final Calendar loanCalendar = Calendar.createRepeatingCalendar(title, calendarStartDate,
                            CalendarType.COLLECTION.getValue(), calendarFrequencyType, frequency, repeatsOnDay, repeatsOnNthDayOfMonth);
                    this.calendarRepository.save(loanCalendar);
                    final CalendarInstance calendarInstance = CalendarInstance.from(loanCalendar, newLoanApplication.getId(),
                            calendarEntityType);
                    this.calendarInstanceRepository.save(calendarInstance);
                }
            }

            // Save linked account information
            SavingsAccount savingsAccount;
            AccountAssociations accountAssociations = null;
            final boolean backdatedTxnsAllowedTill = false;
            final Long savingsAccountId = command.longValueOfParameterNamed("linkAccountId");
            if (savingsAccountId != null) {
                if (newLoanApplication.getLoanType() == 4) {

                    List<GroupSavingsIndividualMonitoringAccountData> childSavings = (List<GroupSavingsIndividualMonitoringAccountData>) gsimReadPlatformService
                            .findGSIMAccountsByGSIMId(savingsAccountId);
                    // List<SavingsAccountSummaryData>
                    // childSavings=gsimAccount.getChildGSIMAccounts();
                    List<BigDecimal> gsimClientMembers = new ArrayList<BigDecimal>();
                    Map<BigDecimal, BigDecimal> clientAccountMappings = new HashMap<>();
                    for (GroupSavingsIndividualMonitoringAccountData childSaving : childSavings) {
                        gsimClientMembers.add(childSaving.getClientId());
                        clientAccountMappings.put(childSaving.getClientId(), childSaving.getChildAccountId());

                    }

                    if (gsimClientMembers.contains(BigDecimal.valueOf(newLoanApplication.getClientId()))) {
                        savingsAccount = this.savingsAccountAssembler.assembleFrom(
                                clientAccountMappings.get(BigDecimal.valueOf(newLoanApplication.getClientId())).longValue(),
                                backdatedTxnsAllowedTill);

                        this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, newLoanApplication);
                        boolean isActive = true;
                        accountAssociations = AccountAssociations.associateSavingsAccount(newLoanApplication, savingsAccount,
                                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                        this.accountAssociationsRepository.save(accountAssociations);

                    } else {
                        throw new GroupMemberNotFoundInGSIMException(newLoanApplication.getClientId());
                    }
                } else {

                    savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsAccountId, backdatedTxnsAllowedTill);
                    this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, newLoanApplication);
                    boolean isActive = true;
                    accountAssociations = AccountAssociations.associateSavingsAccount(newLoanApplication, savingsAccount,
                            AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    this.accountAssociationsRepository.save(accountAssociations);

                }
            }

            // Cupo Link
            final Long cupoId = command.longValueOfParameterNamed(LoanApiConstants.cupoIdParameterName);
            if (cupoId != null) {
                Cupo cupo = this.cupoRepositoryWrapper.findOneWithNotFoundDetection(cupoId);
                this.fromApiJsonDeserializer.validatelinkedCupo(cupo, newLoanApplication);
                if (accountAssociations == null) {
                    accountAssociations = AccountAssociations.associateCupo(newLoanApplication, cupo,
                            AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), true);
                } else {
                    accountAssociations.updateLinkedCupo(cupo);
                }
                this.accountAssociationsRepository.save(accountAssociations);
            }

            // Additional Data (Individual Prequalification)
            if (prequalificationGroup != null && prequalificationGroup.isPrequalificationTypeIndividual()) {
                final JsonElement loanAdditionalDataJson = command.jsonElement(LoanApiConstants.LOAN_ADDITIONAL_DATA);
                if (loanAdditionalDataJson != null && loanAdditionalDataJson.isJsonObject()) {
                    this.fromApiJsonDeserializer.validateLoanAdditionalData(command);
                    final LoanAdditionalData loanAdditionalData = this.fromJsonCommand(command);
                    final String caseId = loanAdditionalData.getCaseId();
                    if (caseId == null) {
                        throw new LoanIndividualAdditionDataException("error.msg.loan.additional.data.case.id.not.provided",
                                "The new individual loan does not have case ID");
                    }
                    final Client loanClient = newLoanApplication.getClient();
                    final LoanAdditionProperties loanAdditionProperties = loanAdditionalData.toEntity();
                    loanAdditionProperties.setCaseId(caseId);
                    loanAdditionProperties.setClient(loanClient);
                    loanAdditionProperties.setLoan(newLoanApplication);
                    loanAdditionalPropertiesRepository.saveAndFlush(loanAdditionProperties);
                } else {
                    throw new LoanIndividualAdditionDataException("error.msg.loan.additional.data.not.provided",
                            "New loan does not have additional data");
                }
            }

            if (command.parameterExists(LoanApiConstants.datatables)) {
                this.entityDatatableChecksWritePlatformService.saveDatatables(StatusEnum.CREATE.getCode().longValue(),
                        EntityTables.LOAN.getName(), newLoanApplication.getId(), newLoanApplication.productId(),
                        command.arrayOfParameterNamed(LoanApiConstants.datatables));
            }

            loanRepositoryWrapper.flush();

            this.entityDatatableChecksWritePlatformService.runTheCheckForProduct(newLoanApplication.getId(), EntityTables.LOAN.getName(),
                    StatusEnum.CREATE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(),
                    newLoanApplication.productId());

            businessEventNotifierService.notifyPostBusinessEvent(new LoanCreatedBusinessEvent(newLoanApplication));

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(newLoanApplication.getId()) //
                    .withOfficeId(newLoanApplication.getOfficeId()) //
                    .withClientId(newLoanApplication.getClientId()) //
                    .withGroupId(newLoanApplication.getGroupId()) //
                    .withLoanId(newLoanApplication.getId()).withGlimId(newLoanApplication.getGlimId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void addExternalLoans(GroupLoanAdditionals groupLoanAdditionals, JsonCommand command) {
        JsonArray externalLoansArray = command.arrayOfParameterNamed(LoanApiConstants.externalLoansParamName);

        List<AdditionalsExtraLoans> additionalLoansList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(externalLoansArray)) {
            for (JsonElement element : externalLoansArray) {
                JsonObject loanData = element.getAsJsonObject();

                String name = null;
                if (loanData.get("institutionName") != null) {
                    name = loanData.get("institutionName").getAsString();
                }
                Long institutionType = null;
                if (loanData.get("institutionType") != null) {
                    institutionType = loanData.get("institutionType").getAsLong();
                }

                Long loanStatus = null;
                if (loanData.get("loanStatus") != null) {
                    loanStatus = loanData.get("loanStatus").getAsLong();
                }

                BigDecimal totalLoanBalance = null;
                if (loanData.get("totalLoanBalance") != null) {
                    totalLoanBalance = new BigDecimal(loanData.get("totalLoanBalance").getAsString().replace(",", "".trim()));
                }

                BigDecimal charges = null;
                if (loanData.get("charges") != null) {
                    charges = new BigDecimal(loanData.get("charges").getAsString().replace(",", "").trim());
                }
                BigDecimal totalLoanAmount = null;
                if (loanData.get("totalLoanAmount") != null) {
                    totalLoanAmount = new BigDecimal(loanData.get("totalLoanAmount").getAsString().replace(",", "").trim());
                }

                AdditionalsExtraLoans additionalsExtraLoans = new AdditionalsExtraLoans(groupLoanAdditionals, institutionType,
                        totalLoanAmount, totalLoanBalance, charges, loanStatus, name);
                additionalLoansList.add(additionalsExtraLoans);

            }
            groupLoanAdditionals.setExtraLoans(additionalLoansList);
        }
    }

    public void checkForProductMixRestrictions(final Loan loan) {

        final List<Long> activeLoansLoanProductIds;
        final Long productId = loan.loanProduct().getId();

        if (loan.isGroupLoan()) {
            activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByGroup(loan.getGroupId(),
                    LoanStatus.ACTIVE.getValue());
        } else {
            activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByClient(loan.getClientId(),
                    LoanStatus.ACTIVE.getValue());
        }
        checkForProductMixRestrictions(activeLoansLoanProductIds, productId, loan.loanProduct().productName());
    }

    private void checkForProductMixRestrictions(final List<Long> activeLoansLoanProductIds, final Long productId,
            final String productName) {

        if (!CollectionUtils.isEmpty(activeLoansLoanProductIds)) {
            final Collection<LoanProductData> restrictedPrdouctsList = this.loanProductReadPlatformService
                    .retrieveRestrictedProductsForMix(productId);
            for (final LoanProductData restrictedProduct : restrictedPrdouctsList) {
                if (activeLoansLoanProductIds.contains(restrictedProduct.getId())) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.loan.applied.or.to.be.disbursed.can.not.co-exist.with.the.loan.already.active.to.this.client",
                            "This loan could not be applied/disbursed as the loan and `" + restrictedProduct
                                    + "` are not allowed to co-exist");
                }
            }
        }
    }

    private void updateProductRelatedDetails(LoanProductRelatedDetail productRelatedDetail, Loan loan) {
        final Boolean amortization = loan.loanProduct().getLoanProductConfigurableAttributes().getAmortizationBoolean();
        final Boolean arrearsTolerance = loan.loanProduct().getLoanProductConfigurableAttributes().getArrearsToleranceBoolean();
        final Boolean graceOnArrearsAging = loan.loanProduct().getLoanProductConfigurableAttributes().getGraceOnArrearsAgingBoolean();
        final Boolean interestCalcPeriod = loan.loanProduct().getLoanProductConfigurableAttributes().getInterestCalcPeriodBoolean();
        final Boolean interestMethod = loan.loanProduct().getLoanProductConfigurableAttributes().getInterestMethodBoolean();
        final Boolean graceOnPrincipalAndInterestPayment = loan.loanProduct().getLoanProductConfigurableAttributes()
                .getGraceOnPrincipalAndInterestPaymentBoolean();
        final Boolean repaymentEvery = loan.loanProduct().getLoanProductConfigurableAttributes().getRepaymentEveryBoolean();
        final Boolean transactionProcessingStrategy = loan.loanProduct().getLoanProductConfigurableAttributes()
                .getTransactionProcessingStrategyBoolean();

        if (!amortization) {
            productRelatedDetail.setAmortizationMethod(loan.loanProduct().getLoanProductRelatedDetail().getAmortizationMethod());
        }
        if (!arrearsTolerance) {
            productRelatedDetail.setInArrearsTolerance(loan.loanProduct().getLoanProductRelatedDetail().getArrearsTolerance());
        }
        if (!graceOnArrearsAging) {
            productRelatedDetail.setGraceOnArrearsAgeing(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnArrearsAgeing());
        }
        if (!interestCalcPeriod) {
            productRelatedDetail.setInterestCalculationPeriodMethod(
                    loan.loanProduct().getLoanProductRelatedDetail().getInterestCalculationPeriodMethod());
        }
        if (!interestMethod) {
            productRelatedDetail.setInterestMethod(loan.loanProduct().getLoanProductRelatedDetail().getInterestMethod());
        }
        if (!graceOnPrincipalAndInterestPayment) {
            productRelatedDetail.setGraceOnInterestPayment(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnInterestPayment());
            productRelatedDetail.setGraceOnPrincipalPayment(loan.loanProduct().getLoanProductRelatedDetail().getGraceOnPrincipalPayment());
        }
        if (!repaymentEvery) {
            productRelatedDetail.setRepayEvery(loan.loanProduct().getLoanProductRelatedDetail().getRepayEvery());
        }
        if (!transactionProcessingStrategy) {
            loan.updateTransactionProcessingStrategy(loan.loanProduct().getRepaymentStrategy());
        }
    }

    private void createAndPersistCalendarInstanceForInterestRecalculation(final Loan loan) {

        LocalDate calendarStartDate = loan.getExpectedDisbursedOnLocalDate();
        Integer repeatsOnDay = null;
        final RecalculationFrequencyType recalculationFrequencyType = loan.loanInterestRecalculationDetails().getRestFrequencyType();
        Integer recalculationFrequencyNthDay = loan.loanInterestRecalculationDetails().getRestFrequencyOnDay();
        if (recalculationFrequencyNthDay == null) {
            recalculationFrequencyNthDay = loan.loanInterestRecalculationDetails().getRestFrequencyNthDay();
            repeatsOnDay = loan.loanInterestRecalculationDetails().getRestFrequencyWeekday();
        }

        Integer frequency = loan.loanInterestRecalculationDetails().getRestInterval();
        CalendarEntityType calendarEntityType = CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL;
        final String title = "loan_recalculation_detail_" + loan.loanInterestRecalculationDetails().getId();

        createCalendar(loan, calendarStartDate, recalculationFrequencyNthDay, repeatsOnDay, recalculationFrequencyType, frequency,
                calendarEntityType, title);

        if (loan.loanInterestRecalculationDetails().getInterestRecalculationCompoundingMethod().isCompoundingEnabled()) {
            LocalDate compoundingStartDate = loan.getExpectedDisbursedOnLocalDate();
            Integer compoundingRepeatsOnDay = null;
            final RecalculationFrequencyType recalculationCompoundingFrequencyType = loan.loanInterestRecalculationDetails()
                    .getCompoundingFrequencyType();
            Integer recalculationCompoundingFrequencyNthDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyOnDay();
            if (recalculationCompoundingFrequencyNthDay == null) {
                recalculationCompoundingFrequencyNthDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyNthDay();
                compoundingRepeatsOnDay = loan.loanInterestRecalculationDetails().getCompoundingFrequencyWeekday();
            }

            Integer compoundingFrequency = loan.loanInterestRecalculationDetails().getCompoundingInterval();
            CalendarEntityType compoundingCalendarEntityType = CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL;
            final String compoundingCalendarTitle = "loan_recalculation_detail_compounding_frequency"
                    + loan.loanInterestRecalculationDetails().getId();

            createCalendar(loan, compoundingStartDate, recalculationCompoundingFrequencyNthDay, compoundingRepeatsOnDay,
                    recalculationCompoundingFrequencyType, compoundingFrequency, compoundingCalendarEntityType, compoundingCalendarTitle);
        }

    }

    private void createCalendar(final Loan loan, LocalDate calendarStartDate, Integer recalculationFrequencyNthDay,
            final Integer repeatsOnDay, final RecalculationFrequencyType recalculationFrequencyType, Integer frequency,
            CalendarEntityType calendarEntityType, final String title) {
        CalendarFrequencyType calendarFrequencyType = CalendarFrequencyType.INVALID;
        Integer updatedRepeatsOnDay = repeatsOnDay;
        switch (recalculationFrequencyType) {
            case DAILY:
                calendarFrequencyType = CalendarFrequencyType.DAILY;
            break;
            case MONTHLY:
                calendarFrequencyType = CalendarFrequencyType.MONTHLY;
            break;
            case SAME_AS_REPAYMENT_PERIOD:
                frequency = loan.repaymentScheduleDetail().getRepayEvery();
                calendarFrequencyType = CalendarFrequencyType.from(loan.repaymentScheduleDetail().getRepaymentPeriodFrequencyType());
                calendarStartDate = loan.getExpectedDisbursedOnLocalDate();
                if (updatedRepeatsOnDay == null) {
                    updatedRepeatsOnDay = calendarStartDate.get(ChronoField.DAY_OF_WEEK);
                }
            break;
            case WEEKLY:
                calendarFrequencyType = CalendarFrequencyType.WEEKLY;
            break;
            default:
            break;
        }

        final Calendar calendar = Calendar.createRepeatingCalendar(title, calendarStartDate, CalendarType.COLLECTION.getValue(),
                calendarFrequencyType, frequency, updatedRepeatsOnDay, recalculationFrequencyNthDay);
        final CalendarInstance calendarInstance = CalendarInstance.from(calendar, loan.loanInterestRecalculationDetails().getId(),
                calendarEntityType.getValue());
        this.calendarInstanceRepository.save(calendarInstance);
    }

    @Transactional
    @Override
    public CommandProcessingResult modifyApplication(final Long loanId, final JsonCommand command) {

        try {
            AppUser currentUser = getAppUserIfPresent();
            final Loan existingLoanApplication = retrieveLoanBy(loanId);
            if (!existingLoanApplication.isSubmittedAndPendingApproval()) {
                throw new LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeModified(loanId);
            }

            final String productIdParamName = "productId";
            LoanProduct newLoanProduct = null;
            if (command.isChangeInLongParameterNamed(productIdParamName, existingLoanApplication.loanProduct().getId())) {
                final Long productId = command.longValueOfParameterNamed(productIdParamName);
                newLoanProduct = this.loanProductRepository.findById(productId)
                        .orElseThrow(() -> new LoanProductNotFoundException(productId));
            }

            final String prequalificationIdParameterName = "prequalificationId";
            PrequalificationGroup prequalificationGroup = existingLoanApplication.getPrequalificationGroup();
            final Long existingPrequalificationId = existingLoanApplication.getPrequalificationGroup() != null
                    ? existingLoanApplication.getPrequalificationGroup().getId()
                    : null;
            if (command.isChangeInLongParameterNamed(prequalificationIdParameterName, existingPrequalificationId)) {
                final Long prequalificationId = command.longValueOfParameterNamed(prequalificationIdParameterName);
                prequalificationGroup = this.prequalificationGroupRepositoryWrapper.findOneWithNotFoundDetection(prequalificationId);
            }
            if (prequalificationGroup == null) {
                throw new PrequalificationNotProvidedException();
            }
            existingLoanApplication.setPrequalificationGroup(prequalificationGroup);

            LoanProduct loanProductForValidations = newLoanProduct == null ? existingLoanApplication.loanProduct() : newLoanProduct;

            this.fromApiJsonDeserializer.validateForModify(command.json(), loanProductForValidations, existingLoanApplication);

            checkClientOrGroupActive(existingLoanApplication);

            final Set<LoanCharge> existingCharges = existingLoanApplication.charges();
            Map<Long, LoanChargeData> chargesMap = new HashMap<>();
            for (LoanCharge charge : existingCharges) {
                LoanChargeData chargeData = new LoanChargeData(charge.getId(), charge.getDueLocalDate(), charge.amountOrPercentage());
                chargesMap.put(charge.getId(), chargeData);
            }
            List<LoanDisbursementDetails> disbursementDetails = this.loanUtilService
                    .fetchDisbursementData(command.parsedJson().getAsJsonObject());

            /**
             * Stores all charges which are passed in during modify loan application
             **/
            // TODO: FBR-369 we need the expected disbursement date and first repayment date. This is hack to get hose
            // but should be considered for improvement
            final LoanApplicationTerms loanApplicationTermsCharges = this.loanScheduleAssembler.assembleLoanTerms(command.parsedJson());
            final Set<LoanCharge> possiblyModifedLoanCharges = this.loanChargeAssembler.fromParsedJson(command.parsedJson(),
                    disbursementDetails, loanApplicationTermsCharges);
            /** Boolean determines if any charge has been modified **/
            boolean isChargeModified = false;

            Set<Charge> newTrancheChages = this.loanChargeAssembler.getNewLoanTrancheCharges(command.parsedJson());
            for (Charge charge : newTrancheChages) {
                existingLoanApplication.addTrancheLoanCharge(charge);
            }

            /**
             * If there are any charges already present, which are now not passed in as a part of the request, deem the
             * charges as modified
             **/
            if (!possiblyModifedLoanCharges.isEmpty()) {
                if (!possiblyModifedLoanCharges.containsAll(existingCharges)) {
                    isChargeModified = true;
                }
            }

            /**
             * If any new charges are added or values of existing charges are modified
             **/
            for (LoanCharge loanCharge : possiblyModifedLoanCharges) {
                if (loanCharge.getId() == null) {
                    isChargeModified = true;
                } else {
                    LoanChargeData chargeData = chargesMap.get(loanCharge.getId());
                    if (loanCharge.amountOrPercentage().compareTo(chargeData.amountOrPercentage()) != 0
                            || (loanCharge.isSpecifiedDueDate() && !loanCharge.getDueLocalDate().equals(chargeData.getDueDate()))) {
                        isChargeModified = true;
                    }
                }
            }

            Set<LoanCollateralManagement> possiblyModifedLoanCollateralItems = null;

            if (command.parameterExists("loanType")) {
                final String loanTypeStr = command.stringValueOfParameterNamed("loanType");
                final AccountType loanType = AccountType.fromName(loanTypeStr);

                if (!StringUtils.isBlank(loanTypeStr) && loanType.isIndividualAccount()) {
                    possiblyModifedLoanCollateralItems = this.loanCollateralAssembler.fromParsedJson(command.parsedJson());
                }
            }

            final Map<String, Object> changes = existingLoanApplication.loanApplicationModification(command, possiblyModifedLoanCharges,
                    possiblyModifedLoanCollateralItems, this.aprCalculator, isChargeModified, loanProductForValidations);

            if (changes.containsKey("expectedDisbursementDate")) {
                this.loanAssembler.validateExpectedDisbursementForHolidayAndNonWorkingDay(existingLoanApplication);
            }

            final String clientIdParamName = "clientId";
            if (changes.containsKey(clientIdParamName)) {
                final Long clientId = command.longValueOfParameterNamed(clientIdParamName);
                final Client client = this.clientRepository.findOneWithNotFoundDetection(clientId);
                if (client.isNotActive()) {
                    throw new ClientNotActiveException(clientId);
                }

                existingLoanApplication.updateClient(client);
            }

            final String groupIdParamName = "groupId";
            if (changes.containsKey(groupIdParamName)) {
                final Long groupId = command.longValueOfParameterNamed(groupIdParamName);
                final Group group = this.groupRepository.findOneWithNotFoundDetection(groupId);
                if (group.isNotActive()) {
                    throw new GroupNotActiveException(groupId);
                }

                existingLoanApplication.updateGroup(group);
            }

            if (newLoanProduct != null) {
                existingLoanApplication.updateLoanProduct(newLoanProduct);
                if (!changes.containsKey("interestRateFrequencyType")) {
                    existingLoanApplication.updateInterestRateFrequencyType();
                }
                final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
                final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
                if (newLoanProduct.useBorrowerCycle()) {
                    final Long clientId = this.fromJsonHelper.extractLongNamed("clientId", command.parsedJson());
                    final Long groupId = this.fromJsonHelper.extractLongNamed("groupId", command.parsedJson());
                    Integer cycleNumber = 0;
                    if (clientId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(clientId, newLoanProduct.getId());
                    } else if (groupId != null) {
                        cycleNumber = this.loanReadPlatformService.retriveLoanCounter(groupId, AccountType.GROUP.getValue(),
                                newLoanProduct.getId());
                    }
                    this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                            newLoanProduct, cycleNumber);
                } else {
                    this.loanProductCommandFromApiJsonDeserializer.validateMinMaxConstraints(command.parsedJson(), baseDataValidator,
                            newLoanProduct);
                }
                if (newLoanProduct.isLinkedToFloatingInterestRate()) {
                    existingLoanApplication.getLoanProductRelatedDetail().updateForFloatingInterestRates();
                } else {
                    existingLoanApplication.setInterestRateDifferential(null);
                    existingLoanApplication.setIsFloatingInterestRate(null);
                }
                if (!dataValidationErrors.isEmpty()) {
                    throw new PlatformApiDataValidationException(dataValidationErrors);
                }
            }

            existingLoanApplication.updateIsInterestRecalculationEnabled();
            validateSubmittedOnDate(existingLoanApplication);

            final LoanProductRelatedDetail productRelatedDetail = existingLoanApplication.repaymentScheduleDetail();
            if (existingLoanApplication.loanProduct().getLoanProductConfigurableAttributes() != null) {
                updateProductRelatedDetails(productRelatedDetail, existingLoanApplication);
            }

            if (existingLoanApplication.getLoanProduct().canUseForTopup() && existingLoanApplication.getClientId() != null) {
                final Boolean isTopup = command.booleanObjectValueOfParameterNamed(LoanApiConstants.isTopup);
                if (command.isChangeInBooleanParameterNamed(LoanApiConstants.isTopup, existingLoanApplication.isTopup())) {
                    existingLoanApplication.setIsTopup(isTopup);
                    changes.put(LoanApiConstants.isTopup, isTopup);
                }

                if (existingLoanApplication.isTopup()) {
                    final Long loanIdToClose = command.longValueOfParameterNamed(LoanApiConstants.loanIdToClose);
                    LoanTopupDetails existingLoanTopupDetails = existingLoanApplication.getTopupLoanDetails();
                    if (existingLoanTopupDetails == null
                            || (existingLoanTopupDetails != null && !existingLoanTopupDetails.getLoanIdToClose().equals(loanIdToClose))
                            || changes.containsKey("submittedOnDate") || changes.containsKey("expectedDisbursementDate")
                            || changes.containsKey("principal") || changes.containsKey(LoanApiConstants.disbursementDataParameterName)) {
                        Long existingLoanIdToClose = null;
                        if (existingLoanTopupDetails != null) {
                            existingLoanIdToClose = existingLoanTopupDetails.getLoanIdToClose();
                        }
                        final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose,
                                existingLoanApplication.getClientId());
                        if (loanToClose == null) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.loanIdToClose.no.active.loan.associated.to.client.found",
                                    "loanIdToClose is invalid, No Active Loan associated with the given Client ID found.");
                        }
                        if (loanToClose.isMultiDisburmentLoan() && !loanToClose.isInterestRecalculationEnabledForProduct()) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.topup.on.multi.tranche.loan.without.interest.recalculation.not.supported",
                                    "Topup on loan with multi-tranche disbursal and without interest recalculation is not supported.");
                        }
                        final LocalDate disbursalDateOfLoanToClose = loanToClose.getDisbursementDate();
                        if (!existingLoanApplication.getSubmittedOnDate().isAfter(disbursalDateOfLoanToClose)) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.submitted.date.should.be.after.topup.loan.disbursal.date",
                                    "Submitted date of this loan application " + existingLoanApplication.getSubmittedOnDate()
                                            + " should be after the disbursed date of loan to be closed " + disbursalDateOfLoanToClose);
                        }
                        if (!loanToClose.getCurrencyCode().equals(existingLoanApplication.getCurrencyCode())) {
                            throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.has.different.currency",
                                    "loanIdToClose is invalid, Currency code is different.");
                        }
                        final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                        if (existingLoanApplication.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                            throw new GeneralPlatformDomainRuleException(
                                    "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                                    "Disbursal date of this loan application " + existingLoanApplication.getDisbursementDate()
                                            + " should be after last transaction date of loan to be closed "
                                            + lastUserTransactionOnLoanToClose);
                        }
                        BigDecimal loanOutstanding = this.loanReadPlatformService.retrieveLoanPrePaymentTemplate(
                                LoanTransactionType.REPAYMENT, loanIdToClose, existingLoanApplication.getDisbursementDate()).getAmount();
                        final BigDecimal firstDisbursalAmount = existingLoanApplication.getFirstDisbursalAmount();
                        if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                            throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                                    "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                        }

                        if (!existingLoanIdToClose.equals(loanIdToClose)) {
                            final LoanTopupDetails topupDetails = new LoanTopupDetails(existingLoanApplication, loanIdToClose);
                            existingLoanApplication.setTopupLoanDetails(topupDetails);
                            changes.put(LoanApiConstants.loanIdToClose, loanIdToClose);
                        }
                    }
                } else {
                    existingLoanApplication.setTopupLoanDetails(null);
                }
            } else {
                if (existingLoanApplication.isTopup()) {
                    existingLoanApplication.setIsTopup(false);
                    existingLoanApplication.setTopupLoanDetails(null);
                    changes.put(LoanApiConstants.isTopup, false);
                }
            }

            final String fundIdParamName = "fundId";
            if (changes.containsKey(fundIdParamName)) {
                final Long fundId = command.longValueOfParameterNamed(fundIdParamName);
                final Fund fund = this.loanAssembler.findFundByIdIfProvided(fundId);

                existingLoanApplication.updateFund(fund);
            }

            final String loanPurposeIdParamName = "loanPurposeId";
            if (changes.containsKey(loanPurposeIdParamName)) {
                final Long loanPurposeId = command.longValueOfParameterNamed(loanPurposeIdParamName);
                final CodeValue loanPurpose = this.loanAssembler.findCodeValueByIdIfProvided(loanPurposeId);
                existingLoanApplication.updateLoanPurpose(loanPurpose);
            }

            final String loanOfficerIdParamName = "loanOfficerId";
            if (changes.containsKey(loanOfficerIdParamName)) {
                final Long loanOfficerId = command.longValueOfParameterNamed(loanOfficerIdParamName);
                final Staff newValue = this.loanAssembler.findLoanOfficerByIdIfProvided(loanOfficerId);
                existingLoanApplication.updateLoanOfficerOnLoanApplication(newValue);
            }

            final String strategyIdParamName = "transactionProcessingStrategyId";
            if (changes.containsKey(strategyIdParamName)) {
                final Long strategyId = command.longValueOfParameterNamed(strategyIdParamName);
                final LoanTransactionProcessingStrategy strategy = this.loanAssembler.findStrategyByIdIfProvided(strategyId);

                existingLoanApplication.updateTransactionProcessingStrategy(strategy);
            }

            /**
             * TODO: Allow other loan types if needed.
             */
            if (command.parameterExists("loanType")) {
                final String loanTypeStr = command.stringValueOfParameterNamed("loanType");
                final AccountType loanType = AccountType.fromName(loanTypeStr);

                if (!StringUtils.isBlank(loanTypeStr) && loanType.isIndividualAccount()) {
                    final String collateralParamName = "collateral";
                    if (changes.containsKey(collateralParamName)) {
                        existingLoanApplication.updateLoanCollateral(possiblyModifedLoanCollateralItems);
                    }
                }
            }

            final String chargesParamName = "charges";
            if (changes.containsKey(chargesParamName)) {
                existingLoanApplication.updateLoanCharges(possiblyModifedLoanCharges);
            }

            if (changes.containsKey("recalculateLoanSchedule")) {
                changes.remove("recalculateLoanSchedule");

                final JsonElement parsedQuery = this.fromJsonHelper.parse(command.json());
                final JsonQuery query = JsonQuery.from(command.json(), parsedQuery, this.fromJsonHelper);

                final LoanScheduleModel loanSchedule = this.calculationPlatformService.calculateLoanSchedule(query, false);
                existingLoanApplication.updateLoanSchedule(loanSchedule);
                existingLoanApplication.recalculateAllCharges();
            }

            // Changes to modify loan rates.
            if (command.hasParameter(LoanProductConstants.RATES_PARAM_NAME)) {
                existingLoanApplication.updateLoanRates(rateAssembler.fromParsedJson(command.parsedJson()));
            }

            this.fromApiJsonDeserializer.validateLoanTermAndRepaidEveryValues(existingLoanApplication.getTermFrequency(),
                    existingLoanApplication.getTermPeriodFrequencyType(), productRelatedDetail.getNumberOfRepayments(),
                    productRelatedDetail.getRepayEvery(), productRelatedDetail.getRepaymentPeriodFrequencyType().getValue(),
                    existingLoanApplication);

            saveAndFlushLoanWithDataIntegrityViolationChecks(existingLoanApplication);

            final String submittedOnNote = command.stringValueOfParameterNamed("submittedOnNote");
            if (StringUtils.isNotBlank(submittedOnNote)) {
                final Note note = Note.loanNote(existingLoanApplication, submittedOnNote);
                this.noteRepository.save(note);
            }

            final Long calendarId = command.longValueOfParameterNamed("calendarId");
            Calendar calendar = null;
            if (calendarId != null && calendarId != 0) {
                calendar = this.calendarRepository.findById(calendarId).orElseThrow(() -> new CalendarNotFoundException(calendarId));
            }

            final List<CalendarInstance> ciList = (List<CalendarInstance>) this.calendarInstanceRepository
                    .findByEntityIdAndEntityTypeId(loanId, CalendarEntityType.LOANS.getValue());
            if (calendar != null) {

                // For loans, allow to attach only one calendar instance per
                // loan
                if (ciList != null && !ciList.isEmpty()) {
                    final CalendarInstance calendarInstance = ciList.get(0);
                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                            calendarInstance.getEntityId(), calendarInstance.getCalendar().getId(),
                            CalendarEntityType.LOANS.getValue().longValue());
                    if (isCalendarAssociatedWithEntity && calendarId == null) {
                        this.calendarRepository.delete(calendarInstance.getCalendar());
                    }
                    if (!calendarInstance.getCalendar().getId().equals(calendar.getId())) {
                        calendarInstance.updateCalendar(calendar);
                        this.calendarInstanceRepository.saveAndFlush(calendarInstance);
                    }
                } else {
                    // attaching new calendar
                    final CalendarInstance calendarInstance = new CalendarInstance(calendar, existingLoanApplication.getId(),
                            CalendarEntityType.LOANS.getValue());
                    this.calendarInstanceRepository.save(calendarInstance);
                }

            } else {
                if (ciList != null && !ciList.isEmpty()) {
                    final CalendarInstance existingCalendarInstance = ciList.get(0);
                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                            existingCalendarInstance.getEntityId(), existingCalendarInstance.getCalendar().getId(),
                            CalendarEntityType.GROUPS.getValue().longValue());
                    if (isCalendarAssociatedWithEntity) {
                        this.calendarInstanceRepository.delete(existingCalendarInstance);
                    }
                }
                if (changes.containsKey("repaymentFrequencyNthDayType") || changes.containsKey("repaymentFrequencyDayOfWeekType")) {
                    if (changes.get("repaymentFrequencyNthDayType") == null) {
                        if (ciList != null && !ciList.isEmpty()) {
                            final CalendarInstance calendarInstance = ciList.get(0);
                            final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService.isCalendarAssociatedWithEntity(
                                    calendarInstance.getEntityId(), calendarInstance.getCalendar().getId(),
                                    CalendarEntityType.LOANS.getValue().longValue());
                            if (isCalendarAssociatedWithEntity) {
                                this.calendarInstanceRepository.delete(calendarInstance);
                                this.calendarRepository.delete(calendarInstance.getCalendar());
                            }
                        }
                    } else {
                        Integer repaymentFrequencyTypeInt = command.integerValueOfParameterNamed("repaymentFrequencyType");
                        if (repaymentFrequencyTypeInt != null) {
                            if (PeriodFrequencyType.fromInt(repaymentFrequencyTypeInt) == PeriodFrequencyType.MONTHS) {
                                final String title = "loan_schedule_" + existingLoanApplication.getId();
                                final Integer typeId = CalendarType.COLLECTION.getValue();
                                final CalendarFrequencyType repaymentFrequencyType = CalendarFrequencyType.MONTHLY;
                                final Integer interval = command.integerValueOfParameterNamed("repaymentEvery");
                                LocalDate startDate = command.localDateValueOfParameterNamed("repaymentsStartingFromDate");
                                if (startDate == null) {
                                    startDate = command.localDateValueOfParameterNamed("expectedDisbursementDate");
                                }
                                final Calendar newCalendar = Calendar.createRepeatingCalendar(title, startDate, typeId,
                                        repaymentFrequencyType, interval, (Integer) changes.get("repaymentFrequencyDayOfWeekType"),
                                        (Integer) changes.get("repaymentFrequencyNthDayType"));
                                if (ciList != null && !ciList.isEmpty()) {
                                    final CalendarInstance calendarInstance = ciList.get(0);
                                    final boolean isCalendarAssociatedWithEntity = this.calendarReadPlatformService
                                            .isCalendarAssociatedWithEntity(calendarInstance.getEntityId(),
                                                    calendarInstance.getCalendar().getId(),
                                                    CalendarEntityType.LOANS.getValue().longValue());
                                    if (isCalendarAssociatedWithEntity) {
                                        final Calendar existingCalendar = calendarInstance.getCalendar();
                                        if (existingCalendar != null) {
                                            String existingRecurrence = existingCalendar.getRecurrence();
                                            if (!existingRecurrence.equals(newCalendar.getRecurrence())) {
                                                existingCalendar.setRecurrence(newCalendar.getRecurrence());
                                                this.calendarRepository.save(existingCalendar);
                                            }
                                        }
                                    }
                                } else {
                                    this.calendarRepository.save(newCalendar);
                                    final Integer calendarEntityType = CalendarEntityType.LOANS.getValue();
                                    final CalendarInstance calendarInstance = new CalendarInstance(newCalendar,
                                            existingLoanApplication.getId(), calendarEntityType);
                                    this.calendarInstanceRepository.save(calendarInstance);
                                }
                            }
                        }
                    }
                }
            }

            // Save linked account information
            final String linkAccountIdParamName = "linkAccountId";
            final boolean backdatedTxnsAllowedTill = false;
            final Long savingsAccountId = command.longValueOfParameterNamed(linkAccountIdParamName);
            AccountAssociations accountAssociations = this.accountAssociationsRepository.findByLoanIdAndType(loanId,
                    AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
            boolean isLinkedAccPresent = false;
            if (savingsAccountId == null) {
                if (accountAssociations != null) {
                    if (this.fromJsonHelper.parameterExists(linkAccountIdParamName, command.parsedJson())) {
                        if (accountAssociations.linkedCupo() != null) {
                            accountAssociations.updateLinkedSavingsAccount(null);
                            this.accountAssociationsRepository.save(accountAssociations);
                        } else {
                            this.accountAssociationsRepository.delete(accountAssociations);
                        }
                        changes.put(linkAccountIdParamName, null);
                    } else {
                        isLinkedAccPresent = true;
                    }
                }
            } else {
                isLinkedAccPresent = true;
                boolean isModified = false;
                if (accountAssociations == null) {
                    isModified = true;
                } else {
                    final SavingsAccount savingsAccount = accountAssociations.linkedSavingsAccount();
                    if (savingsAccount == null || !savingsAccount.getId().equals(savingsAccountId)) {
                        isModified = true;
                    }
                }
                if (isModified) {
                    final SavingsAccount savingsAccount = this.savingsAccountAssembler.assembleFrom(savingsAccountId,
                            backdatedTxnsAllowedTill);
                    this.fromApiJsonDeserializer.validatelinkedSavingsAccount(savingsAccount, existingLoanApplication);
                    if (accountAssociations == null) {
                        boolean isActive = true;
                        accountAssociations = AccountAssociations.associateSavingsAccount(existingLoanApplication, savingsAccount,
                                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                    } else {
                        accountAssociations.updateLinkedSavingsAccount(savingsAccount);
                    }
                    changes.put(linkAccountIdParamName, savingsAccountId);
                    this.accountAssociationsRepository.save(accountAssociations);
                }
            }

            if (!isLinkedAccPresent) {
                final Set<LoanCharge> charges = existingLoanApplication.charges();
                for (final LoanCharge loanCharge : charges) {
                    if (loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()) {
                        final String errorMessage = "one of the charges requires linked savings account for payment";
                        throw new LinkedAccountRequiredException("loanCharge", errorMessage);
                    }
                }
            }

            final Long cupoId = command.longValueOfParameterNamed(LoanApiConstants.cupoIdParameterName);
            if (cupoId == null) {
                if (accountAssociations != null) {
                    if (this.fromJsonHelper.parameterExists(LoanApiConstants.cupoIdParameterName, command.parsedJson())) {
                        if (accountAssociations.linkedSavingsAccount() != null) {
                            accountAssociations.updateLinkedCupo(null);
                            this.accountAssociationsRepository.save(accountAssociations);
                        } else {
                            this.accountAssociationsRepository.delete(accountAssociations);
                        }
                        changes.put(linkAccountIdParamName, null);
                    }
                }
            } else {
                final Cupo cupo = this.cupoRepositoryWrapper.findOneWithNotFoundDetection(cupoId);
                this.fromApiJsonDeserializer.validatelinkedCupo(cupo, existingLoanApplication);
                if (accountAssociations == null) {
                    boolean isActive = true;
                    accountAssociations = AccountAssociations.associateCupo(existingLoanApplication, cupo,
                            AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue(), isActive);
                } else {
                    accountAssociations.updateLinkedCupo(cupo);
                }
                changes.put(LoanApiConstants.cupoIdParameterName, cupoId);
                this.accountAssociationsRepository.save(accountAssociations);
            }

            if ((command.longValueOfParameterNamed(productIdParamName) != null)
                    || (command.longValueOfParameterNamed(clientIdParamName) != null)
                    || (command.longValueOfParameterNamed(groupIdParamName) != null)) {
                Long OfficeId = null;
                if (existingLoanApplication.getClient() != null) {
                    OfficeId = existingLoanApplication.getClient().getOffice().getId();
                } else if (existingLoanApplication.getGroup() != null) {
                    OfficeId = existingLoanApplication.getGroup().getOffice().getId();
                }
                officeSpecificLoanProductValidation(existingLoanApplication.getLoanProduct().getId(), OfficeId);
            }

            if (prequalificationGroup.isPrequalificationTypeIndividual()) {
                final JsonElement loanAdditionalDataJson = command.jsonElement(LoanApiConstants.LOAN_ADDITIONAL_DATA);
                if (loanAdditionalDataJson != null && loanAdditionalDataJson.isJsonObject()) {
                    this.fromApiJsonDeserializer.validateLoanAdditionalData(command);
                    final LoanAdditionalData loanAdditionalData = this.fromJsonCommand(command);
                    final String caseId = loanAdditionalData.getCaseId();
                    final Client loanClient = existingLoanApplication.getClient();
                    final List<LoanAdditionProperties> additionalList = this.loanAdditionalPropertiesRepository
                            .findByClientIdAndLoanId(loanClient.getId(), existingLoanApplication.getId());
                    LoanAdditionProperties loanAdditionEntity;
                    if (!CollectionUtils.isEmpty(additionalList)) {
                        loanAdditionEntity = additionalList.get(0);
                        BeanUtils.copyProperties(loanAdditionalData, loanAdditionEntity);
                    } else {
                        loanAdditionEntity = loanAdditionalData.toEntity();
                    }
                    loanAdditionEntity.setCaseId(caseId);
                    loanAdditionEntity.setClient(loanClient);
                    loanAdditionEntity.setLoan(existingLoanApplication);
                    loanAdditionalPropertiesRepository.saveAndFlush(loanAdditionEntity);
                }
            }

            // updating loan interest recalculation details throwing null
            // pointer exception after saveAndFlush
            // http://stackoverflow.com/questions/17151757/hibernate-cascade-update-gives-null-pointer/17334374#17334374
            this.loanRepositoryWrapper.saveAndFlush(existingLoanApplication);

            GroupLoanAdditionals additionals = this.groupLoanAdditionalsRepository.getGroupLoanAdditionalsByLoan(existingLoanApplication);
            if (additionals==null){
                Long facilitatorId = command.longValueOfParameterNamed("facilitator");
                AppUser facilitator = null;
                if (facilitatorId != null) {
                    facilitator = this.appUserRepository.findById(facilitatorId)
                            .orElseThrow(() -> new GeneralPlatformDomainRuleException("error.msg.loan.facilitator.not.found",
                                    "Facilitator with identifier " + facilitatorId + " does not exist"));
                }
                additionals = GroupLoanAdditionals.assembleFromJson(command,existingLoanApplication, facilitator);
            }else{
                additionals.update(command);
            }
            updateExternalLoans(command,additionals);
            this.groupLoanAdditionalsRepository.save(additionals);

            if (productRelatedDetail.isInterestRecalculationEnabled()) {
                this.fromApiJsonDeserializer.validateLoanForInterestRecalculation(existingLoanApplication);
                if (changes.containsKey(LoanProductConstants.IS_INTEREST_RECALCULATION_ENABLED_PARAMETER_NAME)) {
                    createAndPersistCalendarInstanceForInterestRecalculation(existingLoanApplication);

                }

            }

            return new CommandProcessingResultBuilder() //
                    .withEntityId(loanId) //
                    .withOfficeId(existingLoanApplication.getOfficeId()) //
                    .withClientId(existingLoanApplication.getClientId()) //
                    .withGroupId(existingLoanApplication.getGroupId()) //
                    .withLoanId(existingLoanApplication.getId()) //
                    .with(changes).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void updateExternalLoans(JsonCommand command, GroupLoanAdditionals groupLoanAdditionals) {
        JsonArray externalLoansArray = command.arrayOfParameterNamed(LoanApiConstants.externalLoansParamName);

        List<AdditionalsExtraLoans> additionalLoansList = new ArrayList<>();
        if (!ObjectUtils.isEmpty(externalLoansArray)) {
            for (JsonElement element : externalLoansArray) {
                JsonObject loanData = element.getAsJsonObject();

                String name = null;
                if (loanData.get("institutionName") != null) {
                    name = loanData.get("institutionName").getAsString();
                }
                Long institutionType = null;
                if (loanData.get("institutionType") != null) {
                    institutionType = loanData.get("institutionType").getAsLong();
                }

                Long loanStatus = null;
                if (loanData.get("loanStatus") != null) {
                    loanStatus = loanData.get("loanStatus").getAsLong();
                }

                BigDecimal totalLoanBalance = null;
                if (loanData.get("totalLoanBalance") != null) {
                    totalLoanBalance = new BigDecimal(loanData.get("totalLoanBalance").getAsString().replace(",", "".trim()));
                }

                BigDecimal charges = null;
                if (loanData.get("charges") != null) {
                    charges = new BigDecimal(loanData.get("charges").getAsString().replace(",", "").trim());
                }
                BigDecimal totalLoanAmount = null;
                if (loanData.get("totalLoanAmount") != null) {
                    totalLoanAmount = new BigDecimal(loanData.get("totalLoanAmount").getAsString().replace(",", "").trim());
                }

                AdditionalsExtraLoans additionalsExtraLoans = new AdditionalsExtraLoans(groupLoanAdditionals, institutionType,
                        totalLoanAmount, totalLoanBalance, charges, loanStatus,name);
                additionalLoansList.add(additionalsExtraLoans);

            }
            groupLoanAdditionals.setExtraLoans(additionalLoansList);
        }
    }

    public LoanAdditionalData fromJsonCommand(final JsonCommand jsonCommand) {
        final LoanAdditionalData loanAdditionalData = new LoanAdditionalData();
        final String dateFormat = jsonCommand.dateFormat();
        final String localeAsString = jsonCommand.locale();
        final Locale locale = JsonParserHelper.localeFromString(localeAsString);
        this.fromApiJsonDeserializer.validateLoanAdditionalData(jsonCommand);
        final JsonElement jsonElement = jsonCommand.jsonElement(LoanApiConstants.LOAN_ADDITIONAL_DATA);
        final String caseId = this.fromJsonHelper.extractStringNamed("caseId", jsonElement);
        loanAdditionalData.setCaseId(caseId);

        final Integer ciclosCancelados = this.fromJsonHelper.extractIntegerNamed("ciclosCancelados", jsonElement, locale);
        loanAdditionalData.setCiclosCancelados(ciclosCancelados);

        final Long branchCode = this.fromJsonHelper.extractLongNamed("branchCode", jsonElement);
        loanAdditionalData.setBranchCode(branchCode);

        final String cargoTesorera = this.fromJsonHelper.extractStringNamed("cargoTesorera", jsonElement);
        loanAdditionalData.setCargoTesorera(cargoTesorera);

        final String cargo = this.fromJsonHelper.extractStringNamed("cargo", jsonElement);
        loanAdditionalData.setCargo(cargo);

        final String estadoSolicitud = this.fromJsonHelper.extractStringNamed("estadoSolicitud", jsonElement);
        loanAdditionalData.setEstadoSolicitud(estadoSolicitud);

        final LocalDate fechaInicio = this.fromJsonHelper.extractLocalDateNamed("fechaInicio", jsonElement, dateFormat, locale);
        loanAdditionalData.setFechaInicio(fechaInicio);

        final String producto = this.fromJsonHelper.extractStringNamed("producto", jsonElement);
        loanAdditionalData.setProducto(producto);

        LocalDate fechaSolicitud = this.fromJsonHelper.extractLocalDateNamed("fechaSolicitud", jsonElement, dateFormat, locale);
        if (fechaSolicitud == null) {
            fechaSolicitud = this.fromJsonHelper.extractLocalDateNamed("fecha_solicitud", jsonElement, dateFormat, locale);
        }
        loanAdditionalData.setFechaSolicitud(fechaSolicitud);

        final String codigoCliente = this.fromJsonHelper.extractStringNamed("codigoCliente", jsonElement);
        loanAdditionalData.setCodigoCliente(codigoCliente);

        final String actividadNegocio = this.fromJsonHelper.extractStringNamed("actividadNegocio", jsonElement);
        loanAdditionalData.setActividadNegocio(actividadNegocio);

        final BigDecimal activoCorriente = this.fromJsonHelper.extractBigDecimalNamed("activoCorriente", jsonElement, locale);
        loanAdditionalData.setActivoCorriente(activoCorriente);

        final BigDecimal activoNocorriente = this.fromJsonHelper.extractBigDecimalNamed("activoNocorriente", jsonElement, locale);
        loanAdditionalData.setActivoNocorriente(activoNocorriente);

        final BigDecimal alimentacion = this.fromJsonHelper.extractBigDecimalNamed("alimentacion", jsonElement, locale);
        loanAdditionalData.setAlimentacion(alimentacion);

        final BigDecimal alquilerCliente = this.fromJsonHelper.extractBigDecimalNamed("alquilerCliente", jsonElement, locale);
        loanAdditionalData.setAlquilerCliente(alquilerCliente);

        final BigDecimal alquilerGasto = this.fromJsonHelper.extractBigDecimalNamed("alquilerGasto", jsonElement, locale);
        loanAdditionalData.setAlquilerGasto(alquilerGasto);

        final BigDecimal alquilerLocal = this.fromJsonHelper.extractBigDecimalNamed("alquilerLocal", jsonElement, locale);
        loanAdditionalData.setAlquilerLocal(alquilerLocal);

        final String antiguedadNegocio = this.fromJsonHelper.extractStringNamed("antiguedadNegocio", jsonElement);
        loanAdditionalData.setAntiguedadNegocio(antiguedadNegocio);

        final String apoyoFamilia = this.fromJsonHelper.extractStringNamed("apoyoFamilia", jsonElement);
        loanAdditionalData.setApoyoFamilia(apoyoFamilia);

        final Integer aprobacionesBc = this.fromJsonHelper.extractIntegerNamed("aprobacionesBc", jsonElement, locale);
        loanAdditionalData.setAprobacionesBc(aprobacionesBc);

        final String area = this.fromJsonHelper.extractStringNamed("area", jsonElement);
        loanAdditionalData.setArea(area);

        final Integer bienesInmuebles = this.fromJsonHelper.extractIntegerNamed("bienesInmuebles", jsonElement, locale);
        loanAdditionalData.setBienesInmuebles(bienesInmuebles);

        final Integer bienesInmueblesFamiliares = this.fromJsonHelper.extractIntegerNamed("bienesInmueblesFamiliares", jsonElement, locale);
        loanAdditionalData.setBienesInmueblesFamiliares(bienesInmueblesFamiliares);

        final String cDpi = this.fromJsonHelper.extractStringNamed("cDpi", jsonElement);
        loanAdditionalData.setCDpi(cDpi);

        final Integer cEdad = this.fromJsonHelper.extractIntegerNamed("cEdad", jsonElement, locale);
        loanAdditionalData.setCEdad(cEdad);

        final LocalDate cFechaNacimiento = this.fromJsonHelper.extractLocalDateNamed("cFechaNacimiento", jsonElement, dateFormat, locale);
        loanAdditionalData.setCFechaNacimiento(cFechaNacimiento);

        final String cOtroNombre = this.fromJsonHelper.extractStringNamed("cOtroNombre", jsonElement);
        loanAdditionalData.setCOtroNombre(cOtroNombre);

        final String cPrimerApellido = this.fromJsonHelper.extractStringNamed("cPrimerApellido", jsonElement);
        loanAdditionalData.setCPrimerApellido(cPrimerApellido);

        final String cProfesion = this.fromJsonHelper.extractStringNamed("cProfesion", jsonElement);
        loanAdditionalData.setCProfesion(cProfesion);

        final String cSegundoApellido = this.fromJsonHelper.extractStringNamed("cSegundoApellido", jsonElement);
        loanAdditionalData.setCSegundoApellido(cSegundoApellido);

        final String cSegundoNombre = this.fromJsonHelper.extractStringNamed("cSegundoNombre", jsonElement);
        loanAdditionalData.setCSegundoNombre(cSegundoNombre);

        final String cTelefono = this.fromJsonHelper.extractStringNamed("cTelefono", jsonElement);
        loanAdditionalData.setCTelefono(cTelefono);

        final String cPrimerNombre = this.fromJsonHelper.extractStringNamed("cPrimerNombre", jsonElement);
        loanAdditionalData.setCPrimerNombre(cPrimerNombre);

        final BigDecimal capacidadPago = this.fromJsonHelper.extractBigDecimalNamed("capacidadPago", jsonElement, locale);
        loanAdditionalData.setCapacidadPago(capacidadPago);

        final BigDecimal comunalVigente = this.fromJsonHelper.extractBigDecimalNamed("comunalVigente", jsonElement, locale);
        loanAdditionalData.setComunalVigente(comunalVigente);

        final BigDecimal costoUnitario = this.fromJsonHelper.extractBigDecimalNamed("costoUnitario", jsonElement, locale);
        loanAdditionalData.setCostoUnitario(costoUnitario);

        final BigDecimal costoVenta = this.fromJsonHelper.extractBigDecimalNamed("costoVenta", jsonElement, locale);
        loanAdditionalData.setCostoVenta(costoVenta);

        final BigDecimal cuantoPagar = this.fromJsonHelper.extractBigDecimalNamed("cuantoPagar", jsonElement, locale);
        loanAdditionalData.setCuantoPagar(cuantoPagar);

        final BigDecimal cuentasPorPagar = this.fromJsonHelper.extractBigDecimalNamed("cuentasPorPagar", jsonElement, locale);
        loanAdditionalData.setCuentasPorPagar(cuentasPorPagar);

        final Integer cuota = this.fromJsonHelper.extractIntegerNamed("cuota", jsonElement, locale);
        loanAdditionalData.setCuota(cuota);

        final Integer cuotaOtros = this.fromJsonHelper.extractIntegerNamed("cuotaOtros", jsonElement, locale);
        loanAdditionalData.setCuotaOtros(cuotaOtros);

        final Integer cuotaPuente = this.fromJsonHelper.extractIntegerNamed("cuotaPuente", jsonElement, locale);
        loanAdditionalData.setCuotaPuente(cuotaPuente);

        final Integer cuotasPendientesBc = this.fromJsonHelper.extractIntegerNamed("cuotasPendientesBc", jsonElement, locale);
        loanAdditionalData.setCuotasPendientesBc(cuotasPendientesBc);

        final Integer dependientes = this.fromJsonHelper.extractIntegerNamed("dependientes", jsonElement, locale);
        loanAdditionalData.setDependientes(dependientes);

        final String destinoPrestamo = this.fromJsonHelper.extractStringNamed("destinoPrestamo", jsonElement);
        loanAdditionalData.setDestinoPrestamo(destinoPrestamo);

        final Integer educacion = this.fromJsonHelper.extractIntegerNamed("educacion", jsonElement, locale);
        loanAdditionalData.setEducacion(educacion);

        final BigDecimal efectivo = this.fromJsonHelper.extractBigDecimalNamed("efectivo", jsonElement, locale);
        loanAdditionalData.setEfectivo(efectivo);

        final BigDecimal endeudamientoActual = this.fromJsonHelper.extractBigDecimalNamed("endeudamientoActual", jsonElement, locale);
        loanAdditionalData.setEndeudamientoActual(endeudamientoActual);

        final Integer enf = this.fromJsonHelper.extractIntegerNamed("enf", jsonElement, locale);
        loanAdditionalData.setEnf(enf);

        final String escribe = this.fromJsonHelper.extractStringNamed("escribe", jsonElement);
        loanAdditionalData.setEscribe(escribe);

        final String evolucionNegocio = this.fromJsonHelper.extractStringNamed("evolucionNegocio", jsonElement);
        loanAdditionalData.setEvolucionNegocio(evolucionNegocio);

        final String fPep = this.fromJsonHelper.extractStringNamed("fPep", jsonElement);
        loanAdditionalData.setFPep(fPep);

        final Integer familiares = this.fromJsonHelper.extractIntegerNamed("familiares", jsonElement, locale);
        loanAdditionalData.setFamiliares(familiares);

        final LocalDate fechaPrimeraReunion = this.fromJsonHelper.extractLocalDateNamed("fechaPrimeraReunion", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFechaPrimeraReunion(fechaPrimeraReunion);

        final Integer flujoDisponible = this.fromJsonHelper.extractIntegerNamed("flujoDisponible", jsonElement, locale);
        loanAdditionalData.setFlujoDisponible(flujoDisponible);

        final String garantiaPrestamo = this.fromJsonHelper.extractStringNamed("garantiaPrestamo", jsonElement);
        loanAdditionalData.setGarantiaPrestamo(garantiaPrestamo);

        final BigDecimal gastosFamiliares = this.fromJsonHelper.extractBigDecimalNamed("gastosFamiliares", jsonElement, locale);
        loanAdditionalData.setGastosFamiliares(gastosFamiliares);

        final BigDecimal gastosNegocio = this.fromJsonHelper.extractBigDecimalNamed("gastosNegocio", jsonElement, locale);
        loanAdditionalData.setGastosNegocio(gastosNegocio);

        final Integer herramientas = this.fromJsonHelper.extractIntegerNamed("herramientas", jsonElement, locale);
        loanAdditionalData.setHerramientas(herramientas);

        final Integer hijos = this.fromJsonHelper.extractIntegerNamed("hijos", jsonElement, locale);
        loanAdditionalData.setHijos(hijos);

        final Integer mortgages = this.fromJsonHelper.extractIntegerNamed("mortgages", jsonElement, locale);
        loanAdditionalData.setMortgages(mortgages);

        final Integer impuestos = this.fromJsonHelper.extractIntegerNamed("impuestos", jsonElement, locale);
        loanAdditionalData.setImpuestos(impuestos);

        final String ingresadoPor = this.fromJsonHelper.extractStringNamed("ingresadoPor", jsonElement);
        loanAdditionalData.setIngresadoPor(ingresadoPor);

        final BigDecimal ingresoFamiliar = this.fromJsonHelper.extractBigDecimalNamed("ingresoFamiliar", jsonElement, locale);
        loanAdditionalData.setIngresoFamiliar(ingresoFamiliar);

        final Integer integrantesAdicional = this.fromJsonHelper.extractIntegerNamed("integrantesAdicional", jsonElement, locale);
        loanAdditionalData.setIntegrantesAdicional(integrantesAdicional);

        final BigDecimal inventarios = this.fromJsonHelper.extractBigDecimalNamed("inventarios", jsonElement, locale);
        loanAdditionalData.setInventarios(inventarios);

        final BigDecimal inversionTotal = this.fromJsonHelper.extractBigDecimalNamed("inversionTotal", jsonElement, locale);
        loanAdditionalData.setInversionTotal(inversionTotal);

        final String invertir = this.fromJsonHelper.extractStringNamed("invertir", jsonElement);
        loanAdditionalData.setInvertir(invertir);

        final String lee = this.fromJsonHelper.extractStringNamed("lee", jsonElement);
        loanAdditionalData.setLee(lee);

        final BigDecimal menajeDelHogar = this.fromJsonHelper.extractBigDecimalNamed("menajeDelHogar", jsonElement, locale);
        loanAdditionalData.setMenajeDelHogar(menajeDelHogar);

        final BigDecimal mobiliarioYequipo = this.fromJsonHelper.extractBigDecimalNamed("mobiliarioYequipo", jsonElement, locale);
        loanAdditionalData.setMobiliarioYequipo(mobiliarioYequipo);

        final BigDecimal montoSolicitado = this.fromJsonHelper.extractBigDecimalNamed("montoSolicitado", jsonElement, locale);
        loanAdditionalData.setMontoSolicitado(montoSolicitado);

        final String motivoSolicitud = this.fromJsonHelper.extractStringNamed("motivoSolicitud", jsonElement);
        loanAdditionalData.setMotivoSolicitud(motivoSolicitud);

        final String nit = this.fromJsonHelper.extractStringNamed("nit", jsonElement);
        loanAdditionalData.setNit(nit);

        final String nombrePropio = this.fromJsonHelper.extractStringNamed("nombrePropio", jsonElement);
        loanAdditionalData.setNombrePropio(nombrePropio);

        final BigDecimal pasivoCorriente = this.fromJsonHelper.extractBigDecimalNamed("pasivoCorriente", jsonElement, locale);
        loanAdditionalData.setPasivoCorriente(pasivoCorriente);

        final BigDecimal pasivoNoCorriente = this.fromJsonHelper.extractBigDecimalNamed("pasivoNoCorriente", jsonElement, locale);
        loanAdditionalData.setPasivoNoCorriente(pasivoNoCorriente);

        final BigDecimal pensiones = this.fromJsonHelper.extractBigDecimalNamed("pensiones", jsonElement, locale);
        loanAdditionalData.setPensiones(pensiones);

        final String pep = this.fromJsonHelper.extractStringNamed("pep", jsonElement);
        loanAdditionalData.setPep(pep);

        final Integer plazo = this.fromJsonHelper.extractIntegerNamed("plazo", jsonElement, locale);
        loanAdditionalData.setPlazo(plazo);

        final Integer plazoVigente = this.fromJsonHelper.extractIntegerNamed("plazoVigente", jsonElement, locale);
        loanAdditionalData.setPlazoVigente(plazoVigente);

        final String poseeCuenta = this.fromJsonHelper.extractStringNamed("poseeCuenta", jsonElement);
        loanAdditionalData.setPoseeCuenta(poseeCuenta);

        final Long prestamoPuente = this.fromJsonHelper.extractLongNamed("prestamoPuente", jsonElement);
        loanAdditionalData.setPrestamoPuente(prestamoPuente);

        final BigDecimal propuestaFacilitador = this.fromJsonHelper.extractBigDecimalNamed("propuestaFacilitador", jsonElement, locale);
        loanAdditionalData.setPropuestaFacilitador(propuestaFacilitador);

        final String puntoReunion = this.fromJsonHelper.extractStringNamed("puntoReunion", jsonElement);
        loanAdditionalData.setPuntoReunion(puntoReunion);

        final BigDecimal relacionGastos = this.fromJsonHelper.extractBigDecimalNamed("relacionGastos", jsonElement, locale);
        loanAdditionalData.setRelacionGastos(relacionGastos);

        final BigDecimal rentabilidadNeta = this.fromJsonHelper.extractBigDecimalNamed("rentabilidadNeta", jsonElement, locale);
        loanAdditionalData.setRentabilidadNeta(rentabilidadNeta);

        final BigDecimal rotacionInventario = this.fromJsonHelper.extractBigDecimalNamed("rotacionInventario", jsonElement, locale);
        loanAdditionalData.setRotacionInventario(rotacionInventario);

        final BigDecimal salarioCliente = this.fromJsonHelper.extractBigDecimalNamed("salarioCliente", jsonElement, locale);
        loanAdditionalData.setSalarioCliente(salarioCliente);

        final BigDecimal salarios = this.fromJsonHelper.extractBigDecimalNamed("salarios", jsonElement, locale);
        loanAdditionalData.setSalarios(salarios);

        final String salud = this.fromJsonHelper.extractStringNamed("salud", jsonElement);
        loanAdditionalData.setSalud(salud);

        final String servicios = this.fromJsonHelper.extractStringNamed("servicios", jsonElement);
        loanAdditionalData.setServicios(servicios);

        final BigDecimal serviciosBasicos = this.fromJsonHelper.extractBigDecimalNamed("serviciosBasicos", jsonElement, locale);
        loanAdditionalData.setServiciosBasicos(serviciosBasicos);

        final BigDecimal serviciosGasto = this.fromJsonHelper.extractBigDecimalNamed("serviciosGasto", jsonElement, locale);
        loanAdditionalData.setServiciosGasto(serviciosGasto);

        final BigDecimal serviciosMedicos = this.fromJsonHelper.extractBigDecimalNamed("serviciosMedicos", jsonElement, locale);
        loanAdditionalData.setServiciosMedicos(serviciosMedicos);

        final Integer tarjetas = this.fromJsonHelper.extractIntegerNamed("tarjetas", jsonElement, locale);
        loanAdditionalData.setTarjetas(tarjetas);

        final String tipoVivienda = this.fromJsonHelper.extractStringNamed("tipoVivienda", jsonElement);
        loanAdditionalData.setTipoVivienda(tipoVivienda);

        final BigDecimal totalActivo = this.fromJsonHelper.extractBigDecimalNamed("totalActivo", jsonElement, locale);
        loanAdditionalData.setTotalActivo(totalActivo);

        final BigDecimal totalIngresos = this.fromJsonHelper.extractBigDecimalNamed("totalIngresos", jsonElement, locale);
        loanAdditionalData.setTotalIngresos(totalIngresos);

        final BigDecimal totalIngresosFamiliares = this.fromJsonHelper.extractBigDecimalNamed("totalIngresosFamiliares", jsonElement,
                locale);
        loanAdditionalData.setTotalIngresosFamiliares(totalIngresosFamiliares);

        final BigDecimal totalPasivo = this.fromJsonHelper.extractBigDecimalNamed("totalPasivo", jsonElement, locale);
        loanAdditionalData.setTotalPasivo(totalPasivo);

        final BigDecimal transporteGasto = this.fromJsonHelper.extractBigDecimalNamed("transporteGasto", jsonElement, locale);
        loanAdditionalData.setTransporteGasto(transporteGasto);

        final BigDecimal transporteNegocio = this.fromJsonHelper.extractBigDecimalNamed("transporteNegocio", jsonElement, locale);
        loanAdditionalData.setTransporteNegocio(transporteNegocio);

        final String ubicacionCliente = this.fromJsonHelper.extractStringNamed("ubicacionCliente", jsonElement);
        loanAdditionalData.setUbicacionCliente(ubicacionCliente);

        final String ubicacionNegocio = this.fromJsonHelper.extractStringNamed("ubicacionNegocio", jsonElement);
        loanAdditionalData.setUbicacionNegocio(ubicacionNegocio);

        final BigDecimal utilidadBruta = this.fromJsonHelper.extractBigDecimalNamed("utilidadBruta", jsonElement, locale);
        loanAdditionalData.setUtilidadBruta(utilidadBruta);

        final BigDecimal utilidadNeta = this.fromJsonHelper.extractBigDecimalNamed("utilidadNeta", jsonElement, locale);
        loanAdditionalData.setUtilidadNeta(utilidadNeta);

        final Integer validFiador = this.fromJsonHelper.extractIntegerNamed("validFiador", jsonElement, locale);
        loanAdditionalData.setValidFiador(validFiador);

        final BigDecimal valorGarantia = this.fromJsonHelper.extractBigDecimalNamed("valorGarantia", jsonElement, locale);
        loanAdditionalData.setValorGarantia(valorGarantia);

        final Integer vehiculos = this.fromJsonHelper.extractIntegerNamed("vehiculos", jsonElement, locale);
        loanAdditionalData.setVehiculos(vehiculos);

        final BigDecimal vestimenta = this.fromJsonHelper.extractBigDecimalNamed("vestimenta", jsonElement, locale);
        loanAdditionalData.setVestimenta(vestimenta);

        final String visitoNegocio = this.fromJsonHelper.extractStringNamed("visitoNegocio", jsonElement);
        loanAdditionalData.setVisitoNegocio(visitoNegocio);

        final String externalId = this.fromJsonHelper.extractStringNamed("externalId", jsonElement);
        loanAdditionalData.setExternalId(externalId);

        final String ownerId = this.fromJsonHelper.extractStringNamed("ownerId", jsonElement);
        loanAdditionalData.setOwnerId(ownerId);

        final String caseName = this.fromJsonHelper.extractStringNamed("caseName", jsonElement);
        loanAdditionalData.setCaseName(caseName);

        final LocalDate fechaFin = this.fromJsonHelper.extractLocalDateNamed("fechaFin", jsonElement, dateFormat, locale);
        loanAdditionalData.setFechaFin(fechaFin);

        final BigDecimal ventas = this.fromJsonHelper.extractBigDecimalNamed("ventas", jsonElement, locale);
        loanAdditionalData.setVentas(ventas);

        final String excepcion = this.fromJsonHelper.extractStringNamed("excepcion", jsonElement);
        loanAdditionalData.setExcepcion(excepcion);

        final BigDecimal cuentasPorCobrar = this.fromJsonHelper.extractBigDecimalNamed("cuentasPorCobrar", jsonElement, locale);
        loanAdditionalData.setCuentasPorCobrar(cuentasPorCobrar);

        final String descripcionExcepcion = this.fromJsonHelper.extractStringNamed("descripcionExcepcion", jsonElement);
        loanAdditionalData.setDescripcionExcepcion(descripcionExcepcion);

        final BigDecimal endeudamientoFuturo = this.fromJsonHelper.extractBigDecimalNamed("endeudamientoFuturo", jsonElement, locale);
        loanAdditionalData.setEndeudamientoFuturo(endeudamientoFuturo);

        final BigDecimal hipotecas = this.fromJsonHelper.extractBigDecimalNamed("hipotecas", jsonElement, locale);
        loanAdditionalData.setHipotecas(hipotecas);

        final Integer tipoExcepcion = this.fromJsonHelper.extractIntegerNamed("tipoExcepcion", jsonElement, locale);
        loanAdditionalData.setTipoExcepcion(tipoExcepcion);

        final BigDecimal montoAutorizado = this.fromJsonHelper.extractBigDecimalNamed("montoAutorizado", jsonElement, locale);
        loanAdditionalData.setMontoAutorizado(montoAutorizado);

        final String observaciones = this.fromJsonHelper.extractStringNamed("observaciones", jsonElement);
        loanAdditionalData.setObservaciones(observaciones);

        final BigDecimal capitalDdeTrabajo = this.fromJsonHelper.extractBigDecimalNamed("capitalDdeTrabajo", jsonElement, locale);
        loanAdditionalData.setCapitalDdeTrabajo(capitalDdeTrabajo);

        final BigDecimal montoOtrosIngresos = this.fromJsonHelper.extractBigDecimalNamed("montoOtrosIngresos", jsonElement, locale);
        loanAdditionalData.setMontoOtrosIngresos(montoOtrosIngresos);

        final String origenOtrosIngresos = this.fromJsonHelper.extractStringNamed("origenOtrosIngresos", jsonElement);
        loanAdditionalData.setOrigenOtrosIngresos(origenOtrosIngresos);

        final String otrosIngresos = this.fromJsonHelper.extractStringNamed("otrosIngresos", jsonElement);
        loanAdditionalData.setOtrosIngresos(otrosIngresos);

        final BigDecimal relacionOtrosIngresos = this.fromJsonHelper.extractBigDecimalNamed("relacionOtrosIngresos", jsonElement, locale);
        loanAdditionalData.setRelacionOtrosIngresos(relacionOtrosIngresos);

        final String programa = this.fromJsonHelper.extractStringNamed("Programa", jsonElement);
        loanAdditionalData.setPrograma(programa);

        final String aldeaVivienda = this.fromJsonHelper.extractStringNamed("aldeaVivienda", jsonElement);
        loanAdditionalData.setAldeaVivienda(aldeaVivienda);

        final Integer aniosComunidad = this.fromJsonHelper.extractIntegerNamed("aniosComunidad", jsonElement, locale);
        loanAdditionalData.setAniosComunidad(aniosComunidad);

        final Integer aniosDeActividadNegocio = this.fromJsonHelper.extractIntegerNamed("aniosDeActividadNegocio", jsonElement, locale);
        loanAdditionalData.setAniosDeActividadNegocio(aniosDeActividadNegocio);

        final String apellidoCasadaSolicitante = this.fromJsonHelper.extractStringNamed("apellidoCasadaSolicitante", jsonElement);
        loanAdditionalData.setApellidoCasadaSolicitante(apellidoCasadaSolicitante);

        final String cActividadEconomica = this.fromJsonHelper.extractStringNamed("cActividadEconomica", jsonElement);
        loanAdditionalData.setCActividadEconomica(cActividadEconomica);

        final String cApellidoDeCasada = this.fromJsonHelper.extractStringNamed("cApellidoDeCasada", jsonElement);
        loanAdditionalData.setCApellidoDeCasada(cApellidoDeCasada);

        final String cDepartamento = this.fromJsonHelper.extractStringNamed("cDepartamento", jsonElement);
        loanAdditionalData.setCDepartamento(cDepartamento);

        final String cDepartamentoDpi = this.fromJsonHelper.extractStringNamed("cDepartamentoDpi", jsonElement);
        loanAdditionalData.setCDepartamentoDpi(cDepartamentoDpi);

        final String cDescripcionNegocio = this.fromJsonHelper.extractStringNamed("cDescripcionNegocio", jsonElement);
        loanAdditionalData.setCDescripcionNegocio(cDescripcionNegocio);

        final String descripcionNegocio = this.fromJsonHelper.extractStringNamed("descripcionNegocio", jsonElement);
        loanAdditionalData.setDescripcionNegocio(descripcionNegocio);

        final String cLugarNacimiento = this.fromJsonHelper.extractStringNamed("cLugarNacimiento", jsonElement);
        loanAdditionalData.setCLugarNacimiento(cLugarNacimiento);

        final String cMunicipio = this.fromJsonHelper.extractStringNamed("cMunicipio", jsonElement);
        loanAdditionalData.setCMunicipio(cMunicipio);

        final String cMunicipioDpi = this.fromJsonHelper.extractStringNamed("cMunicipioDpi", jsonElement);
        loanAdditionalData.setCMunicipioDpi(cMunicipioDpi);

        final String cNit = this.fromJsonHelper.extractStringNamed("cNit", jsonElement);
        loanAdditionalData.setCNit(cNit);

        final String cSectorEconomico = this.fromJsonHelper.extractStringNamed("cSectorEconomico", jsonElement);
        loanAdditionalData.setCSectorEconomico(cSectorEconomico);

        final String calleNegocio = this.fromJsonHelper.extractStringNamed("calleNegocio", jsonElement);
        loanAdditionalData.setCalleNegocio(calleNegocio);

        final String calleVivienda = this.fromJsonHelper.extractStringNamed("calleVivienda", jsonElement);
        loanAdditionalData.setCalleVivienda(calleVivienda);

        final String casaNegocio = this.fromJsonHelper.extractStringNamed("casaNegocio", jsonElement);
        loanAdditionalData.setCasaNegocio(casaNegocio);

        final String celularSolicitante = this.fromJsonHelper.extractStringNamed("celularSolicitante", jsonElement);
        loanAdditionalData.setCelularSolicitante(celularSolicitante);

        final String coloniaNegocio = this.fromJsonHelper.extractStringNamed("coloniaNegocio", jsonElement);
        loanAdditionalData.setColoniaNegocio(coloniaNegocio);

        final String coloniaVivienda = this.fromJsonHelper.extractStringNamed("coloniaVivienda", jsonElement);
        loanAdditionalData.setColoniaVivienda(coloniaVivienda);

        final String correoElectronico = this.fromJsonHelper.extractStringNamed("correoElectronico", jsonElement);
        loanAdditionalData.setCorreoElectronico(correoElectronico);

        final Integer cuentas_uso_familia = this.fromJsonHelper.extractIntegerNamed("cuentas_uso_familia", jsonElement, locale);
        loanAdditionalData.setCuentas_uso_familia(cuentas_uso_familia);

        final Integer cuentas_uso_negocio = this.fromJsonHelper.extractIntegerNamed("cuentas_uso_negocio", jsonElement, locale);
        loanAdditionalData.setCuentas_uso_negocio(cuentas_uso_negocio);

        final String datos_moviles = this.fromJsonHelper.extractStringNamed("datos_moviles", jsonElement);
        loanAdditionalData.setDatos_moviles(datos_moviles);

        final String departamento_dpi_solicitante = this.fromJsonHelper.extractStringNamed("departamento_dpi_solicitante", jsonElement);
        loanAdditionalData.setDepartamento_dpi_solicitante(departamento_dpi_solicitante);

        final String departamento_solicitante = this.fromJsonHelper.extractStringNamed("departamento_solicitante", jsonElement);
        loanAdditionalData.setDepartamento_solicitante(departamento_solicitante);

        final String departamento_vivienda = this.fromJsonHelper.extractStringNamed("departamento_vivienda", jsonElement);
        loanAdditionalData.setDepartamento_vivienda(departamento_vivienda);

        final String descripcion_giro_negocio = this.fromJsonHelper.extractStringNamed("descripcion_giro_negocio", jsonElement);
        loanAdditionalData.setDescripcion_giro_negocio(descripcion_giro_negocio);

        final BigDecimal detalle_compras = this.fromJsonHelper.extractBigDecimalNamed("detalle_compras", jsonElement, locale);
        loanAdditionalData.setDetalle_compras(detalle_compras);
        final String detalle_de_inversion = this.fromJsonHelper.extractStringNamed("detalle_de_inversion", jsonElement);
        loanAdditionalData.setDetalle_de_inversion(detalle_de_inversion);

        final BigDecimal detalle_otros_ingresos = this.fromJsonHelper.extractBigDecimalNamed("detalle_otros_ingresos", jsonElement, locale);
        loanAdditionalData.setDetalle_otros_ingresos(detalle_otros_ingresos);

        final String detalle_prendaria = this.fromJsonHelper.extractStringNamed("detalle_prendaria", jsonElement);
        loanAdditionalData.setDetalle_prendaria(detalle_prendaria);

        final BigDecimal detalle_recuperacion_cuentas = this.fromJsonHelper.extractBigDecimalNamed("detalle_recuperacion_cuentas",
                jsonElement, locale);
        loanAdditionalData.setDetalle_recuperacion_cuentas(detalle_recuperacion_cuentas);

        final BigDecimal detalle_ventas = this.fromJsonHelper.extractBigDecimalNamed("detalle_ventas", jsonElement, locale);
        loanAdditionalData.setDetalle_ventas(detalle_ventas);

        final Integer edad_solicitante = this.fromJsonHelper.extractIntegerNamed("edad_solicitante", jsonElement, locale);
        loanAdditionalData.setEdad_solicitante(edad_solicitante);

        final BigDecimal efectivo_uso_familia = this.fromJsonHelper.extractBigDecimalNamed("efectivo_uso_familia", jsonElement, locale);
        loanAdditionalData.setEfectivo_uso_familia(efectivo_uso_familia);

        final BigDecimal efectivo_uso_negocio = this.fromJsonHelper.extractBigDecimalNamed("efectivo_uso_negocio", jsonElement, locale);
        loanAdditionalData.setEfectivo_uso_negocio(efectivo_uso_negocio);

        final String entorno_del_negocio = this.fromJsonHelper.extractStringNamed("entorno_del_negocio", jsonElement);
        loanAdditionalData.setEntorno_del_negocio(entorno_del_negocio);

        final String escolaridad_solicitante = this.fromJsonHelper.extractStringNamed("escolaridad_solicitante", jsonElement);
        loanAdditionalData.setEscolaridad_solicitante(escolaridad_solicitante);

        final String estado_civil_solicitante = this.fromJsonHelper.extractStringNamed("estado_civil_solicitante", jsonElement);
        loanAdditionalData.setEstado_civil_solicitante(estado_civil_solicitante);

        final String etnia_maya = this.fromJsonHelper.extractStringNamed("etnia_maya", jsonElement);
        loanAdditionalData.setEtnia_maya(etnia_maya);

        final String etnia_no_maya = this.fromJsonHelper.extractStringNamed("etnia_no_maya", jsonElement);
        loanAdditionalData.setEtnia_no_maya(etnia_no_maya);

        final String explique_el_tema = this.fromJsonHelper.extractStringNamed("explique_el_tema", jsonElement);
        loanAdditionalData.setExplique_el_tema(explique_el_tema);

        final String facilitador = this.fromJsonHelper.extractStringNamed("facilitador", jsonElement);
        loanAdditionalData.setFacilitador(facilitador);

        final LocalDate fecha_estacionalidad = this.fromJsonHelper.extractLocalDateNamed("fecha_estacionalidad", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFecha_estacionalidad(fecha_estacionalidad);

        final LocalDate fecha_inico_operaciones = this.fromJsonHelper.extractLocalDateNamed("fecha_inico_operaciones", jsonElement,
                dateFormat, locale);
        loanAdditionalData.setFecha_inico_operaciones(fecha_inico_operaciones);

        final LocalDate fecha_integraciones = this.fromJsonHelper.extractLocalDateNamed("fecha_integraciones", jsonElement, dateFormat,
                locale);
        loanAdditionalData.setFecha_integraciones(fecha_integraciones);

        final LocalDate fecha_inventario = this.fromJsonHelper.extractLocalDateNamed("fecha_inventario", jsonElement, dateFormat, locale);
        loanAdditionalData.setFecha_inventario(fecha_inventario);

        final LocalDate fecha_nacimiento_solicitante = this.fromJsonHelper.extractLocalDateNamed("fecha_nacimiento_solicitante",
                jsonElement, dateFormat, locale);
        loanAdditionalData.setFecha_nacimiento_solicitante(fecha_nacimiento_solicitante);

        final LocalDate fecha_visita = this.fromJsonHelper.extractLocalDateNamed("fecha_visita", jsonElement, dateFormat, locale);
        loanAdditionalData.setFecha_visita(fecha_visita);

        final String frecuencia_compras = this.fromJsonHelper.extractStringNamed("frecuencia_compras", jsonElement);
        loanAdditionalData.setFrecuencia_compras(frecuencia_compras);

        final String frecuencia_ventas = this.fromJsonHelper.extractStringNamed("frecuencia_ventas", jsonElement);
        loanAdditionalData.setFrecuencia_ventas(frecuencia_ventas);

        final String genero = this.fromJsonHelper.extractStringNamed("genero", jsonElement);
        loanAdditionalData.setGenero(genero);

        final String grupo_etnico = this.fromJsonHelper.extractStringNamed("grupo_etnico", jsonElement);
        loanAdditionalData.setGrupo_etnico(grupo_etnico);

        final String habla_espaniol = this.fromJsonHelper.extractStringNamed("habla_espaniol", jsonElement);
        loanAdditionalData.setHabla_espaniol(habla_espaniol);

        final String institucion = this.fromJsonHelper.extractStringNamed("institucion", jsonElement);
        loanAdditionalData.setInstitucion(institucion);

        final String inversion_actual = this.fromJsonHelper.extractStringNamed("inversion_actual", jsonElement);
        loanAdditionalData.setInversion_actual(inversion_actual);

        final String local_negocio = this.fromJsonHelper.extractStringNamed("local_negocio", jsonElement);
        loanAdditionalData.setLocal_negocio(local_negocio);

        final String lote_negocio = this.fromJsonHelper.extractStringNamed("lote_negocio", jsonElement);
        loanAdditionalData.setLote_negocio(lote_negocio);

        final String lote_vivienda = this.fromJsonHelper.extractStringNamed("lote_vivienda", jsonElement);
        loanAdditionalData.setLote_vivienda(lote_vivienda);

        final String manzana_negocio = this.fromJsonHelper.extractStringNamed("manzana_negocio", jsonElement);
        loanAdditionalData.setManzana_negocio(manzana_negocio);

        final String manzana_vivienda = this.fromJsonHelper.extractStringNamed("manzana_vivienda", jsonElement);
        loanAdditionalData.setManzana_vivienda(manzana_vivienda);

        final String municipio_dpi_solicitante = this.fromJsonHelper.extractStringNamed("manzana_vivienda", jsonElement);
        loanAdditionalData.setMunicipio_dpi_solicitante(municipio_dpi_solicitante);

        final String municipio_negocio = this.fromJsonHelper.extractStringNamed("municipio_negocio", jsonElement);
        loanAdditionalData.setMunicipio_negocio(municipio_negocio);

        final String municipio_solicitante = this.fromJsonHelper.extractStringNamed("municipio_solicitante", jsonElement);
        loanAdditionalData.setMunicipio_solicitante(municipio_solicitante);

        final String municipio_vivienda = this.fromJsonHelper.extractStringNamed("municipio_vivienda", jsonElement);
        loanAdditionalData.setMunicipio_vivienda(municipio_vivienda);

        final String nacimiento_solicitante = this.fromJsonHelper.extractStringNamed("nacimiento_solicitante", jsonElement);
        loanAdditionalData.setNacimiento_solicitante(nacimiento_solicitante);

        final String nit_negocio = this.fromJsonHelper.extractStringNamed("nit_negocio", jsonElement);
        loanAdditionalData.setNit_negocio(nit_negocio);

        final String no_casa_vivienda = this.fromJsonHelper.extractStringNamed("no_casa_vivienda", jsonElement);
        loanAdditionalData.setNo_casa_vivienda(no_casa_vivienda);

        final String nombre_negocio = this.fromJsonHelper.extractStringNamed("nombre_negocio", jsonElement);
        loanAdditionalData.setNombre_negocio(nombre_negocio);

        final Integer num_contador_vivienda = this.fromJsonHelper.extractIntegerNamed("num_contador_vivienda", jsonElement, locale);
        loanAdditionalData.setNum_contador_vivienda(num_contador_vivienda);

        final Integer numero_fiadores = this.fromJsonHelper.extractIntegerNamed("numero_fiadores", jsonElement, locale);
        loanAdditionalData.setNumero_fiadores(numero_fiadores);

        final String observaciones_visita = this.fromJsonHelper.extractStringNamed("observaciones_visita", jsonElement);
        loanAdditionalData.setObservaciones_visita(observaciones_visita);

        final BigDecimal otros_activos_familia = this.fromJsonHelper.extractBigDecimalNamed("otros_activos_familia", jsonElement, locale);
        loanAdditionalData.setOtros_activos_familia(otros_activos_familia);

        final BigDecimal otros_activos_negocio = this.fromJsonHelper.extractBigDecimalNamed("otros_activos_negocio", jsonElement, locale);
        loanAdditionalData.setOtros_activos_negocio(otros_activos_negocio);

        final String otros_ingresos_de_la_solicitante = this.fromJsonHelper.extractStringNamed("otros_ingresos_de_la_solicitante",
                jsonElement);
        loanAdditionalData.setOtros_ingresos_de_la_solicitante(otros_ingresos_de_la_solicitante);

        final String patente_sociedad = this.fromJsonHelper.extractStringNamed("patente_sociedad", jsonElement);
        loanAdditionalData.setPatente_sociedad(patente_sociedad);

        final String primer_apellido_solicitante = this.fromJsonHelper.extractStringNamed("primer_apellido_solicitante", jsonElement);
        loanAdditionalData.setPrimer_apellido_solicitante(primer_apellido_solicitante);

        final String primer_nombre_solicitante = this.fromJsonHelper.extractStringNamed("primer_nombre_solicitante", jsonElement);
        loanAdditionalData.setPrimer_nombre_solicitante(primer_nombre_solicitante);

        final String profesion_solicitante = this.fromJsonHelper.extractStringNamed("profesion_solicitante", jsonElement);
        loanAdditionalData.setProfesion_solicitante(profesion_solicitante);

        final String punto_de_referencia = this.fromJsonHelper.extractStringNamed("punto_de_referencia", jsonElement);
        loanAdditionalData.setPunto_de_referencia(punto_de_referencia);

        final String razon_social = this.fromJsonHelper.extractStringNamed("razon_social", jsonElement);
        loanAdditionalData.setRazon_social(razon_social);

        final String referencias_vecinos = this.fromJsonHelper.extractStringNamed("referencias_vecinos", jsonElement);
        loanAdditionalData.setReferencias_vecinos(referencias_vecinos);

        final String sector_economico_negocio = this.fromJsonHelper.extractStringNamed("sector_economico_negocio", jsonElement);
        loanAdditionalData.setSector_economico_negocio(sector_economico_negocio);

        final String sector_vivienda = this.fromJsonHelper.extractStringNamed("sector_vivienda", jsonElement);
        loanAdditionalData.setSector_vivienda(sector_vivienda);

        final String segundo_apellido_solicitante = this.fromJsonHelper.extractStringNamed("segundo_apellido_solicitante", jsonElement);
        loanAdditionalData.setSegundo_apellido_solicitante(segundo_apellido_solicitante);

        final String segundo_nombre_solicitante = this.fromJsonHelper.extractStringNamed("segundo_nombre_solicitante", jsonElement);
        loanAdditionalData.setSegundo_nombre_solicitante(segundo_nombre_solicitante);

        final BigDecimal tasa = this.fromJsonHelper.extractBigDecimalNamed("tasa", jsonElement, locale);
        loanAdditionalData.setTasa(tasa);

        final String telefono_negocio = this.fromJsonHelper.extractStringNamed("telefono_negocio", jsonElement);
        loanAdditionalData.setTelefono_negocio(telefono_negocio);

        final String tiene_correo = this.fromJsonHelper.extractStringNamed("tiene_correo", jsonElement);
        loanAdditionalData.setTiene_correo(tiene_correo);

        final String tipo_credito = this.fromJsonHelper.extractStringNamed("tipo_credito", jsonElement);
        loanAdditionalData.setTipo_credito(tipo_credito);

        final String telefono_fijo = this.fromJsonHelper.extractStringNamed("telefono_fijo", jsonElement);
        loanAdditionalData.setTelefono_fijo(telefono_fijo);

        final String tipo_direccion_negocio = this.fromJsonHelper.extractStringNamed("tipo_direccion_negocio", jsonElement);
        loanAdditionalData.setTipo_direccion_negocio(tipo_direccion_negocio);

        final BigDecimal total_costo_ventas = this.fromJsonHelper.extractBigDecimalNamed("total_costo_ventas", jsonElement, locale);
        loanAdditionalData.setTotal_costo_ventas(total_costo_ventas);

        final BigDecimal total_cuentas_por_cobrar = this.fromJsonHelper.extractBigDecimalNamed("total_cuentas_por_cobrar", jsonElement,
                locale);
        loanAdditionalData.setTotal_cuentas_por_cobrar(total_cuentas_por_cobrar);

        final BigDecimal total_cuota_mensual = this.fromJsonHelper.extractBigDecimalNamed("total_cuota_mensual", jsonElement, locale);
        loanAdditionalData.setTotal_cuota_mensual(total_cuota_mensual);

        final BigDecimal total_deuda = this.fromJsonHelper.extractBigDecimalNamed("total_deuda", jsonElement, locale);
        loanAdditionalData.setTotal_deuda(total_deuda);

        final BigDecimal total_efectivo = this.fromJsonHelper.extractBigDecimalNamed("total_efectivo", jsonElement, locale);
        loanAdditionalData.setTotal_efectivo(total_efectivo);

        final BigDecimal total_gastos_negocio = this.fromJsonHelper.extractBigDecimalNamed("total_gastos_negocio", jsonElement, locale);
        loanAdditionalData.setTotal_gastos_negocio(total_gastos_negocio);

        final BigDecimal total_gastos_vivienda = this.fromJsonHelper.extractBigDecimalNamed("total_gastos_vivienda", jsonElement, locale);
        loanAdditionalData.setTotal_gastos_vivienda(total_gastos_vivienda);

        final BigDecimal total_inmueble_familia = this.fromJsonHelper.extractBigDecimalNamed("total_inmueble_familia", jsonElement, locale);
        loanAdditionalData.setTotal_inmueble_familia(total_inmueble_familia);

        final BigDecimal total_inmueble_negocio = this.fromJsonHelper.extractBigDecimalNamed("total_inmueble_negocio", jsonElement, locale);
        loanAdditionalData.setTotal_inmueble_negocio(total_inmueble_negocio);

        final BigDecimal total_inmuebles = this.fromJsonHelper.extractBigDecimalNamed("total_inmuebles", jsonElement, locale);
        loanAdditionalData.setTotal_inmuebles(total_inmuebles);

        final BigDecimal total_inventario = this.fromJsonHelper.extractBigDecimalNamed("total_inventario", jsonElement, locale);
        loanAdditionalData.setTotal_inventario(total_inventario);

        final BigDecimal total_maquinaria = this.fromJsonHelper.extractBigDecimalNamed("total_maquinaria", jsonElement, locale);
        loanAdditionalData.setTotal_maquinaria(total_maquinaria);

        final BigDecimal total_menaje_de_hogar = this.fromJsonHelper.extractBigDecimalNamed("total_menaje_de_hogar", jsonElement, locale);
        loanAdditionalData.setTotal_menaje_de_hogar(total_menaje_de_hogar);

        final BigDecimal total_mobiliario_equipo = this.fromJsonHelper.extractBigDecimalNamed("total_mobiliario_equipo", jsonElement,
                locale);
        loanAdditionalData.setTotal_mobiliario_equipo(total_mobiliario_equipo);

        final BigDecimal total_otros_activos = this.fromJsonHelper.extractBigDecimalNamed("total_otros_activos", jsonElement, locale);
        loanAdditionalData.setTotal_otros_activos(total_otros_activos);

        final BigDecimal total_precio_ventas = this.fromJsonHelper.extractBigDecimalNamed("total_precio_ventas", jsonElement, locale);
        loanAdditionalData.setTotal_precio_ventas(total_precio_ventas);

        final BigDecimal total_recibido = this.fromJsonHelper.extractBigDecimalNamed("total_recibido", jsonElement, locale);
        loanAdditionalData.setTotal_recibido(total_recibido);

        final Integer total_vehiculo_familia = this.fromJsonHelper.extractIntegerNamed("total_vehiculo_familia", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculo_familia(total_vehiculo_familia);

        final Integer total_vehiculo_negocio = this.fromJsonHelper.extractIntegerNamed("total_vehiculo_negocio", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculo_negocio(total_vehiculo_negocio);

        final Integer total_vehiculos = this.fromJsonHelper.extractIntegerNamed("total_vehiculos", jsonElement, locale);
        loanAdditionalData.setTotal_vehiculos(total_vehiculos);

        final String ubicacion_cliente = this.fromJsonHelper.extractStringNamed("ubicacion_cliente", jsonElement);
        loanAdditionalData.setUbicacion_cliente(ubicacion_cliente);

        final String ubicacion_negocio = this.fromJsonHelper.extractStringNamed("ubicacion_negocio", jsonElement);
        loanAdditionalData.setUbicacion_negocio(ubicacion_negocio);

        final String usa_facebook = this.fromJsonHelper.extractStringNamed("usa_facebook", jsonElement);
        loanAdditionalData.setUsa_facebook(usa_facebook);

        final String verificacion_negocio = this.fromJsonHelper.extractStringNamed("verificacion_negocio", jsonElement);
        loanAdditionalData.setVerificacion_negocio(verificacion_negocio);

        final String verificacion_vivienda = this.fromJsonHelper.extractStringNamed("verificacion_vivienda", jsonElement);
        loanAdditionalData.setVerificacion_vivienda(verificacion_vivienda);

        final String whatsapp = this.fromJsonHelper.extractStringNamed("whatsapp", jsonElement);
        loanAdditionalData.setWhatsapp(whatsapp);

        final Integer zona_negocio = this.fromJsonHelper.extractIntegerNamed("zona_negocio", jsonElement, locale);
        loanAdditionalData.setZona_negocio(zona_negocio);

        final Integer zona_vivienda = this.fromJsonHelper.extractIntegerNamed("zona_vivienda", jsonElement, locale);
        loanAdditionalData.setZona_vivienda(zona_vivienda);

        final String detalle_fiadores = this.fromJsonHelper.extractStringNamed("detalle_fiadores", jsonElement);
        loanAdditionalData.setDetalle_fiadores(detalle_fiadores);

        final String dpi_solicitante = this.fromJsonHelper.extractStringNamed("dpi_solicitante", jsonElement);
        loanAdditionalData.setDpi_solicitante(dpi_solicitante);

        final String semarecuperacion_cuentasnal = this.fromJsonHelper.extractStringNamed("recuperacion_cuentas", jsonElement);
        loanAdditionalData.setRecuperacion_cuentas(semarecuperacion_cuentasnal);

        final String tercer_nombre_solicitante = this.fromJsonHelper.extractStringNamed("tercer_nombre_solicitante", jsonElement);
        loanAdditionalData.setTercer_nombre_solicitante(tercer_nombre_solicitante);

        return loanAdditionalData;
    }

    /*
     * Guaranteed to throw an exception no matter what the data integrity issue is.
     */
    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {

        if (realCause.getMessage().contains("loan_account_no_UNIQUE")
                || (realCause.getCause() != null && realCause.getCause().getMessage().contains("loan_account_no_UNIQUE"))) {

            final String accountNo = command.stringValueOfParameterNamed("accountNo");
            throw new PlatformDataIntegrityException("error.msg.loan.duplicate.accountNo",
                    "Loan with accountNo `" + accountNo + "` already exists", "accountNo", accountNo);
        } else if (realCause.getMessage().contains("loan_externalid_UNIQUE")
                || (realCause.getCause() != null && realCause.getCause().getMessage().contains("loan_externalid_UNIQUE"))) {
            final String externalId = command.stringValueOfParameterNamed("externalId");
            throw new PlatformDataIntegrityException("error.msg.loan.duplicate.externalId",
                    "Loan with externalId `" + externalId + "` already exists", "externalId", externalId);
        }

        logAsErrorUnexpectedDataIntegrityException(dve);
        throw new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }

    private void logAsErrorUnexpectedDataIntegrityException(final Exception dve) {
        LOG.error("Error occured.", dve);
    }

    @Transactional
    @Override
    public CommandProcessingResult deleteApplication(final Long loanId) {

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        if (loan.isNotSubmittedAndPendingApproval()) {
            throw new LoanApplicationNotInSubmittedAndPendingApprovalStateCannotBeDeleted(loanId);
        }

        final List<Note> relatedNotes = this.noteRepository.findByLoanId(loan.getId());
        this.noteRepository.deleteAllInBatch(relatedNotes);

        final AccountAssociations accountAssociations = this.accountAssociationsRepository.findByLoanIdAndType(loanId,
                AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
        if (accountAssociations != null) {
            this.accountAssociationsRepository.delete(accountAssociations);
        }

        Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
        for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
            BigDecimal quantity = loanCollateralManagement.getQuantity();
            ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();
            clientCollateralManagement.updateQuantityAfterLoanClosed(quantity);
            loanCollateralManagement.setIsReleased(true);
            loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
        }

        this.loanRepositoryWrapper.delete(loanId);

        return new CommandProcessingResultBuilder() //
                .withEntityId(loanId) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loan.getId()) //
                .build();
    }

    public void validateMultiDisbursementData(final JsonCommand command, LocalDate expectedDisbursementDate) {
        final String json = command.json();
        final JsonElement element = this.fromJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        final BigDecimal principal = this.fromJsonHelper.extractBigDecimalWithLocaleNamed("approvedLoanAmount", element);
        fromApiJsonDeserializer.validateLoanMultiDisbursementDate(element, baseDataValidator, expectedDisbursementDate, principal);
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult approveGLIMLoanAppication(final Long loanId, final JsonCommand command) {

        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        JsonArray approvalFormData = command.arrayOfParameterNamed("approvalFormData");

        JsonObject jsonObject = null;
        JsonCommand childCommand = null;
        Long[] childLoanId = new Long[approvalFormData.size()];
        BigDecimal parentPrincipalAmount = command.bigDecimalValueOfParameterNamed("glimPrincipal");

        for (int i = 0; i < approvalFormData.size(); i++) {

            jsonObject = approvalFormData.get(i).getAsJsonObject();

            childLoanId[i] = jsonObject.get("loanId").getAsLong();
        }

        CommandProcessingResult result = null;
        int count = 0;
        int j = 0;
        for (JsonElement approvals : approvalFormData) {

            childCommand = JsonCommand.fromExistingCommand(command, approvals);

            result = approveApplication(childLoanId[j++], childCommand);

            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setPrincipalAmount(parentPrincipalAmount);
                    parentLoan.setLoanStatus(LoanStatus.APPROVED.getValue());
                    glimRepository.save(parentLoan);
                }

            }

        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult approveApplication(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();
        LocalDate expectedDisbursementDate = null;

        this.loanApplicationTransitionApiJsonValidator.validateApproval(command.json());

        final Loan loan = retrieveLoanBy(loanId);

        final JsonArray disbursementDataArray = command.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);

        expectedDisbursementDate = command.localDateValueOfParameterNamed(LoanApiConstants.disbursementDateParameterName);
        if (expectedDisbursementDate == null) {
            expectedDisbursementDate = loan.getExpectedDisbursedOnLocalDate();
        }
        if (loan.loanProduct().isMultiDisburseLoan()) {
            this.validateMultiDisbursementData(command, expectedDisbursementDate);
        }

        checkClientOrGroupActive(loan);
        Boolean isSkipRepaymentOnFirstMonth = false;
        Integer numberOfDays = 0;
        // validate expected disbursement date against meeting date
        if (loan.isSyncDisbursementWithMeeting() && (loan.isGroupLoan() || loan.isJLGLoan())) {
            final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                    CalendarEntityType.LOANS.getValue());
            Calendar calendar = null;
            if (calendarInstance != null) {
                calendar = calendarInstance.getCalendar();
            }
            // final Calendar calendar = calendarInstance.getCalendar();
            boolean isSkipRepaymentOnFirstMonthEnabled = this.configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
            if (isSkipRepaymentOnFirstMonthEnabled) {
                isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                if (isSkipRepaymentOnFirstMonth) {
                    numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue();
                }
            }
            this.loanScheduleAssembler.validateDisbursementDateWithMeetingDates(expectedDisbursementDate, calendar,
                    isSkipRepaymentOnFirstMonth, numberOfDays);

        }

        final Map<String, Object> changes = loan.loanApplicationApproval(currentUser, command, disbursementDataArray,
                defaultLoanLifecycleStateMachine());

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.APPROVE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        if (!changes.isEmpty()) {

            // If loan approved amount less than loan demanded amount, then need
            // to recompute the schedule
            if (changes.containsKey(LoanApiConstants.approvedLoanAmountParameterName) || changes.containsKey("recalculateLoanSchedule")
                    || changes.containsKey("expectedDisbursementDate")) {
                LocalDate recalculateFrom = null;
                ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
            }

            if (loan.isTopup() && loan.getClientId() != null) {
                final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
                final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
                if (loanToClose == null) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.topup.is.not.active",
                            "Loan to be closed with this topup is not active.");
                }

                final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                if (loan.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)) {
                    throw new GeneralPlatformDomainRuleException(
                            "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                            "Disbursal date of this loan application " + loan.getDisbursementDate()
                                    + " should be after last transaction date of loan to be closed " + lastUserTransactionOnLoanToClose);
                }
                BigDecimal loanOutstanding = this.loanReadPlatformService
                        .retrieveLoanPrePaymentTemplate(LoanTransactionType.REPAYMENT, loanIdToClose, expectedDisbursementDate).getAmount();
                final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
                if (loanOutstanding.compareTo(firstDisbursalAmount) > 0) {
                    throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                            "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                }
                BigDecimal netDisbursalAmount = loan.getApprovedPrincipal().subtract(loanOutstanding);
                loan.adjustNetDisbursalAmount(netDisbursalAmount);
            }

            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                changes.put("note", noteText);
                this.noteRepository.save(note);
            }

            // Post Journal Entry
            final LocalDate approvedOnDate = command.localDateValueOfParameterNamed(LoanApiConstants.approvedOnDateParameterName);
            final BigDecimal principal = command.bigDecimalValueOfParameterNamed(LoanApiConstants.approvedLoanAmountParameterName);
            BitaCoraMaster bitaCoraMaster = this.lumaAccountingProcessorForLoan
                    .createJournalEntry(LumaBitacoraTransactionTypeEnum.LOANS_APPROVAL, loan, approvedOnDate, principal);
            if (bitaCoraMaster != null) {
                this.bitaCoraMasterRepository.save(bitaCoraMaster);
            }

            businessEventNotifierService.notifyPostBusinessEvent(new LoanApprovedBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult undoGLIMLoanApplicationApproval(final Long loanId, final JsonCommand command) {

        // GroupLoanIndividualMonitoringAccount
        // glimAccount=glimRepository.findOne(loanId);
        final Long parentLoanId = loanId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(loanId);

        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = undoApplicationApproval(loan.getId(), command);

            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are approved, mark the parent loan as
                // approved
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue());
                    glimRepository.save(parentLoan);
                }

            }

        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult undoApplicationApproval(final Long loanId, final JsonCommand command) {

        AppUser currentUser = getAppUserIfPresent();

        this.fromApiJsonDeserializer.validateForUndo(command.json());

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        final Map<String, Object> changes = loan.undoApproval(defaultLoanLifecycleStateMachine());
        if (!changes.isEmpty()) {

            // If loan approved amount is not same as loan amount demanded, then
            // during undo, restore the demand amount to principal amount.

            if (changes.containsKey(LoanApiConstants.approvedLoanAmountParameterName)
                    || changes.containsKey(LoanApiConstants.disbursementPrincipalParameterName)) {
                LocalDate recalculateFrom = null;
                ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                loan.regenerateRepaymentSchedule(scheduleGeneratorDTO);
            }

            loan.adjustNetDisbursalAmount(loan.getProposedPrincipal());

            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
            businessEventNotifierService.notifyPostBusinessEvent(new LoanUndoApprovalBusinessEvent(loan));
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult rejectGLIMApplicationApproval(final Long glimId, final JsonCommand command) {

        // GroupLoanIndividualMonitoringAccount
        // glimAccount=glimRepository.findOne(loanId);
        final Long parentLoanId = glimId;
        GroupLoanIndividualMonitoringAccount parentLoan = glimRepository.findById(parentLoanId).orElseThrow();
        List<Loan> childLoans = this.loanRepository.findByGlimId(glimId);

        CommandProcessingResult result = null;
        int count = 0;
        for (Loan loan : childLoans) {
            result = rejectApplication(loan.getId(), command);

            if (result.getLoanId() != null) {
                count++;
                // if all the child loans are Rejected, mark the parent loan as
                // rejected
                if (count == parentLoan.getChildAccountsCount()) {
                    parentLoan.setLoanStatus(LoanStatus.REJECTED.getValue());
                    glimRepository.save(parentLoan);
                }

            }

        }

        return result;
    }

    @Transactional
    @Override
    public CommandProcessingResult rejectApplication(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanApplicationTransitionApiJsonValidator.validateRejection(command.json());

        final Loan loan = retrieveLoanBy(loanId);

        checkClientOrGroupActive(loan);

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.REJECTED.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        final Map<String, Object> changes = loan.loanApplicationRejection(currentUser, command, defaultLoanLifecycleStateMachine());
        if (!changes.isEmpty()) {
            this.loanRepositoryWrapper.saveAndFlush(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
        }
        businessEventNotifierService.notifyPostBusinessEvent(new LoanRejectedBusinessEvent(loan));
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    @Transactional
    @Override
    public CommandProcessingResult applicantWithdrawsFromApplication(final Long loanId, final JsonCommand command) {

        final AppUser currentUser = getAppUserIfPresent();

        this.loanApplicationTransitionApiJsonValidator.validateApplicantWithdrawal(command.json());

        final Loan loan = retrieveLoanBy(loanId);
        checkClientOrGroupActive(loan);

        entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                StatusEnum.WITHDRAWN.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

        final Map<String, Object> changes = loan.loanApplicationWithdrawnByApplicant(currentUser, command,
                defaultLoanLifecycleStateMachine());

        // Release attached collaterals
        if (AccountType.fromInt(loan.getLoanType()).isIndividualAccount()) {
            Set<LoanCollateralManagement> loanCollateralManagements = loan.getLoanCollateralManagements();
            for (LoanCollateralManagement loanCollateralManagement : loanCollateralManagements) {
                ClientCollateralManagement clientCollateralManagement = loanCollateralManagement.getClientCollateralManagement();
                clientCollateralManagement
                        .updateQuantity(clientCollateralManagement.getQuantity().add(loanCollateralManagement.getQuantity()));
                loanCollateralManagement.setClientCollateralManagement(clientCollateralManagement);
                loanCollateralManagement.setIsReleased(true);
            }
            loan.updateLoanCollateral(loanCollateralManagements);
        }

        if (!changes.isEmpty()) {
            this.loanRepositoryWrapper.saveAndFlush(loan);

            final String noteText = command.stringValueOfParameterNamed("note");
            if (StringUtils.isNotBlank(noteText)) {
                final Note note = Note.loanNote(loan, noteText);
                this.noteRepository.save(note);
            }
        }

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(loan.getId()) //
                .withOfficeId(loan.getOfficeId()) //
                .withClientId(loan.getClientId()) //
                .withGroupId(loan.getGroupId()) //
                .withLoanId(loanId) //
                .with(changes) //
                .build();
    }

    private Loan retrieveLoanBy(final Long loanId) {
        final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
        loan.setHelpers(defaultLoanLifecycleStateMachine(), this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
        return loan;
    }

    private void validateSubmittedOnDate(final Loan loan) {
        final LocalDate startDate = loan.loanProduct().getStartDate();
        final LocalDate closeDate = loan.loanProduct().getCloseDate();
        final LocalDate expectedFirstRepaymentOnDate = loan.getExpectedFirstRepaymentOnDate();
        final LocalDate submittedOnDate = loan.getSubmittedOnDate();

        String defaultUserMessage = "";
        if (startDate != null && submittedOnDate.isBefore(startDate)) {
            defaultUserMessage = "submittedOnDate cannot be before the loan product startDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.before.the.loan.product.start.date", defaultUserMessage,
                    submittedOnDate.toString(), startDate.toString());
        }

        if (closeDate != null && submittedOnDate.isAfter(closeDate)) {
            defaultUserMessage = "submittedOnDate cannot be after the loan product closeDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.after.the.loan.product.close.date", defaultUserMessage,
                    submittedOnDate.toString(), closeDate.toString());
        }

        if (expectedFirstRepaymentOnDate != null && submittedOnDate.isAfter(expectedFirstRepaymentOnDate)) {
            defaultUserMessage = "submittedOnDate cannot be after the loans  expectedFirstRepaymentOnDate.";
            throw new LoanApplicationDateException("submitted.on.date.cannot.be.after.the.loan.expected.first.repayment.date",
                    defaultUserMessage, submittedOnDate.toString(), expectedFirstRepaymentOnDate.toString());
        }
    }

    private void checkClientOrGroupActive(final Loan loan) {
        final Client client = loan.client();
        if (client != null) {
            if (client.isNotActive()) {
                throw new ClientNotActiveException(client.getId());
            }
        }
        final Group group = loan.group();
        if (group != null) {
            if (group.isNotActive()) {
                throw new GroupNotActiveException(group.getId());
            }
        }
    }

    private void saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final JpaSystemException | DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.application");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) {
                throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                        dataValidationErrors, e);
            }
        }
    }

    private AppUser getAppUserIfPresent() {
        AppUser user = null;
        if (this.context != null) {
            user = this.context.getAuthenticatedUserIfPresent();
        }
        return user;
    }

    private void officeSpecificLoanProductValidation(final Long productId, final Long officeId) {
        final GlobalConfigurationProperty restrictToUserOfficeProperty = this.globalConfigurationRepository
                .findOneByNameWithNotFoundDetection(FineractEntityAccessConstants.GLOBAL_CONFIG_FOR_OFFICE_SPECIFIC_PRODUCTS);
        if (restrictToUserOfficeProperty.isEnabled()) {
            FineractEntityRelation fineractEntityRelation = fineractEntityRelationRepository
                    .findOneByCodeName(FineractEntityAccessType.OFFICE_ACCESS_TO_LOAN_PRODUCTS.toStr());
            FineractEntityToEntityMapping officeToLoanProductMappingList = this.repository.findListByProductId(fineractEntityRelation,
                    productId, officeId);
            if (officeToLoanProductMappingList == null) {
                throw new NotOfficeSpecificProductException(productId, officeId);
            }

        }
    }

    @Transactional
    @Override
    public CommandProcessingResult disburseLoanByCheques(JsonCommand command) {
        final AppUser currentUser = this.context.authenticatedUser();
        List<DisburseByChequesCommand> disburseByChequesCommands = this.disburseByChequesCommandFromApiJsonDeserializer
                .commandFromApiJson(command.json());
        for (final DisburseByChequesCommand disburseByChequesCommand : disburseByChequesCommands) {
            final Loan loanAccount = this.loanRepositoryWrapper.findOneWithNotFoundDetection(disburseByChequesCommand.getLoanId());
            final Cheque cheque = this.chequeBatchRepositoryWrapper
                    .findOneChequeWithNotFoundDetection(disburseByChequesCommand.getChequeId());
            loanAccount.setCheque(cheque);
            loanAccount.setLoanStatus(LoanStatus.DISBURSE_AUTHORIZATION_PENDING.getValue());
            cheque.setStatus(BankChequeStatus.PENDING_ISSUANCE.getValue());
            cheque.setDescription(disburseByChequesCommand.getDescription());
            cheque.setGuaranteeAmount(disburseByChequesCommand.getActualGuaranteeAmount());
            cheque.setRequiredGuaranteeAmount(disburseByChequesCommand.getRequiredGuaranteeAmount());
            cheque.setDepositGuaranteeNo(disburseByChequesCommand.getDepositGuaranteeNo());
            final LocalDateTime localDateTime = DateUtils.getLocalDateTimeOfSystem();
            LocalDate localDate = DateUtils.getBusinessLocalDate();
            final Long currentUserId = currentUser.getId();
            cheque.stampAudit(currentUserId, localDateTime);
            loanAccount.setDisbursedByChequeDate(localDate);
            loanAccount.setDisbursedByChequeAppUser(currentUser);

            this.loanRepositoryWrapper.saveAndFlush(loanAccount);
            this.chequeBatchRepositoryWrapper.updateCheque(cheque);

            // TODO: FBR-47 Handle deposit to guarantee savings account here
            BigDecimal depositAmount = cheque.getGuaranteeAmount().subtract(cheque.getRequiredGuaranteeAmount());
            if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) < 0) {
                CommandProcessingResult depositCommandResult = this.savingsAccountWritePlatformService
                        .depositAndHoldToClientGuaranteeAccount(depositAmount.abs(), cheque.getRequiredGuaranteeAmount(),
                                loanAccount.getClientId(), loanAccount.getId(), localDate);
            }
        }
        return new CommandProcessingResultBuilder().withCommandId(command.commandId()).build();
    }

    @Override
    public CommandProcessingResult editLoanFund(final JsonCommand command) {
        final Long loanId = command.getLoanId();
        final Loan existingLoanApplication = retrieveLoanBy(loanId);
        if (existingLoanApplication.isClosed()) {
            throw new LoanApplicationNotInClosedStateCannotBeModified(loanId);
        }
        final String fundIdParameterName = "fundId";
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
        if (command.parameterExists(fundIdParameterName)) {
            final Long fundId = command.longValueOfParameterNamed(fundIdParameterName);
            baseDataValidator.reset().parameter(fundIdParameterName).value(fundId).ignoreIfNull().integerGreaterThanZero();
            final Fund fund = this.loanAssembler.findFundByIdIfProvided(fundId);
            existingLoanApplication.updateFund(fund);
        }
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
        this.loanRepositoryWrapper.saveAndFlush(existingLoanApplication);
        return new CommandProcessingResultBuilder().withEntityId(loanId).withOfficeId(existingLoanApplication.getOfficeId())
                .withClientId(existingLoanApplication.getClientId()).withGroupId(existingLoanApplication.getGroupId())
                .withLoanId(existingLoanApplication.getId()).build();
    }
}
