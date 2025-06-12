package org.jeecg.modules.system.test;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

public class SwitchServiceExecuteImpl implements ILoginModelService {

    Map<Integer, IExecuteService> executeServiceMap = new HashMap<Integer, IExecuteService>();


    @PostConstruct
    public void init() {
        executeServiceMap.put(1, new MysqlModelExecuteService());
        executeServiceMap.put(2, new RedisHashModelExecuteService());
        executeServiceMap.put(3, new RedisKeyValueModelExecuteService());
    }

    @Override
    public void execute(SwitchDTO dto) {
        executeServiceMap.get(dto.getType()).execute(dto);
    }
}
