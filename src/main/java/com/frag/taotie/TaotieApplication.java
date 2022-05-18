package com.frag.taotie;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @author liuhj
 */
@Slf4j
@SpringBootApplication
public class TaotieApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext ac = SpringApplication.run(TaotieApplication.class, args);
        ServerProperties server = ac.getBean(ServerProperties.class);
        log.info("tomcat配置 :: {}", server.getTomcat().getMaxThreads());
    }

}
