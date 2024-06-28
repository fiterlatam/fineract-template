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
package org.apache.fineract.organisation.centerGroup.exception;

import java.time.LocalTime;
import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;
import org.springframework.dao.EmptyResultDataAccessException;

/**
 * A {@link RuntimeException} thrown when center group resources are not found.
 */
public class CenterGroupMeetingTimeCollisionException extends AbstractPlatformResourceNotFoundException {

    public CenterGroupMeetingTimeCollisionException(final String groupName, final Long groupId, final LocalTime startTime,
            final LocalTime endTime) {
        super("error.msg.center.group.meeting.times.collision", "Center group " + groupName + " with id " + groupId
                + " In the destination center has meetings in given time: " + startTime + "-" + endTime, groupName, groupId, startTime,
                endTime);
    }

    public CenterGroupMeetingTimeCollisionException(final String groupName, final Long groupId, final LocalTime startTime,
            final LocalTime endTime, EmptyResultDataAccessException e) {
        super("error.msg.center.group.meeting.times.collision", "Center group " + groupName + " with id " + groupId
                + " In the destination center has meetings in given time: " + startTime + "-" + endTime, groupName, groupId, startTime,
                endTime);
    }

}
