# Partial Data Migration Guide: SU+ Core Banking Application (Clients and Loans)

## 1. Introduction

This guide outlines the procedure for performing a partial data migration for the SU+ Core Banking Application. The scope of this migration is specifically focused on client and loan data, which will be handled in two primary phases. Adherence to the steps and prerequisites detailed herein is crucial for a successful migration.

## 2. Prerequisites

Before commencing the data migration, ensure that the following conditions are met:

- **Migration Data Availability**: Confirm that the client data is populated within the tmp_clientes2_migrar table and the loan data is present in the tmp_creditos2_migrar table. These tables serve as the source for the migration.

- **Core Banking Application Status**: Verify that the Mifos/Fineract Core Banking Application is fully operational and accessible.

- **Migration Script Availability**: Ensure the existence and accessibility of the scripts/partial_migration_script.sql file.

## 3. Migration Process

Follow these steps meticulously to execute the partial data migration:

1. **Script Execution**: Open the partial_migration_script.sql file. Execute the SQL commands sequentially from top to bottom, paying close attention to the embedded comments that provide guidance for each step.

2. **Validation**: Prior to proceeding with the actual data insertion, execute all validation queries provided within the script. It is imperative that all validation checks pass successfully to ensure data integrity and readiness for migration.

3. **Post-Migration Verification**: Upon the successful completion of the migration script, log in to the Mifos/Fineract application. Conduct thorough verification of the migrated client and loan data to confirm its accuracy and completeness.

## 4. Important Considerations

- **Batch Processing for Large Datasets**: In scenarios involving a substantial volume of data, it is highly recommended to perform the migration in batches. This approach can mitigate performance issues and reduce the risk of timeouts.

- **Script Customization for Batching**: Implementing batch processing will necessitate modifications to the migration scripts. Specifically, ensure that the SQL queries are appropriately filtered to process only the data pertinent to the current batch, thereby maintaining control and efficiency.
