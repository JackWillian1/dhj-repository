package org.jeecg.modules.monitor.service.impl;

import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

import org.springframework.data.redis.core.RedisTemplate;

import javax.annotation.Resource;

import cn.hutool.core.date.DateUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.monitor.domain.RedisInfo;
import org.jeecg.modules.monitor.exception.RedisConnectException;
import org.jeecg.modules.monitor.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

/**
 * Redis 监控信息获取
 *
 * @Author MrBird
 */
@Service("redisService")
@Slf4j
public class RedisServiceImpl implements RedisService {
	//author：DHJ
	@Resource
	private StringRedisTemplate stringRedisTemplate;
	@Resource
	private RedisTemplate<String, String> redisTemplate;
	//

	@Resource
	private RedisConnectionFactory redisConnectionFactory;

    /**
     * redis信息
     */
    private static final String REDIS_MESSAGE = "3";

	/**
	 * redis性能信息记录
	 */
	private static final Map<String,List<Map<String, Object>>> REDIS_METRICS = new HashMap<>(2);

	/**
	 * Redis详细信息
	 */
	@Override
	public List<RedisInfo> getRedisInfo() throws RedisConnectException {
		Properties info = redisConnectionFactory.getConnection().info();
		List<RedisInfo> infoList = new ArrayList<>();
		RedisInfo redisInfo = null;
		for (Map.Entry<Object, Object> entry : info.entrySet()) {
			redisInfo = new RedisInfo();
			redisInfo.setKey(oConvertUtils.getString(entry.getKey()));
			redisInfo.setValue(oConvertUtils.getString(entry.getValue()));
			infoList.add(redisInfo);
		}
		return infoList;
	}

	@Override
	public Map<String, Object> getKeysSize() throws RedisConnectException {
		Long dbSize = redisConnectionFactory.getConnection().dbSize();
		Map<String, Object> map = new HashMap(5);
		map.put("create_time", System.currentTimeMillis());
		map.put("dbSize", dbSize);

		log.debug("--getKeysSize--: " + map.toString());
		return map;
	}

	@Override
	public Map<String, Object> getMemoryInfo() throws RedisConnectException {
		Map<String, Object> map = null;
		Properties info = redisConnectionFactory.getConnection().info();
		for (Map.Entry<Object, Object> entry : info.entrySet()) {
			String key = oConvertUtils.getString(entry.getKey());
			if ("used_memory".equals(key)) {
				map = new HashMap(5);
				map.put("used_memory", entry.getValue());
				map.put("create_time", System.currentTimeMillis());
			}
		}
		log.debug("--getMemoryInfo--: " + map.toString());
		return map;
	}

    /**
     * 查询redis信息for报表
     * @param type 1redis key数量 2 占用内存 3redis信息
     * @return
     * @throws RedisConnectException
     */
	@Override
	public Map<String, JSONArray> getMapForReport(String type)  throws RedisConnectException {
		Map<String,JSONArray> mapJson=new HashMap(5);
		JSONArray json = new JSONArray();
		if(REDIS_MESSAGE.equals(type)){
			List<RedisInfo> redisInfo = getRedisInfo();
			for(RedisInfo info:redisInfo){
				Map<String, Object> map= Maps.newHashMap();
				BeanMap beanMap = BeanMap.create(info);
				for (Object key : beanMap.keySet()) {
					map.put(key+"", beanMap.get(key));
				}
				json.add(map);
			}
			mapJson.put("data",json);
			return mapJson;
		}
		int length = 5;
		for(int i = 0; i < length; i++){
			JSONObject jo = new JSONObject();
			Map<String, Object> map;
			if("1".equals(type)){
				map= getKeysSize();
				jo.put("value",map.get("dbSize"));
			}else{
				map = getMemoryInfo();
				Integer usedMemory = Integer.valueOf(map.get("used_memory").toString());
				jo.put("value",usedMemory/1000);
			}
			String createTime = DateUtil.formatTime(DateUtil.date((Long) map.get("create_time")-(4-i)*1000));
			jo.put("name",createTime);
			json.add(jo);
		}
		mapJson.put("data",json);
		return mapJson;
	}

