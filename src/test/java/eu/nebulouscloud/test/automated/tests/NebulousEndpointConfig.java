package eu.nebulouscloud.test.automated.tests;

import eu.nebulouscloud.config.ConnectionConfig;
import jakarta.jms.ConnectionFactory;
import org.citrusframework.dsl.endpoint.CitrusEndpoints;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.security.HttpAuthentication;
import org.citrusframework.jms.endpoint.JmsEndpoint;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.jms.annotation.EnableJms;

@Configuration
@EnableJms
@Import(ConnectionConfig.class)
public class NebulousEndpointConfig {

    static Logger logger = LoggerFactory.getLogger(NebulousEndpointConfig.class);

    @Autowired
    private Environment env;

    @Bean
    public JmsEndpoint appCreationEndpoint(ConnectionFactory connectionFactory) {
        String appcreationDestination = env.getProperty("jms.topic.nebulous.optimiser");
        logger.debug("Inbound Destination: " + appcreationDestination);
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
        logger.debug("Inbound Destination: " + metricModelDestination);
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
        logger.debug("Inbound Destination: " + evaluatorDestination);
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
        logger.debug("Inbound Destination: " + nodeCandidatesRequestCFSBDestination);
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
        logger.debug("Inbound Destination: " + nodeCandidatesRequestSALDestination);
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
        logger.debug("Inbound Destination: " + nodeCandidatesReplySALDestination);
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
        logger.debug("Inbound Destination: " + nodeCandidatesReplyCFSBDestination);
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
        logger.debug("Inbound Destination: " + defineClusterDestination);
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
        logger.debug("Inbound Destination: " + deployClusterDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(deployClusterDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public JmsEndpoint appStatusEndpoint(ConnectionFactory connectionFactory) {
        String appStatusDestination = env.getProperty("optimiser_controller_state");
        logger.debug("Inbound Destination: " + appStatusDestination);
        return CitrusEndpoints.jms()
                .asynchronous()
                .connectionFactory(connectionFactory)
                .destination(appStatusDestination)
                .pubSubDomain(true)
                .autoStart(true)
                .build();
    }

    @Bean
    public HttpClient salEndpoint() {
        return CitrusEndpoints
                .http()
                .client()
                .requestUrl(env.getProperty("sal.api.url")+"/sal")
                .authentication(HttpAuthentication.basic(
                        env.getProperty("sal.api.username"),
                        env.getProperty("sal.api.password")))
                .build();
    }
}
