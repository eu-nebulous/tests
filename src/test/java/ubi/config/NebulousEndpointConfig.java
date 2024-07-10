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
    private final String metricModelDestination = config.getValue("jms.topic.nebulous.metric_model", String.class);
    private final String evaluatorDestination = config.getValue("jms.topic.nebulous.evaluator", String.class);
    private final String nodeCandidatesRequestCFSBDestination = config.getValue("jms.topic.nebulous.node_canditates_request_cfsb", String.class);
    private final String nodeCandidatesRequestSALDestination = config.getValue("jms.topic.nebulous.node_canditates_request_sal", String.class);
    private final String nodeCandidatesReplySALDestination = config.getValue("jms.topic.nebulous.node_canditates_reply_sal", String.class);
    private final String nodeCandidatesReplyCFSBDestination = config.getValue("jms.topic.nebulous.node_canditates_reply_cfsb", String.class);
    private final String defineClusterDestination = config.getValue("jms.topic.nebulous.optimiser_define_cluster", String.class);
    private final String deployClusterDestination = config.getValue("jms.topic.nebulous.optimiser_deploy_cluster", String.class);


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

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint metricModelEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + metricModelDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(metricModelDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint evaluatorEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + evaluatorDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(evaluatorDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint nodeCandidatesRequestCFSBEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + nodeCandidatesRequestCFSBDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesRequestCFSBDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint nodeCandidatesRequestSALEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + nodeCandidatesRequestSALDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesRequestSALDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint nodeCandidatesReplySALEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + nodeCandidatesReplySALDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesReplySALDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint nodeCandidatesReplyCFSBEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + nodeCandidatesReplyCFSBDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesReplyCFSBDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint defineClusterEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + defineClusterDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(defineClusterDestination)
                .build();
    }

    @Bean
    @Produces
    @Singleton
    public JmsEndpoint deployClusterEndpoint(@AppCreation ConnectionFactory connectionFactory) {
        log.info("Inbound Destination: " + deployClusterDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(deployClusterDestination)
                .build();
    }
}
