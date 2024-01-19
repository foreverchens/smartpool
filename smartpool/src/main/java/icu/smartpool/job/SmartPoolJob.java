package icu.smartpool.job;


import com.alibaba.fastjson.JSON;

import icu.smartpool.common.Config;
import icu.smartpool.model.ShakeScore;
import icu.smartpool.service.CzClient;
import icu.smartpool.service.SmartPoolService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
public class SmartPoolJob {

	private static final int LIMIT = 10;
	private static final Map<Integer, PriorityBlockingQueue<ShakeScore>> DATA_CACHE = new HashMap<>();


	public static void run() {
		while (true) {
			try {
				loop();
				refresh();
			} catch (Exception ex) {
				ex.printStackTrace();
			}

			try {
				TimeUnit.HOURS.sleep(1);
			} catch (InterruptedException e) {
				return;
			}
		}
	}

	private static void loop() {
		List<String> symbolList = CzClient.listSymbol();
		for (Integer cycle : Config.CYCLE_LIST) {
			PriorityBlockingQueue<ShakeScore> queue = new PriorityBlockingQueue<>(LIMIT, (e1, e2) -> e2.getScore() - e1.getScore());
			log.info("cycle:{} 运行ing...", cycle);
			for (String symbol : symbolList) {
				log.info("{} starting.....", symbol);
				queue.add(SmartPoolService.analyze(symbol, cycle));
			}
			DATA_CACHE.put(cycle, queue);
		}
	}

	private static void refresh() {
		for (Map.Entry<Integer, PriorityBlockingQueue<ShakeScore>> entry : DATA_CACHE.entrySet()) {
			Integer cycle = entry.getKey();
			PriorityBlockingQueue<ShakeScore> dataQueue = entry.getValue();
			// todo 对外暴露
			Iterator<ShakeScore> iterator = dataQueue.iterator();
			log.info("～～周期{}前{}排名～～", cycle, LIMIT);
			while (iterator.hasNext()) {
				ShakeScore next = iterator.next();
				log.info("{}", JSON.toJSONString(next));
			}
			log.info("~~~~~~~~");
		}
	}
}

