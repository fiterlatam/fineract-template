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
package org.apache.fineract.useradministration.service;

import static org.apache.fineract.useradministration.service.AppUserConstants.CLIENTS;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import jakarta.persistence.PersistenceException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.exception.ErrorHandler;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.PlatformEmailSendException;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.organisation.staff.service.StaffReadPlatformService;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.useradministration.api.AppUserApiConstant;
import org.apache.fineract.useradministration.data.AppUserData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserPreviousPassword;
import org.apache.fineract.useradministration.domain.AppUserPreviousPasswordRepository;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
import org.apache.fineract.useradministration.exception.InvalidDeactivationDateRangeException;
import org.apache.fineract.useradministration.exception.PasswordPreviouslyUsedException;
import org.apache.fineract.useradministration.exception.RoleNotFoundException;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

@Slf4j
@RequiredArgsConstructor
public class AppUserWritePlatformServiceJpaRepositoryImpl implements AppUserWritePlatformService {

    private final PlatformSecurityContext context;
    private final UserDomainService userDomainService;
    private final PlatformPasswordEncoder platformPasswordEncoder;
    private final AppUserRepository appUserRepository;
    private final OfficeRepositoryWrapper officeRepositoryWrapper;
    private final RoleRepository roleRepository;
    private final UserDataValidator fromApiJsonDeserializer;
    private final AppUserPreviousPasswordRepository appUserPreviewPasswordRepository;
    private final StaffRepositoryWrapper staffRepositoryWrapper;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final RoleReadPlatformService roleReadPlatformService;
    private final StaffReadPlatformService staffReadPlatformService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult createUser(final JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final String officeIdParamName = "officeId";
            final Long officeId = command.longValueOfParameterNamed(officeIdParamName);

            final Office userOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            final String[] roles = command.arrayValueOfParameterNamed("roles");
            final Set<Role> allRoles = assembleSetOfRoles(roles);

            final String staffIdParamName = "staffId";
            final Long staffId = command.longValueOfParameterNamed(staffIdParamName);

            Staff linkedStaff;
            if (staffId != null) {
                linkedStaff = this.staffRepositoryWrapper.findByOfficeWithNotFoundDetection(staffId, userOffice.getId());
            } else {
                linkedStaff = null;
            }

            Collection<Client> clients;
            if (command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)
                    && command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER)
                    && command.hasParameter(CLIENTS)) {
                JsonArray clientsArray = command.arrayOfParameterNamed(CLIENTS);
                Collection<Long> clientIds = new HashSet<>();
                for (JsonElement clientElement : clientsArray) {
                    clientIds.add(clientElement.getAsLong());
                }
                clients = this.clientRepositoryWrapper.findAll(clientIds);
            } else {
                clients = null;
            }

            AppUser appUser = AppUser.fromJson(userOffice, linkedStaff, allRoles, clients, command);

            final Boolean sendPasswordToEmail = command.booleanObjectValueOfParameterNamed("sendPasswordToEmail");
            this.userDomainService.create(appUser, sendPasswordToEmail);

            final Set<Role> userRoles = appUser.getRoles();
            final String roleIDs = userRoles.stream().map(r -> String.valueOf(r.getId())).collect(Collectors.joining(", "));
            final String roleNames = userRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            final String registroPosterior = objectMapper.writeValueAsString(appUser);
            final String usuarioNombre = appUser.getUsername();
            final Long usuarioId = appUser.getId();
            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(appUser.getId()) //
                    .withOfficeId(userOffice.getId()) //
                    .withRegistroPosterior(registroPosterior).withUsuarioNombre(usuarioNombre).withUsuarioId(usuarioId).withRolId(roleIDs)
                    .withRolNombre(roleNames).withUsuarioCreacionNombre(usuarioCreacionNombre).build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException dve) {
            log.error("createUser: JpaSystemException | PersistenceException | AuthenticationServiceException", dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            throw handleDataIntegrityIssues(command, throwable, dve);
        } catch (final PlatformEmailSendException e) {
            log.error("createUser: PlatformEmailSendException", e);

            final String email = command.stringValueOfParameterNamed("email");
            final ApiParameterError error = ApiParameterError.parameterError("error.msg.user.email.invalid",
                    "Sending email failed; is parameter email is invalid? More details available in server log: " + e.getMessage(), "email",
                    email);

            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    List.of(error), e);
        }
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult updateUser(final Long userId, final JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            this.fromApiJsonDeserializer.validateForUpdate(command.json(), this.context.authenticatedUser());
            final AppUser userToUpdate = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final String registroAnteriorJson = objectMapper.writeValueAsString(userToUpdate);
            final AppUserPreviousPassword currentPasswordToSaveAsPreview = getCurrentPasswordToSaveAsPreview(userToUpdate, command);

            Collection<Client> clients = null;
            boolean isSelfServiceUser = userToUpdate.isSelfServiceUser();
            if (command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)) {
                isSelfServiceUser = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER);
            }

            if (isSelfServiceUser && command.hasParameter(CLIENTS)) {
                JsonArray clientsArray = command.arrayOfParameterNamed(CLIENTS);
                Collection<Long> clientIds = new HashSet<>();
                for (JsonElement clientElement : clientsArray) {
                    clientIds.add(clientElement.getAsLong());
                }
                clients = this.clientRepositoryWrapper.findAll(clientIds);
            }

            final Map<String, Object> changes = userToUpdate.update(command, this.platformPasswordEncoder, clients);

            if (changes.containsKey("officeId")) {
                final Long officeId = (Long) changes.get("officeId");
                final Office office = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);
                userToUpdate.changeOffice(office);
            }

            if (changes.containsKey("staffId")) {
                final Long staffId = (Long) changes.get("staffId");
                Staff linkedStaff = null;
                if (staffId != null) {
                    linkedStaff = this.staffRepositoryWrapper.findByOfficeWithNotFoundDetection(staffId, userToUpdate.getOffice().getId());
                }
                userToUpdate.changeStaff(linkedStaff);
            }

            if (changes.containsKey("roles")) {
                final String[] roleIds = (String[]) changes.get("roles");
                final Set<Role> allRoles = assembleSetOfRoles(roleIds);

                userToUpdate.updateRoles(allRoles);
            }

            if (!changes.isEmpty()) {
                this.appUserRepository.saveAndFlush(userToUpdate);

                if (currentPasswordToSaveAsPreview != null) {
                    this.appUserPreviewPasswordRepository.save(currentPasswordToSaveAsPreview);
                }

            }
            final Set<Role> userRoles = userToUpdate.getRoles();
            final String roleIDs = userRoles.stream().map(r -> String.valueOf(r.getId())).collect(Collectors.joining(", "));
            final String roleNames = userRoles.stream().map(Role::getName).collect(Collectors.joining(", "));
            final String registroPosterior = objectMapper.writeValueAsString(userToUpdate);
            final String usuarioNombre = userToUpdate.getUsername();
            final Long usuarioId = userToUpdate.getId();
            return new CommandProcessingResultBuilder() //
                    .withEntityId(userId) //
                    .withOfficeId(userToUpdate.getOffice().getId()) //
                    .with(changes) //
                    .withRegistroAnterior(registroAnteriorJson).withRegistroPosterior(registroPosterior).withUsuarioNombre(usuarioNombre)
                    .withUsuarioCreacionNombre(usuarioCreacionNombre).withUsuarioId(usuarioId).withRolId(roleIDs).withRolNombre(roleNames)
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException dve) {
            log.error("updateUser: JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException ",
                    dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            throw handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    /**
     * Encode the new submitted password and retrieve the last N used passwords to check if the current submitted
     * password matches with one of them.
     */
    private AppUserPreviousPassword getCurrentPasswordToSaveAsPreview(final AppUser user, final JsonCommand command) {
        final String passWordEncodedValue = user.getEncodedPassword(command, this.platformPasswordEncoder);

        AppUserPreviousPassword currentPasswordToSaveAsPreview = null;

        if (passWordEncodedValue != null) {
            PageRequest pageRequest = PageRequest.of(0, AppUserApiConstant.numberOfPreviousPasswords, Sort.Direction.DESC, "removalDate");
            final List<AppUserPreviousPassword> nLastUsedPasswords = this.appUserPreviewPasswordRepository.findByUserId(user.getId(),
                    pageRequest);
            for (AppUserPreviousPassword aPreviewPassword : nLastUsedPasswords) {
                if (aPreviewPassword.getPassword().equals(passWordEncodedValue)) {
                    throw new PasswordPreviouslyUsedException();
                }
            }

            currentPasswordToSaveAsPreview = new AppUserPreviousPassword(user);
        }

        return currentPasswordToSaveAsPreview;
    }

    private Set<Role> assembleSetOfRoles(final String[] rolesArray) {
        final Set<Role> allRoles = new HashSet<>();

        if (!ObjectUtils.isEmpty(rolesArray)) {
            for (final String roleId : rolesArray) {
                final Long id = Long.valueOf(roleId);
                final Role role = this.roleRepository.findById(id).orElseThrow(() -> new RoleNotFoundException(id));
                allRoles.add(role);
            }
        }

        return allRoles;
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult deleteUser(final Long userId, JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final String usuarioNombre = user.getUsername();
            final Long usuarioId = user.getId();
            final String registroAnteriorJson = objectMapper.writeValueAsString(user);
            if (user.isDeleted()) {
                throw new UserNotFoundException(userId);
            }
            user.delete();
            this.appUserRepository.save(user);
            return new CommandProcessingResultBuilder().withEntityId(userId).withOfficeId(user.getOffice().getId())
                    .withRegistroAnterior(registroAnteriorJson).withUsuarioNombre(usuarioNombre).withUsuarioId(usuarioId)
                    .withUsuarioCreacionNombre(usuarioCreacionNombre).build();
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException dve) {
            log.error("updateUser: JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException ",
                    dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            throw handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult deactivateUser(Long userId, JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final String usuarioNombre = user.getUsername();
            final Long usuarioId = user.getId();
            if (user.isInactive()) {
                throw new UserNotFoundException(userId);
            }
            final String registroAnteriorJson = objectMapper.writeValueAsString(user);
            final String deactivationType = command.stringValueOfParameterNamed("deactivationType");
            if ("TEMPORARY".equals(deactivationType)) {
                final LocalDate deactivatedFromDate = command.localDateValueOfParameterNamed("deactivatedFromDate");
                final LocalDate deactivatedToDate = command.localDateValueOfParameterNamed("deactivatedToDate");
                final boolean isValidDateRange = (DateUtils.isEqual(deactivatedToDate, deactivatedFromDate)
                        || DateUtils.isAfter(deactivatedToDate, deactivatedFromDate))
                        && (!DateUtils.isBeforeBusinessDate(deactivatedFromDate) && !DateUtils.isBeforeBusinessDate(deactivatedToDate));
                if (!isValidDateRange) {
                    throw new InvalidDeactivationDateRangeException(deactivatedFromDate, deactivatedToDate);
                }
                user.deactivateTemporarily(deactivatedFromDate, deactivatedToDate);
            } else {
                user.deactivatePermanently();
            }
            this.appUserRepository.save(user);
            final String registroPosterior = objectMapper.writeValueAsString(user);
            return new CommandProcessingResultBuilder().withEntityId(userId).withOfficeId(user.getOffice().getId())
                    .withRegistroAnterior(registroAnteriorJson).withRegistroPosterior(registroPosterior).withUsuarioNombre(usuarioNombre)
                    .withUsuarioId(usuarioId).withUsuarioCreacionNombre(usuarioCreacionNombre).build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException dve) {
            log.error(
                    "reactivateUser: JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException ",
                    dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            throw handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult reactivateUser(Long userId, JsonCommand command) {
        try {
            final AppUser authenticatedUser = this.context.authenticatedUser();
            final String usuarioCreacionNombre = authenticatedUser.getUsername();
            final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
            final String usuarioNombre = user.getUsername();
            final Long usuarioId = user.getId();
            if (user.isActive()) {
                throw new UserNotFoundException(userId);
            }
            final String registroAnteriorJson = objectMapper.writeValueAsString(user);
            user.activate();
            this.appUserRepository.save(user);
            final String registroPosterior = objectMapper.writeValueAsString(user);
            return new CommandProcessingResultBuilder().withEntityId(userId).withOfficeId(user.getOffice().getId())
                    .withRegistroAnterior(registroAnteriorJson).withRegistroPosterior(registroPosterior).withUsuarioNombre(usuarioNombre)
                    .withUsuarioId(usuarioId).withUsuarioCreacionNombre(usuarioCreacionNombre).build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException dve) {
            log.error(
                    "reactivateUser: JpaSystemException | PersistenceException | AuthenticationServiceException | JsonProcessingException ",
                    dve);
            Throwable throwable = ExceptionUtils.getRootCause(dve.getCause());
            throw handleDataIntegrityIssues(command, throwable, dve);
        }
    }

    @Override
    public void reactivateAppUsers() {
        final AppUser currentUser = this.context.authenticatedUser();
        final String hierarchy = currentUser.getOffice().getHierarchy();
        final String hierarchySearchString = hierarchy + "%";
        final LocalDate localDate = DateUtils.getBusinessLocalDate();
        final AppUserReadPlatformServiceImpl.AppUserMapper mapper = new AppUserReadPlatformServiceImpl.AppUserMapper(
                this.roleReadPlatformService, this.staffReadPlatformService);
        final String sql = "SELECT " + mapper.schema() + " AND u.status_enum = 400 AND u.deactivated_to_date <= ? ORDER BY u.username";
        List<AppUserData> appUsers = this.jdbcTemplate.query(sql, mapper, new Object[] { hierarchySearchString, localDate });
        if (!CollectionUtils.isEmpty(appUsers)) {
            for (final AppUserData appUserData : appUsers) {
                final Long userId = appUserData.getId();
                final AppUser appUserEntity = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
                appUserEntity.activate();
                this.appUserRepository.saveAndFlush(appUserEntity);
            }
        }
    }

    /*
     * Return an exception to throw, no matter what the data integrity issue is.
     */
    private RuntimeException handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause, final Exception dve) {
        // TODO: this needs to be fixed. The error condition should be independent from the underlying message and
        // naming of the constraint
        if (realCause.getMessage().contains("username_org")) {
            final String username = command.stringValueOfParameterNamed("username");
            final String defaultMessage = "User with username " + username + " already exists.";
            return new PlatformDataIntegrityException("error.msg.user.duplicate.username", defaultMessage, "username", username);
        }
        // TODO: this needs to be fixed. The error condition should be independent from the underlying message and
        // naming of the constraint
        if (realCause.getMessage().contains("unique_self_client")) {
            return new PlatformDataIntegrityException("error.msg.user.self.service.user.already.exist",
                    "Self Service User Id is already created. Go to Admin->Users to edit or delete the self-service user.");
        }

        log.error("handleDataIntegrityIssues: Neither duplicate username nor existing user; unknown error occured", dve);
        return ErrorHandler.getMappable(dve, "error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}
