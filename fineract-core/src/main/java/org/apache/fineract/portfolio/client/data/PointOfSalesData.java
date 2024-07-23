package org.apache.fineract.portfolio.client.data;

import java.io.Serializable;
import lombok.*;

@ToString
@Getter
@Setter
@EqualsAndHashCode
@AllArgsConstructor
@NoArgsConstructor
public class PointOfSalesData implements Serializable {

    private Long id;
    private String name;
    private String code;
    private Long clientAllyId;
    private Integer rowIndex;

    public static PointOfSalesData instance(Long Id, String name, String code, Long clientAllyId) {
        return new PointOfSalesData(Id, name, code, clientAllyId, null);
    }

    public static PointOfSalesData instance(final String code, final Integer rowIndex) {
        return new PointOfSalesData(null, null, code, null, rowIndex);
    }
}
