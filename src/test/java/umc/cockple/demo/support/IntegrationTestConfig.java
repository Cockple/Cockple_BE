package umc.cockple.demo.support;

import com.google.firebase.messaging.FirebaseMessaging;
import com.redis.testcontainers.RedisContainer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.mockito.Mockito.mock;

@TestConfiguration(proxyBeanMethods = false)
public class IntegrationTestConfig {

    private static final MySQLContainer<?> mysql =
            new MySQLContainer<>("mysql:8.0.36")
                    .withConfigurationOverride("mysql-conf");

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

    @Bean
    @Primary
    RedisConnectionFactory testRedisConnectionFactory(RedisContainer redisContainer) {
        RedisStandaloneConfiguration configuration =
                new RedisStandaloneConfiguration(redisContainer.getHost(), redisContainer.getMappedPort(6379));
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    FirebaseMessaging firebaseMessaging() {
        return mock(FirebaseMessaging.class);
    }
}
