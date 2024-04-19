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
import org.apache.fineract.organisation.portfolioCenter.domain.PortfolioCenterFrecuencyMeeting;

public final class PortfolioCenterGroupUtil {

    /**
     *
     * @param startDate
     * @param startDay
     * @param endDay
     * @param dayOfWeekNumber
     * @return
     */
    public static LocalDate getNextMeetingDate(LocalDate startDate, int startDay, int endDay, int dayOfWeekNumber, LocalDate startDateRange,
                                               LocalDate endDateRange) {
        DayOfWeek dow = DayOfWeek.of(dayOfWeekNumber);
        int weekOfMonth = getWeekOfMonth(startDay, endDay);

        // Find the next meeting date within the current month
        LocalDate nextMeetingDate = startDate.withDayOfMonth(1).with(dow).plusWeeks(weekOfMonth - 1);
        while (nextMeetingDate.isBefore(startDateRange) || nextMeetingDate.isAfter(endDateRange)) {
            nextMeetingDate = nextMeetingDate.plusMonths(1); // Move to next month
        }

        return nextMeetingDate;
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

    /**
     *
     * @param startDay
     * @param endDay
     * @return
     */
    private static int getWeekOfMonth(int startDay, int endDay) {
        int weekOfMonth = 0;
        if (startDay == 1 && endDay == 7) {
            weekOfMonth = 1;
        } else if (startDay == 8 && endDay == 14) {
            weekOfMonth = 2;
        } else if (startDay == 15 && endDay == 21) {
            weekOfMonth = 3;
        } else if (startDay == 22 && endDay == 28) {
            weekOfMonth = 4;
        }

        return weekOfMonth;
    }

}
