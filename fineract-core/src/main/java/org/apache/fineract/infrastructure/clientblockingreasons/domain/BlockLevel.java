package org.apache.fineract.infrastructure.clientblockingreasons.domain;

public enum BlockLevel {

    CLIENT(1, "blockLevelType.client"),

    CREDIT(2, "blockLevelType.credit");

    private final Integer value;
    private final String code;

    BlockLevel(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    public static BlockLevel fromInt(final Integer type) {

        BlockLevel blockLevel = null;
        switch (type) {
            case 1:
                blockLevel = BlockLevel.CLIENT;
            break;
            case 2:
                blockLevel = BlockLevel.CREDIT;
            break;
        }
        return blockLevel;
    }

    public boolean isClient() {
        return this.value.equals(BlockLevel.CLIENT.getValue());
    }

    public boolean isCredit() {
        return this.value.equals(BlockLevel.CREDIT.getValue());
    }
}
