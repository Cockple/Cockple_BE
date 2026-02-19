package umc.cockple.demo.support;

import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0.36");

    private static final RedisContainer redis =
            new RedisContainer(DockerImageName.parse("redis:7.2-alpine"));

    static {
        mysql.start();
        redis.start();
    }

    @Bean
    @ServiceConnection
    MySQLContainer<?> mySQLContainer() {
        return mysql;
    }

    @Bean
    @ServiceConnection
    RedisContainer redisContainer() {
        return redis;
    }
}
