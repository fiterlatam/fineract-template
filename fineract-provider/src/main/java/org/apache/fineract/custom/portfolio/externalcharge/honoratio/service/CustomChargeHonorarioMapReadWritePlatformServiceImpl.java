/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.service;

import jakarta.persistence.PersistenceException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.ClientAdditionalInformationRepository;
import org.apache.fineract.custom.infrastructure.dataqueries.domain.IndividualAdditionalInformationRepository;
import org.apache.fineract.custom.portfolio.ally.domain.ClientAllyRepository;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.data.CustomChargeHonorarioMapData;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMap;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.domain.CustomChargeHonorarioMapRepository;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.exception.CustomChargeHonorarioMapNotFoundException;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.mapper.CustomChargeHonorarioMapMapper;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.external.HonorarioApiService;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.external.HonorarioRetrofitConfig;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.external.data.ExternalCustomChargeHonorarioMapData;
import org.apache.fineract.custom.portfolio.externalcharge.honoratio.validator.CustomChargeHonorarioMapDataValidator;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.database.DatabaseSpecificSQLGenerator;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.client.domain.ClientRepository;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargeData;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepository;
import org.apache.fineract.portfolio.loanaccount.exception.InstallmentNotFoundException;
import org.apache.fineract.portfolio.loanaccount.exception.LoanNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanChargeReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;

