package org.apache.fineract.portfolio.collectionhousemanagement.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.*;
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
public class CollectionHouseHistoryValidator {

    private final FromJsonHelper fromApiJsonHelper;
    private final ConfigurationReadPlatformService configurationReadPlatformService;

    private static final String COLLECTIONNIT = "nit";
    private static final String COLLECTIONCODE = "collectionHouseCode";
    private static final String CLIENTACCOUNTNO = "clientAccountNo";

    public void validateForCreateCollectionHouse(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final JsonElement element = this.fromApiJsonHelper.parse(json);
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("collectionhousemanagement");
        final Set<String> supportedParameters = new HashSet<>(Arrays.asList(COLLECTIONCODE, COLLECTIONNIT, CLIENTACCOUNTNO));
        if (this.fromApiJsonHelper.parameterExists("collectionHouseUpdates", element)) {
            final JsonObject collectionHouseHistorylJsonElement = element.getAsJsonObject();
            final JsonArray array = collectionHouseHistorylJsonElement.get("collectionHouseUpdates").getAsJsonArray();
            for (int i = 1; i <= array.size(); i++) {
                final Type arrayObjectParameterTypeOfMap = new TypeToken<Map<String, Object>>() {}.getType();

                final JsonObject collectionHouseHistoryElement = array.get(i - 1).getAsJsonObject();
                final String arrayObjectJson = this.fromApiJsonHelper.toJson(collectionHouseHistoryElement);
                this.fromApiJsonHelper.checkForUnsupportedParameters(arrayObjectParameterTypeOfMap, arrayObjectJson, supportedParameters);

                String collectionNit = this.fromApiJsonHelper.extractStringNamed(COLLECTIONNIT, collectionHouseHistoryElement);
                baseDataValidator.reset().parameter(COLLECTIONNIT).parameterAtIndexArray(COLLECTIONNIT, i).value(collectionNit).notBlank();

                String collectionCode = this.fromApiJsonHelper.extractStringNamed(COLLECTIONCODE, collectionHouseHistoryElement);
                baseDataValidator.reset().parameter(COLLECTIONCODE).parameterAtIndexArray(COLLECTIONCODE, i).value(collectionCode)
                        .notBlank();

                String clientAccountNo = this.fromApiJsonHelper.extractStringNamed(CLIENTACCOUNTNO, collectionHouseHistoryElement);
                baseDataValidator.reset().parameter(CLIENTACCOUNTNO).parameterAtIndexArray(CLIENTACCOUNTNO, i).value(clientAccountNo)
                        .notBlank();
            }
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateForUpdate(final String json) {
        validateForCreateCollectionHouse(json);
    }
}
