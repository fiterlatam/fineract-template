# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements. See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership. The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License. You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied. See the License for the
# specific language governing permissions and limitations
# under the License.
#
FROM azul/zulu-openjdk:17 AS builder

RUN apt-get update -qq && apt-get install -y wget vim unzip

COPY . fineract
WORKDIR /fineract

RUN unzip -d /opt/gradle /fineract/libs/gradle/gradle-8.5-bin.zip

ENV GRADLE_HOME /opt/gradle/gradle-8.5
ENV PATH $PATH:$GRADLE_HOME/bin

RUN gradle wrapper

RUN sed -i 's|services.gradle.org/distributions/gradle-8.5-bin.zip|sumasparati.mundosumas.com/libs/gradle/8.5/gradle-8.5-bin.zip|' /fineract/gradle/wrapper/gradle-wrapper.properties

RUN ./gradlew --no-daemon -q  -x compileTestJava -x test bootJar
RUN mv /fineract/fineract-provider/build/libs/*.jar /fineract/fineract-provider/build/libs/fineract-provider.jar

WORKDIR /app/libs
RUN wget -q https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/8.0.33/mysql-connector-j-8.0.33.jar
# =========================================

FROM azul/zulu-openjdk:17 as fineract
#pentaho copy
COPY --from=builder /fineract/fineract-provider/pentahoReports/*.properties /root/.mifosx/pentahoReports/
COPY --from=builder /fineract/fineract-provider/pentahoReports/*.prpt /root/.mifosx/pentahoReports/
#Pentaho to run on postgresDB
COPY --from=builder /fineract/fineract-provider/pentahoReportsPostgres/*.properties /root/.mifosx/pentahoReportsPostgres/
COPY --from=builder /fineract/fineract-provider/pentahoReportsPostgres/*.prpt /root/.mifosx/pentahoReportsPostgres/
COPY --from=builder /fineract/fineract-provider/build/libs/ /app
COPY --from=builder /app/libs /app/libs

ENV TZ="UTC"
ENV FINERACT_HIKARI_DRIVER_SOURCE_CLASS_NAME="org.postgresql.Driver"
ENV FINERACT_HIKARI_MINIMUM_IDLE="1"
ENV FINERACT_HIKARI_MAXIMUM_POOL_SIZE="20"
ENV FINERACT_HIKARI_IDLE_TIMEOUT="120000"
ENV FINERACT_HIKARI_CONNECTION_TIMEOUT="300000"
ENV FINERACT_HIKARI_TEST_QUERY="SELECT 1"
ENV FINERACT_HIKARI_AUTO_COMMIT="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_CACHE_PREP_STMTS="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_PREP_STMT_CACHE_SIZE="250"
ENV FINERACT_HIKARI_DS_PROPERTIES_PREP_STMT_CACHE_SQL_LIMIT="2048"
ENV FINERACT_HIKARI_DS_PROPERTIES_USE_SERVER_PREP_STMTS="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_USE_LOCAL_SESSION_STATE="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_REWRITE_BATCHED_STATEMENTS="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_CACHE_RESULT_SET_METADATA="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_CACHE_SERVER_CONFIGURATION="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_ELIDE_SET_AUTO_COMMITS="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_MAINTAIN_TIME_STATS="false"
ENV FINERACT_HIKARI_DS_PROPERTIES_LOG_SLOW_QUERIES="true"
ENV FINERACT_HIKARI_DS_PROPERTIES_DUMP_QUERIES_IN_EXCEPTION="true"
ENV FINERACT_DEFAULT_TENANTDB_TIMEZONE="America/Bogota"
ENV FINERACT_DEFAULT_TENANTDB_IDENTIFIER="default"
ENV FINERACT_DEFAULT_TENANTDB_NAME="fineract_default"
ENV FINERACT_DEFAULT_TENANTDB_DESCRIPTION="Default Demo Tenant"
ENV FINERACT_SERVER_SSL_ENABLED="false"
ENV FINERACT_SERVER_PORT="8080"

ENTRYPOINT ["java", "-Dloader.path=/app/libs/", "-jar", "/app/fineract-provider.jar"]