@Slf4j
@Service
public class CustomChargeHonorarioMapReadWritePlatformServiceImpl implements CustomChargeHonorarioMapReadWritePlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final DatabaseSpecificSQLGenerator sqlGenerator;
    private final CustomChargeHonorarioMapDataValidator validatorClass;
    private final PlatformSecurityContext context;

    private final ClientRepository clientRepository;
    private final LoanRepository loanRepository;
    private final ClientAllyRepository clientAllyRepository;

    private final IndividualAdditionalInformationRepository individualAdditionalInformationRepository;
    private final ClientAdditionalInformationRepository camposClienteEmpresaRepository;

    private final LoanChargeReadPlatformService loanChargeReadPlatformService;

    @Autowired
    public CustomChargeHonorarioMapReadWritePlatformServiceImpl(final JdbcTemplate jdbcTemplate,
            final DatabaseSpecificSQLGenerator sqlGenerator, final CustomChargeHonorarioMapDataValidator validatorClass,
            final PlatformSecurityContext context, final ClientRepository clientRepository, final LoanRepository loanRepository,
            final ClientAllyRepository clientAllyRepository,
            final IndividualAdditionalInformationRepository individualAdditionalInformationRepository,
            final ClientAdditionalInformationRepository camposClienteEmpresaRepository,
            final LoanChargeReadPlatformService loanChargeReadPlatformService) {
        this.jdbcTemplate = jdbcTemplate;
        this.sqlGenerator = sqlGenerator;
        this.validatorClass = validatorClass;
        this.context = context;
        this.clientRepository = clientRepository;
        this.loanRepository = loanRepository;
        this.clientAllyRepository = clientAllyRepository;
        this.individualAdditionalInformationRepository = individualAdditionalInformationRepository;
        this.camposClienteEmpresaRepository = camposClienteEmpresaRepository;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
    }

    @Autowired
    private CustomChargeHonorarioMapRepository repository;

    @Autowired
    private Retrofit retrofit;

    @Autowired
    private HonorarioRetrofitConfig honorarioRetrofitConfig;

    @Override
    public List<CustomChargeHonorarioMapData> findAllActive() {
        return CustomChargeHonorarioMapMapper.toDTO(repository.findAll());
    }

    @Override
    public CustomChargeHonorarioMapData findById(Long id) {
        Optional<CustomChargeHonorarioMap> entity = repository.findById(id);
        if (!entity.isPresent()) {
            throw new CustomChargeHonorarioMapNotFoundException();
        }
        return CustomChargeHonorarioMapMapper.toDTO(entity.get());
    }

    @Transactional
    @Override
    public CommandProcessingResult create(final JsonCommand command) {
        return new CommandProcessingResultBuilder().build();
    }

    @Transactional
    private CommandProcessingResult create(final ExternalCustomChargeHonorarioMapData dto) {

        try {
            this.context.authenticatedUser();

            final CustomChargeHonorarioMap entity = this.validatorClass.validateForCreate(dto);

            // Validate Loan
            Long loanId = 0L;
            Optional<Loan> loan = loanRepository.findById(entity.getLoanId());
            if (loan.isPresent()) {
                Loan curr = loan.get();
                loanId = curr.getId();

                entity.setLoanId(curr.getId());
                entity.setLoanInstallmentNr(entity.getLoanInstallmentNr());

                // Validate Installment
                if (curr.getLoanRepaymentScheduleInstallmentsSize().compareTo(entity.getLoanInstallmentNr()) < 0) {
                    throw new InstallmentNotFoundException(entity.getLoanInstallmentNr().longValue());
                }

            } else {
                throw new LoanNotFoundException(loanId);
            }

            // Check if the entity already exists
            Optional<CustomChargeHonorarioMap> entityOpt = repository.findByNitLoanIdLoanInstallmentNr(entity.getNit(), entity.getLoanId(),
                    entity.getLoanInstallmentNr());

            // If yes, update disabled, updated and changed fields AND insert a new one with the new data
            if (entityOpt.isPresent()) {
                CustomChargeHonorarioMap current = entityOpt.get();

                // Just save in case of any changes, in order to the db´s table not become too big
                if (current.getFeeTotalAmount().compareTo(entity.getFeeTotalAmount()) != 0
                        || current.getFeeBaseAmount().compareTo(entity.getFeeBaseAmount()) != 0
                        || current.getFeeVatAmount().compareTo(entity.getFeeVatAmount()) != 0) {

                    current.setUpdatedBy(context.authenticatedUser().getId());
                    current.setUpdatedAt(DateUtils.getLocalDateTimeOfTenant());

                    current.setDisabledBy(context.authenticatedUser().getId());
                    current.setDisabledAt(DateUtils.getLocalDateTimeOfTenant());

                    repository.save(current);

                } else {
                    entity.setId(current.getId());
                    entity.setUpdatedBy(context.authenticatedUser().getId());
                    entity.setUpdatedAt(DateUtils.getLocalDateTimeOfTenant());
                }
            }

            repository.saveAndFlush(entity);

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    @Transactional
    @Override
    public CommandProcessingResult delete(final Long id) {
        this.context.authenticatedUser();

        Optional<CustomChargeHonorarioMap> entity = repository.findById(id);
        if (entity.isPresent()) {
            repository.delete(entity.get());
            repository.flush();
        } else {
            throw new CustomChargeHonorarioMapNotFoundException();
        }

        return new CommandProcessingResultBuilder().withEntityId(id).build();
    }

    @Transactional
    @Override
    public CommandProcessingResult update(final JsonCommand command, Long id) {

        try {
            this.context.authenticatedUser();

            final CustomChargeHonorarioMap entity = this.validatorClass.validateForUpdate(command.json(), id);
            Optional<CustomChargeHonorarioMap> dbEntity = repository.findById(id);

            if (dbEntity.isPresent()) {
                entity.setId(id);
                repository.save(entity);
            } else {
                throw new CustomChargeHonorarioMapNotFoundException();
            }

            return new CommandProcessingResultBuilder().withEntityId(entity.getId()).build();
        } catch (final JpaSystemException | DataIntegrityViolationException dve) {
            handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
            return CommandProcessingResult.empty();
        } catch (final PersistenceException dve) {
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            handleDataIntegrityIssues(command, throwable, dve);
            return CommandProcessingResult.empty();
        }
    }

    private void handleDataIntegrityIssues(final Throwable realCause, final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.customchargehonorariomap.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    private void handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        throw new PlatformDataIntegrityException("error.msg.customchargehonorariomap.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }

    @Override
    public List<Throwable> executeJobLoanCustomChargeHonorarioUpdate(List<Throwable> exceptions, Long loanId) {

        Optional<Loan> loanEntityOpt = loanRepository.findById(loanId);

        if (loanEntityOpt.isPresent()) {
            Loan loanEntity = loanEntityOpt.get();

            Collection<LoanChargeData> chargesList = loanChargeReadPlatformService.retrieveLoanChargesForAccrual(loanId);

            // TODO - Filter loans that HAVE custom charge honorario instead all charges
            // Later, after understanding their charges, we have to go back here and find by Calculation Type instead
            Long rowsFound = chargesList.stream().count();
            if (0l == rowsFound) {
                return exceptions;
            }

            persistNewRates(exceptions, loanId);

        } else {
            return exceptions;
        }

        return exceptions;
    }

    private void persistNewRates(List<Throwable> exceptions, Long loanId) {
        List<ExternalCustomChargeHonorarioMapData> honorarioToPersistList = new ArrayList<>();

        try {
            honorarioToPersistList = fetchDataFromExternalProvider(loanId);

            // For each item, call the service to add/update the percentages
            for (ExternalCustomChargeHonorarioMapData currentMap : honorarioToPersistList) {

                try {
                    create(currentMap);

                } catch (Exception e) {
                    log.error("Apply Charges due for overdue loans failed for account {}", currentMap.getLoanId(), e);
                    exceptions.add(e);
                }
            }
        } catch (Exception e) {
            log.error("Apply Charges due for overdue loans failed for account {}", loanId, e);
            exceptions.add(e);
        }
    }

    private List<ExternalCustomChargeHonorarioMapData> fetchDataFromExternalProvider(Long loanId) throws IOException {
        honorarioRetrofitConfig.apiRequestDetailsRenewal(retrofit);

        HonorarioApiService service = honorarioRetrofitConfig.getRetrofitInstance().create(HonorarioApiService.class);

        Call<List<ExternalCustomChargeHonorarioMapData>> call = service.getData(loanId);

        List<ExternalCustomChargeHonorarioMapData> ret = new ArrayList<>();
        Response<List<ExternalCustomChargeHonorarioMapData>> response = call.execute();

        if (response.isSuccessful()) {
            ret = response.body();
        } else {
            throw new IOException("Request Status " + response.code() + " for " + retrofit.baseUrl()
                    + " Check if endpoint is correct and if the service is up.");
        }

        return ret;
    }
}
