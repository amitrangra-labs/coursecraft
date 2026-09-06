package com.coursecraft;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CourseCraft backend entry point.
 *
 * <p>House style: strict hexagonal layout, explicit bean wiring only. Every bean is built by
 * hand in exactly three {@code @Configuration} classes — {@code DomainConfig},
 * {@code InboundConfig}, {@code OutboundConfig}. No {@code @Component}/{@code @Service}/
 * {@code @Repository}/{@code @Controller}, no {@code @Autowired}. HTTP is served via functional
 * routing (RouterFunctions), not annotated controllers.
 */
@SpringBootApplication(proxyBeanMethods = false)
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
