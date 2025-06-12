package org.jeecg.modules.system.controller;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONObject;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.jeecg.common.api.vo.Result;
import org.jeecg.common.constant.CacheConstant;
import org.jeecg.common.constant.CommonConstant;
import org.jeecg.common.constant.SymbolConstant;
import org.jeecg.common.constant.enums.DySmsEnum;
import org.jeecg.common.system.util.JwtUtil;
import org.jeecg.common.system.vo.LoginUser;
import org.jeecg.common.util.*;
import org.jeecg.common.util.encryption.EncryptedString;
import org.jeecg.config.JeecgBaseConfig;
import org.jeecg.config.userlogin.LoginRestrict;
import org.jeecg.modules.base.service.BaseCommonService;
import org.jeecg.modules.system.entity.SysDepart;
import org.jeecg.modules.system.entity.SysRoleIndex;
import org.jeecg.modules.system.entity.SysUser;

import org.jeecg.modules.system.model.SysLoginModel;
import org.jeecg.modules.system.service.*;
import org.jeecg.modules.system.service.impl.SysBaseApiImpl;
import org.jeecg.modules.system.strategy.ILoginAndLogoutStrategy;
import org.jeecg.modules.system.strategy.LoginAndLogoutStrategyFactory;
//import org.jeecg.modules.system.test.SwitchDTO;
import org.jeecg.modules.system.util.RandImageUtil;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.ParseException;
import java.util.*;

/**
 * @Author scott
 * @since 2018-12-17
 */
@RestController
@RequestMapping("/sys")
@Tag(name="用户登录")
@Slf4j
public class LoginController {
	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private ISysPermissionService sysPermissionService;
	@Autowired
	private SysBaseApiImpl sysBaseApi;
	@Autowired
	private ISysLogService logService;
	@Autowired
    private RedisUtil redisUtil;
	@Autowired
    private ISysDepartService sysDepartService;
	@Autowired
    private ISysDictService sysDictService;
	@Resource
	private BaseCommonService baseCommonService;
	@Autowired
	private JeecgBaseConfig jeecgBaseConfig;
/*	@Resource
	ILoginModelService loginModelService;*/

	//author：DHJ
	@Resource
	private LoginRestrict loginRestrict;
	@Resource
	private LoginAndLogoutStrategyFactory loginAndLogoutStrategyFactory;
	//

