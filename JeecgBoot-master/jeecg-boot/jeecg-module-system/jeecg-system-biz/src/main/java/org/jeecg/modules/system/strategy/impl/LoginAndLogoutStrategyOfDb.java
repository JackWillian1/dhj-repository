package org.jeecg.modules.system.strategy.impl;

import com.alibaba.fastjson.JSONObject;
import org.jeecg.config.userlogin.LoginRestrict;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.service.impl.ExpandSysUserServiceImpl;
import org.jeecg.modules.system.strategy.ILoginAndLogoutStrategy;
import org.springframework.stereotype.Component;
import org.jeecg.common.api.vo.Result;

import javax.annotation.Resource;

@Component("dbLoginStrategy")
public class LoginAndLogoutStrategyOfDb implements ILoginAndLogoutStrategy {
    @Resource
    private ExpandSysUserServiceImpl expandSysUserServiceImpl;
    @Resource
    private LoginRestrict loginRestrict;

    @Override
    public Result<JSONObject> login(String syspassword, String userpassword, String username, SysUser sysUser) {
        Result<JSONObject> result = new Result<JSONObject>();

        if (syspassword.equals(userpassword) && expandSysUserServiceImpl.isTodayOrNot(sysUser) && sysUser.isExitLabel()) {
            result.error500("您当天主动退出登录，账户已被锁定，请勿继续尝试登录");
            return result;
        }
        if (sysUser.isAccountLock() && expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
            //记录当天暴力破解次数
            expandSysUserServiceImpl.recordForceAttacksCount(username);
            result.error500("账户已被锁定");
            return result;
        }

        if (!syspassword.equals(userpassword)) {

            if (expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
                //记录错误次数
                expandSysUserServiceImpl.recordFailureLoginCount(username);

                //记录时间
                expandSysUserServiceImpl.recordCurrentLoginTime(username);

                if (sysUser.getLoginFailureCount() + 1 >= loginRestrict.getFailureCount()) {
                    //账户上锁
                    expandSysUserServiceImpl.openUserLock(username);
                    result.error500("登录失败次数达到" + loginRestrict.getFailureCount() + "次，账户已被锁定");
                } else {
                    result.error500("用户名或密码错误");
                }
            } else {

                //清空非当天错误次数，错误次数置1
                expandSysUserServiceImpl.cleanFailureLoginCount(username, 1);
                //清空非当天的暴力破解次数，暴力破解次数置0
                expandSysUserServiceImpl.cleanForceAttacksCount(username, 0);
                //记录时间
                expandSysUserServiceImpl.recordCurrentLoginTime(username);

                result.error500("用户名或密码错误");
            }

            return result;
        }


        if (expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
            if (sysUser.getLoginSuccessCount() >= loginRestrict.getSuccessCount()) {

                //账户上锁 mysql
                expandSysUserServiceImpl.openUserLock(username);

                result.error500("账户当天仅可登录" + loginRestrict.getSuccessCount() + "次，次数已用完，账户已被锁定");

                return result;
            }
        } else {
            //关闭账户锁
            expandSysUserServiceImpl.closeUserLock(username);
            //重置登录标签
            expandSysUserServiceImpl.label(username, false);
            //清空非当天成功次数，成功次数置1
            expandSysUserServiceImpl.cleanSuccessLoginCount(username, 1);
            //清空非当天失败次数，次数置0
            expandSysUserServiceImpl.cleanFailureLoginCount(username, 0);
            //清空非当天暴力破解次数，次数置0
            expandSysUserServiceImpl.cleanForceAttacksCount(username, 0);
            //记录时间
            expandSysUserServiceImpl.recordCurrentLoginTime(username);
        }
        // 记录成功次数
        expandSysUserServiceImpl.recordSuccessLoginCount(username);
        // 记录时间
        expandSysUserServiceImpl.recordCurrentLoginTime(username);
        return null;
    }

    @Override
    public void logout(String username) {
        expandSysUserServiceImpl.openUserLock(username);
        expandSysUserServiceImpl.label(username, true);
    }
}
