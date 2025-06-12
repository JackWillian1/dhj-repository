package org.jeecg.modules.system.strategy;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.jeecg.modules.system.entity.SysUser;

public interface ILoginAndLogoutStrategy {
      Result<JSONObject> login(String syspassword, String userpassword, String username, SysUser sysUser);
      void logout(String username);
}
