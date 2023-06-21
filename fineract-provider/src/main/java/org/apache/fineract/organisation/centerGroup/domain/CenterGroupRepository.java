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
package org.apache.fineract.organisation.centerGroup.domain;

import java.time.LocalTime;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CenterGroupRepository extends JpaRepository<CenterGroup, Long>, JpaSpecificationExecutor<CenterGroup> {
    // no added behaviour

    String FIND_CENTERS_BY_MEETING_TIMES_AND_CENTER_ID = "Select cgroup from CenterGroup cgroup where cgroup.portfolioCenter.id = :center "
            + "and( ( :startTime >= cgroup.meetingStartTime and :startTime <= cgroup.meetingEndTime) "
            + "OR ( :endTime >= cgroup.meetingStartTime and :endTime <= cgroup.meetingEndTime) )";

    @Query(FIND_CENTERS_BY_MEETING_TIMES_AND_CENTER_ID)
    Collection<CenterGroup> findCenterGroupsByCenterIdAndMeetingTimes(@Param("center") Long center, @Param("startTime") LocalTime startTime,
            @Param("endTime") LocalTime endTime);




    @Query("SELECT e FROM CenterGroup e where e.portfolioCenter.id=:portfolioCenterId AND ( (e.meetingStartTime >= :startTime AND e.meetingStartTime <= :endTime) "
            + "OR (e.meetingEndTime >= :startTime AND e.meetingEndTime <= :endTime) OR (e.meetingStartTime <= :startTime AND e.meetingEndTime >= :endTime) )")
    List<CenterGroup> findOverlappingCenterGroups(Long portfolioCenterId, LocalTime startTime, LocalTime endTime);

}
