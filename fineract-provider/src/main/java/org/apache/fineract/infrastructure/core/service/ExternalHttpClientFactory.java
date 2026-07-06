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
package org.apache.fineract.infrastructure.core.service;

import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import org.apache.fineract.infrastructure.core.config.FineractProperties;
import org.apache.fineract.infrastructure.core.config.FineractProperties.FineractHttpProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Factory for HTTP clients used to call external services, with connect and read timeouts configured to prevent
 * indefinite blocking when remote systems are slow or unresponsive.
 */
@Component
public class ExternalHttpClientFactory {

    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public ExternalHttpClientFactory(final FineractProperties fineractProperties) {
        final FineractHttpProperties http = fineractProperties.getHttp() != null ? fineractProperties.getHttp()
                : new FineractHttpProperties();
        this.connectTimeoutMs = http.getConnectTimeoutMs();
        this.readTimeoutMs = http.getReadTimeoutMs();
    }

    public int getConnectTimeoutMs() {
        return connectTimeoutMs;
    }

    public int getReadTimeoutMs() {
        return readTimeoutMs;
    }

    public RestTemplate createRestTemplate() {
        final SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeoutMs);
        requestFactory.setReadTimeout(readTimeoutMs);
        return new RestTemplate(requestFactory);
    }

    public OkHttpClient.Builder createOkHttpClientBuilder() {
        return new OkHttpClient.Builder().connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS).writeTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
                .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
    }

    public OkHttpClient createOkHttpClient() {
        return createOkHttpClientBuilder().build();
    }

    public void configureConnectionTimeouts(final HttpURLConnection connection) {
        connection.setConnectTimeout(connectTimeoutMs);
        connection.setReadTimeout(readTimeoutMs);
    }
}