	//update-begin---author:chenrui ---date:20240514  for：[QQYUN-9247]系统监控功能优化------------
	/**
	 * 获取历史性能指标
	 * @return
	 * @author chenrui
	 * @date 2024/5/14 14:57
	 */
	@Override
	public Map<String, List<Map<String, Object>>> getMetricsHistory() {
		return REDIS_METRICS;
	}

	/**
	 * 记录近一小时redis监控数据 <br/>
	 * 60s一次,,记录存储keysize和内存
	 * @throws RedisConnectException
	 * @author chenrui
	 * @date 2024/5/14 14:09
	 */
	@Scheduled(fixedRate = 60000)
	public void recordCustomMetric() throws RedisConnectException {
		List<Map<String, Object>> list= new ArrayList<>();
		if(REDIS_METRICS.containsKey("dbSize")){
			list = REDIS_METRICS.get("dbSize");
		}else{
			REDIS_METRICS.put("dbSize",list);
		}
		if(list.size()>60){
			list.remove(0);
		}
		list.add(getKeysSize());
		list= new ArrayList<>();
		if(REDIS_METRICS.containsKey("memory")){
			list = REDIS_METRICS.get("memory");
		}else{
			REDIS_METRICS.put("memory",list);
		}
		if(list.size()>60){
			list.remove(0);
		}
		list.add(getMemoryInfo());
	}
	//update-end---author:chenrui ---date:20240514  for：[QQYUN-9247]系统监控功能优化------------

// author:DHJ
	/* 采用 Redis Hash 形式
	 一个 key 对应一个哈希表，哈希表中的每个字段（field）都有一个对应的值（value）,它适合存储对象数据。
	 */
	// 开启或关闭账户锁 当天 0 点失效
	public void setAccountLockOfHash(String username, boolean status) {
		stringRedisTemplate.opsForHash().put(username,"accountLock", String.valueOf(status));
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}

	//获取账户锁状态
	public boolean getAccountLockStatusOfHash(String username) {
		String value = (String) stringRedisTemplate.opsForHash().get(username, "accountLock");
		boolean status = false;
		if(value != null){
			status = Boolean.parseBoolean(value);
		}
		return status;
	}

	//记录登录成功次数 采用increment方法保证原子性 当天 0 点失效
	public void recordSuccessLoginCountOfHash(String username) {
		stringRedisTemplate.opsForHash().increment(username,"loginSuccessCount", 1);
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}

	//获取登录成功次数
	public int getSuccessLoginCountOfHash(String username) {
		String value = (String) stringRedisTemplate.opsForHash().get(username, "loginSuccessCount");
		int count = 0;
		if(value != null){
			count = Integer.parseInt(value);
		}
		return count;
	}

	//记录失败次数 采用increment方法保证原子性 当天 0 点失效
	public void recordFailureLoginCountOfHash(String username) {
		stringRedisTemplate.opsForHash().increment(username, "loginFailureCount", 1);
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}

	//获取登录失败次数
	public int getFailureLoginCountOfHash(String username) {
		String value = (String) stringRedisTemplate.opsForHash().get(username, "loginFailureCount");
		int count = 0;
		if(value != null){
			count = Integer.parseInt(value);
		}
		return count;
	}

	//记录暴力破解次数 采用increment方法保证原子性 当天 0 点失效
	public void recordForceAttacksCountOfHash(String username) {
		stringRedisTemplate.opsForHash().increment(username, "forceAttacksCount", 1);
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}

	//设置主动退出标签状态
	public void setExitlabelStatusOfHash(String username, boolean labelValue) {
		stringRedisTemplate.opsForHash().put(username, "exitLabel", String.valueOf(labelValue));
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}

	//获取标签状态
	public boolean getExitlabelStatusOfHash(String username) {
		String value = (String) stringRedisTemplate.opsForHash().get(username, "exitLabel");
		boolean status = false;
		if(value != null){
			status = Boolean.parseBoolean(value);
		}
		return status;
	}

