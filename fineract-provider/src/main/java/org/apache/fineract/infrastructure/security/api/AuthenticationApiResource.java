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
package org.apache.fineract.infrastructure.security.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.constants.TwoFactorConstants;
import org.apache.fineract.infrastructure.security.data.AuthenticatedUserData;
import org.apache.fineract.infrastructure.security.service.SpringSecurityPlatformSecurityContext;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.useradministration.data.RoleData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.fineract.useradministration.domain.AppUserRepository;
import org.apache.fineract.useradministration.domain.Role;
import org.apache.fineract.useradministration.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@ConditionalOnProperty("fineract.security.basicauth.enabled")
@Path("/v1/authentication")
@Tag(name = "Authentication HTTP Basic", description = "An API capability that allows client applications to verify authentication details using HTTP Basic Authentication.")
@RequiredArgsConstructor
public class AuthenticationApiResource {

    @Value("${fineract.security.2fa.enabled}")
    private boolean twoFactorEnabled;

    @Value("${azure.activedirectory.client-id}")
    private String azureClientId;

    @Value("${azure.activedirectory.client-secret}")
    private String azureClientSecret;

    @Value("${azure.activedirectory.app-id-uri}")
    private String azureRedirectUri;

    @Value("${azure.activedirectory.tenant-id}")
    private String azureTenantId;

    public static class AuthenticateRequest {

        private String username;
        private String password;
        private String authorizationCode;
        private boolean isMicrosoftSsoLogin;
    }

