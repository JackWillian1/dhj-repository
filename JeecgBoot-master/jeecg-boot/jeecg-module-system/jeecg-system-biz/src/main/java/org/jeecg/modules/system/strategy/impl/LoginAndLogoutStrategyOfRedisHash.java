package org.jeecg.modules.system.strategy.impl;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.common.api.vo.Result;
import org.jeecg.config.userlogin.LoginRestrict;
import org.jeecg.modules.monitor.service.impl.RedisServiceImpl;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.strategy.ILoginAndLogoutStrategy;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisHashLoginStrategy")
public class LoginAndLogoutStrategyOfRedisHash implements ILoginAndLogoutStrategy {

    @Resource
    private RedisServiceImpl redisServiceImpl;
    @Resource
    private LoginRestrict loginRestrict;

    @Override
    public Result<JSONObject> login(String syspassword, String userpassword, String username, SysUser sysUser) {
        Result<JSONObject> result = new Result<JSONObject>();
        if (redisServiceImpl.getExitlabelStatusOfHash(username)
                && redisServiceImpl.getAccountLockStatusOfHash(username)) {
            redisServiceImpl.recordForceAttacksCountOfHash(username);
            result.error500("您当天主动退出登录，账户已被锁定，请勿继续尝试登录");
            return result;
        } else if (redisServiceImpl.getFailureLoginCountOfHash(username) >= loginRestrict.getFailureCount()
                && redisServiceImpl.getAccountLockStatusOfHash(username)) {
            redisServiceImpl.recordForceAttacksCountOfHash(username);
            result.error500("您当天登录失败次数过多，账户已被锁定，请勿继续尝试登录");
            return result;
        } else if (redisServiceImpl.getSuccessLoginCountOfHash(username) >= loginRestrict.getSuccessCount()
                && redisServiceImpl.getAccountLockStatusOfHash(username)) {
            redisServiceImpl.recordForceAttacksCountOfHash(username);
            result.error500("您当天登录次数已用完，账户已被锁定，请勿继续尝试登录");
            return result;
        }


        if (redisServiceImpl.getSuccessLoginCountOfHash(username) >= loginRestrict.getSuccessCount()) {
            redisServiceImpl.setAccountLockOfHash(username, true);
            result.error500("账户当天仅可登录" + loginRestrict.getSuccessCount() + "次，次数已用完，账户已被锁定");
            return result;
        }
        if (!syspassword.equals(userpassword)) {
            redisServiceImpl.recordFailureLoginCountOfHash(username);
            if (redisServiceImpl.getFailureLoginCountOfHash(username) >= loginRestrict.getFailureCount()) {
                redisServiceImpl.setAccountLockOfHash(username, true);
                result.error500("登录失败次数达到" + loginRestrict.getFailureCount() + "次，账户已被锁定");
                return result;
            }
            result.error500("用户名或密码错误");
            return result;
        }
        redisServiceImpl.recordSuccessLoginCountOfHash(username);
        return null;
    }

    @Override
    public void logout(String username) {
        redisServiceImpl.setExitlabelStatusOfHash(username, true);
        redisServiceImpl.setAccountLockOfHash(username, true);
    }
}
