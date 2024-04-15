package org.apache.fineract.infrastructure.clientBlockingSettings.service;

import org.apache.fineract.infrastructure.clientBlockingSettings.data.BlockingReasonsData;

public interface ManageBlockingReasonsReadPlatformService {

    BlockingReasonsData retrieveTemplate();
}
