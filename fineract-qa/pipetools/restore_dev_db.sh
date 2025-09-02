#!/bin/bash
#
# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

PGPASSWORD=postgres pg_restore \
  -h localhost \
  -p 5432 \
  -U postgres \
  -d fineract_default \
  --no-owner \
  --no-comments \
  --no-privileges \
  --no-publications \
  --if-exists \
  --clean \
  fineract-qa/dbdump/db_backup.dump

# PGPASSWORD=postgres psql -h localhost -U postgres -d fineract_default -f fineract-qa/pipetools/sync_up_tables.sql

rm -rf db_backup.dump

postgres
pwd
ls -la