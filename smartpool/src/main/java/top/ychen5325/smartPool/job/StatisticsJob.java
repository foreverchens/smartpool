package top.ychen5325.smartPool.job;

import cn.hutool.core.util.ObjectUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.SymbolShake;

import javax.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用于发现可能成为优秀网格投资标的币种
 *
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Component
public class StatisticsJob {

    private Map<IntervalEnum, Map<String, Double[]>> shakeCache;
    private Map<IntervalEnum, Map<String, Integer>> indexCache;

    @PostConstruct
    private void init() {
        shakeCache = new HashMap<>();
        indexCache = new HashMap<>();
        for (IntervalEnum interval : IntervalEnum.jqPoolPeriodList) {
            shakeCache.put(interval, new HashMap<>(16));
            indexCache.put(interval, new HashMap<>(16));
        }
    }

    public void addShakeData(IntervalEnum interval, List<SymbolShake> shakeData) {
        Map<String, Double[]> shakeValMap = shakeCache.get(interval);
        Map<String, Integer> IndexMap = indexCache.get(interval);
        if (ObjectUtil.isEmpty(shakeValMap) || ObjectUtil.isEmpty(IndexMap)) {
            shakeValMap = new HashMap(16);
            shakeCache.put(interval, shakeValMap);
            IndexMap = new HashMap<>();
            indexCache.put(interval, IndexMap);
        }
        for (SymbolShake data : shakeData) {
            String symbol = data.getSymbol();
            double shakeVal = data.getShakeVal();
            if (shakeValMap.containsKey(symbol)) {
                Double[] shakeArr = shakeValMap.get(symbol);
                Integer index = IndexMap.get(symbol);
                index = (index + 1) % shakeArr.length;
                shakeArr[index] = shakeVal;
                shakeValMap.put(symbol, shakeArr);
                IndexMap.put(symbol, index);
            } else {
                Double[] shakeArr = new Double[10];
                shakeArr[0] = shakeVal;
                shakeValMap.put(symbol, shakeArr);
                IndexMap.put(symbol, 0);
            }
        }
    }

    // 随便分析下
    // @Scheduled(initialDelay = 2 * 1000, fixedRate = 1 * 60 * 1000)
    public void analyze() {
        for (IntervalEnum interval : shakeCache.keySet()) {
            Map<String, Double[]> symbolShakeMap = shakeCache.get(interval);
            Map<String, Double> result = new HashMap<>();
            for (String symbol : symbolShakeMap.keySet()) {
                // shake data arr and cur index
                Double[] shakeArr = symbolShakeMap.get(symbol);
                Integer index = indexCache.get(interval).get(symbol);
                // 当前值较过去最低值的涨幅
                double curVal = shakeArr[index];
                result.put(symbol, curVal - Arrays.stream(shakeArr).filter(e -> ObjectUtil.isNotEmpty(e)).min(Double::compareTo).get());
            }
            List<Map.Entry<String, Double>> collect = result.entrySet().stream().sorted(
                    (ele01, ele02) -> ele02.getValue().intValue() - ele01.getValue().intValue()
            ).collect(Collectors.toList());
            for (Map.Entry<String, Double> entry : collect) {
                String symbol = entry.getKey();
                Double diffVal = entry.getValue();
                System.out.println(symbol + " : " + diffVal);
            }
        }
    }
}
