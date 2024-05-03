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
package org.apache.fineract.infrastructure.clientblockingreasons.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

public class BlockReasonSettingNotFoundException extends AbstractPlatformResourceNotFoundException {

    public BlockReasonSettingNotFoundException(final Long id) {
        super("error.msg.id.does.not.exist", "Blocking Reason Settings with identifier " + id + " does not exist", id);
    }

    public BlockReasonSettingNotFoundException(final Integer priority, final String level) {
        super("error.msg.priority.is.already.assigned.to.client.level",
                "Priority  [" + priority + "] is already assigned to client level " + level, priority, level);
    }

    public BlockReasonSettingNotFoundException(final String reason, final String level) {
        super("error.msg.block.reason.is.already.assigned.to.level",
                "Block Reason  [" + reason + "] is already assigned to client level " + level, reason, level);
    }

    public BlockReasonSettingNotFoundException(String message) {
        super("error.msg.block.reason.not.found", message);
    }

}
