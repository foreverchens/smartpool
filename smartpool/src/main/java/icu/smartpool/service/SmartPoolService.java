package icu.smartpool.service;


import icu.smartpool.common.Config;
import icu.smartpool.model.H1Kline;
import icu.smartpool.model.Kline;
import icu.smartpool.model.KlineParam;
import icu.smartpool.model.ShakeScore;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
public class SmartPoolService {

	private static final long HOUR = 1000 * 60 * 60;

	private static final int DEQUE_SIZE = Config.MAX_DAY * 24;

	private static final Map<String, Deque<H1Kline>> KLINE_CACHE;

	static {
		List<String> symbols = CzClient.listSymbol();
		KLINE_CACHE = new HashMap<>(symbols.size());
		for (String symbol : symbols) {
			KLINE_CACHE.put(symbol, new ArrayDeque<>(DEQUE_SIZE));
		}
	}

	public static ShakeScore analyze(String symbol, int hours) {
		List<H1Kline> klineList = listH1Kline(symbol, hours);

		BigDecimal minP = BigDecimal.valueOf(Long.MAX_VALUE);
		BigDecimal maxP = BigDecimal.ZERO;
		for (H1Kline kline : klineList) {
			minP = minP.compareTo(kline.getLowP()) > 0
				   ? kline.getLowP()
				   : minP;
			maxP = maxP.compareTo(kline.getHighP()) < 0
				   ? kline.getHighP()
				   : maxP;
		}
		BigDecimal arrScale = minP.multiply(Config.SCALE);
		int[] dataArr = new int[(maxP.subtract(minP).divide(arrScale, 1, RoundingMode.DOWN).intValue())];
		for (H1Kline kline : klineList) {
			int[] itemDataArr = kline.getDataArr();
			BigDecimal itemLowP = kline.getLowP();
			int startIndex = itemLowP.subtract(minP).divide(arrScale, 1, RoundingMode.DOWN).intValue();
			for (int i = 0; i < itemDataArr.length; i++) {
				dataArr[startIndex + i] += itemDataArr[i];
			}
		}
		// 总点数
		int countPt = Arrays.stream(dataArr).sum();
		// 去掉区间上下的稀疏点各10%、点位分布曲线砍去两边10%、定为震荡区间、

		int subCountPt = (int) (countPt * 0.2);
		int l = 0, r = dataArr.length - 1;
		while (subCountPt > 0) {
			while (dataArr[l] < 1) {
				l++;
			}
			subCountPt -= dataArr[l++];
			if (subCountPt < 1) {
				break;
			}
			while (dataArr[r] < 1) {
				r--;
			}
			subCountPt -= dataArr[r--];
		}
		BigDecimal lowP = minP.add(arrScale.multiply(BigDecimal.valueOf(l)));
		BigDecimal highP = minP.add(arrScale.multiply(BigDecimal.valueOf(r)));
		BigDecimal amplitude = highP.subtract(lowP).multiply(BigDecimal.valueOf(100)).divide(lowP, 2,
																							 RoundingMode.DOWN);
		int score = BigDecimal.valueOf(countPt * 0.8).divide(amplitude, 1, RoundingMode.DOWN).intValue();
		return ShakeScore.builder().symbol(symbol).score(score).lowP(
				lowP.setScale(4, RoundingMode.DOWN).toEngineeringString()).highP(
				highP.setScale(4, RoundingMode.DOWN).toEngineeringString()).amplitude(
				amplitude.setScale(2, RoundingMode.DOWN).doubleValue()).build();
	}

	private static List<H1Kline> listH1Kline(String symbol, int hours) {
		Deque<H1Kline> deque = KLINE_CACHE.get(symbol);
		// 检查更新
		updateH1Kline(symbol);
		// 获取数据
		Iterator<H1Kline> iterator = deque.iterator();
		List<H1Kline> rlt = new ArrayList<>(hours);
		while (iterator.hasNext() && hours-- > 0) {
			rlt.add(iterator.next());
		}
		return rlt;
	}

	private static void updateH1Kline(String symbol) {
		Deque<H1Kline> deque = KLINE_CACHE.get(symbol);
		long lastTime = System.currentTimeMillis() / HOUR * HOUR - HOUR;
		long startTime = deque.isEmpty()
						 ? System.currentTimeMillis() / HOUR * HOUR - DEQUE_SIZE * HOUR
						 : deque.peek().getOpenT() + HOUR;
		for (int maxHour = 16; maxHour > 0; maxHour--) {
			while ((lastTime - startTime) / HOUR >= maxHour) {
				KlineParam param = KlineParam.builder()
											 .symbol(symbol)
											 .interval("1m")
											 .startTime(startTime)
											 .endTime(startTime + HOUR * maxHour)
											 .limit(60 * maxHour)
											 .build();
				List<Kline> klines = CzClient.listKline(param);
				for (int i = 0; i < klines.size(); i += 60) {
					H1Kline newH1Kline = klineListToH1Kline(klines.subList(i, Math.min(i + 60, klines.size())));
					newH1Kline.setOpenT(startTime += HOUR);
					if (deque.size() >= DEQUE_SIZE) {
						deque.removeLast();
					}
					deque.push(newH1Kline);
				}
			}
		}
	}

	private static H1Kline klineListToH1Kline(List<Kline> klines) {
		BigDecimal lowP = BigDecimal.valueOf(Long.MAX_VALUE);
		BigDecimal highP = BigDecimal.ZERO;
		for (Kline kline : klines) {
			lowP = lowP.compareTo(kline.getLowP()) > 0
				   ? kline.getLowP()
				   : lowP;
			highP = highP.compareTo(kline.getHighP()) < 0
					? kline.getHighP()
					: highP;
		}
		BigDecimal arrScale = lowP.multiply(Config.SCALE);
		int[] dataArr = new int[(highP.subtract(lowP).divide(arrScale, 1, RoundingMode.DOWN).intValue())];
		for (Kline kline : klines) {
			BigDecimal openP = kline.getOpenP();
			BigDecimal closeP = kline.getCloseP();
			if (openP.compareTo(closeP) > 0) {
				//  开盘价高于收盘价、即下跌、交换数值使closeP大于openP、方便计算
				BigDecimal tmp = closeP;
				closeP = openP;
				openP = tmp;
			}
			if (closeP.subtract(openP).compareTo(arrScale) < 0) {
				// 低波动k线过滤
				continue;
			}
			int startIndex = openP.subtract(lowP).divide(arrScale, 1, RoundingMode.DOWN).intValue();
			int endIndex = closeP.subtract(lowP).divide(arrScale, 1, RoundingMode.DOWN).intValue();
			for (int i = startIndex; i < endIndex; i++) {
				dataArr[i]++;
			}
		}
		return H1Kline.builder().lowP(lowP).highP(highP).dataArr(dataArr).build();
	}
}