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

import javax.servlet.http.HttpServletRequest;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.security.service.PlatformPasswordEncoder;
import org.apache.fineract.useradministration.domain.AppUser;

public interface AppUserWritePlatformService {

    CommandProcessingResult createUser(JsonCommand command);

    CommandProcessingResult updateUser(Long userId, JsonCommand command);

    CommandProcessingResult deleteUser(Long userId);

    void logUserAuthenticationDetails(AppUser appUser, HttpServletRequest servletRequest, String action, String result, String username, boolean processed);

    AppUser selfResetUserPassword(Long userId, String apiRequestBodyAsJson, PlatformPasswordEncoder platformPasswordEncoder);

    AppUser completePasswordReset(String username, String otp, Boolean logoutDevices, PlatformPasswordEncoder platformPasswordEncoder);

    AppUser requestPasswordReset(String username);
}
