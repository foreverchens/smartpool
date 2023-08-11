package top.ychen5325.smartPool.job;

import top.ychen5325.smartPool.common.CallResult;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.SymbolShake;
import top.ychen5325.smartPool.service.CzClient;
import top.ychen5325.smartPool.service.SmartPoolService;

import cn.hutool.core.collection.CollectionUtil;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
@Component
public class SmartPoolJob {

	/**
	 * key -> 震荡周期
	 * val -> 币种的震荡状态
	 */
	private Map<IntervalEnum, List<SymbolShake>> symbolShockPoolCache = new HashMap<>();

	/**
	 * 扫描的周期
	 */
	private List<IntervalEnum> intervals;
	@Resource
	private CzClient czClient;

	@Resource
	private SmartPoolService smartPoolService;

	/**
	 * 慢启动
	 */
	private int limit = 5;

	@Value("${interval.list}")
	private void setIntervals(List<String> list) {
		intervals = new ArrayList<>();
		for (String key : list) {
			intervals.add(IntervalEnum.valueOf(key));
		}
	}

	/**
	 * 5分钟一次
	 */
	@Scheduled(initialDelay = 2 * 1000, fixedRate = 2 * 60 * 1000)
	public void executor() {
		List<String> symbols = czClient.listSymbol();

		// 预防出现HTTP 429问题、
		// 某币第一次处理时、需要缓存其大量kline数据、过多缓存操作容易出现429问题、
		// 顾通过limit值来保障每次只处理20个新币、平缓缓存压力
		symbols = CollectionUtil.sub(symbols, 0, limit);
		limit = Math.min(limit, symbols.size()) + 10;

		if (CollectionUtil.isEmpty(symbols)) {
			log.info("symbolService.listContractSymbol() return empty");
			return;
		}
		for (IntervalEnum interval : intervals) {
			// 传入币种列表和周期获取其计算结果
			List<SymbolShake> symbolShakeList = smartPoolService.klineAnalyze(symbols, interval);
			symbolShockPoolCache.put(interval, symbolShakeList);
			log.info("周期:{},震荡池更新完成。。。", interval);
		}
	}


	public CallResult<List<SymbolShake>> list(String interval, int top) {
		List<SymbolShake> shakeList = symbolShockPoolCache.get(IntervalEnum.valueOf(interval));
		if (CollectionUtil.isEmpty(shakeList)) {
			return CallResult.failure();
		}
		return CallResult.success(CollectionUtil.sub(shakeList, 0, top));
	}
}

