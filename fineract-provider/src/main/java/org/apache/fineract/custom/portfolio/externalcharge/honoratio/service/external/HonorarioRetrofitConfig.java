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
package org.apache.fineract.custom.portfolio.externalcharge.honoratio.service.external;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.Collection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import lombok.Getter;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.fineract.infrastructure.clientblockingreasons.data.BlockingReasonsDataValidator;
import org.apache.fineract.infrastructure.configuration.data.ExternalServicesPropertiesData;
import org.apache.fineract.infrastructure.configuration.domain.ExternalServicesPropertiesRepository;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesConstants;
import org.apache.fineract.infrastructure.configuration.service.ExternalServicesPropertiesReadPlatformService;
import org.apache.tika.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

@Component
public class HonorarioRetrofitConfig {

    private static final Logger LOG = LoggerFactory.getLogger(BlockingReasonsDataValidator.class);

    public static final String STRING_FALLBACK_URL = "https://localhost:8443/";
    public static final String STRING_FALLBACK_API_KEY = StringUtils.EMPTY;

    @Autowired
    private ExternalServicesPropertiesReadPlatformService externalServicesPropertiesReadPlatformService;

    @Autowired
    private ExternalServicesPropertiesRepository externalServicesPropertiesRepository;

    private Retrofit retrofitInstance;

    private String baseUrl = STRING_FALLBACK_URL;

    @Getter
    private String apiKey = STRING_FALLBACK_API_KEY;

    private void assignBaseUrlAndAPIKey() {
        try {
            Collection<ExternalServicesPropertiesData> externalServicesPropertiesDataList = externalServicesPropertiesReadPlatformService
                    .retrieveOne(ExternalServicesConstants.CUSTOM_CHARGE_HONORARIO_SERVICE_NAME);

            baseUrl = externalServicesPropertiesDataList.stream()
                    .filter(obj -> obj.getName().equals(ExternalServicesConstants.CUSTOM_CHARGE_HONORARIO_URL))
                    .map(ExternalServicesPropertiesData::getValue).findFirst().orElse(STRING_FALLBACK_URL);

            if (Boolean.FALSE.equals(baseUrl.endsWith("/"))) {
                baseUrl = baseUrl + "/";
            }

            apiKey = externalServicesPropertiesDataList.stream()
                    .filter(obj -> obj.getName().equals(ExternalServicesConstants.CUSTOM_CHARGE_HONORARIO_API_KEY))
                    .map(ExternalServicesPropertiesData::getValue).findFirst().orElse(STRING_FALLBACK_API_KEY);

        } catch (Exception e) {
            LOG.error("Error whilst fetching Honorario service properties. Using fallback values.");

            baseUrl = STRING_FALLBACK_URL;
            apiKey = STRING_FALLBACK_API_KEY;
        }
    }

    @Bean
    public Retrofit retrofit() {
        retrofitInstance = new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create()).build();
        return retrofitInstance;
    }

    public void apiRequestDetailsRenewal(Retrofit retrofitInstance) {
        assignBaseUrlAndAPIKey();

        this.retrofitInstance = new Retrofit.Builder().baseUrl(baseUrl).addConverterFactory(GsonConverterFactory.create())
                .client(createOkHttpClient()).build();
        ;
    }

    public Retrofit getRetrofitInstance() {
        return retrofitInstance;
    }

    private OkHttpClient createOkHttpClient() {
        try {
            X509TrustManager trustManager = new X509TrustManager() {

                @Override
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                @Override
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return new java.security.cert.X509Certificate[] {};
                }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new X509TrustManager[] { trustManager }, null);

            return new OkHttpClient.Builder().sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .hostnameVerifier((hostname, session) -> true).addInterceptor(new Interceptor() {

                        @Override
                        public Response intercept(Chain chain) throws IOException {
                            Request originalRequest = chain.request();

                            // Interceptor to add API_KEY header for each request...
                            Request newRequest = originalRequest.newBuilder().header("API_KEY", apiKey).build();

                            // This is for DEMO purposes to connect to a Mock Service, as the Sumas external provider...
                            if (originalRequest.url().toString().contains("sumas-dev.fiter.io")
                                    || originalRequest.url().toString().contains(STRING_FALLBACK_URL)) {

                                newRequest = originalRequest.newBuilder().header("API_KEY", apiKey)
                                        .header("Fineract-Platform-TenantId", "default")
                                        .header("Authorization", "Basic bWlmb3M6cGFzc3dvcmQ=").build();
                            }

                            return chain.proceed(newRequest);
                        }
                    }).build();
        } catch (Exception e) {
            LOG.error("Error whilst creating OkHttpClient", e);
            throw new RuntimeException("Error whilst creating OkHttpClient", e);
        }
    }
}
