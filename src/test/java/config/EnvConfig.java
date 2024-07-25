package config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class EnvConfig {

    @Bean
    public static Dotenv dotenv() {
        return Dotenv.configure().ignoreIfMissing().load();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(ConfigurableEnvironment environment) {
        Dotenv dotenv = dotenv();
        Map<String, Object> envMap = new HashMap<>();

        dotenv.entries().forEach(entry -> envMap.put(entry.getKey(), entry.getValue()));

        MapPropertySource envPropertySource = new MapPropertySource("dotenv", envMap);
        environment.getPropertySources().addLast(envPropertySource);

        return new PropertySourcesPlaceholderConfigurer();
    }
}
