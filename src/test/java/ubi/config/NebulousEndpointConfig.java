package ubi.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.springframework.context.annotation.Bean;
import jakarta.jms.ConnectionFactory;
import jakarta.inject.Singleton;

@ApplicationScoped
public class NebulousEndpointConfig {

    private static final Logger log = Logger.getLogger(NebulousEndpointConfig.class);

    private final Config config = ConfigProvider.getConfig();

    private final String brokerUrl = config.getValue("quarkus.qpid-jms.url", String.class);
    private final String username = config.getValue("quarkus.qpid-jms.username", String.class);
    private final String password = config.getValue("quarkus.qpid-jms.password", String.class);
    private final String appcreationDestination = config.getValue("jms.topic.nebulous.optimiser", String.class);

    @Bean
    @Produces
    @Singleton
    @AppCreation
    public ConnectionFactory connectionFactory() {
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint appCreationEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + appcreationDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(appcreationDestination)
                .build();
    }
}
