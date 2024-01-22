package icu.smartpool.common;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public interface Config {

	String LIST_SYMBOL = "https://api.binance.com/api/v3/exchangeInfo";

	String KLINE = "https://api.binance.com/api/v3/klines";

	/**
	 * 最大可分析周期
	 */
	int MAX_DAY = 1;

	int MAX_POOL_SIZE = 1;

	/**
	 * 分析周期列表
	 */
	List<Integer> CYCLE_LIST = Arrays.asList(MAX_DAY * 24);

	/**
	 * 分析粒度、与周期正比
	 */
	BigDecimal SCALE = BigDecimal.valueOf(0.0001);
}
