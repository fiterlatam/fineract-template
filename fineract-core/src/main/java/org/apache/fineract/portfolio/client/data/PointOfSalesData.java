package org.apache.fineract.portfolio.client.data;

import java.io.Serializable;
import lombok.*;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
public class PointOfSalesData implements Serializable {

    private Long id;
    private String name;
    private String code;
    private Long clientAllyId;

    public static PointOfSalesData instance(Long Id, String name, String code, Long clientAllyId) {
        return new PointOfSalesData(Id, name, code, clientAllyId);
    }
}
