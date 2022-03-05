package top.ychen5325.smartPool.job;

import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.SymbolShake;
import top.ychen5325.smartPool.service.SmartPoolService;
import top.ychen5325.smartPool.service.SymbolService;

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
 * @wx ychen5325
 * @email yangyouyuhd@163.com
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
    private SmartPoolService smartPoolService;
    @Resource
    private SymbolService symbolService;


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
    @Scheduled(initialDelay = 2 * 1000, fixedRate = 5 * 60 * 1000)
    public void executor() {
        List<String> symbols = symbolService.listContractSymbol();
        if (CollectionUtil.isEmpty(symbols)) {
            log.info("symbolService.listContractSymbol() return empty");
            return;
        }
        for (IntervalEnum period : intervals) {
            // 传入币种列表和周期获取其计算结果
            List<SymbolShake> symbolShakeList = smartPoolService.klineAnalyze(symbols, period);
            symbolShockPoolCache.put(period, symbolShakeList);
            log.info("周期:{},震荡池回测池更新成。。。", period.toString());
        }
    }
}

