package org.jeecg.modules.system.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.jeecg.modules.system.entity.SysUser;
import org.jeecg.modules.system.mapper.ExpandSysUserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
public class ExpandSysUserServiceImpl {

    @Autowired
    private ExpandSysUserMapper expandSysUserMapper;


    //开启账户锁
    public void openUserLock(String username) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("account_lock", true);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //关闭账户锁
    public void closeUserLock(String username) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("account_lock", false);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //记录成功次数
    public void recordSuccessLoginCount(String username) {
       UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
       updateWrapper.eq("username", username);
       updateWrapper.setSql("success_login_count = success_login_count + 1");
       expandSysUserMapper.update(null, updateWrapper);
    }

    //重置成功次数
    public void cleanSuccessLoginCount(String username,int count) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("success_login_count", count);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //记录失败次数

    @Transactional(rollbackFor = Exception.class)
    public void recordFailureLoginCount(String username) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.setSql("fail_login_count = fail_login_count + 1");
        expandSysUserMapper.update(null, updateWrapper);
    }

    //重置失败次数
    public void  cleanFailureLoginCount(String username,int count) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("fail_login_count", count);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //记录暴力破解次数
    public void recordForceAttacksCount(String username) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.setSql("force_attacks_count = force_attacks_count + 1");
        expandSysUserMapper.update(null, updateWrapper);

    }

    //重置暴力破解次数
    public void cleanForceAttacksCount(String username,int count) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("force_attacks_count", count);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //主动退出标签
    public void label(String username,boolean labelValue) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("exit_label", labelValue);
        expandSysUserMapper.update(null, updateWrapper);
    }

    //记录当前时间
    public void recordCurrentLoginTime(String username) {
        UpdateWrapper<SysUser> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("username", username);
        updateWrapper.set("current_login_time", DateUtil.now());
        expandSysUserMapper.update(null, updateWrapper);
    }

    //判断是否为同一天
    public boolean isTodayOrNot(SysUser sysUser) {
        //获取用户记录在数据库的时间
        Date currentLoginTime = sysUser.getCurrentLoginTime();
        //只提取年月日
        String dateStr = DateUtil.formatDate(currentLoginTime);
        //跟当前的系统时间进行比对，判断是否为同一天
        return DateUtil.today().equals(dateStr);
    }

}


