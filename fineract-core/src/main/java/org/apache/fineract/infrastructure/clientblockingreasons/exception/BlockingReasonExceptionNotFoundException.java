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

public class BlockingReasonExceptionNotFoundException extends AbstractPlatformResourceNotFoundException {

    public BlockingReasonExceptionNotFoundException(final Long id) {
        super("errors.error.msg.id.does.not.exist", "Blocking Reason Settings with identifier " + id + " does not exist", id);
    }

    public BlockingReasonExceptionNotFoundException(final Integer priority, final String level) {
        super("errors.error.msg.priority.is.already.assigned.to.client.level",
                "Priority  [" + priority + "] is already assigned to client level " + level, priority, level);
    }

    public BlockingReasonExceptionNotFoundException(final String reason, final String level) {
        super("errors.error.msg.block.reason.is.already.assigned.to.level",
                "Block Reason  [" + reason + "] is already assigned to client level " + level, reason, level);
    }

}
