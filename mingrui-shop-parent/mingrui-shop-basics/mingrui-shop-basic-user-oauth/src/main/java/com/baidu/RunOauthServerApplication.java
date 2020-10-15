package com.baidu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * @ClassName RunOauthServerApplication
 * @Description: TODO
 * @Author luchenchen
 * @Date 2020/10/15
 * @Version V1.0
 **/
@SpringBootApplication
@EnableEurekaClient
public class RunOauthServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RunOauthServerApplication.class,args);
    }
}
