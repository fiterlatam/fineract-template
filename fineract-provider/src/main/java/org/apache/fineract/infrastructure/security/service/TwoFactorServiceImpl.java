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
package org.apache.fineract.infrastructure.security.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.EmailDetail;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.PlatformEmailService;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.OTPDeliveryMethod;
import org.apache.fineract.infrastructure.security.data.OTPRequest;
import org.apache.fineract.infrastructure.security.domain.BasicPasswordEncodablePlatformUser;
import org.apache.fineract.infrastructure.security.domain.OTPRequestRepository;
import org.apache.fineract.infrastructure.security.domain.PlatformUser;
import org.apache.fineract.infrastructure.security.domain.TFAccessToken;
import org.apache.fineract.infrastructure.security.domain.TFAccessTokenRepository;
import org.apache.fineract.infrastructure.security.exception.AccessTokenInvalidIException;
import org.apache.fineract.infrastructure.security.exception.OTPDeliveryMethodInvalidException;
import org.apache.fineract.infrastructure.security.exception.OTPTokenInvalidException;
import org.apache.fineract.infrastructure.sms.domain.SmsMessage;
import org.apache.fineract.infrastructure.sms.domain.SmsMessageRepository;
import org.apache.fineract.infrastructure.sms.scheduler.SmsMessageScheduledJobService;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserDevices;
import org.apache.fineract.useradministration.domain.AppUserDevicesRepository;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.exception.DevicesLimitException;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty("fineract.security.2fa.enabled")
public class TwoFactorServiceImpl implements TwoFactorService {

