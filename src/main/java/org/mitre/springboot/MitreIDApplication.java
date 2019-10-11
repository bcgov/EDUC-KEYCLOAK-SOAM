package org.mitre.springboot;

import org.mitre.springboot.config.annotation.EnableOpenIDConnectServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Configuration;

@Configuration
@SpringBootApplication
@EnableOpenIDConnectServer
public class MitreIDApplication extends SpringBootServletInitializer{

    public static void main(final String[] args){
        SpringApplication.run(MitreIDApplication.class, args);
    }

}
