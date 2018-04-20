package org.killbill.billing.plugin.notification.womplyClient;

public class EmailRequestModel {

    private String emailType;
    private Integer invoiceNumber;
    private String mlid;


    public EmailRequestModel(String emailType, Integer invoiceNumber, String mlid) {
        this.emailType = emailType;
        this.invoiceNumber = invoiceNumber;
        this.mlid = mlid;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public Integer getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(Integer invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getMlid() {
        return mlid;
    }

    public void setMlid(String mlid) {
        this.mlid = mlid;
    }
}
