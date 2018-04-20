package org.killbill.billing.plugin.notification.womplyClient;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.dropwizard.jersey.jackson.JacksonMessageBodyProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;

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

        ObjectMapper jsonMapper = new ObjectMapper();

        jsonMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        jsonMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        jsonMapper.registerModule(new Jdk8Module());
        jsonMapper.registerModule(new GuavaModule());
        jsonMapper.registerModule(new JavaTimeModule());

        jsonMapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        jsonMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

        jsonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        ClientConfig config = new ClientConfig();
        config.property(ClientProperties.READ_TIMEOUT, readTimeOut);
        config.property(ClientProperties.CONNECT_TIMEOUT, connectTimeout);
        config.register(new JacksonMessageBodyProvider(jsonMapper));

        client = ClientBuilder.newClient(config);
    }

}
