package org.killbill.billing.plugin.notification.womplyClient;

import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SubscriptionClientImpl extends BaseServiceClient {

    private WebTarget subscriptionTarget;

    public SubscriptionClientImpl(final String uri, final int readTimeout, final int connectTimeout) {
        super("Subscription Service", uri, readTimeout * 1000, connectTimeout * 1000);
        subscriptionTarget = client.target(uri).path("/v1/receipt/email");

    }

    public boolean sendEmailRequest(String emailType, Invoice invoice, Account account) {
        //Getting mlid from accountId
        String accountId = account.getExternalKey();
        String mlid = accountId;
        if (accountId.startsWith("W_")) {
            mlid = accountId.substring(2);
        }
        int invoiceId = 4;
        if (invoice != null){
            invoiceId = invoice.getInvoiceNumber();
        }
        EmailRequestModel emailRequest = new EmailRequestModel(emailType, invoiceId, mlid);
        System.out.println("Sending request for " + emailType);

        Response response = subscriptionTarget
                .resolveTemplate("emailType", emailType)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.json(emailRequest));

        return response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
    }

}
