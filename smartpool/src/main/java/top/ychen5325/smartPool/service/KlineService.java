package top.ychen5325.smartPool.service;

import top.ychen5325.smartPool.model.Kline;
import top.ychen5325.smartPool.model.KlineParam;

import cn.hutool.core.collection.CollectionUtil;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 维护所有币种最近周期的k线数据
 *
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
@Data
@Component
public class KlineService {

	/**
	 * key -> 币种
	 * val -> klineList
	 */
	private Map<String, Kline[]> klineCache;
	/**
	 * key -> 币种
	 * val -> klineCache的索引
	 */
	private Map<String, Integer> indexCache;

	/**
	 * 三天前开始、可配置
	 */
	@Value("${kline.beforeHour:3}")
	private Long beforeHours;

	/**
	 * 最长维护五天、可配置
	 */
	@Value("${kline.storeHour:5}")
	private int maxStoreHours;

	@Resource
	private CzClient czClient;

	/**
	 * 获取所有币种、初始化kline起始点、
	 */
	@PostConstruct
	public void init() throws IOException {
		List<String> symbols = czClient.listSymbol();
		klineCache = new HashMap<>(symbols.size());
		indexCache = new HashMap<>(symbols.size());
		// 初始化最近时间kline
		Long initTime =
				System.currentTimeMillis() / 1000 / 60 * 1000 * 60 - beforeHours * 60 * 60 * 1000;
		for (String symbol : symbols) {
			Kline[] klines = new Kline[maxStoreHours * 60];
			klines[0] = Kline.builder().openTime(initTime).build();
			klineCache.put(symbol, klines);
			indexCache.put(symbol, 0);
		}
	}

	/**
	 * 更新kline缓存
	 */
	public void updateKline(String symbol) {
		// 指针和kline
		Kline[] klineArr = klineCache.get(symbol);
		int index = indexCache.get(symbol);
		// 获取最新的kline开盘时间
		Kline lastKline = klineArr[index];
		long lastOpenTime = lastKline.getOpenTime();

		Long curTime = System.currentTimeMillis() / 1000 / 60 * 1000 * 60;
		while (lastOpenTime < curTime) {
			// 如果时间差小于12个小时、则全量获取、如果大于12个小时、则获取720条1m级别k线

			List<Kline> klineList =
					czClient.listKline(KlineParam.builder().symbol(symbol).interval("1m").startTime(lastOpenTime).endTime(curTime).limit(720).build());
			if (CollectionUtil.isEmpty(klineList)) {
				break;
			}
			// 先回走一步、后续在前进一步、把其指向的kline更新一下
			index = (index + klineArr.length - 1) % klineArr.length;
			for (Kline kline : klineList) {
				// 获取下一更新索引
				index = (index + 1) % klineArr.length;
				klineArr[index] = kline;
			}
			indexCache.put(symbol, index);
			lastOpenTime += 43200000 - 60000;
		}
	}

	/**
	 * 根据币种和周期获取klineList
	 */
	public List<Kline> getKline(String symbol, long period) {
		// 获取kline和指针
		Kline[] klineArr = klineCache.get(symbol);
		int curIndex = indexCache.get(symbol);
		// 获取最新kline的开盘时间戳和当前时间戳
		Long lastOpenTime = klineArr[curIndex].getOpenTime();
		Long curTime = System.currentTimeMillis() / 1000 / 60 * 1000 * 60;
		// 获取结果kline集合的时间区间下限
		long timeLimit = curTime - period;
		// 计算最小时间戳kline所在的索引位置
		int timeLen = Math.toIntExact((lastOpenTime - timeLimit) / 1000 / 60);
		if (timeLen < 0) {
			// 最近period时间区间内、没有k线缓存、是不科学的场景、应该抛异常
			throw new RuntimeException("timeLen < 0");
		}
		int startIndex = (curIndex + klineArr.length - timeLen) % klineArr.length;
		// 顺序遍历返回
		List<Kline> result = new ArrayList<>(timeLen * 3 / 2);
		if ((startIndex = startIndex < 0 ? -startIndex : startIndex) > curIndex) {
			curIndex += klineArr.length;
		}
		while (startIndex < curIndex) {
			Kline cacheKline = klineArr[startIndex % klineArr.length];
			Kline val = new Kline();
			if (cacheKline == null) {
				throw new RuntimeException("cacheKline ==null");
			}
			BeanUtils.copyProperties(cacheKline, val);
			result.add(val);
			startIndex++;
		}
		return result;
	}
}


