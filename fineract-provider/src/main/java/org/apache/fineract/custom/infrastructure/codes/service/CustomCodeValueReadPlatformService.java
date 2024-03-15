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
package org.apache.fineract.custom.infrastructure.codes.service;

import org.apache.fineract.custom.infrastructure.codes.data.CustomCodeValueData;
import org.apache.fineract.infrastructure.codes.service.CodeValueReadPlatformService;

import java.util.Collection;

/**
 * A service for retrieving code value information based on the code itself.
 *
 * There are two types of code information in the platform:
 * <ol>
 * <li>System defined codes</li>
 * <li>User defined codes</li>
 * </ol>
 *
 * <p>
 * System defined codes cannot be altered or removed but their code values may be allowed to be added to or removed.
 * </p>
 *
 * <p>
 * User defined codes can be changed in any way by application users with system permissions.
 * </p>
 */
public interface CustomCodeValueReadPlatformService extends CodeValueReadPlatformService {
    Collection<CustomCodeValueData> retrieveCodeValuesByCodeAndParent(String code, Long parentId);
}
