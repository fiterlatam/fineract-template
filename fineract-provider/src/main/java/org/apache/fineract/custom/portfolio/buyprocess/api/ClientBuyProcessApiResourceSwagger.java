package org.apache.fineract.custom.portfolio.buyprocess.api;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public class ClientBuyProcessApiResourceSwagger {

    public ClientBuyProcessApiResourceSwagger() {}

    @Schema(description = "PostClientBuyProcessRequest")
    public static final class PostClientBuyProcessRequest {

        public PostClientBuyProcessRequest() {}

        @Schema(example = "001")
        public String clientDocumentId;
        @Schema(example = "001")
        public String pointOfSalesCode;
        @Schema(example = "1")
        public Long productId;
        @Schema(example = "0")
        public Integer interestRatePoints;
        @Schema(example = "1")
        public Long creditId;
        @Schema(example = "25/06/2024")
        public String requestedDate;
        @Schema(example = "1000")
        public BigDecimal amount;
        @Schema(example = "3")
        public Integer term;
        @Schema(example = "1ae8d4db830eed577c6023998337d0hags546f1a3ba08e5df1ef0d1673431a3")
        public String channelHash;
        @Schema(example = "dd/MM/yyyy")
        public String dateFormat;
        @Schema(example = "es")
        public String locale;
        @Schema(example = "0")
        public Long codigoSeguro;
        @Schema(example = "0")
        public Long cedulaSeguroVoluntario;

    }

}
