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
package org.apache.fineract.infrastructure.hooks.interceptor;

import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;

@Slf4j
public class LoggingInterceptor implements Interceptor {

    public static final String STRING_CRLF = "\r\n";

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();

        StringBuilder sb = new StringBuilder();

        sb.append("🔹 Request:").append(STRING_CRLF);
        sb.append(request.method()).append(" ").append(request.url()).append(STRING_CRLF);
        sb.append("Headers: ").append(request.headers()).append(STRING_CRLF);

        if (request.body() != null) {
            Buffer buffer = new Buffer();
            request.body().writeTo(buffer);
            sb.append("Body: ").append(buffer.readUtf8()).append(STRING_CRLF);
        }

        Response response = chain.proceed(request);

        ResponseBody responseBody = response.body();
        String bodyString = responseBody.string();

        sb.append("🔸 Response:").append(STRING_CRLF);
        sb.append("Status code: ").append(response.code()).append(STRING_CRLF);
        sb.append("Headers: ").append(response.headers()).append(STRING_CRLF);
        sb.append("Body: ").append(bodyString).append(STRING_CRLF);

        log.info(sb.toString());

        return response.newBuilder().body(ResponseBody.create(bodyString, responseBody.contentType())).build();
    }
}