    private final AccessTokenGenerationService accessTokenGenerationService;
    private final PlatformEmailService emailService;
    private final SmsMessageScheduledJobService smsMessageScheduledJobService;
    private final OTPRequestRepository otpRequestRepository;
    private final TFAccessTokenRepository tfAccessTokenRepository;
    private final AppUserDevicesRepository appUserDevicesRepository;
    private final AppUserRepository appUserRepository;
    private final SmsMessageRepository smsMessageRepository;
    private final FromJsonHelper fromApiJsonHelper;
    private final TwoFactorConfigurationService configurationService;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TwoFactorServiceImpl(AccessTokenGenerationService accessTokenGenerationService, PlatformEmailService emailService,
            SmsMessageScheduledJobService smsMessageScheduledJobService, OTPRequestRepository otpRequestRepository,
            TFAccessTokenRepository tfAccessTokenRepository, SmsMessageRepository smsMessageRepository,
            TwoFactorConfigurationService configurationService,final AppUserDevicesRepository appUserDevicesRepository,
            AppUserRepository appUserRepository,FromJsonHelper fromApiJsonHelper,
            JdbcTemplate jdbcTemplate) {
        this.accessTokenGenerationService = accessTokenGenerationService;
        this.emailService = emailService;
        this.smsMessageScheduledJobService = smsMessageScheduledJobService;
        this.otpRequestRepository = otpRequestRepository;
        this.tfAccessTokenRepository = tfAccessTokenRepository;
        this.smsMessageRepository = smsMessageRepository;
        this.configurationService = configurationService;
        this.appUserDevicesRepository = appUserDevicesRepository;
        this.appUserRepository = appUserRepository;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<OTPDeliveryMethod> getDeliveryMethodsForUser(final AppUser user) {
        List<OTPDeliveryMethod> deliveryMethods = new ArrayList<>();

        OTPDeliveryMethod smsMethod = getSMSDeliveryMethodForUser(user);
        if (smsMethod != null) {
            deliveryMethods.add(smsMethod);
        }
        OTPDeliveryMethod emailDelivery = getEmailDeliveryMethodForUser(user);
        if (emailDelivery != null) {
            deliveryMethods.add(emailDelivery);
        }

        return deliveryMethods;
    }

    @Override
    public OTPRequest createNewOTPToken(final AppUser user, final String deliveryMethodName, final boolean extendedAccessToken) {
        if (TwoFactorConstants.SMS_DELIVERY_METHOD_NAME.equalsIgnoreCase(deliveryMethodName)) {
            OTPDeliveryMethod smsDelivery = getSMSDeliveryMethodForUser(user);
            if (smsDelivery == null) {
                throw new OTPDeliveryMethodInvalidException();
            }
            final OTPRequest request = generateNewToken(smsDelivery, extendedAccessToken);
            final String smsText = configurationService.getFormattedSmsTextFor(user, request);
            SmsMessage smsMessage = SmsMessage.pendingSms(null, null, null, user.getStaff(), smsText, user.getStaff().mobileNo(), null,
                    false);
            this.smsMessageRepository.save(smsMessage);
            smsMessageScheduledJobService.sendTriggeredMessage(Collections.singleton(smsMessage), configurationService.getSMSProviderId());
            otpRequestRepository.addOTPRequest(user, request);
            return request;
        } else if (TwoFactorConstants.EMAIL_DELIVERY_METHOD_NAME.equalsIgnoreCase(deliveryMethodName)) {
            OTPDeliveryMethod emailDelivery = getEmailDeliveryMethodForUser(user);
            if (emailDelivery == null) {
                throw new OTPDeliveryMethodInvalidException();
            }
            final OTPRequest request = generateNewToken(emailDelivery, extendedAccessToken);
            final String emailSubject = configurationService.getFormattedEmailSubjectFor(user, request);
            final String emailBody = configurationService.getFormattedEmailBodyFor(user, request);
            final EmailDetail emailData = new EmailDetail(emailSubject, emailBody, user.getEmail(),
                    user.getFirstname() + " " + user.getLastname());
            emailService.sendDefinedEmail(emailData);
            otpRequestRepository.addOTPRequest(user, request);
            return request;
        }

        throw new OTPDeliveryMethodInvalidException();
    }

    @Override
    @CachePut(value = "userTFAccessToken", key = "T(org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil)"
            + ".getTenant().getTenantIdentifier().concat(#user.username).concat(#result.token + 'tok')")
    public TFAccessToken createAccessTokenFromOTP(final AppUser user, final String otpToken) {

        OTPRequest otpRequest = otpRequestRepository.getOTPRequestForUser(user);
        if (otpRequest == null || !otpRequest.isValid() || !otpRequest.getToken().equalsIgnoreCase(otpToken)) {
            throw new OTPTokenInvalidException();
        }

        otpRequestRepository.deleteOTPRequestForUser(user);

        String token = accessTokenGenerationService.generateRandomToken();
        int liveTime;
        if (otpRequest.getMetadata().isExtendedAccessToken()) {
            liveTime = configurationService.getAccessTokenExtendedLiveTime();
        } else {
            liveTime = configurationService.getAccessTokenLiveTime();
        }
        TFAccessToken accessToken = TFAccessToken.create(token, user, liveTime);
        tfAccessTokenRepository.save(accessToken);
        return accessToken;
    }

    @Override
    public void validateTwoFactorAccessToken(AppUser user, String token) {
        TFAccessToken accessToken = fetchAccessTokenForUser(user, token);

        if (accessToken == null || !accessToken.isValid()) {
            throw new AccessTokenInvalidIException();
        }
    }

    @Override
    @CacheEvict(value = "userTFAccessToken", key = "T(org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil)"
            + ".getTenant().getTenantIdentifier().concat(#user.username).concat(#result.token + 'tok')")
    public TFAccessToken invalidateAccessToken(final AppUser user, final JsonCommand command) {

        final String token = command.stringValueOfParameterNamed("token");
        final TFAccessToken accessToken = fetchAccessTokenForUser(user, token);

        if (accessToken == null || !accessToken.isValid()) {
            throw new AccessTokenInvalidIException();
        }

        accessToken.setEnabled(false);
        tfAccessTokenRepository.save(accessToken);

        return accessToken;
    }

    @Override
    public void updateRegisteredDevices(AppUser user, String fingerprint) {
        if (StringUtils.isBlank(fingerprint)) {
            return;
        }

        Collection<AppUserDevices> existingDevices = appUserDevicesRepository.findByUser(user);
        boolean deviceExists = existingDevices.stream().anyMatch(device ->
                fingerprint.equals(device.getDeviceId()));
        if (existingDevices.size() >= 5 && !deviceExists) {
            throw new DevicesLimitException(user.getUsername(), configurationService.getMaximumUserDevices());
        }
        if (!deviceExists) {
            AppUserDevices newDevice = AppUserDevices.createNew(user, fingerprint);
            appUserDevicesRepository.save(newDevice);
        }
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

    @Override
    public AppUser completePasswordReset(String username, String otp, Boolean logoutDevices, PlatformPasswordEncoder platformPasswordEncoder) {
        AppUser appUser = this.appUserRepository.findAppUserByName(username);
        if (appUser == null) {
            throw new UsernameNotFoundException(username);
        }
        OTPRequest otpRequest = otpRequestRepository.getOTPRequestForUser(appUser);
        if (otpRequest == null || !otpRequest.isValid() || !otpRequest.getToken().equalsIgnoreCase(otp)) {
            throw new OTPTokenInvalidException();
        }
        otpRequestRepository.deleteOTPRequestForUser(appUser);
        String newPassword = this.accessTokenGenerationService.generateRandomToken().substring(0, 8);

        final PlatformUser dummyPlatformUser = new BasicPasswordEncodablePlatformUser(appUser.getId(), "", newPassword);
        String encodedPass = platformPasswordEncoder.encode(dummyPlatformUser);

        this.jdbcTemplate.update("UPDATE m_appuser SET reset_password = ?, password=?, nonlocked=true WHERE id = ?",
                true, encodedPass, appUser.getId());

        final String emailSubject = "Password Reset Successful";
        final String emailBody = "Your password has been reset. Your new password is: " + newPassword;
        final EmailDetail emailData = new EmailDetail(emailSubject, emailBody,
                appUser.getEmail(), appUser.getFirstname() + " " + appUser.getLastname());
        emailService.sendDefinedEmail(emailData);

        if (logoutDevices){
            this.appUserDevicesRepository.findByUser(appUser).forEach(device -> {
                this.appUserDevicesRepository.delete(device);
            });
        }
        List<TFAccessToken> tfAccessTokens = this.tfAccessTokenRepository.findByUser(appUser);
        tfAccessTokens.forEach(token-> {
            this.tfAccessTokenRepository.delete(token);
        });

        return appUser;
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
            final JsonCommand command = JsonCommand.from(jsonCommand, asJsonObject, this.fromApiJsonHelper, null, userId, null,
                    null, null, null, null, null, null, null, null, null);
            final String passwordEncodedValue = command.passwordValueOfParameterNamed("password",platformPasswordEncoder ,
                    appUser.getId());
            appUser.updatePassword(passwordEncodedValue);
            appUser.updateResetPassword(false);
            this.appUserRepository.save(appUser);
        }
        return appUser;
    }

    @Override
    @Cacheable(value = "userTFAccessToken", key = "T(org.apache.fineract.infrastructure.core.service.ThreadLocalContextUtil)"
            + ".getTenant().getTenantIdentifier().concat(#user.username).concat(#token + 'tok')")
    public TFAccessToken fetchAccessTokenForUser(final AppUser user, final String token) {
        return tfAccessTokenRepository.findByUserAndToken(user, token);
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

        int accessTokenExtendedLiveTime = (this.configurationService.getAccessTokenExtendedLiveTime()/86400);
        return new OTPDeliveryMethod(TwoFactorConstants.SMS_DELIVERY_METHOD_NAME, mobileNo, Integer.toString(accessTokenExtendedLiveTime));
    }

    private OTPDeliveryMethod getEmailDeliveryMethodForUser(final AppUser user) {
        if (!configurationService.isEmailEnabled()) {
            return null;
        }
        int accessTokenExtendedLiveTime = (this.configurationService.getAccessTokenExtendedLiveTime()/86400);
        return new OTPDeliveryMethod(TwoFactorConstants.EMAIL_DELIVERY_METHOD_NAME, user.getEmail(), Integer.toString(accessTokenExtendedLiveTime));
    }

    private OTPRequest generateNewToken(final OTPDeliveryMethod deliveryMethod, final boolean extendedAccessToken) {
        int tokenLiveTime = configurationService.getOTPTokenLiveTime();
        int otpLength = configurationService.getOTPTokenLength();
        String token = new RandomOTPGenerator(otpLength).generate();
        return OTPRequest.create(token, tokenLiveTime, extendedAccessToken, deliveryMethod);
    }
}
