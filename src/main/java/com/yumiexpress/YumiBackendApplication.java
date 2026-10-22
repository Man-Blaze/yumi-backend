package com.yumiexpress;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling; // <--- ADD THIS IMPORT


@SpringBootApplication
@EnableScheduling
public class YumiBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YumiBackendApplication.class, args);
    }

}
