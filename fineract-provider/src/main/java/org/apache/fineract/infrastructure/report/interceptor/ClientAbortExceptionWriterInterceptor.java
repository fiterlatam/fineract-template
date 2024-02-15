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
package org.apache.fineract.infrastructure.report.interceptor;

import java.io.IOException;
import java.io.OutputStream;
import javax.annotation.Priority;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.WriterInterceptor;
import javax.ws.rs.ext.WriterInterceptorContext;
import org.apache.catalina.connector.ClientAbortException;
import org.apache.commons.io.output.ProxyOutputStream;

@Provider
@Priority(1)
public class ClientAbortExceptionWriterInterceptor implements WriterInterceptor {

    @Override
    public void aroundWriteTo(WriterInterceptorContext writerInterceptorContext) throws IOException, WebApplicationException {
        writerInterceptorContext.setOutputStream(new ClientAbortExceptionOutputStream(writerInterceptorContext.getOutputStream()));
        try {
            writerInterceptorContext.proceed();
        } catch (Throwable t) {
            for (Throwable cause = t; cause != null; cause = cause.getCause()) {
                if (cause instanceof ClientAbortException) {
                    return;
                }
            }
            throw t;
        }
    }

    private static class ClientAbortExceptionOutputStream extends ProxyOutputStream {

        public ClientAbortExceptionOutputStream(OutputStream outputStream) {
            super(outputStream);
        }

        @Override
        protected void handleIOException(IOException e) throws IOException {
            throw new ClientAbortException(e);
        }
    }

}
