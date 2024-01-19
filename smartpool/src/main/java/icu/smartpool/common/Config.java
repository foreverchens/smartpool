package icu.smartpool.common;

import java.util.Arrays;
import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
public interface Config {

	String LIST_SYMBOL = "https://api.binance.com/api/v3/exchangeInfo";
	String KLINE = "https://api.binance.com/api/v3/klines";
	int MAX_DAY = 7;
	List<Integer> CYCLE_LIST = Arrays.asList(24 * 7);
}