	//记录当前时间
	public void recordCurrentLoginTime(String username) {
		stringRedisTemplate.opsForHash().put(username, "currentLoginTime",  String.valueOf(DateUtil.now()));
		stringRedisTemplate.expire(username, Duration.ofSeconds(getLiveSecond()));
	}


	/* 采用 Redis Key value 形式 */
	//设置当天登录成功次数
	public void successLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "successLoginCount";
		stringRedisTemplate.opsForValue().increment(key, 1);//原子性
		stringRedisTemplate.expire(key, Duration.ofSeconds(getLiveSecond()));
	}

	//获取当天登录成功次数
	public int getValueOfSuccessLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "successLoginCount";
		String value =  stringRedisTemplate.opsForValue().get(key);
        int valueOfSuccessLoginCount = 0;
        if (value != null) {
			valueOfSuccessLoginCount = Integer.parseInt(value);
        }
        return valueOfSuccessLoginCount;
	}

	//设置当天登录失败次数
	public void failureLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "failureLoginCount";
		stringRedisTemplate.opsForValue().increment(key, 1);//原子性
		stringRedisTemplate.expire(key, Duration.ofSeconds(getLiveSecond()));
	}

	//获取当天登录失败次数
	public int getValueOfFailureLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "failureLoginCount";
		String value =  stringRedisTemplate.opsForValue().get(key);
		int valueOfFailureLoginCount = 0;
		if (value != null) {
			valueOfFailureLoginCount = Integer.parseInt(value);
		}
		return valueOfFailureLoginCount;
	}

	//设置账户锁
	public void setAccountLock(String username,String status) {
		String key = DateUtil.today() + ":" + username + ":" + "accountLock";
		stringRedisTemplate.opsForValue().set(key, status);
		stringRedisTemplate.expire(key, Duration.ofSeconds(getLiveSecond()));
	}

	//获取账户锁状态: true代表开启,false代表关闭
	public boolean getAccountLockStatus(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "accountLock";
		String value =  stringRedisTemplate.opsForValue().get(key);
		boolean accountLockStatus = false;
		if (value != null) {
			accountLockStatus = Boolean.parseBoolean(value);
		}
		return accountLockStatus;
	}

	//累计当天暴力破解次数
	public void forceAttackLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "forceAttackLoginCount";
		stringRedisTemplate.opsForValue().increment(key, 1);//原子性
		stringRedisTemplate.expire(key, Duration.ofSeconds(getLiveSecond()));
	}

	// 获取当天暴力破解次数
	public int getForceAttackLoginCount(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "forceAttackLoginCount";
		String value =  stringRedisTemplate.opsForValue().get(key);
		int forceAttackLoginCount = 0;
		if (value != null) {
			forceAttackLoginCount = Integer.parseInt(value);
		}
		return forceAttackLoginCount;
	}

	//设置主动退出标签
	public void setExitLabel(String username,String status) {
		String key = DateUtil.today() + ":" + username + ":" + "exitLabel";
		stringRedisTemplate.opsForValue().set(key, status);
		stringRedisTemplate.expire(key, Duration.ofSeconds(getLiveSecond()));
	}

	// 获取主动退出标签
	public boolean getExitLabel(String username) {
		String key = DateUtil.today() + ":" + username + ":" + "exitLabel";
		String value =  stringRedisTemplate.opsForValue().get(key);
		boolean exitLabel = false;
		if (value != null) {
			exitLabel = Boolean.parseBoolean(value);
		}
		return exitLabel;
	}

	// 设置存活时间 单位:秒
	public int getLiveSecond() {
		int totalSecondsInDay = 24 * 60 * 60; // 1天的秒数
		int passedSeconds = LocalTime.now().toSecondOfDay();// 今天已经过去的秒数
        // 今天剩余的秒数
        return totalSecondsInDay - passedSeconds;
	}
//
}





