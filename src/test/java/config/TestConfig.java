package config;

import jakarta.jms.ConnectionFactory;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;

@Configuration
@Import(EnvConfig.class)
@PropertySource(value = "classpath:application.yaml", factory = YamlPropertySourceFactory.class)
public class TestConfig {

    @Autowired
    private Environment env;

    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        String brokerUrl = env.getProperty("qpid-jms.url");
        String username = env.getProperty("qpid-jms.username");
        String password = env.getProperty("qpid-jms.password");

        if (brokerUrl == null || username == null || password == null) {
            throw new IllegalArgumentException("JMS Connection properties are not properly configured.");
        }

        JmsConnectionFactory connectionFactory = new JmsConnectionFactory(brokerUrl);
        connectionFactory.setUsername(username);
        connectionFactory.setPassword(password);
        return connectionFactory;
    }
}
