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

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.persistence.PersistenceException;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.PlatformEmailSendException;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.OTPDeliveryMethod;
import org.apache.fineract.infrastructure.security.domain.BasicPasswordEncodablePlatformUser;
import org.apache.fineract.infrastructure.security.domain.OTPRequest;
import org.apache.fineract.infrastructure.security.domain.OTPRequestRepository;
import org.apache.fineract.infrastructure.security.domain.PlatformUser;
import org.apache.fineract.infrastructure.security.domain.TFAccessToken;
import org.apache.fineract.infrastructure.security.domain.TFAccessTokenRepository;
import org.apache.fineract.infrastructure.security.exception.OTPDeliveryMethodInvalidException;
import org.apache.fineract.infrastructure.security.exception.OTPTokenInvalidException;
import org.apache.fineract.infrastructure.security.service.AccessTokenGenerationService;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.infrastructure.security.service.RandomOTPGenerator;
import org.apache.fineract.infrastructure.security.service.TwoFactorConfigurationService;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.office.domain.OfficeRepositoryWrapper;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.staff.domain.StaffRepositoryWrapper;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.useradministration.api.AppUserApiConstant;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserDevices;
import org.apache.fineract.useradministration.domain.AppUserDevicesRepository;
import org.apache.fineract.useradministration.domain.AppUserPreviousPassword;
import org.apache.fineract.useradministration.domain.AppUserPreviousPasswordRepository;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.domain.RoleRepository;
import org.apache.fineract.useradministration.domain.UserDomainService;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

