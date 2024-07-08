package ubi.example.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.springframework.context.annotation.Bean;
import jakarta.jms.ConnectionFactory;
import jakarta.inject.Singleton;

@ApplicationScoped
public class EndpointConfig {

    private final Config config = ConfigProvider.getConfig();

    private final String brokerUrl = config.getValue("quarkus.qpid-jms.url", String.class);
    private final String username = config.getValue("quarkus.qpid-jms.username", String.class);
    private final String password = config.getValue("quarkus.qpid-jms.password", String.class);
    private final String inboundDestination = config.getValue("jms.topic.mock.inbound", String.class);
    private final String reportDestination = config.getValue("jms.topic.mock.report", String.class);

    @Bean
    @Produces
    @Singleton
    public ConnectionFactory connectionFactory() {
        System.out.println("Broker URL: " + brokerUrl);
        System.out.println("Username: " + username);
        System.out.println("Password: " + password);
        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint todoJmsEndpoint(ConnectionFactory connectionFactory) {
        System.out.println("Inbound Destination: " + inboundDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(inboundDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint todoReportEndpoint(ConnectionFactory connectionFactory) {
        System.out.println("Report Destination: " + reportDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(reportDestination)
                .build();
    }
}
