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
package org.apache.fineract.infrastructure.security.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Locale;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.data.OTPDeliveryMethod;
import org.apache.fineract.infrastructure.security.data.OTPMetadata;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "twofactor_otp_request", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "appuser_id" }, name = "uk_twofactor_otp_request_user") })
public class OTPRequest extends AbstractPersistableCustom {

    @ManyToOne
    @JoinColumn(name = "appuser_id", nullable = false)
    private AppUser user;

    /** SHA-256 hex digest of the (uppercased) OTP. The plain OTP is never persisted. */
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    /**
     * The plain OTP, only populated in-memory between generation and dispatch (SMS/email templating). Marked
     * {@link Transient} so it never reaches the database; once the entity is reloaded from storage this field is
     * {@code null}.
     */
    @Transient
    private String plainToken;

    @Column(name = "request_time", nullable = false)
    private LocalDateTime requestTime;

    @Column(name = "token_live_time_in_sec", nullable = false)
    private int tokenLiveTimeInSec;

    @Column(name = "extended_access_token", nullable = false)
    private boolean extendedAccessToken;

    @Column(name = "delivery_method", nullable = false, length = 32)
    private String deliveryMethod;

    @Column(name = "delivery_target", nullable = false, length = 200)
    private String deliveryTarget;

    public OTPRequest() {}

    private OTPRequest(AppUser user, String plainToken, LocalDateTime requestTime, int tokenLiveTimeInSec, boolean extendedAccessToken,
            String deliveryMethod, String deliveryTarget) {
        this.user = user;
        this.plainToken = plainToken;
        this.tokenHash = hashToken(plainToken);
        this.requestTime = requestTime;
        this.tokenLiveTimeInSec = tokenLiveTimeInSec;
        this.extendedAccessToken = extendedAccessToken;
        this.deliveryMethod = deliveryMethod;
        this.deliveryTarget = deliveryTarget;
    }

    public static OTPRequest create(AppUser user, String plainToken, int tokenLiveTimeInSec, boolean extendedAccessToken,
            OTPDeliveryMethod deliveryMethod) {
        return new OTPRequest(user, plainToken, DateUtils.getLocalDateTimeOfTenant(), tokenLiveTimeInSec, extendedAccessToken,
                deliveryMethod.getName(), deliveryMethod.getTarget());
    }

    /**
     * Replace the mutable fields of this request with values from a freshly generated one. Used when a user requests a
     * new OTP while an existing record is still present, so that we update the existing row instead of inserting a new
     * one (the {@code appuser_id} column has a UNIQUE constraint).
     */
    public void refreshFrom(final OTPRequest other) {
        this.tokenHash = other.tokenHash;
        this.plainToken = other.plainToken;
        this.requestTime = other.requestTime;
        this.tokenLiveTimeInSec = other.tokenLiveTimeInSec;
        this.extendedAccessToken = other.extendedAccessToken;
        this.deliveryMethod = other.deliveryMethod;
        this.deliveryTarget = other.deliveryTarget;
    }

    public boolean isValid() {
        final LocalDateTime expireTime = requestTime.plusSeconds(tokenLiveTimeInSec);
        return DateUtils.getLocalDateTimeOfTenant().isBefore(expireTime);
    }

    /**
     * Validates a candidate OTP against the stored hash. The candidate is normalized and hashed the same way the
     * original token was, then compared in constant time to avoid timing side channels.
     */
    public boolean matchesToken(final String candidateToken) {
        if (candidateToken == null || tokenHash == null) {
            return false;
        }
        final String candidateHash = hashToken(candidateToken);
        return MessageDigest.isEqual(tokenHash.getBytes(StandardCharsets.UTF_8), candidateHash.getBytes(StandardCharsets.UTF_8));
    }

    public OTPMetadata getMetadata() {
        final ZonedDateTime zonedRequestTime = requestTime.atZone(DateUtils.getDateTimeZoneOfTenant());
        final OTPDeliveryMethod method = new OTPDeliveryMethod(deliveryMethod, deliveryTarget, null);
        return new OTPMetadata(zonedRequestTime, tokenLiveTimeInSec, extendedAccessToken, method);
    }

    /**
     * Returns the plain OTP. Only populated for the in-memory window between generation and dispatch; entities loaded
     * from the database return {@code null} since the plain token is never persisted.
     */
    public String getToken() {
        return plainToken;
    }

    private static String hashToken(final String token) {
        // RandomOTPGenerator produces uppercase alphanumeric characters; previous comparison was case-insensitive,
        // so we normalize before hashing to preserve that behavior across both write and validate paths.
        final String normalized = token.toUpperCase(Locale.ROOT);
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            final StringBuilder hex = new StringBuilder(hash.length * 2);
            for (final byte b : hash) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    public AppUser getUser() {
        return user;
    }

    public LocalDateTime getRequestTime() {
        return requestTime;
    }

    public int getTokenLiveTimeInSec() {
        return tokenLiveTimeInSec;
    }

    public boolean isExtendedAccessToken() {
        return extendedAccessToken;
    }

    public String getDeliveryMethod() {
        return deliveryMethod;
    }

    public String getDeliveryTarget() {
        return deliveryTarget;
    }
}
