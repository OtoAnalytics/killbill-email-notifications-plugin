package org.killbill.billing.plugin.notification.womplyClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import org.osgi.service.log.LogService;

public abstract class BaseServiceClient {

    protected final Client client;
    protected final String baseUri;
    protected final String serviceName;
    protected final LogService logService;

    protected BaseServiceClient(final String serviceName,
                                final String uri,
                                final LogService logService,
                                final int readTimeOut,
                                final int connectTimeout) {

        this.serviceName = serviceName;
        this.baseUri = uri;
        this.logService = logService;

        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeOut);
        config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);

        client = Client.create(config);
    }
}
