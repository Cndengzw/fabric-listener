package com.fabric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import tk.mybatis.spring.annotation.MapperScan;

@SpringBootApplication(exclude={HibernateJpaAutoConfiguration.class})
@MapperScan("com.fabric.display.mapper")
public class FabricApplication {

    public static void main(String[] args) {
        SpringApplication.run(FabricApplication.class, args);
    }

}