@Service
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
    private final JdbcTemplate jdbcTemplate;
    private final FromJsonHelper fromApiJsonHelper;
    private final OTPRequestRepository otpRequestRepository;
    private final AccessTokenGenerationService accessTokenGenerationService;
    private final PlatformEmailService emailService;
    private final AppUserDevicesRepository appUserDevicesRepository;
    private final TFAccessTokenRepository tfAccessTokenRepository;
    private final TwoFactorConfigurationService configurationService;
    private final SmsMessageRepository smsMessageRepository;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;

    @Override
    @Transactional
    @Caching(evict = { @CacheEvict(value = "users", allEntries = true), @CacheEvict(value = "usersByUsername", allEntries = true) })
    public CommandProcessingResult createUser(final JsonCommand command) {
        try {
            this.context.authenticatedUser();

            this.fromApiJsonDeserializer.validateForCreate(command.json());

            final String officeIdParamName = "officeId";
            final Long officeId = command.longValueOfParameterNamed(officeIdParamName);

            final Office userOffice = this.officeRepositoryWrapper.findOneWithNotFoundDetection(officeId);

            final String[] roles = command.arrayValueOfParameterNamed("roles");
            final Set<Role> allRoles = assembleSetOfRoles(roles);

            final String staffIdParamName = "staffId";
            final Long staffId = command.longValueOfParameterNamed(staffIdParamName);

            Staff linkedStaff;
            if (staffId != null && staffId != 0) {
                linkedStaff = this.staffRepositoryWrapper.findByOfficeWithNotFoundDetection(staffId, userOffice.getId());
            } else {
                linkedStaff = null;
            }

            Collection<Client> clients;
            if (command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)
                    && command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER)
                    && command.hasParameter(AppUserConstants.CLIENTS)) {
                JsonArray clientsArray = command.arrayOfParameterNamed(AppUserConstants.CLIENTS);
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

            return new CommandProcessingResultBuilder() //
                    .withCommandId(command.commandId()) //
                    .withEntityId(appUser.getId()) //
                    .withOfficeId(userOffice.getId()) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException dve) {
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
            this.context.authenticatedUser(new CommandWrapperBuilder().updateUser(null).build());

            this.fromApiJsonDeserializer.validateForUpdate(command.json());

            final AppUser userToUpdate = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

            final AppUserPreviousPassword currentPasswordToSaveAsPreview = getCurrentPasswordToSaveAsPreview(userToUpdate, command);

            Collection<Client> clients = null;
            boolean isSelfServiceUser = userToUpdate.isSelfServiceUser();
            if (command.hasParameter(AppUserConstants.IS_SELF_SERVICE_USER)) {
                isSelfServiceUser = command.booleanPrimitiveValueOfParameterNamed(AppUserConstants.IS_SELF_SERVICE_USER);
            }

            if (isSelfServiceUser && command.hasParameter(AppUserConstants.CLIENTS)) {
                JsonArray clientsArray = command.arrayOfParameterNamed(AppUserConstants.CLIENTS);
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

            return new CommandProcessingResultBuilder() //
                    .withEntityId(userId) //
                    .withOfficeId(userToUpdate.getOffice().getId()) //
                    .with(changes) //
                    .build();
        } catch (final DataIntegrityViolationException dve) {
            throw handleDataIntegrityIssues(command, dve.getMostSpecificCause(), dve);
        } catch (final JpaSystemException | PersistenceException | AuthenticationServiceException dve) {
            log.error("updateUser: JpaSystemException | PersistenceException | AuthenticationServiceException", dve);
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
    public CommandProcessingResult deleteUser(final Long userId) {
        final AppUser user = this.appUserRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (user.isDeleted()) {
            throw new UserNotFoundException(userId);
        }

        user.delete();
        this.appUserRepository.save(user);

        return new CommandProcessingResultBuilder().withEntityId(userId).withOfficeId(user.getOffice().getId()).build();
    }

    @Override
    public void logUserAuthenticationDetails(AppUser appUser, HttpServletRequest servletRequest, String action, String result,
            String username, boolean processed) {
        if (appUser == null) {
            appUser = this.appUserRepository.findAppUserByName(username);
        }
        String clientIp = "Unknown IP Address";
        if (servletRequest != null) {
            clientIp = servletRequest.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = servletRequest.getRemoteAddr();
            } else {
                // The X-Forwarded-For header can contain multiple IP addresses, in case of proxies.
                // The first IP in the list is the original client.
                clientIp = Iterables.get(Splitter.on(',').split(clientIp), 0);
                ;
            }
        }
        Long userId = appUser.getId();
        this.jdbcTemplate.update("insert into m_portfolio_command_source "
                + "(action_name,entity_name,office_id,api_get_url,command_as_json,resource_id,maker_id,made_on_date,processing_result_enum) "
                + "values(?, ?,?,?,?,?,?,current_timestamp ,?) ", action, "AUTHENTICATION", appUser.getOffice().getId(), "/authenticate",
                "{ipAddress:\"" + clientIp + "\", result: \"" + result + "\"}", userId, userId, processed);
    }

    @Override
    public AppUser selfResetUserPassword(Long userId, String requestData, PlatformPasswordEncoder platformPasswordEncoder) {
        Optional<AppUser> appUserOptional = this.appUserRepository.findById(userId);
        if (appUserOptional.isEmpty()) throw new UserNotFoundException(userId);
        AppUser appUser = appUserOptional.get();
        JsonElement jsonElement = this.fromApiJsonHelper.parse(requestData);
        JsonObject asJsonObject = jsonElement.getAsJsonObject();
        if (jsonElement.isJsonObject()) {
            String password = this.fromApiJsonHelper.extractStringNamed("password", jsonElement);
            String passwordRepeat = this.fromApiJsonHelper.extractStringNamed("repeatPassword", jsonElement);
            final String jsonCommand = asJsonObject.toString();
            final JsonCommand command = JsonCommand.from(jsonCommand, asJsonObject, this.fromApiJsonHelper, null, userId, null, null, null,
                    null, null, null, null, null, null, null);
            final String passwordEncodedValue = command.passwordValueOfParameterNamed("password", platformPasswordEncoder, appUser.getId());
            appUser.updatePassword(passwordEncodedValue);
            appUser.updateResetPassword(false);
            this.appUserRepository.save(appUser);
        }
        return appUser;
    }

    @Override
    public AppUser completePasswordReset(String username, String otp, Boolean logoutDevices,
            PlatformPasswordEncoder platformPasswordEncoder) {
        AppUser appUser = this.appUserRepository.findAppUserByName(username);
        if (appUser == null) {
            throw new UsernameNotFoundException(username);
        }
        OTPRequest otpRequest = otpRequestRepository.findByUser(appUser);
        if (otpRequest == null || !otpRequest.isValid() || !otpRequest.matchesToken(otp)) {
            throw new OTPTokenInvalidException();
        }
        otpRequestRepository.deleteAllByUser(appUser);
        String newPassword = this.accessTokenGenerationService.generateRandomToken().substring(0, 8);

        final PlatformUser dummyPlatformUser = new BasicPasswordEncodablePlatformUser(appUser.getId(), "", newPassword);
        String encodedPass = platformPasswordEncoder.encode(dummyPlatformUser);

        this.jdbcTemplate.update(
                "UPDATE m_appuser SET reset_password = ?, password=?, nonlocked=true, incorrect_access_count=? WHERE id = ?", true,
                encodedPass, 0, appUser.getId());

        final String emailSubject = "Restablecimiento de contraseña exitosa";
        final String emailBody = "Tu contraseña ha sido restablecida. Tu nueva contraseña es: " + newPassword;
        final EmailDetail emailData = new EmailDetail(emailSubject, emailBody, appUser.getEmail(),
                appUser.getFirstname() + " " + appUser.getLastname());
        emailService.sendDefinedEmail(emailData);

        // always log out of all devices.
        Collection<AppUserDevices> devices = this.appUserDevicesRepository.findByUser(appUser);
        for (AppUserDevices device : devices) {
            this.appUserDevicesRepository.delete(device);
        }

        List<TFAccessToken> tfAccessTokens = this.tfAccessTokenRepository.findByUser(appUser);
        tfAccessTokens.forEach(token -> {
            this.tfAccessTokenRepository.delete(token);
        });

        return appUser;
    }

    @Override
    public AppUser requestPasswordReset(String username) {
        AppUser appUserByName = this.appUserRepository.findAppUserByName(username);
        if (appUserByName == null) {
            throw new UsernameNotFoundException(username);
        }
        OTPRequest request = createNewOTPToken(appUserByName, TwoFactorConstants.EMAIL_DELIVERY_METHOD_NAME, false);

        return appUserByName;
    }

    public OTPRequest createNewOTPToken(final AppUser user, final String deliveryMethodName, final boolean extendedAccessToken) {
        if (TwoFactorConstants.SMS_DELIVERY_METHOD_NAME.equalsIgnoreCase(deliveryMethodName)) {
            OTPDeliveryMethod smsDelivery = getSMSDeliveryMethodForUser(user);
            if (smsDelivery == null) {
                throw new OTPDeliveryMethodInvalidException();
            }
            final OTPRequest request = generateNewToken(user, smsDelivery, extendedAccessToken);
            final String smsText = configurationService.getFormattedSmsTextFor(user, request);
            SmsMessage smsMessage = SmsMessage.pendingSms(null, null, null, user.getStaff(), smsText, user.getStaff().mobileNo(), null,
                    false);
            this.smsMessageRepository.save(smsMessage);
            smsMessageScheduledJobService.sendTriggeredMessage(Collections.singleton(smsMessage), configurationService.getSMSProviderId());
            return persistOTPRequest(user, request);
        } else if (TwoFactorConstants.EMAIL_DELIVERY_METHOD_NAME.equalsIgnoreCase(deliveryMethodName)) {
            OTPDeliveryMethod emailDelivery = getEmailDeliveryMethodForUser(user);
            if (emailDelivery == null) {
                throw new OTPDeliveryMethodInvalidException();
            }
            final OTPRequest request = generateNewToken(user, emailDelivery, extendedAccessToken);
            final String emailSubject = configurationService.getFormattedEmailSubjectFor(user, request);
            final String emailBody = configurationService.getFormattedEmailBodyFor(user, request);
            final EmailDetail emailData = new EmailDetail(emailSubject, emailBody, user.getEmail(),
                    user.getFirstname() + " " + user.getLastname());
            emailService.sendDefinedEmail(emailData);
            return persistOTPRequest(user, request);
        }

        throw new OTPDeliveryMethodInvalidException();
    }

    private OTPRequest persistOTPRequest(final AppUser user, final OTPRequest request) {
        // Enforce one active OTP per user: update the existing row in place when present, otherwise insert a new one.
        // The twofactor_otp_request table also has a UNIQUE(appuser_id) constraint as a safety net.
        final OTPRequest existing = otpRequestRepository.findByUser(user);
        if (existing != null) {
            existing.refreshFrom(request);
            return otpRequestRepository.save(existing);
        }
        return otpRequestRepository.save(request);
    }

    private OTPDeliveryMethod getSMSDeliveryMethodForUser(final AppUser user) {
        if (!configurationService.isSMSEnabled()) {
            return null;
        }

        if (configurationService.getSMSProviderId() == null) {
            return null;
        }

        if (user.getStaff() == null) {
            return null;
        }
        String mobileNo = user.getStaff().mobileNo();
        if (StringUtils.isBlank(mobileNo)) {
            return null;
        }

        int accessTokenExtendedLiveTime = (this.configurationService.getAccessTokenExtendedLiveTime() / 86400);
        return new OTPDeliveryMethod(TwoFactorConstants.SMS_DELIVERY_METHOD_NAME, mobileNo, Integer.toString(accessTokenExtendedLiveTime));
    }

    private OTPDeliveryMethod getEmailDeliveryMethodForUser(final AppUser user) {
        if (!configurationService.isEmailEnabled()) {
            return null;
        }
        int accessTokenExtendedLiveTime = (this.configurationService.getAccessTokenExtendedLiveTime() / 86400);
        return new OTPDeliveryMethod(TwoFactorConstants.EMAIL_DELIVERY_METHOD_NAME, user.getEmail(),
                Integer.toString(accessTokenExtendedLiveTime));
    }

    private OTPRequest generateNewToken(final AppUser user, final OTPDeliveryMethod deliveryMethod, final boolean extendedAccessToken) {
        int tokenLiveTime = configurationService.getOTPTokenLiveTime();
        int otpLength = configurationService.getOTPTokenLength();
        String token = new RandomOTPGenerator(otpLength).generate();
        return OTPRequest.create(user, token, tokenLiveTime, extendedAccessToken, deliveryMethod);
    }

    /*
     * Return an exception to throw, no matter what the data integrity issue is.
     */
    private PlatformDataIntegrityException handleDataIntegrityIssues(final JsonCommand command, final Throwable realCause,
            final Exception dve) {
        // TODO: this needs to be fixed. The error condition should be independent from the underlying message and
        // naming
        // of the constraint
        if (realCause.getMessage().contains("username_org")) {
            final String username = command.stringValueOfParameterNamed("username");
            final StringBuilder defaultMessageBuilder = new StringBuilder("User with username ").append(username)
                    .append(" already exists.");
            return new PlatformDataIntegrityException("error.msg.user.duplicate.username", defaultMessageBuilder.toString(), "username",
                    username);
        }

        // TODO: this needs to be fixed. The error condition should be independent from the underlying message and
        // naming
        // of the constraint
        if (realCause.getMessage().contains("unique_self_client")) {
            return new PlatformDataIntegrityException("error.msg.user.self.service.user.already.exist",
                    "Self Service User Id is already created. Go to Admin->Users to edit or delete the self-service user.");
        }

        log.error("handleDataIntegrityIssues: Neither duplicate username nor existing user; unknown error occured", dve);
        return new PlatformDataIntegrityException("error.msg.unknown.data.integrity.issue", "Unknown data integrity issue with resource.");
    }
}