	private final String BASE_CHECK_CODES = "qwertyuiplkjhgfdsazxcvbnmQWERTYUPLKJHGFDSAZXCVBNM1234567890";
	@Operation(summary = "登录接口，三种校验模式： 1.MySQL、2.Redis Hash、3.Redis KEY VALUE")
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public Result<JSONObject> login(@RequestBody SysLoginModel sysLoginModel, HttpServletRequest request) throws ParseException {
		/*switch (login_model) {
			case 1:
				Result<JSONObject> result = new Result<JSONObject>();
				String username = sysLoginModel.getUsername();
				String password = sysLoginModel.getPassword();

				// step.1 验证码check
				String captcha = sysLoginModel.getCaptcha();
				if(captcha==null){
					result.error500("验证码无效");
					return result;
				}
				// 把字符串中所有的 英文字母 转换成 小写，其他字符（如数字）保持不变。
				String lowerCaseCaptcha = captcha.toLowerCase();
				// 加入密钥作为混淆，避免简单的拼接，被外部利用，用户自定义该密钥即可
				//update-begin---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				String keyPrefix = Md5Util.md5Encode(sysLoginModel.getCheckKey()+jeecgBaseConfig.getSignatureSecret(), "utf-8");
				String realKey = keyPrefix + lowerCaseCaptcha;
				//update-end---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				Object checkCode = redisUtil.get(realKey);
				//当进入登录页时，有一定几率出现验证码错误 #1714
				if(checkCode==null || !checkCode.toString().equals(lowerCaseCaptcha)) {
					log.warn("验证码错误，key= {} , Ui checkCode= {}, Redis checkCode = {}", sysLoginModel.getCheckKey(), lowerCaseCaptcha, checkCode);
					result.error500("验证码错误");
					// 改成特殊的code 便于前端判断
					result.setCode(HttpStatus.PRECONDITION_FAILED.value());
					return result;
				}

				// step.2 校验用户是否存在且有效
				// MyBatis-Plus提供的LambdaQueryWrapper
				LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
				// 构造查询条件，条件为：sys_user表中username字段值等于前端传进来的参数username
				queryWrapper.eq(SysUser::getUsername,username);
				// getOne 是 MyBatis-Plus 提供的一个方法，用于从数据库中查询单个记录，并将结果映射到指定的实体类中。
				SysUser sysUser = sysUserService.getOne(queryWrapper);
				result = sysUserService.checkUserIsEffective(sysUser);
				if(!result.isSuccess()) {
					return result;
				}

				// step.3 校验用户名或密码是否正确
				String userpassword = PasswordUtil.encrypt(username, password, sysUser.getSalt());
				String syspassword = sysUser.getPassword();

				// author: DHJ
				if(syspassword.equals(userpassword) && expandSysUserServiceImpl.isTodayOrNot(sysUser) && sysUser.isExitLabel()){
					result.error500("今天登录成功过，不允许再登录了");
					return result;
				}
				if (sysUser.isAccountLock() && expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
					//记录当天暴力破解次数
					expandSysUserServiceImpl.recordForceAttacksCount(username);
					result.error500("账户已被锁定");
					return result;
				}
				//
				if (!syspassword.equals(userpassword)) {
					addLoginFailOvertimes(username);
					//result.error500("用户名或密码错误");
					//author:DHJ
					if (expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
						//记录错误次数
						expandSysUserServiceImpl.recordFailureLoginCount(username);
						System.out.println("错误次数"+sysUser.getLoginFailureCount());
						//记录时间
						expandSysUserServiceImpl.recordCurrentLoginTime(username);

						if (sysUser.getLoginFailureCount() + 1>= loginRestrict.getFailureCount()) {
							//账户上锁
							expandSysUserServiceImpl.openUserLock(username);
							result.error500("用户名或密码错误,账户已被锁定");
						} else {
							result.error500("用户名或密码错误");
						}
					} else {
						//清空非当天错误次数，错误次数置1
						expandSysUserServiceImpl.cleanFailureLoginCount(username,1);
						//清空非当天的暴力破解次数，暴力破解次数置0
						expandSysUserServiceImpl.cleanForceAttacksCount(username,0);
						//记录时间
						expandSysUserServiceImpl.recordCurrentLoginTime(username);
						result.error500("用户名或密码错误");
					}
					//
					return result;
				}

				//author:DHJ
				if (expandSysUserServiceImpl.isTodayOrNot(sysUser)) {
					if (sysUser.getLoginSuccessCount() >= loginRestrict.getSuccessCount()) {
						//账户上锁 mysql
						expandSysUserServiceImpl.openUserLock(username);
						result.error500("当天只允许登录"+loginRestrict.getSuccessCount()+"次");
						return result;
					}
				} else {
					//关闭账户锁
					expandSysUserServiceImpl.closeUserLock(username);
					//重置登录标签
					expandSysUserServiceImpl.label(username,false);
					//清空非当天成功次数，成功次数置1
					expandSysUserServiceImpl.cleanSuccessLoginCount(username,1);
					//清空非当天失败次数，次数置0
					expandSysUserServiceImpl.cleanFailureLoginCount(username,0);
					//清空非当天暴力破解次数，次数置0
					expandSysUserServiceImpl.cleanForceAttacksCount(username,0);
					//记录时间
					expandSysUserServiceImpl.recordCurrentLoginTime(username);
				}

				// step.4  登录成功获取用户信息
				userInfo(sysUser, result, request);

				// step.5  登录成功删除验证码
				redisUtil.del(realKey);
				redisUtil.del(CommonConstant.LOGIN_FAIL + username);

				// step.6  记录用户登录日志
				LoginUser loginUser = new LoginUser();
				BeanUtils.copyProperties(sysUser, loginUser);
				// author: DHJ
				// 记录成功次数
				expandSysUserServiceImpl.recordSuccessLoginCount(username);
				// 记录时间
				expandSysUserServiceImpl.recordCurrentLoginTime(username);
				//
				baseCommonService.addLog("用户名: " + username + ",登录成功！", CommonConstant.LOG_TYPE_1, null,loginUser);

				// author: DHJ {"username":"admin","action":"login","timestamp":"loginUsr.getCreateTime"}
				// String loginTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(sysUser.getCurrentLoginTime());
				// 构造 JSON 字符串
				// String json = "{\"username\":\"" + loginUser.getUsername() + "\",\"action\":\"登录\",\"loginTime\":\"" + loginTime + "\"}";
				// System.out.println(json);
				// producer.sendLog(json);
				return result;

			case 2:
				Result<JSONObject> result2 = new Result<JSONObject>();
				String username2 = sysLoginModel.getUsername();
				String password2 = sysLoginModel.getPassword();

				if(redisServiceImpl.getExitlabelStatusOfHash(username2)
						&& redisServiceImpl.getAccountLockStatusOfHash(username2)) {
					redisServiceImpl.recordForceAttacksCountOfHash(username2);
					result2.error500("您当天主动退出登录，账户已被锁定，请勿继续尝试登录");
					return result2;
				} else if (redisServiceImpl.getFailureLoginCountOfHash(username2) >= loginRestrict.getFailureCount()
						&& redisServiceImpl.getAccountLockStatusOfHash(username2)) {
					redisServiceImpl.recordForceAttacksCountOfHash(username2);
					result2.error500("您当天登录失败次数过多，账户已被锁定，请勿继续尝试登录");
					return result2;
				} else if (redisServiceImpl.getSuccessLoginCountOfHash(username2) >= loginRestrict.getSuccessCount()
						&& redisServiceImpl.getAccountLockStatusOfHash(username2)) {
					redisServiceImpl.recordForceAttacksCountOfHash(username2);
					result2.error500("您当天登录次数已用完，账户已被锁定，请勿继续尝试登录");
					return result2;
				}

				// step.1 验证码check
				String captcha2 = sysLoginModel.getCaptcha();
				if(captcha2==null){
					result2.error500("验证码无效");
					return result2;
				}
				// 把字符串中所有的 英文字母 转换成 小写，其他字符（如数字）保持不变。
				String lowerCaseCaptcha2 = captcha2.toLowerCase();
				// 加入密钥作为混淆，避免简单的拼接，被外部利用，用户自定义该密钥即可
				//update-begin---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				String keyPrefix2 = Md5Util.md5Encode(sysLoginModel.getCheckKey()+jeecgBaseConfig.getSignatureSecret(), "utf-8");
				String realKey2 = keyPrefix2 + lowerCaseCaptcha2;
				//update-end---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				Object checkCode2 = redisUtil.get(realKey2);
				//当进入登录页时，有一定几率出现验证码错误 #1714
				if(checkCode2==null || !checkCode2.toString().equals(lowerCaseCaptcha2)) {
					log.warn("验证码错误，key= {} , Ui checkCode= {}, Redis checkCode = {}", sysLoginModel.getCheckKey(), lowerCaseCaptcha2, checkCode2);
					result2.error500("验证码错误");
					// 改成特殊的code 便于前端判断
					result2.setCode(HttpStatus.PRECONDITION_FAILED.value());
					return result2;
				}

				// step.2 校验用户是否存在且有效
				// MyBatis-Plus提供的LambdaQueryWrapper
				LambdaQueryWrapper<SysUser> queryWrapper2 = new LambdaQueryWrapper<>();
				// 构造查询条件，条件为：sys_user表中username字段值等于前端传进来的参数username
				queryWrapper2.eq(SysUser::getUsername,username2);
				// getOne 是 MyBatis-Plus 提供的一个方法，用于从数据库中查询单个记录，并将结果映射到指定的实体类中。
				SysUser sysUser2 = sysUserService.getOne(queryWrapper2);
				result2 = sysUserService.checkUserIsEffective(sysUser2);
				if(!result2.isSuccess()) {
					return result2;
				}

				// step.3 校验用户名或密码是否正确
				String userpassword2 = PasswordUtil.encrypt(username2, password2, sysUser2.getSalt());
				String syspassword2 = sysUser2.getPassword();

				if(redisServiceImpl.getSuccessLoginCountOfHash(username2) >= loginRestrict.getSuccessCount()){
					redisServiceImpl.setAccountLockOfHash(username2,true);
					result2.error500("账户当天仅可登录" + loginRestrict.getSuccessCount() + "次，次数已用完，账户已被锁定");
					return result2;
				}

				if (!syspassword2.equals(userpassword2)) {
					addLoginFailOvertimes(username2);
					redisServiceImpl.recordFailureLoginCountOfHash(username2);
					if(redisServiceImpl.getFailureLoginCountOfHash(username2) >= loginRestrict.getFailureCount()){
						redisServiceImpl.setAccountLockOfHash(username2,true);
						result2.error500("登录失败次数达到" + loginRestrict.getFailureCount() + "次，账户已被锁定");
						return result2;
					}
					result2.error500("账户名或密码错误");
					return result2;
				}

				///  查询数据库状态(当前情况，比如登录成功次数)
				///  记录当前次数(更新数据库)
				///  判断

				// step.4  登录成功获取用户信息
				userInfo(sysUser2, result2, request);

				// step.5  登录成功删除验证码
				redisUtil.del(realKey2);
				redisUtil.del(CommonConstant.LOGIN_FAIL + username2);

				// step.6  记录用户登录日志
				LoginUser loginUser2 = new LoginUser();
				BeanUtils.copyProperties(sysUser2, loginUser2);
				redisServiceImpl.recordSuccessLoginCountOfHash(username2);
				baseCommonService.addLog("用户名: " + username2 + ",登录成功！", CommonConstant.LOG_TYPE_1, null,loginUser2);
				return result2;

			case 3:
				Result<JSONObject> result3 = new Result<JSONObject>();
				String username3 = sysLoginModel.getUsername();
				String password3 = sysLoginModel.getPassword();
				// author:DHJ
				// 账户被锁定后不再进行密码校验并累计暴力攻击次数
				if(redisServiceImpl.getExitLabel(username3) && redisServiceImpl.getAccountLockStatus(username3)){
					redisServiceImpl.forceAttackLoginCount(username3);
					result3.error500("账户主动退出，已被锁定，明天再试");
					return result3;
				}
				else if (redisServiceImpl.getAccountLockStatus(username3)) {
					redisServiceImpl.forceAttackLoginCount(username3);
					result3.error500("账户已被锁定，明天再试");
					return result3;
				}
				//
				// step.1 验证码check
				String captcha3 = sysLoginModel.getCaptcha();
				if(captcha3==null){
					result3.error500("验证码无效");
					return result3;
				}
				// 把字符串中所有的 英文字母 转换成 小写，其他字符（如数字）保持不变。
				String lowerCaseCaptcha3 = captcha3.toLowerCase();
				// 加入密钥作为混淆，避免简单的拼接，被外部利用，用户自定义该密钥即可
				// update-begin---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				String keyPrefix3 = Md5Util.md5Encode(sysLoginModel.getCheckKey()+jeecgBaseConfig.getSignatureSecret(), "utf-8");
				String realKey3 = keyPrefix3 + lowerCaseCaptcha3;
				// update-end---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
				Object checkCode3 = redisUtil.get(realKey3);
				// 当进入登录页时，有一定几率出现验证码错误 #1714
				if(checkCode3==null || !checkCode3.toString().equals(lowerCaseCaptcha3)) {
					log.warn("验证码错误，key= {} , Ui checkCode= {}, Redis checkCode = {}", sysLoginModel.getCheckKey(), lowerCaseCaptcha3, checkCode3);
					result3.error500("验证码错误");
					// 改成特殊的code 便于前端判断
					result3.setCode(HttpStatus.PRECONDITION_FAILED.value());
					return result3;
				}
				// step.2 校验用户是否存在且有效
				// MyBatis-Plus提供的 LambdaQueryWrapper
				LambdaQueryWrapper<SysUser> queryWrapper3 = new LambdaQueryWrapper<>();
				// 构造查询条件，条件为：sys_user表中 username字段值等于前端传进来的参数 username
				queryWrapper3.eq(SysUser::getUsername,username3);
				// getOne 是 MyBatis-Plus 提供的一个方法，用于从数据库中查询单个记录，并将结果映射到指定的实体类中。
				SysUser sysUser3 = sysUserService.getOne(queryWrapper3);
				result3 = sysUserService.checkUserIsEffective(sysUser3);
				if(!result3.isSuccess()) {
					return result3;
				}
				// step.3 校验用户名或密码是否正确，并对登录次数进行限制
				String userpassword3 = PasswordUtil.encrypt(username3, password3, sysUser3.getSalt());
				String syspassword3 = sysUser3.getPassword();
				// author:DHJ
				// 限制一天可登录次数
				if (redisServiceImpl.getValueOfSuccessLoginCount(username3)>= loginRestrict.getSuccessCount()){
					redisServiceImpl.setAccountLock(username3,"true");
					result3.error500("账户今天登录次数超过"+loginRestrict.getSuccessCount()+"次，账户已被锁定，明天再试");
					return result3;
				}
				//
				if (!syspassword3.equals(userpassword3)) {
					addLoginFailOvertimes(username3);
					// author:DHJ
					// 统计当天登录失败次数
					redisServiceImpl.failureLoginCount(username3);
					// 限制一天允许尝试登录的次数
					if(redisServiceImpl.getValueOfFailureLoginCount(username3)>=loginRestrict.getFailureCount()){
						redisServiceImpl.setAccountLock(username3,"true");
						result3.error500("账户今天登录失败次数超过"+loginRestrict.getFailureCount()+"次，账户已被锁定，明天再试");
						return result3;
					}
					//
					result3.error500("用户名或密码错误");
					return result3;
				}
				// step.4  登录成功获取用户信息
				userInfo(sysUser3, result3, request);
				// step.5  登录成功删除验证码
				redisUtil.del(realKey3);
				redisUtil.del(CommonConstant.LOGIN_FAIL + username3);
				// step.6  记录用户登录日志
				LoginUser loginUser3 = new LoginUser();
				BeanUtils.copyProperties(sysUser3, loginUser3);
				// author DHJ
				// 统计当天登录成功次数
				redisServiceImpl.successLoginCount(username3);
				//
				baseCommonService.addLog("用户名: " + username3 + ",登录成功！", CommonConstant.LOG_TYPE_1, null,loginUser3);
				return result3;
			default:
				return null;
		}*/

		Result<JSONObject> result = new Result<JSONObject>();
		String username = sysLoginModel.getUsername();
		String password = sysLoginModel.getPassword();

		// step.1 验证码check
		String captcha = sysLoginModel.getCaptcha();
		if(captcha==null){
			result.error500("验证码无效");
			return result;
		}
		// 把字符串中所有的 英文字母 转换成 小写，其他字符（如数字）保持不变。
		String lowerCaseCaptcha = captcha.toLowerCase();
		// 加入密钥作为混淆，避免简单的拼接，被外部利用，用户自定义该密钥即可
		//update-begin---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
		String keyPrefix = Md5Util.md5Encode(sysLoginModel.getCheckKey()+jeecgBaseConfig.getSignatureSecret(), "utf-8");
		String realKey = keyPrefix + lowerCaseCaptcha;
		//update-end---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
		Object checkCode = redisUtil.get(realKey);
		//当进入登录页时，有一定几率出现验证码错误 #1714
		if(checkCode==null || !checkCode.toString().equals(lowerCaseCaptcha)) {
			log.warn("验证码错误，key= {} , Ui checkCode= {}, Redis checkCode = {}", sysLoginModel.getCheckKey(), lowerCaseCaptcha, checkCode);
			result.error500("验证码错误");
			// 改成特殊的code 便于前端判断
			result.setCode(HttpStatus.PRECONDITION_FAILED.value());
			return result;
		}

		// step.2 校验用户是否存在且有效
		// MyBatis-Plus提供的LambdaQueryWrapper
		LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<>();
		// 构造查询条件，条件为：sys_user表中username字段值等于前端传进来的参数username
		queryWrapper.eq(SysUser::getUsername,username);
		// getOne 是 MyBatis-Plus 提供的一个方法，用于从数据库中查询单个记录，并将结果映射到指定的实体类中。
		SysUser sysUser = sysUserService.getOne(queryWrapper);
		result = sysUserService.checkUserIsEffective(sysUser);
		if(!result.isSuccess()) {
			return result;
		}

		// step.3 校验用户名或密码是否正确
		String userpassword = PasswordUtil.encrypt(username, password, sysUser.getSalt());
		String syspassword = sysUser.getPassword();


		/*author:DHJ
			功能：登录功能限制，限制一天登录失败或成功登录的次数，超过该次数锁定账号
			采用接口 + 登录策略模式 从 1、MySQL，2、Redis Hash，3、Redis Key-value3种登录模式中选择一种登录模式
		* */
		ILoginAndLogoutStrategy loginStrategy = loginAndLogoutStrategyFactory.getStrategy(loginRestrict.getLoginModel());
		if(loginStrategy!=null){
			Result tempResult = loginStrategy.login(syspassword,userpassword,username,sysUser);
			if(tempResult != null){
				return tempResult;
			}
		}

		// step.4  登录成功获取用户信息
		userInfo(sysUser, result, request);

		// step.5  登录成功删除验证码
		redisUtil.del(realKey);
		redisUtil.del(CommonConstant.LOGIN_FAIL + username);

		// step.6  记录用户登录日志
		LoginUser loginUser = new LoginUser();
		BeanUtils.copyProperties(sysUser, loginUser);
		baseCommonService.addLog("用户名: " + username + ",登录成功！", CommonConstant.LOG_TYPE_1, null,loginUser);
		return result;

	}

