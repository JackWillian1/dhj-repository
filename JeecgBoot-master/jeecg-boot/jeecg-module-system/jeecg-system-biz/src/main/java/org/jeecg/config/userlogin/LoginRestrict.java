package org.jeecg.config.userlogin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "myconfig")
@Data
public class LoginRestrict {
    private int failureCount;
    private int successCount;
    private int loginModel;
}
