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
package org.apache.fineract.useradministration.domain;

import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;

@Slf4j
@Getter
@Setter
@Entity
@Table(name = "m_appuser_devices")
public class AppUserDevices extends AbstractPersistableCustom {

    @Column(name = "device_id", nullable = false, unique = true, length = 200)
    private String deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static AppUserDevices createNew(AppUser user, String fingerprint) {
        AppUserDevices appUserDevices = new AppUserDevices();
        appUserDevices.setUser(user);
        appUserDevices.setDeviceId(fingerprint);
        appUserDevices.setCreatedAt(DateUtils.getLocalDateTimeOfTenant());
        return appUserDevices;
    }
}
