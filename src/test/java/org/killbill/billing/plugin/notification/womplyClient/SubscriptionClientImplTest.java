package org.killbill.billing.plugin.notification.womplyClient;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.killbill.billing.account.api.Account;
import org.killbill.billing.invoice.api.Invoice;
import org.osgi.framework.ServiceReference;
import org.osgi.service.log.LogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SubscriptionClientImplTest {

    protected static final String SEND_EMAIL_REQUEST_PATH = "/v1/receipt/email";

    private final Logger log = LoggerFactory.getLogger(SubscriptionClientImplTest.class);

    protected static final int PORT = 8098;

    protected SubscriptionClientImpl client;
    protected WireMockServer wireMockServer;

    @BeforeClass
    void startWireMock() throws Exception {
        LogService logService = new LogService() {
            @Override
            public void log(int i, String s) {
                log.info(s);
            }

            @Override
            public void log(int i, String s, Throwable throwable) {
                log.info(s, throwable);
            }

            @Override
            public void log(ServiceReference serviceReference, int i, String s) {
            }

            @Override
            public void log(ServiceReference serviceReference, int i, String s, Throwable throwable) {
            }
        };

        wireMockServer = new WireMockServer(wireMockConfig().port(PORT));
        wireMockServer.start();

        WireMock.configureFor(PORT);

        client = new SubscriptionClientImpl("http://localhost:8098", logService, 10, 10);
    }

    @AfterClass
    void stopWireMock() throws Exception {
        wireMockServer.stop();
    }

    @BeforeMethod
    void init() throws Exception {
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
                        .withStatus(204)));

        boolean response = client.sendEmailRequest("Test_Sent", mockInvoice, mockAccount);

        Assert.assertTrue(response);
    }
}
