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
package org.apache.fineract.organisation.portfolioCenter.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterFrecuencyMeeting;

public final class PortfolioCenterGroupUtil {

    private PortfolioCenterGroupUtil() {

    }

    /**
     *
     * @param startingDate
     * @param startDay
     * @param endDay
     * @param dayOfWeekNumber
     * @param weekOfMonth
     * @param frequencyMeeting
     * @return
     */
    public static LocalDate calculateMeetingDate(LocalDate startingDate, int startDay, int endDay, int dayOfWeekNumber, int weekOfMonth,
            PortfolioCenterFrecuencyMeeting frequencyMeeting) {
        LocalDate meetingDate = null;
        boolean isMeeetingDateInRange;

        switch (frequencyMeeting) {
            case MENSUAL:
                // calculate first meeting date
                DayOfWeek dow = DayOfWeek.of(dayOfWeekNumber);
                meetingDate = startingDate.with(WeekFields.ISO.weekOfMonth(), weekOfMonth).with(dow);
                isMeeetingDateInRange = dateInRange(meetingDate.getDayOfMonth(), startDay, endDay);

                if (meetingDate.isBefore(startingDate) || meetingDate.isEqual(startingDate)
                        || meetingDate.getMonthValue() == startingDate.getMonthValue() || !isMeeetingDateInRange) {
                    // Use next month instead
                    meetingDate = startingDate.plusMonths(1).with(WeekFields.ISO.weekOfMonth(), weekOfMonth).with(dow);
                } else {
                    return meetingDate;
                }
            break;
            default:
            break;
        }
        return meetingDate;
    }

    /**
     *
     * @param startingDate
     * @param startDay
     * @param endDay
     * @param dayOfWeekNumber
     * @param frequencyMeeting
     * @return
     */
    public static LocalDate calculateNextMeetingDate(LocalDate startingDate, int startDay, int endDay, int dayOfWeekNumber,
            PortfolioCenterFrecuencyMeeting frequencyMeeting) {
        LocalDate meetingDate = null;
        DayOfWeek dow = DayOfWeek.of(dayOfWeekNumber);

        if (frequencyMeeting.isMensual()) {
            meetingDate = startingDate.with(TemporalAdjusters.firstDayOfNextMonth()).with(TemporalAdjusters.next(dow));
            boolean isMeeetingDateInRange;
            do {
                isMeeetingDateInRange = dateInRange(meetingDate.getDayOfMonth(), startDay, endDay);
                if (!isMeeetingDateInRange) {
                    meetingDate = meetingDate.with(TemporalAdjusters.next(dow));
                }
            } while (!isMeeetingDateInRange);
        }
        return meetingDate;
    }

    /**
     *
     * @param day
     * @param startDay
     * @param endDay
     * @return
     */
    private static boolean dateInRange(int day, int startDay, int endDay) {
        if (day >= startDay && day <= endDay)
            return true;
        else
            return false;
    }
}