	/**
	 * 【vue3专用】获取用户信息
	 */
	@GetMapping("/user/getUserInfo")
	public Result<JSONObject> getUserInfo(HttpServletRequest request){
		long start = System.currentTimeMillis();
		Result<JSONObject> result = new Result<JSONObject>();
		String  username = JwtUtil.getUserNameByToken(request);
		if(oConvertUtils.isNotEmpty(username)) {
			// 根据用户名查询用户信息
			SysUser sysUser = sysUserService.getUserByName(username);
			JSONObject obj=new JSONObject();
			log.info("1 获取用户信息耗时（用户基础信息）" + (System.currentTimeMillis() - start) + "毫秒");

			//update-begin---author:scott ---date:2022-06-20  for：vue3前端，支持自定义首页-----------
			String vue3Version = request.getHeader(CommonConstant.VERSION);
			//update-begin---author:liusq ---date:2022-06-29  for：接口返回值修改，同步修改这里的判断逻辑-----------
			SysRoleIndex roleIndex = sysUserService.getDynamicIndexByUserRole(username, vue3Version);
			if (oConvertUtils.isNotEmpty(vue3Version) && roleIndex != null && oConvertUtils.isNotEmpty(roleIndex.getUrl())) {
				String homePath = roleIndex.getUrl();
				if (!homePath.startsWith(SymbolConstant.SINGLE_SLASH)) {
					homePath = SymbolConstant.SINGLE_SLASH + homePath;
				}
				sysUser.setHomePath(homePath);
			}
			//update-begin---author:liusq ---date:2022-06-29  for：接口返回值修改，同步修改这里的判断逻辑-----------
			//update-end---author:scott ---date::2022-06-20  for：vue3前端，支持自定义首页--------------
			log.info("2 获取用户信息耗时 (首页面配置)" + (System.currentTimeMillis() - start) + "毫秒");
			
			obj.put("userInfo",sysUser);
			obj.put("sysAllDictItems", sysDictService.queryAllDictItems());
			log.info("3 获取用户信息耗时 (字典数据)" + (System.currentTimeMillis() - start) + "毫秒");
			
			result.setResult(obj);
			result.success("");
		}
		log.info("end 获取用户信息耗时 " + (System.currentTimeMillis() - start) + "毫秒");
		return result;

	}


