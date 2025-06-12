package org.jeecg.modules.system.test;

import org.springframework.stereotype.Service;

public class RedisKeyValueModelExecuteService implements IExecuteService {

    @Override
    public void execute(SwitchDTO dto) {
//        ///
        System.out.println("redis key value:" + dto);

    }

}
