package com.vulnuris.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "gateway.security.jwt.secret=12345678901234567890123456789012"
})
class APIGatewayApplicationTests {

    @Test
    void contextLoads() {
    }
}
