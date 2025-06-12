package org.jeecg.modules.system.strategy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LoginAndLogoutStrategyFactory {
    private final Map<Integer, ILoginAndLogoutStrategy> strategyMap = new HashMap<>();
    @Autowired
    public LoginAndLogoutStrategyFactory(
            @Qualifier("dbLoginStrategy") ILoginAndLogoutStrategy dbLoginStrategy,
            @Qualifier("redisHashLoginStrategy") ILoginAndLogoutStrategy redisHashLoginStrategy,
            @Qualifier("redisKeyValueStrategy") ILoginAndLogoutStrategy redisKeyValueStrategy
    ){
        strategyMap.put(1,dbLoginStrategy);
        strategyMap.put(2,redisHashLoginStrategy);
        strategyMap.put(3,redisKeyValueStrategy);
    }

    public ILoginAndLogoutStrategy getStrategy(int loginModel){
        return strategyMap.get(loginModel);
    }
}
