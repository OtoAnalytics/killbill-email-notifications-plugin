package org.killbill.billing.plugin.notification.womplyClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.osgi.service.log.LogService;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class SubscriptionClientImpl extends BaseServiceClient {

    private WebResource subscriptionReceiptTarget;

    private ObjectMapper jsonMapper;

    public SubscriptionClientImpl(final String uri,
                                  final LogService logService,
                                  final int readTimeout,
                                  final int connectTimeout) {
        super("Subscription Service", uri, logService, readTimeout * 1000, connectTimeout * 1000);
        logService.log(LogService.LOG_INFO,
                String.format("Starting a client for Subscription service pointing to %s", uri));
        subscriptionReceiptTarget = client.resource(uri).path("/v1/receipt/email");
        jsonMapper = new ObjectMapper();
        jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        jsonMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        jsonMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public boolean sendEmailRequest(String emailType, Invoice invoice, Account account) {
        return sendEmailRequest(emailType, invoice, account, "");
    }

    public boolean sendEmailRequest(String emailType, Account account, String subscriptionId) {
        return sendEmailRequest(emailType, null, account, subscriptionId);
    }

    public boolean sendEmailRequest(String emailType, Invoice invoice, Account account, String subscriptionId) {

        String accountId = account.getExternalKey();
        String mlid = accountId;
        if (accountId.startsWith("W_")) {
            mlid = accountId.substring(2);
        }
        String invoiceId = "";
        if (invoice != null) {
            invoiceId = invoice.getId().toString();
        }

        logService.log(LogService.LOG_INFO,
                String.format("Data for %s", subscriptionReceiptTarget.getURI().toString()));
        EmailRequestModel emailRequestModel = new EmailRequestModel(emailType, invoiceId, subscriptionId, mlid);
        logService.log(LogService.LOG_INFO, String.format("Sending subscription service request for %s", emailType));

        ClientResponse response;
        try {
            response = subscriptionReceiptTarget
                    .accept(MediaType.APPLICATION_JSON_TYPE)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .post(ClientResponse.class, jsonMapper.writeValueAsString(emailRequestModel));
        } catch (JsonProcessingException e) {
            logService.log(LogService.LOG_ERROR,
                    String.format("Could not send %s request for %s or %s", emailType, invoiceId, subscriptionId));
            throw new RuntimeException(e);
        }

        if (response != null) {
            return response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
        }
        return false;
    }
}
