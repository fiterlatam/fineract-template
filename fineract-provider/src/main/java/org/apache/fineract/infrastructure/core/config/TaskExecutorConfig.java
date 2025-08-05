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
package org.apache.fineract.infrastructure.core.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class TaskExecutorConfig {

    private static final Logger log = LoggerFactory.getLogger(TaskExecutorConfig.class);

    @Autowired
    private FineractProperties fineractProperties;

    @Bean(TaskExecutorConstant.DEFAULT_TASK_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor fineractDefaultThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        threadPoolTaskExecutor.setCorePoolSize(fineractProperties.getTaskExecutor().getDefaultTaskExecutorCorePoolSize());
        threadPoolTaskExecutor.setMaxPoolSize(fineractProperties.getTaskExecutor().getDefaultTaskExecutorMaxPoolSize());
        return threadPoolTaskExecutor;
    }

    @Bean(TaskExecutorConstant.CONFIGURABLE_TASK_EXECUTOR_BEAN_NAME)
    @Scope(scopeName = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ThreadPoolTaskExecutor fineractConfigurableThreadPoolTaskExecutor() {
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();

        // Get configuration values
        int corePoolSize = fineractProperties.getTaskExecutor().getDefaultTaskExecutorCorePoolSize();
        int maxPoolSize = fineractProperties.getTaskExecutor().getDefaultTaskExecutorMaxPoolSize();

        // Validate configuration
        if (corePoolSize <= 0) {
            throw new IllegalArgumentException("Default task executor core pool size must be greater than 0, but was: " + corePoolSize);
        }
        if (maxPoolSize <= 0) {
            throw new IllegalArgumentException("Default task executor max pool size must be greater than 0, but was: " + maxPoolSize);
        }
        if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException(
                    "Core pool size (" + corePoolSize + ") cannot be greater than max pool size (" + maxPoolSize + ")");
        }

        // Set initial values that allow for dynamic reconfiguration
        threadPoolTaskExecutor.setCorePoolSize(corePoolSize);
        threadPoolTaskExecutor.setMaxPoolSize(maxPoolSize);
        // Enable core thread timeout to allow dynamic scaling
        threadPoolTaskExecutor.setAllowCoreThreadTimeOut(true);
        // Set a reasonable queue capacity
        threadPoolTaskExecutor.setQueueCapacity(1000);
        // Set thread name prefix for better monitoring
        threadPoolTaskExecutor.setThreadNamePrefix("configurable-task-");

        log.debug("Created configurable ThreadPoolTaskExecutor with corePoolSize={}, maxPoolSize={}", corePoolSize, maxPoolSize);
        return threadPoolTaskExecutor;
    }
}
