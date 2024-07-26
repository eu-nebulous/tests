package ubi.tests;

import config.TestConfig;
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
@Import(TestConfig.class)
public class NebulousEndpointConfig {

    @Autowired
    private Environment env;

    @Bean
    public JmsEndpoint appCreationEndpoint(ConnectionFactory connectionFactory) {
        String appcreationDestination = env.getProperty("jms.topic.nebulous.optimiser");
        System.out.println("Inbound Destination: " + appcreationDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(appcreationDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint metricModelEndpoint(ConnectionFactory connectionFactory) {
        String metricModelDestination = env.getProperty("jms.topic.nebulous.metric_model");
        System.out.println("Inbound Destination: " + metricModelDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(metricModelDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint evaluatorEndpoint(ConnectionFactory connectionFactory) {
        String evaluatorDestination = env.getProperty("jms.topic.nebulous.evaluator");
        System.out.println("Inbound Destination: " + evaluatorDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(evaluatorDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint nodeCandidatesRequestCFSBEndpoint(ConnectionFactory connectionFactory) {
        String nodeCandidatesRequestCFSBDestination = env.getProperty("jms.topic.nebulous.node_canditates_request_cfsb");
        System.out.println("Inbound Destination: " + nodeCandidatesRequestCFSBDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesRequestCFSBDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint nodeCandidatesRequestSALEndpoint(ConnectionFactory connectionFactory) {
        String nodeCandidatesRequestSALDestination = env.getProperty("jms.topic.nebulous.node_canditates_request_sal");
        System.out.println("Inbound Destination: " + nodeCandidatesRequestSALDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesRequestSALDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint nodeCandidatesReplySALEndpoint(ConnectionFactory connectionFactory) {
        String nodeCandidatesReplySALDestination = env.getProperty("jms.topic.nebulous.node_canditates_reply_sal");
        System.out.println("Inbound Destination: " + nodeCandidatesReplySALDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesReplySALDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint nodeCandidatesReplyCFSBEndpoint(ConnectionFactory connectionFactory) {
        String nodeCandidatesReplyCFSBDestination = env.getProperty("jms.topic.nebulous.node_canditates_reply_cfsb");
        System.out.println("Inbound Destination: " + nodeCandidatesReplyCFSBDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(nodeCandidatesReplyCFSBDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint defineClusterEndpoint(ConnectionFactory connectionFactory) {
        String defineClusterDestination = env.getProperty("jms.topic.nebulous.optimiser_define_cluster");
        System.out.println("Inbound Destination: " + defineClusterDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(defineClusterDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint deployClusterEndpoint(ConnectionFactory connectionFactory) {
        String deployClusterDestination = env.getProperty("jms.topic.nebulous.optimiser_deploy_cluster");
        System.out.println("Inbound Destination: " + deployClusterDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(deployClusterDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }
}
