package com.github.messenger4j.quickstart.boot;

import com.github.messenger4j.MessengerPlatform;
import com.github.messenger4j.send.MessengerSendClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.util.Assert;

/**
 * Entry point for the Spring Boot Application.
 *
 * <p>
 * The Spring Context will be bootstrapped and the application will be configured properly.
 * In addition a {@code MessengerReceiveClientBuilder} and a {@code MessengerSendClientBuilder} will be exposed as
 * a singleton Spring Bean, so they are injectable.
 * </p>
 *
 * <p>
 * The injected configuration values can be found in the {@code application.properties} file.
 * As you can see in the {@code application.properties} file, by default the values are provided
 * through {@code environment variables}.
 * </p>
 *
 * @author Max Grabenhorst
 * @since 0.6.0
 */
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    @Bean
    public MessengerSendClient messengerSendClient(@Value("${messenger4j.pageAccessToken}") String pageAccessToken) {
        logger.debug("Initializing MessengerSendClient - pageAccessToken: {}", pageAccessToken);
        return MessengerPlatform.newSendClientBuilder(pageAccessToken).build();
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
