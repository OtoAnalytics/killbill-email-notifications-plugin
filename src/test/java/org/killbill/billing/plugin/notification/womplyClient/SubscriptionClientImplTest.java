package org.killbill.billing.plugin.notification.womplyClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.eclipse.jetty.http.HttpStatus;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.killbill.billing.plugin.notification.womplyClient.mockModels.MockAccount;
import org.killbill.billing.plugin.notification.womplyClient.mockModels.MockInvoice;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;


public class SubscriptionClientImplTest {

    protected static final String SEND_EMAIL_REQUEST_PATH = "/v1/receipt/email";

    protected static final int PORT = 8098;

    protected WireMockServer wireMockServer;
    protected SubscriptionClientImpl client;


    @BeforeClass
    void startWireMock() throws Exception {
        wireMockServer = new WireMockServer(wireMockConfig().port(PORT));
        wireMockServer.start();

        WireMock.configureFor(PORT);

        client = new SubscriptionClientImpl("http://localhost:8098", 10, 10);
    }

    @AfterClass
    void stopWireMock() throws Exception {
        wireMockServer.stop();
    }

    @BeforeMethod
    void init() {
        WireMock.resetToDefault();
    }

    @Test
    public void testSendEmailRequest() throws Exception {

        Invoice mockInvoice = new MockInvoice();

        Account mockAccount = new MockAccount();

        stubFor(post(urlEqualTo(SEND_EMAIL_REQUEST_PATH))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("")
                        .withStatus(HttpStatus.NO_CONTENT_204)));

        boolean response = client.sendEmailRequest("Payment", mockInvoice, mockAccount);

        Assert.assertTrue(response);
    }
}