	/**
	 * 退出登录
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/logout")
	public Result<Object> logout(HttpServletRequest request,HttpServletResponse response) {
		//用户退出逻辑
	    String token = request.getHeader(CommonConstant.X_ACCESS_TOKEN);
	    if(oConvertUtils.isEmpty(token)) {
	    	return Result.error("退出登录失败！");
	    }
	    String username = JwtUtil.getUsername(token);
		LoginUser sysUser = sysBaseApi.getUserByName(username);
	    if(sysUser!=null) {
			//update-begin--Author:wangshuai  Date:20200714  for：登出日志没有记录人员
			baseCommonService.addLog("用户名: "+sysUser.getRealname()+",退出成功！", CommonConstant.LOG_TYPE_1, null,sysUser);
			//update-end--Author:wangshuai  Date:20200714  for：登出日志没有记录人员



			// zlei23
			/*
			SwitchDTO dto = new SwitchDTO();
			dto.setId(sysUser.getId());
			dto.setName(sysUser.getUsername());
			dto.setType(login_model);
			loginModelService.execute(dto);
			*/
			//

			// author:DHJ
			/*switch (login_model) {
				case 1:
					// 采用 MySQL
					// 主动退出则开启账户锁，并且把退出标签值置true
					expandSysUserServiceImpl.openUserLock(username);
					expandSysUserServiceImpl.label(username,true);
					break;
				case 2:
					// 对应采用 Redis 哈希表 形式的登录接口
					redisServiceImpl.setExitlabelStatusOfHash(username,true);
					redisServiceImpl.setAccountLockOfHash(username,true);
					break;
				case 3:
					// 对应采用 Redis key value 形式的登录接口
					redisServiceImpl.setAccountLock(username,"true");
					redisServiceImpl.setExitLabel(username,"true");
					break;
				default:
					break;
			}*/
			ILoginAndLogoutStrategy loginStrategy = loginAndLogoutStrategyFactory.getStrategy(loginRestrict.getLoginModel());
			if(loginStrategy != null){
				loginStrategy.logout(username);
			}
			//

