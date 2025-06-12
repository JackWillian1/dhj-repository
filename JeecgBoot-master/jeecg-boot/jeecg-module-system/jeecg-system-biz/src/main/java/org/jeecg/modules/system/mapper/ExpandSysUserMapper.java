package org.jeecg.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.jeecg.modules.system.entity.SysUser;

import javax.validation.constraints.Max;

@Mapper
public interface ExpandSysUserMapper extends BaseMapper<SysUser> {
}
