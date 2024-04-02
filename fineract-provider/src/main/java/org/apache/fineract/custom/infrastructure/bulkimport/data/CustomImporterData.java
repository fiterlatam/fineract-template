package org.apache.fineract.custom.infrastructure.bulkimport.data;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class CustomImporterData {

    // import fields
    protected transient Integer rowIndex;
    protected String dateFormat;
    protected String locale;
    protected String validationErrorMessage;

    public Integer getRowIndex() {
        return rowIndex;
    }

    public void setRowIndex(Integer rowIndex) {
        this.rowIndex = rowIndex;
    }

    public String getDateFormat() {
        return dateFormat;
    }

    public void setDateFormat(String dateFormat) {
        this.dateFormat = dateFormat;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    public void setValidationErrorMessage(String validationErrorMessage) {
        this.validationErrorMessage = validationErrorMessage;
    }
}
