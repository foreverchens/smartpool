package top.ychen5325.smartPool.model;/**
 *
 */

import lombok.Builder;
import lombok.Data;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
@Builder
public class KlineParam {

	private String symbol;
	private String interval;
	private Long startTime;
	private Long endTime;
	private Integer limit;
}
