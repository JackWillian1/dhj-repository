package org.jeecg.modules.system.test;

import cn.hutool.core.util.StrUtil;
import org.springframework.stereotype.Service;

public class MysqlModelExecuteService implements IExecuteService {

    @Override
    public void execute(SwitchDTO dto) {
//
        System.out.println("mysql:" + dto);
        if (StrUtil.equals("login", dto.getEvent())) {
            login();
        }

    }

    private void login() {

        /// throw
    }

}
