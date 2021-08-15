package top.ychen5325.smartPool.job;

import cn.hutool.core.collection.CollectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.SymbolShake;
import top.ychen5325.smartPool.server.BackTestService;
import top.ychen5325.smartPool.server.SmartPoolService;
import top.ychen5325.smartPool.server.SymbolService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Component
public class SmartPoolJob {

    Map<IntervalEnum, List<SymbolShake>> symbolShockPoolCache = new HashMap<>();

    Map<IntervalEnum, List<String>> symbolBackTestPoolCache = new HashMap<>();

    List<IntervalEnum> IntervalEnums;

    @Resource
    SmartPoolService smartPoolService;
    @Resource
    BackTestService backTestService;
    @Resource
    SymbolService symbolService;

    @Resource
    StatisticsJob statisticsJob;

    @PostConstruct
    private void init() {
        IntervalEnums = new ArrayList<>();
        IntervalEnums.addAll(IntervalEnum.jqPoolPeriodList);
    }

    /**
     * 5分钟一次
     */
    @Scheduled(initialDelay = 2 * 1000, fixedRate = 5 * 60 * 1000)
    public void executor() {
        List<String> symbols = symbolService.listContractSymbol();
        if (CollectionUtil.isEmpty(symbols)) {
            log.info("symbolService.listContractSymbol() return empty");
            return;
        }
        for (IntervalEnum period : IntervalEnums) {
            // 传入币种列表和周期获取其计算结果
            List<SymbolShake> symbolShakeList = smartPoolService.klineAnalyze(symbols, period);
            symbolShockPoolCache.put(period, symbolShakeList);
            // 针对计算结果在指定周期内进行月化回测
//            List<String> backTestPool = backTestService.backTestHandler(period, symbolShockList);
            // 更新缓存
//            symbolBackTestPoolCache.put(period, backTestPool);
//            log.info("周期:{},震荡池回测池更新成。。。", period.toString());
            // add
            statisticsJob.addShakeData(period, symbolShakeList);
        }
    }
}

