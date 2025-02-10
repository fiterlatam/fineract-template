
package org.apache.fineract.portfolio.collectionhousemanagement.data;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "collectionHouseUpdates" })

public class CollectionHouseUpdates {

    @JsonProperty("collectionHouseUpdates")
    private List<CollectionHouseUpdate> collectionHouseUpdates;

    @JsonProperty("collectionHouseUpdates")
    public List<CollectionHouseUpdate> getCollectionHouseUpdates() {
        if (collectionHouseUpdates == null) {
            collectionHouseUpdates = new ArrayList<CollectionHouseUpdate>();
        }
        return collectionHouseUpdates;
    }

    @JsonProperty("collectionHouseUpdates")
    public void setCollectionHouseUpdates(List<CollectionHouseUpdate> collectionHouseUpdates) {
        this.collectionHouseUpdates = collectionHouseUpdates;
    }

}
