package com.coursecraft.domain.config;

import com.coursecraft.domain.service.ProfileService;
import com.coursecraft.port.UserStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * One of exactly three wiring classes. Builds the domain services by hand, injecting the
 * outbound ports (implemented in {@code OutboundConfig}). No component scanning, no
 * {@code @Autowired}.
 */
@Configuration(proxyBeanMethods = false)
public class DomainConfig {

    @Bean
    ProfileService profileService(UserStore userStore) {
        return new ProfileService(userStore);
    }
}
