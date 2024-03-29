package icu.smartpool.job;


import com.alibaba.fastjson.JSON;

import icu.smartpool.common.Config;
import icu.smartpool.model.ShakeScore;
import icu.smartpool.service.CzClient;
import icu.smartpool.service.SmartPoolService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
public class SmartPoolJob {

	private static final int LIMIT = 5;

	private static final int MIN_AMPLITUDE = 2;

	private static final int MAX_AMPLITUDE = 10;

	/**
	 * map结构: cycle -> 该周期下所有币对的震荡数据
	 * 子集合结构:
	 *   - 低波动的和超高波动的币对
	 *   - 振幅[2,4)的币对
	 *   - 振幅[4,6)的币对
	 *   - 振幅[6,8)的币对
	 *   - 振幅[8,10)的币对
	 */
	private static final Map<Integer, List<List<ShakeScore>>> DATA_CACHE = new HashMap<>();


	private static final ThreadPoolExecutor THREAD_POOL = new ThreadPoolExecutor(1, Config.MAX_POOL_SIZE, 1,
																				 TimeUnit.MINUTES,
																				 new ArrayBlockingQueue<>(1),
																				 Executors.defaultThreadFactory(),
																				 new ThreadPoolExecutor.CallerRunsPolicy());

	public static void run() {
		while (true) {
			long l = System.currentTimeMillis();
			try {
				loop();
				refresh();
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			log.info("耗时：" + (System.currentTimeMillis() - l) / 1000);
			try {
				TimeUnit.HOURS.sleep(1);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	private static void loop() throws InterruptedException {
		List<String> symbolList = CzClient.listSymbol();
		for (Integer cycle : Config.CYCLE_LIST) {
			Queue<ShakeScore> queue = new ArrayBlockingQueue<>(symbolList.size());
			log.info("{}小时周期starting...", cycle);
			CountDownLatch cdl = new CountDownLatch(symbolList.size());
			for (String symbol : symbolList) {
				CompletableFuture.runAsync(() -> {
					log.info("{} {}==>{}.....", symbol, symbolList.size() - cdl.getCount(), symbolList.size());
					try {
						queue.add(SmartPoolService.analyze(symbol, cycle));
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					cdl.countDown();
				}, THREAD_POOL);
			}
			cdl.await();
			List<List<ShakeScore>> rlt = new ArrayList<>();
			for (int i = 0; i < 5; i++) {
				rlt.add(new ArrayList<>());
			}
			for (ShakeScore shakeScore : queue) {
				int index = range(shakeScore);
				rlt.get(index).add(shakeScore);
			}
			DATA_CACHE.put(cycle, rlt);
		}
	}

	private static void refresh() {
		for (Map.Entry<Integer, List<List<ShakeScore>>> entry : DATA_CACHE.entrySet()) {
			Integer cycle = entry.getKey();
			List<List<ShakeScore>> rlt = entry.getValue();
			for (int i = rlt.size() - 1; i >= 0; i--) {
				List<ShakeScore> itemList = rlt.get(i);
				itemList.sort((e1, e2) -> e2.getScore() - e1.getScore());
				log.info("～～周期{}H-振幅-[{},{})-前{}排名～～", cycle, i * 2, i * 2 + 2, LIMIT);
				for (int j = 0; j < itemList.size() && j < LIMIT; j++) {
					System.out.println(JSON.toJSONString(itemList.get(j)));
				}
			}
		}
	}

	private static int range(ShakeScore e2) {
		int amplitude = (int) e2.getAmplitude();
		// 区间幅度过大过小都不行、
		// 分为[2,4),[4,6),[6,8),[8,10)四个区间、振幅所落区间相同、则比较score项
		if (amplitude < MIN_AMPLITUDE || amplitude >= MAX_AMPLITUDE) {
			return 0;
		}
		return amplitude / 2;
	}
}

