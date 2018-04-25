package org.killbill.billing.plugin.notification.womplyClient;

public class EmailRequestModel {

    private String emailType;
    private String invoiceId;
    private String subscriptionId;
    private String businessLocationId;

    public EmailRequestModel(String emailType,
                             String invoiceId,
                             String subscriptionId,
                             String businessLocationId) {
        this.emailType = emailType;
        this.invoiceId = invoiceId;
        this.subscriptionId = subscriptionId;
        this.businessLocationId = businessLocationId;
    }

    public String getEmailType() {
        return emailType;
    }

    public void setEmailType(String emailType) {
        this.emailType = emailType;
    }

    public String getInvoiceId() { return invoiceId; }

    public void setInvoiceId(String invoiceId) { this.invoiceId = invoiceId; }

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
