package org.killbill.billing.plugin.notification.womplyClient;

public class EmailRequestModel {

    private String emailType;
    private String invoiceNumber;
    private String subscriptionId;
    private String businessLocationId;

    public EmailRequestModel(String emailType,
                             String invoiceNumber,
                             String subscriptionId,
                             String businessLocationId) {
        this.emailType = emailType;
        this.invoiceNumber = invoiceNumber;
        this.subscriptionId = subscriptionId;
        this.businessLocationId = businessLocationId;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public String getInvoiceNumber() { return invoiceNumber; }

    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    public String getBusinessLocationId() {
        return businessLocationId;
    }

    public void setBusinessLocationId(String businessLocationId) {
        this.businessLocationId = businessLocationId;
    }
}
