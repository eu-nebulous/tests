package eu.nebulouscloud.test.automated.example.config;


import eu.nebulouscloud.config.ConnectionConfig;
import jakarta.jms.ConnectionFactory;
import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.EnableJms;

@Configuration
@EnableJms
@Import(ConnectionConfig.class)
public class EndpointConfig {

    @Autowired
    private Environment env;

    @Bean
    public JmsEndpoint todoJmsEndpoint(ConnectionFactory connectionFactory) {
        String inboundDestination = env.getProperty("jms.topic.mock.inbound");

        System.out.println("Inbound Destination: " + inboundDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(inboundDestination)
                .build();
    }

    @Bean
    public JmsEndpoint todoReportEndpoint(ConnectionFactory connectionFactory) {
        String reportDestination = env.getProperty("jms.topic.mock.report");

        System.out.println("Report Destination: " + reportDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(reportDestination)
                .build();
    }
}
