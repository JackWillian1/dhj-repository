package org.jeecg.common.api.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.jeecg.common.constant.CommonConstant;

import java.io.Serializable;

/**
 *   接口返回数据格式
 * @author scott
 * @email jeecgos@163.com
 * @date  2019年1月19日
 */
@Data
@Schema(description="接口返回对象")
public class Result<T> implements Serializable {
	/*用于序列化时版本兼容性验证。*/
	private static final long serialVersionUID = 1L;

	/**
	 * 成功标志
	 */
	@Schema(description = "成功标志")
	/*表示接口调用是否成功，默认是 true。*/
	private boolean success = true;

	/**
	 * 返回处理消息
	 */
	@Schema(description = "返回处理消息")
	/*返回的提示信息，如 "操作成功" 或 "系统异常"。*/
	private String message = "";

	/**
	 * 返回代码 HTTP 状态码或业务自定义状态码，比如 200 成功、500 错误等。
	 */
	@Schema(description = "返回代码")
	private Integer code = 0;
	
	/**
	 * 返回数据对象 data 返回的真正数据内容，比如用户信息、列表、分页对象等。
	 */
	@Schema(description = "返回数据对象")
	private T result;
	
	/**
	 * 时间戳 返回结果的时间戳，用于前端处理响应延迟或展示时间。
	 */
	@Schema(description = "时间戳")
	private long timestamp = System.currentTimeMillis();

	/*默认构造函数。*/
	public Result() {
	}

    /**
     * 兼容VUE3版token失效不跳转登录页面
     * @param code
     * @param message
     */
	public Result(Integer code, String message) {
		this.code = code;
		this.message = message;
	}

	/*设置成功信息的快捷方法，返回自身支持链式调用。*/
	public Result<T> success(String message) {
		this.message = message;
		this.code = CommonConstant.SC_OK_200;
		this.success = true;
		return this;
	}

	/*提供不同参数组合的静态方法，便于快速创建成功返回对象。
	注意：有的写法 ok()，有的写法 OK()，是为了兼容旧版本的调用方式。*/
	public static<T> Result<T> ok() {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		return r;
	}

	public static<T> Result<T> ok(String msg) {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		//Result OK(String msg)方法会造成兼容性问题 issues/I4IP3D
		r.setResult((T) msg);
		r.setMessage(msg);
		return r;
	}

	public static<T> Result<T> ok(T data) {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		r.setResult(data);
		return r;
	}

	public static<T> Result<T> OK() {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		return r;
	}

	/**
	 * 此方法是为了兼容升级所创建
	 *
	 * @param msg
	 * @param <T>
	 * @return
	 */
	public static<T> Result<T> OK(String msg) {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		r.setMessage(msg);
		//Result OK(String msg)方法会造成兼容性问题 issues/I4IP3D
		r.setResult((T) msg);
		return r;
	}

	public static<T> Result<T> OK(T data) {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		r.setResult(data);
		return r;
	}

	public static<T> Result<T> OK(String msg, T data) {
		Result<T> r = new Result<T>();
		r.setSuccess(true);
		r.setCode(CommonConstant.SC_OK_200);
		r.setMessage(msg);
		r.setResult(data);
		return r;
	}

	/* error(String msg, T data)、error(String msg)、error(int code, String msg)创建错误返回对象。*/
	public static<T> Result<T> error(String msg, T data) {
		Result<T> r = new Result<T>();
		r.setSuccess(false);
		r.setCode(CommonConstant.SC_INTERNAL_SERVER_ERROR_500);
		r.setMessage(msg);
		r.setResult(data);
		return r;
	}

	public static<T> Result<T> error(String msg) {
		return error(CommonConstant.SC_INTERNAL_SERVER_ERROR_500, msg);
	}
	
	public static<T> Result<T> error(int code, String msg) {
		Result<T> r = new Result<T>();
		r.setCode(code);
		r.setMessage(msg);
		r.setSuccess(false);
		return r;
	}

	/*实例方法，设置为 500 错误。*/
	public Result<T> error500(String message) {
		this.message = message;
		this.code = CommonConstant.SC_INTERNAL_SERVER_ERROR_500;
		this.success = false;
		return this;
	}

	/**
	 * 无权限访问返回结果 没有权限时的快捷构造方法。
	 */
	public static<T> Result<T> noauth(String msg) {
		return error(CommonConstant.SC_JEECG_NO_AUTHZ, msg);
	}


	/*内部字段，用于在线表单功能，不在 JSON 返回中暴露。*/
	@JsonIgnore
	private String onlTable;


}