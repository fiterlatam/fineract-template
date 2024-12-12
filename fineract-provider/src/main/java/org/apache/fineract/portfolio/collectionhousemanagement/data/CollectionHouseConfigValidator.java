package org.apache.fineract.portfolio.collectionhousemanagement.data;

import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.configuration.service.ConfigurationReadPlatformService;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class CollectionHouseConfigValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;
    private static final String COLLECTIONNAME = "collectionName";
    private static final String COLLECTIONNIT = "collectionNit";
    private static final String COLLECTIONCODE = "collectionCode";
    private static final String COLLECTIONVERIFICATIONCODE = "collectionVerificationCode";

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("collectionhousemanagement");

        String collectionName = this.fromApiJsonHelper.extractStringNamed(COLLECTIONNAME, element);
        baseDataValidator.reset().parameter(COLLECTIONNAME).value(collectionName).notBlank();

        String collectionNit = this.fromApiJsonHelper.extractStringNamed(COLLECTIONNIT, element);
        baseDataValidator.reset().parameter(COLLECTIONNIT).value(collectionNit).notBlank();

        String collectionCode = this.fromApiJsonHelper.extractStringNamed(COLLECTIONCODE, element);
        baseDataValidator.reset().parameter(COLLECTIONCODE).value(collectionCode).notBlank();

        String collectionVerificationCode = this.fromApiJsonHelper.extractStringNamed(COLLECTIONVERIFICATIONCODE, element);
        baseDataValidator.reset().parameter(COLLECTIONVERIFICATIONCODE).value(collectionVerificationCode).notBlank();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            //
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateForUpdate(final String json) {
        validateForCreate(json);
    }

}
