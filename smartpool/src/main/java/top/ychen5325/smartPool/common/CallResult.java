package top.ychen5325.smartPool.common;

import lombok.Data;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
public class CallResult<T> {
	private static final long serialVersionUID = -7704551131927143131L;
	private static final int CODE_FAILURE = 500;
	private static final int CODE_SUCCESS = 200;
	private Boolean success;
	private Integer code;
	public String message;
	private T data;

	private CallResult() {
	}

	public static <T> CallResult<T> response(int code, String msg) {
		boolean success = false;
		if (code == 200) {
			success = true;
		}

		return new CallResult(success, code, msg, (Object) null);
	}

	public CallResult(boolean isSuccess, int code, String message, T resultObject) {
		this.success = isSuccess;
		this.code = code;
		this.message = message;
		this.data = resultObject;
	}

	public static <T> CallResult<T> success() {
		return new CallResult(true, 200, "Success", (Object) null);
	}

	public static <T> CallResult<T> success(T resultObject) {
		return new CallResult(true, 200, "Success", resultObject);
	}

	public static <T> CallResult<T> success(int code, String msg, T resultObject) {
		return new CallResult(true, code, msg, resultObject);
	}

	public static <T> CallResult<T> failure() {
		return new CallResult(false, 500, "Failure", (Object) null);
	}

	public static <T> CallResult<T> failure(String msg) {
		return new CallResult(false, 500, msg, (Object) null);
	}

	public static <T> CallResult<T> failure(int code) {
		return new CallResult(false, code, "Failure", (Object) null);
	}

	public static <T> CallResult<T> failure(int code, String msg) {
		return new CallResult(false, code, msg, (Object) null);
	}

	@Override
	public String toString() {
		return "CallResult(success=" + this.getSuccess() + ", code=" + this.getCode() + ", message" +
                "=" + this.getMessage() + ", data=" + this.getData() + ")";
	}

}
