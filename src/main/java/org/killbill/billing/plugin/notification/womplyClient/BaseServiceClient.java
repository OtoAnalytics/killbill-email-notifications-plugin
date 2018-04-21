package org.killbill.billing.plugin.notification.womplyClient;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;

public abstract class BaseServiceClient {

    protected final Client client;
    protected final String baseUri;
    protected final String serviceName;

    protected BaseServiceClient(
            final String serviceName,
            final String uri,
            final int readTimeOut,
            final int connectTimeout) {
        this.serviceName = serviceName;
        this.baseUri = uri;

        ClientConfig config = new DefaultClientConfig();
        config.getProperties().put(ClientConfig.PROPERTY_READ_TIMEOUT, readTimeOut);
        config.getProperties().put(ClientConfig.PROPERTY_CONNECT_TIMEOUT, connectTimeout);
        //config.register(new JacksonMessageBodyProvider(jsonMapper));

        client = Client.create(config);
    }

}