    @Qualifier("customAuthenticationProvider")
    private final DaoAuthenticationProvider customAuthenticationProvider;
    private final ToApiJsonSerializer<AuthenticatedUserData> apiJsonSerializerService;
    private final SpringSecurityPlatformSecurityContext springSecurityPlatformSecurityContext;
    private final ClientReadPlatformService clientReadPlatformService;
    private final AppUserRepository appUserRepository;

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    @Operation(summary = "Verify authentication", description = "Authenticates the credentials provided and returns the set roles and permissions allowed.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = AuthenticationApiResourceSwagger.PostAuthenticationRequest.class)))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(schema = @Schema(implementation = AuthenticationApiResourceSwagger.PostAuthenticationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Unauthenticated. Please login") })
    @SuppressWarnings("all")
    public String authenticate(@Parameter(hidden = true) final String apiRequestBodyAsJson,
            @QueryParam("returnClientList") @DefaultValue("false") boolean returnClientList) throws ParseException {
        AuthenticateRequest request = new Gson().fromJson(apiRequestBodyAsJson, AuthenticateRequest.class);
        if (request == null) {
            throw new GeneralPlatformDomainRuleException("error.msg.invalid.authentication.request.body",
                    "Invalid JSON in BODY (no longer URL param; see FINERACT-726) of POST to /authentication: " + apiRequestBodyAsJson);
        }
        AppUser principal;
        String base64EncodedAuthenticationKey;
        AuthenticatedUserData authenticatedUserData;
        final Collection<String> permissions = new ArrayList<>();
        if (request.isMicrosoftSsoLogin) {
            final String authorizationCode = request.authorizationCode;
            final RestTemplate restTemplate = new RestTemplate();
            final String azureTokenEndpoint = "https://login.microsoftonline.com/" + azureTenantId + "/oauth2/v2.0/token";
            final HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/x-www-form-urlencoded");
            final MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("client_id", this.azureClientId);
            body.add("client_secret", this.azureClientSecret);
            body.add("code", authorizationCode);
            body.add("redirect_uri", this.azureRedirectUri);
            body.add("grant_type", "authorization_code");
            body.add("code_verifier", "entreamigos");
            body.add("scope", "user.read");
            final HttpEntity<MultiValueMap<String, String>> tokenRequest = new HttpEntity<>(body, headers);

            ResponseEntity<String> apiResponse;
            try {
                apiResponse = restTemplate.exchange(azureTokenEndpoint, HttpMethod.POST, tokenRequest, String.class);
            } catch (final Exception exception) {
                log.error("Error occurred while trying to authenticate with Microsoft SSO" + body.toString(), exception);
                throw new GeneralPlatformDomainRuleException("error.msg.microsoft.sso.login.failed", "Microsoft SSO login failed");
            }
            final String apiResponseBody = apiResponse.getBody();
            if (apiResponseBody == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.microsoft.sso.login.failed", "Microsoft SSO login failed");
            }
            final JsonObject apiResponseJson = JsonParser.parseString(apiResponseBody).getAsJsonObject();
            final String authenticationAccessToken = apiResponseJson.get("access_token").getAsString();
            final SignedJWT signedJWT = SignedJWT.parse(authenticationAccessToken);
            final JWTClaimsSet jwtClaimsSet = signedJWT.getJWTClaimsSet();
            final Map<String, Object> claimsJsonObject = jwtClaimsSet.toJSONObject();
            String email = null;
            if (claimsJsonObject.get("email") != null) {
                email = claimsJsonObject.get("email").toString();
            } else if (claimsJsonObject.get("upn") != null) {
                email = claimsJsonObject.get("upn").toString();
            } else {
                email = String.valueOf(claimsJsonObject.get("unique_name"));
            }
            final String name = String.valueOf(claimsJsonObject.get("name"));
            principal = this.appUserRepository.findAppUserByEmail(email).orElseThrow(() -> new UserNotFoundException(name));
            final List<GrantedAuthority> grantedAuthorities = principal.populateGrantedAuthorities();
            for (final GrantedAuthority grantedAuthority : grantedAuthorities) {
                permissions.add(grantedAuthority.getAuthority());
            }
            final String username = principal.getUsername();
            final String password = principal.getPassword();
            byte[] base64EncodedAuthenticationKeyBytes = Base64.getEncoder()
                    .encode((username + ":" + password).getBytes(StandardCharsets.UTF_8));
            base64EncodedAuthenticationKey = new String(base64EncodedAuthenticationKeyBytes, StandardCharsets.UTF_8);
        } else {
            if (request.username == null || request.password == null) {
                throw new GeneralPlatformDomainRuleException("error.msg.invalid.authentication.request.body",
                        "Username or Password is null in JSON (see FINERACT-726) of POST to /authentication: " + apiRequestBodyAsJson
                                + "; username=" + request.username + ", password=" + request.password);
            }
            final Authentication authentication = new UsernamePasswordAuthenticationToken(request.username, request.password);
            final Authentication authenticationCheck = this.customAuthenticationProvider.authenticate(authentication);
            if (authenticationCheck.isAuthenticated()) {
                final Collection<GrantedAuthority> authorities = new ArrayList<>(authenticationCheck.getAuthorities());
                for (final GrantedAuthority grantedAuthority : authorities) {
                    permissions.add(grantedAuthority.getAuthority());
                }
                byte[] base64EncodedAuthenticationKeyBytes = Base64.getEncoder()
                        .encode((request.username + ":" + request.password).getBytes(StandardCharsets.UTF_8));
                base64EncodedAuthenticationKey = new String(base64EncodedAuthenticationKeyBytes, StandardCharsets.UTF_8);

            } else {
                throw new GeneralPlatformDomainRuleException("error.msg.authentication.failed", "Authentication failed");
            }
            principal = (AppUser) authenticationCheck.getPrincipal();
        }

        final Collection<RoleData> roles = new ArrayList<>();
        final Set<Role> userRoles = principal.getRoles();
        for (final Role role : userRoles) {
            roles.add(role.toData());
        }
        final Long officeId = principal.getOffice().getId();
        final String officeName = principal.getOffice().getName();
        final Long staffId = principal.getStaffId();
        final String staffDisplayName = principal.getStaffDisplayName();
        final EnumOptionData organisationalRole = principal.organisationalRoleData();
        boolean isTwoFactorRequired = this.twoFactorEnabled
                && !principal.hasSpecificPermissionTo(TwoFactorConstants.BYPASS_TWO_FACTOR_PERMISSION);
        final Long userId = principal.getId();
        final String username = principal.getUsername();
        final String email = principal.getEmail();
        if (this.springSecurityPlatformSecurityContext.doesPasswordHasToBeRenewed(principal)) {
            authenticatedUserData = new AuthenticatedUserData().setUsername(username).setUserId(userId).setEmail(email)
                    .setBase64EncodedAuthenticationKey(base64EncodedAuthenticationKey).setAuthenticated(true).setShouldRenewPassword(true)
                    .setTwoFactorAuthenticationRequired(isTwoFactorRequired);
        } else {
            authenticatedUserData = new AuthenticatedUserData().setUsername(username).setOfficeId(officeId).setEmail(email)
                    .setOfficeName(officeName).setStaffId(staffId).setStaffDisplayName(staffDisplayName)
                    .setOrganisationalRole(organisationalRole).setRoles(roles).setPermissions(permissions).setUserId(principal.getId())
                    .setAuthenticated(true).setBase64EncodedAuthenticationKey(base64EncodedAuthenticationKey)
                    .setTwoFactorAuthenticationRequired(isTwoFactorRequired)
                    .setClients(returnClientList ? clientReadPlatformService.retrieveUserClients(userId) : null);
        }
        return this.apiJsonSerializerService.serialize(authenticatedUserData);
    }
}
