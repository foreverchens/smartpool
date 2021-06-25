package top.ychen5325.smartPool.job;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.SymbolShock;
import top.ychen5325.smartPool.server.BackTestService;
import top.ychen5325.smartPool.server.SmartPoolService;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Component
public class SmartPoolJob {


    private Map<IntervalEnum, List<SymbolShock>> symbolShockPoolCache = new HashMap<>();

    private Map<IntervalEnum, List<String>> symbolBackTestPoolCache = new HashMap<>();

    List<IntervalEnum> IntervalEnums;
    @Resource
    SmartPoolService smartPoolService;
    @Resource
    BackTestService backTestService;

    @PostConstruct
    public void init() {
        IntervalEnums = new ArrayList<>();
        IntervalEnums.addAll(IntervalEnum.jqPoolPeriodList);
    }

    /**
     * 每小时一次
     */
    @Scheduled(initialDelay = 2 * 1000, fixedRate = 60 * 60 * 1000)
    public void executor() {
        List<String> symbols = smartPoolService.listContractSymbol();
        for (IntervalEnum period : IntervalEnums) {
            // 传入币种列表和周期获取其计算结果
            List<SymbolShock> symbolShockList = smartPoolService.shockAnalyzeHandler(symbols, period);
            symbolShockPoolCache.put(period, symbolShockList);
            // 针对计算结果在指定周期内进行月化回测
            List<String> backTestPool = backTestService.backTestHandler(period, symbolShockList);
            // 更新缓存
            symbolBackTestPoolCache.put(period, backTestPool);
            log.info("周期:{},震荡池回测池更新成。。。", period.toString());
        }
    }
}

