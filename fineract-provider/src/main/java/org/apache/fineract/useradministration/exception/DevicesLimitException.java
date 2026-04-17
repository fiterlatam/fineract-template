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
package org.apache.fineract.useradministration.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * A {@link RuntimeException} thrown when user resources are not found.
 */
public class DevicesLimitException extends AbstractPlatformResourceNotFoundException {

    public DevicesLimitException(final Long id, final long limit) {
        super("error.msg.user.devices.limit.exceeded", "User with ID " + id + " has exceeded the set devices limit", id, limit);
    }

    public DevicesLimitException(final String userName, final long limit) {
        super("error.msg.user.devices.limit.exceeded", "User with name '" + userName + "' Has exceeded the set devices limit", userName,
                limit);
    }
}
