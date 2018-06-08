package com.github.messenger4j.quickstart.boot;

import com.github.messenger4j.Messenger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Entry point for the Spring Boot Application.
 *
 * <p>
 * The Spring Context will be bootstrapped and the application will be configured properly.
 * In addition a {@code MessengerSendClient} will be exposed as a singleton Spring Bean, so it is injectable.
 * </p>
 *
 * @author Max Grabenhorst
 */
@SpringBootApplication
public class Application {

    @Bean
    public Messenger messenger(@Value("${messenger4j.pageAccessToken}") String pageAccessToken,
                               @Value("${messenger4j.appSecret}") final String appSecret,
                               @Value("${messenger4j.verifyToken}") final String verifyToken) {
        return Messenger.create(pageAccessToken, appSecret, verifyToken);
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
