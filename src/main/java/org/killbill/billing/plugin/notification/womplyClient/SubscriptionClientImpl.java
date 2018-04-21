package org.killbill.billing.plugin.notification.womplyClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;


public class SubscriptionClientImpl extends BaseServiceClient {

    private WebResource subscriptionTarget;


    private ObjectMapper jsonMapper;

    public SubscriptionClientImpl(final String uri, final int readTimeout, final int connectTimeout) {
        super("Subscription Service", uri, readTimeout * 1000, connectTimeout * 1000);
        System.out.println("Starting a client pointing to " + uri);
        subscriptionTarget = client.resource(uri).path("/v1/receipt/email");
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        //jsonMapper.registerModule(new Jdk8Module());
        //jsonMapper.registerModule(new GuavaModule());
        //jsonMapper.registerModule(new JavaTimeModule());

        jsonMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        jsonMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
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

        ClientResponse response = null;
        try {
            response = subscriptionTarget
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, jsonMapper.writeValueAsString(emailRequest));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        return response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
    }

}
