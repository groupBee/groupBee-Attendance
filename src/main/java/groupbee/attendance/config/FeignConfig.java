package groupbee.attendance.config;

import feign.Logger;
import feign.RequestInterceptor;
import groupbee.attendance.interceptor.SessionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class FeignConfig {
    @Bean
    public RequestInterceptor sessionInterceptor() {
        return new SessionInterceptor();
    }

    @Bean
    public RequestInterceptor requestInterceptor() {
        return template -> {
            System.out.println("Request: " + template.url());
            template.headers().forEach((key, value) ->
                    System.out.println(key + ": " + value));
        };
    }

    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }
}