	    	log.info(" 用户名:  "+sysUser.getRealname()+",退出成功！ ");
	    	//清空用户登录Token缓存
	    	redisUtil.del(CommonConstant.PREFIX_USER_TOKEN + token);
	    	//清空用户登录Shiro权限缓存
			redisUtil.del(CommonConstant.PREFIX_USER_SHIRO_CACHE + sysUser.getId());
			//清空用户的缓存信息（包括部门信息），例如sys:cache:user::<username>
			redisUtil.del(String.format("%s::%s", CacheConstant.SYS_USERS_CACHE, sysUser.getUsername()));
			//调用shiro的logout
			SecurityUtils.getSubject().logout();
	    	return Result.ok("退出登录成功！");
	    }else {
	    	return Result.error("Token无效!");
	    }
	}


	/**
	 * 获取访问量
	 * @return
	 */
	@GetMapping("loginfo")
	public Result<JSONObject> loginfo() {
		Result<JSONObject> result = new Result<JSONObject>();
		JSONObject obj = new JSONObject();
		//update-begin--Author:zhangweijian  Date:20190428 for：传入开始时间，结束时间参数
		// 获取一天的开始和结束时间
		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.HOUR_OF_DAY, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		calendar.set(Calendar.MILLISECOND, 0);
		Date dayStart = calendar.getTime();
		calendar.add(Calendar.DATE, 1);
		Date dayEnd = calendar.getTime();
		// 获取系统访问记录
		Long totalVisitCount = logService.findTotalVisitCount();
		obj.put("totalVisitCount", totalVisitCount);
		Long todayVisitCount = logService.findTodayVisitCount(dayStart,dayEnd);
		obj.put("todayVisitCount", todayVisitCount);
		Long todayIp = logService.findTodayIp(dayStart,dayEnd);
		//update-end--Author:zhangweijian  Date:20190428 for：传入开始时间，结束时间参数
		obj.put("todayIp", todayIp);
		result.setResult(obj);
		result.success("登录成功");
		return result;
	}


	/**
	 * 获取访问量
	 * @return
	 */
	@GetMapping("/visitInfo")
	public Result<List<Map<String,Object>>> visitInfo() {
		Result<List<Map<String,Object>>> result = new Result<List<Map<String,Object>>>();
		Calendar calendar = new GregorianCalendar();
		calendar.set(Calendar.HOUR_OF_DAY,0);
        calendar.set(Calendar.MINUTE,0);
        calendar.set(Calendar.SECOND,0);
        calendar.set(Calendar.MILLISECOND,0);
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        Date dayEnd = calendar.getTime();
        calendar.add(Calendar.DAY_OF_MONTH, -7);
        Date dayStart = calendar.getTime();
        List<Map<String,Object>> list = logService.findVisitCount(dayStart, dayEnd);
		result.setResult(oConvertUtils.toLowerCasePageList(list));
		return result;
	}
	
	
	/**
	 * 登陆成功选择用户当前部门
	 * @param user
	 * @return
	 */
	@RequestMapping(value = "/selectDepart", method = RequestMethod.PUT)
	public Result<JSONObject> selectDepart(@RequestBody SysUser user) {
		Result<JSONObject> result = new Result<JSONObject>();
		String username = user.getUsername();
		if(oConvertUtils.isEmpty(username)) {
			LoginUser sysUser = (LoginUser)SecurityUtils.getSubject().getPrincipal();
			username = sysUser.getUsername();
		}
		
		//获取登录部门
		String orgCode= user.getOrgCode();
		//获取登录租户
		Integer tenantId = user.getLoginTenantId();
		//设置用户登录部门和登录租户
		this.sysUserService.updateUserDepart(username, orgCode,tenantId);
		SysUser sysUser = sysUserService.getUserByName(username);
		JSONObject obj = new JSONObject();
		obj.put("userInfo", sysUser);
		result.setResult(obj);
		return result;
	}

	/**
	 * 短信登录接口
	 * 
	 * @param jsonObject
	 * @return
	 */
	@PostMapping(value = "/sms")
	public Result<String> sms(@RequestBody JSONObject jsonObject,HttpServletRequest request) {
		Result<String> result = new Result<String>();
		String clientIp = IpUtils.getIpAddr(request);
		String mobile = jsonObject.get("mobile").toString();
		//手机号模式 登录模式: "2"  注册模式: "1"
		String smsmode=jsonObject.get("smsmode").toString();
		log.info("-------- IP:{}, 手机号：{}，获取绑定验证码", clientIp, mobile);
		
		if(oConvertUtils.isEmpty(mobile)){
			result.setMessage("手机号不允许为空！");
			result.setSuccess(false);
			return result;
		}
		
		//update-begin-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
		String redisKey = CommonConstant.PHONE_REDIS_KEY_PRE+mobile;
		Object object = redisUtil.get(redisKey);
		//update-end-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
		
		if (object != null) {
			result.setMessage("验证码10分钟内，仍然有效！");
			result.setSuccess(false);
			return result;
		}

		//-------------------------------------------------------------------------------------
		//增加 check防止恶意刷短信接口
		if(!DySmsLimit.canSendSms(clientIp)){
			log.warn("--------[警告] IP地址:{}, 短信接口请求太多-------", clientIp);
			result.setMessage("短信接口请求太多，请稍后再试！");
			result.setCode(CommonConstant.PHONE_SMS_FAIL_CODE);
			result.setSuccess(false);
			return result;
		}
		//-------------------------------------------------------------------------------------

		//随机数
		String captcha = RandomUtil.randomNumbers(6);
		JSONObject obj = new JSONObject();
    	obj.put("code", captcha);
		try {
			boolean b = false;
			//注册模板
			if (CommonConstant.SMS_TPL_TYPE_1.equals(smsmode)) {
				SysUser sysUser = sysUserService.getUserByPhone(mobile);
				if(sysUser!=null) {
					result.error500(" 手机号已经注册，请直接登录！");
					baseCommonService.addLog("手机号已经注册，请直接登录！", CommonConstant.LOG_TYPE_1, null);
					return result;
				}
				b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.REGISTER_TEMPLATE_CODE);
			}else {
				//登录模式，校验用户有效性
				SysUser sysUser = sysUserService.getUserByPhone(mobile);
				result = sysUserService.checkUserIsEffective(sysUser);
				if(!result.isSuccess()) {
					String message = result.getMessage();
					String userNotExist="该用户不存在，请注册";
					if(userNotExist.equals(message)){
						result.error500("该用户不存在或未绑定手机号");
					}
					return result;
				}
				
				/**
				 * smsmode 短信模板方式  0 .登录模板、1.注册模板、2.忘记密码模板
				 */
				if (CommonConstant.SMS_TPL_TYPE_0.equals(smsmode)) {
					//登录模板
					b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.LOGIN_TEMPLATE_CODE);
				} else if(CommonConstant.SMS_TPL_TYPE_2.equals(smsmode)) {
					//忘记密码模板
					b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.FORGET_PASSWORD_TEMPLATE_CODE);
				}
			}

			if (b == false) {
				result.setMessage("短信验证码发送失败,请稍后重试");
				result.setSuccess(false);
				return result;
			}
			
			//update-begin-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
			//验证码10分钟内有效
			redisUtil.set(redisKey, captcha, 600);
			//update-end-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
			
			//update-begin--Author:scott  Date:20190812 for：issues#391
			//result.setResult(captcha);
			//update-end--Author:scott  Date:20190812 for：issues#391
			result.setSuccess(true);

		} catch (ClientException e) {
			e.printStackTrace();
			result.error500(" 短信接口未配置，请联系管理员！");
			return result;
		}
		return result;
	}
	

	/**
	 * 手机号登录接口
	 * 
	 * @param jsonObject
	 * @return
	 */
	@Operation(summary = "手机号登录接口")
	@PostMapping("/phoneLogin")
	public Result<JSONObject> phoneLogin(@RequestBody JSONObject jsonObject, HttpServletRequest request) {
		Result<JSONObject> result = new Result<JSONObject>();
		String phone = jsonObject.getString("mobile");
		//update-begin-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
		if(isLoginFailOvertimes(phone)){
			return result.error500("该用户登录失败次数过多，请于10分钟后再次登录！");
		}
		//update-end-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
		//校验用户有效性
		SysUser sysUser = sysUserService.getUserByPhone(phone);
		result = sysUserService.checkUserIsEffective(sysUser);
		if(!result.isSuccess()) {
			return result;
		}
		
		String smscode = jsonObject.getString("captcha");

		//update-begin-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
		String redisKey = CommonConstant.PHONE_REDIS_KEY_PRE+phone;
		Object code = redisUtil.get(redisKey);
		//update-end-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906

		if (!smscode.equals(code)) {
			//update-begin-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
			addLoginFailOvertimes(phone);
			//update-end-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
			return Result.error("手机验证码错误");
		}
		//用户信息
		userInfo(sysUser, result, request);
		//添加日志
		baseCommonService.addLog("用户名: " + sysUser.getUsername() + ",登录成功！", CommonConstant.LOG_TYPE_1, null);

		return result;
	}


	/**
	 * 用户信息
	 *
	 * @param sysUser
	 * @param result
	 * @return
	 */
	private Result<JSONObject> userInfo(SysUser sysUser, Result<JSONObject> result, HttpServletRequest request) {
		String username = sysUser.getUsername();
		String syspassword = sysUser.getPassword();
		// 获取用户部门信息
		JSONObject obj = new JSONObject(new LinkedHashMap<>());

		//1.生成token
		String token = JwtUtil.sign(username, syspassword);
		// 设置token缓存有效时间
		redisUtil.set(CommonConstant.PREFIX_USER_TOKEN + token, token);
		redisUtil.expire(CommonConstant.PREFIX_USER_TOKEN + token, JwtUtil.EXPIRE_TIME * 2 / 1000);
		obj.put("token", token);

		//2.设置登录租户
		Result<JSONObject> loginTenantError = sysUserService.setLoginTenant(sysUser, obj, username,result);
		if (loginTenantError != null) {
			return loginTenantError;
		}

		//3.设置登录用户信息
		obj.put("userInfo", sysUser);
		
		//4.设置登录部门
		List<SysDepart> departs = sysDepartService.queryUserDeparts(sysUser.getId());
		obj.put("departs", departs);
		if (departs == null || departs.size() == 0) {
			obj.put("multi_depart", 0);
		} else if (departs.size() == 1) {
			sysUserService.updateUserDepart(username, departs.get(0).getOrgCode(),null);
			obj.put("multi_depart", 1);
		} else {
			//查询当前是否有登录部门
			// update-begin--Author:wangshuai Date:20200805 for：如果用戶为选择部门，数据库为存在上一次登录部门，则取一条存进去
			SysUser sysUserById = sysUserService.getById(sysUser.getId());
			if(oConvertUtils.isEmpty(sysUserById.getOrgCode())){
				sysUserService.updateUserDepart(username, departs.get(0).getOrgCode(),null);
			}
			// update-end--Author:wangshuai Date:20200805 for：如果用戶为选择部门，数据库为存在上一次登录部门，则取一条存进去
			obj.put("multi_depart", 2);
		}

		//update-begin---author:scott ---date:2024-01-05  for：【QQYUN-7802】前端在登录时加载了两次数据字典，建议优化下，避免数据字典太多时可能产生的性能问题 #956---
		// login接口，在vue3前端下不加载字典数据，vue2下加载字典
		String vue3Version = request.getHeader(CommonConstant.VERSION);
		if(oConvertUtils.isEmpty(vue3Version)){
			obj.put("sysAllDictItems", sysDictService.queryAllDictItems());
		}
		//end-begin---author:scott ---date:2024-01-05  for：【QQYUN-7802】前端在登录时加载了两次数据字典，建议优化下，避免数据字典太多时可能产生的性能问题 #956---
		
		result.setResult(obj);
		result.success("登录成功");
		return result;
	}

	/**
	 * 获取加密字符串
	 * @return
	 */
	@GetMapping(value = "/getEncryptedString")
	public Result<Map<String,String>> getEncryptedString(){
		Result<Map<String,String>> result = new Result<Map<String,String>>();
		Map<String,String> map = new HashMap(5);
		map.put("key", EncryptedString.key);
		map.put("iv",EncryptedString.iv);
		result.setResult(map);
		return result;
	}

	/**
	 * 后台生成图形验证码 ：有效
	 * @param response
	 * @param key
	 */
	@Operation(summary = "获取验证码")
	@GetMapping(value = "/randomImage/{key}")
	public Result<String> randomImage(HttpServletResponse response,@PathVariable("key") String key){
		Result<String> res = new Result<String>();
		try {
			//生成验证码
			String code = RandomUtil.randomString(BASE_CHECK_CODES,4);
			//存到redis中
			String lowerCaseCode = code.toLowerCase();
			
			//update-begin-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
			// 加入密钥作为混淆，避免简单的拼接，被外部利用，用户自定义该密钥即可
			//update-begin---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
			String keyPrefix = Md5Util.md5Encode(key+jeecgBaseConfig.getSignatureSecret(), "utf-8");
			String realKey = keyPrefix + lowerCaseCode;
			//update-end-author:taoyan date:2022-9-13 for: VUEN-2245 【漏洞】发现新漏洞待处理20220906
			redisUtil.removeAll(keyPrefix);
			//update-end---author:chenrui ---date:20250107  for：[QQYUN-10775]验证码可以复用 #7674------------
			redisUtil.set(realKey, lowerCaseCode, 60);
			log.info("获取验证码，Redis key = {}，checkCode = {}", realKey, code);
			//返回前端
			String base64 = RandImageUtil.generate(code);
			res.setSuccess(true);
			res.setResult(base64);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			res.error500("获取验证码失败,请检查redis配置!");
			return res;
		}
		return res;
	}

	/**
	 * 切换菜单表为vue3的表
	 */
	@RequiresRoles({"admin"})
	@GetMapping(value = "/switchVue3Menu")
	public Result<String> switchVue3Menu(HttpServletResponse response) {
		Result<String> res = new Result<String>();	
		sysPermissionService.switchVue3Menu();
		return res;
	}
	
	/**
	 * app登录
	 * @param sysLoginModel
	 * @return
	 * @throws Exception
	 */
	@RequestMapping(value = "/mLogin", method = RequestMethod.POST)
	public Result<JSONObject> mLogin(@RequestBody SysLoginModel sysLoginModel) throws Exception {
		Result<JSONObject> result = new Result<JSONObject>();
		String username = sysLoginModel.getUsername();
		String password = sysLoginModel.getPassword();
		JSONObject obj = new JSONObject();
		
		//update-begin-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
		if(isLoginFailOvertimes(username)){
			return result.error500("该用户登录失败次数过多，请于10分钟后再次登录！");
		}
		//update-end-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
		//1. 校验用户是否有效
		SysUser sysUser = sysUserService.getUserByName(username);
		result = sysUserService.checkUserIsEffective(sysUser);
		if(!result.isSuccess()) {
			return result;
		}
		
		//2. 校验用户名或密码是否正确
		String userpassword = PasswordUtil.encrypt(username, password, sysUser.getSalt());
		String syspassword = sysUser.getPassword();
		if (!syspassword.equals(userpassword)) {
			//update-begin-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
			addLoginFailOvertimes(username);
			//update-end-author:taoyan date:2022-11-7 for: issues/4109 平台用户登录失败锁定用户
			result.error500("用户名或密码错误");
			return result;
		}
		
		//3.设置登录部门
		String orgCode = sysUser.getOrgCode();
		if(oConvertUtils.isEmpty(orgCode)) {
			//如果当前用户无选择部门 查看部门关联信息
			
			List<SysDepart> departs = sysDepartService.queryUserDeparts(sysUser.getId());
			//update-begin-author:taoyan date:20220117 for: JTC-1068【app】新建用户，没有设置部门及角色，点击登录提示暂未归属部，一直在登录页面 使用手机号登录 可正常
			if (departs == null || departs.size() == 0) {
				/*result.error500("用户暂未归属部门,不可登录!");
				
				return result;*/
			}else{
				orgCode = departs.get(0).getOrgCode();
				sysUser.setOrgCode(orgCode);
				this.sysUserService.updateUserDepart(username, orgCode,null);
			}
			//update-end-author:taoyan date:20220117 for: JTC-1068【app】新建用户，没有设置部门及角色，点击登录提示暂未归属部，一直在登录页面 使用手机号登录 可正常
		}

		//4. 设置登录租户
		Result<JSONObject> loginTenantError = sysUserService.setLoginTenant(sysUser, obj, username, result);
		if (loginTenantError != null) {
			return loginTenantError;
		}

		//5. 设置登录用户信息
		obj.put("userInfo", sysUser);
		
		//6. 生成token
		String token = JwtUtil.sign(username, syspassword);
		// 设置超时时间
		redisUtil.set(CommonConstant.PREFIX_USER_TOKEN + token, token);
		redisUtil.expire(CommonConstant.PREFIX_USER_TOKEN + token, JwtUtil.EXPIRE_TIME*2 / 1000);

		//token 信息
		obj.put("token", token);
		result.setResult(obj);
		result.setSuccess(true);
		result.setCode(200);
		baseCommonService.addLog("用户名: " + username + ",登录成功[移动端]！", CommonConstant.LOG_TYPE_1, null);
		return result;
	}

	/**
	 * 图形验证码
	 * @param sysLoginModel
	 * @return
	 */
	@RequestMapping(value = "/checkCaptcha", method = RequestMethod.POST)
	public Result<?> checkCaptcha(@RequestBody SysLoginModel sysLoginModel){
		String captcha = sysLoginModel.getCaptcha();
		String checkKey = sysLoginModel.getCheckKey();
		if(captcha==null){
			return Result.error("验证码无效");
		}
		String lowerCaseCaptcha = captcha.toLowerCase();
		String realKey = Md5Util.md5Encode(lowerCaseCaptcha+checkKey, "utf-8");
		Object checkCode = redisUtil.get(realKey);
		if(checkCode==null || !checkCode.equals(lowerCaseCaptcha)) {
			return Result.error("验证码错误");
		}
		return Result.ok();
	}

	/**
	 * 登录二维码
	 */
	@Operation(summary = "登录二维码")
	@GetMapping("/getLoginQrcode")
	public Result<?>  getLoginQrcode() {
		String qrcodeId = CommonConstant.LOGIN_QRCODE_PRE+IdWorker.getIdStr();
		//定义二维码参数
		Map params = new HashMap(5);
		params.put("qrcodeId", qrcodeId);
		//存放二维码唯一标识30秒有效
		redisUtil.set(CommonConstant.LOGIN_QRCODE + qrcodeId, qrcodeId, 30);
		return Result.OK(params);
	}

	/**
	 * 扫码二维码
	 */
	@Operation(summary = "扫码登录二维码")
	@PostMapping("/scanLoginQrcode")
	public Result<?> scanLoginQrcode(@RequestParam String qrcodeId, @RequestParam String token) {
		Object check = redisUtil.get(CommonConstant.LOGIN_QRCODE + qrcodeId);
		if (oConvertUtils.isNotEmpty(check)) {
			//存放token给前台读取
			redisUtil.set(CommonConstant.LOGIN_QRCODE_TOKEN+qrcodeId, token, 60);
		} else {
			return Result.error("二维码已过期,请刷新后重试");
		}
		return Result.OK("扫码成功");
	}

	/**
	 * 获取用户扫码后保存的token
	 */
	@Operation(summary = "获取用户扫码后保存的token")
	@GetMapping("/getQrcodeToken")
	public Result getQrcodeToken(@RequestParam String qrcodeId) {
		Object token = redisUtil.get(CommonConstant.LOGIN_QRCODE_TOKEN + qrcodeId);
		Map result = new HashMap(5);
		Object qrcodeIdExpire = redisUtil.get(CommonConstant.LOGIN_QRCODE + qrcodeId);
		if (oConvertUtils.isEmpty(qrcodeIdExpire)) {
			//二维码过期通知前台刷新
			result.put("token", "-2");
			return Result.OK(result);
		}
		if (oConvertUtils.isNotEmpty(token)) {
			result.put("success", true);
			result.put("token", token);
		} else {
			result.put("token", "-1");
		}
		return Result.OK(result);
	}

	/**
	 * 登录失败超出次数5 返回true
	 * @param username
	 * @return
	 */
	private boolean isLoginFailOvertimes(String username){

		String key = CommonConstant.LOGIN_FAIL + username;
		Object failTime = redisUtil.get(key);
		if(failTime!=null){
			Integer val = Integer.parseInt(failTime.toString());
			if(val>5){
				return true;
			}
		}
		return false;
	}

	/**
	 * 记录登录失败次数
	 * @param username
	 */
	private void addLoginFailOvertimes(String username){
		String key = CommonConstant.LOGIN_FAIL + username;
		Object failTime = redisUtil.get(key);
		Integer val = 0;
		if(failTime!=null){
			val = Integer.parseInt(failTime.toString());
		}
		// 10分钟，一分钟为60s
		redisUtil.set(key, ++val, 600);
	}

	/**
	 * 发送短信验证码接口(修改密码)
	 *
	 * @param jsonObject
	 * @return
	 */
	@PostMapping(value = "/sendChangePwdSms")
	public Result<String> sendSms(@RequestBody JSONObject jsonObject) {
		Result<String> result = new Result<>();
		String mobile = jsonObject.get("mobile").toString();
		if (oConvertUtils.isEmpty(mobile)) {
			result.setMessage("手机号不允许为空！");
			result.setSuccess(false);
			return result;
		}
		LoginUser sysUser = (LoginUser) SecurityUtils.getSubject().getPrincipal();
		String username = sysUser.getUsername();
		LambdaQueryWrapper<SysUser> query = new LambdaQueryWrapper<>();
		query.eq(SysUser::getUsername, username).eq(SysUser::getPhone, mobile);
		SysUser user = sysUserService.getOne(query);
		if (null == user) {
			return Result.error("当前登录用户和绑定的手机号不匹配，无法修改密码！");
		}
		String redisKey = CommonConstant.PHONE_REDIS_KEY_PRE + mobile;
		Object object = redisUtil.get(redisKey);
		if (object != null) {
			result.setMessage("验证码10分钟内，仍然有效！");
			result.setSuccess(false);
			return result;
		}
		//随机数
		String captcha = RandomUtil.randomNumbers(6);
		JSONObject obj = new JSONObject();
		obj.put("code", captcha);
		try {
			boolean b = DySmsHelper.sendSms(mobile, obj, DySmsEnum.CHANGE_PASSWORD_TEMPLATE_CODE);
			if (!b) {
				result.setMessage("短信验证码发送失败,请稍后重试");
				result.setSuccess(false);
				return result;
			}
			//验证码5分钟内有效
			redisUtil.set(redisKey, captcha, 300);
			result.setSuccess(true);
		} catch (ClientException e) {
			e.printStackTrace();
			result.error500(" 短信接口未配置，请联系管理员！");
			return result;
		}
		return result;
	}

	
	/**
	 * 图形验证码
	 * @param sysLoginModel
	 * @return
	 */
	@RequestMapping(value = "/smsCheckCaptcha", method = RequestMethod.POST)
	public Result<?> smsCheckCaptcha(@RequestBody SysLoginModel sysLoginModel, HttpServletRequest request){
		String captcha = sysLoginModel.getCaptcha();
		String checkKey = sysLoginModel.getCheckKey();
		if(captcha==null){
			return Result.error("验证码无效");
		}
		String lowerCaseCaptcha = captcha.toLowerCase();
		String realKey = Md5Util.md5Encode(lowerCaseCaptcha+checkKey+jeecgBaseConfig.getSignatureSecret(), "utf-8");
		Object checkCode = redisUtil.get(realKey);
		if(checkCode==null || !checkCode.equals(lowerCaseCaptcha)) {
			return Result.error("验证码错误");
		}
		String clientIp = IpUtils.getIpAddr(request);
		//清空短信记录数量
		DySmsLimit.clearSendSmsCount(clientIp);
		redisUtil.removeAll(realKey);
		return Result.ok();
	}



}