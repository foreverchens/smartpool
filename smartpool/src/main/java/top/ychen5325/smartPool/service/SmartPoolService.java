package top.ychen5325.smartPool.service;

import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.model.KlineForBa;
import top.ychen5325.smartPool.model.SymbolShake;

import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Component
public class SmartPoolService {

    @Resource
    private KlineService klineService;

    /**
     * 入口函数
     *
     * @param symbols 币种列表
     * @param period  周期
     * @return 币种的分析结果集合
     */
    public List<SymbolShake> klineAnalyze(List<String> symbols, IntervalEnum period) {
        log.info("周期:{},开始更新震荡池......", period.toString());
        List<SymbolShake> shakeModels = new ArrayList<>(symbols.size() * 2);
        for (String symbol : symbols) {
//            log.info("币种:{} start....", symbol);
            /**
             * 获取震荡区间和震频量化值
             * res[0] 震频值 根据算法得到的震荡指标、可直接参考。
             * res[1] 震荡区间下限价格
             * res[2] 震荡区间上限价格
             * res[3] 均值方差、方差整体平稳情况、数值越小、相对越平稳
             */
            Double[] res = this.doKlineAnalyze(symbol, period);
            if (Objects.isNull(res)) {
                continue;
            }
            // 结果为 ${incRate}%  震荡区间上下跨幅
            BigDecimal incRate = BigDecimal.valueOf((res[2] - res[1]) * 100 / res[1]).setScale(3, RoundingMode.DOWN);
            double meanVariance = res[3];
            shakeModels.add(SymbolShake.builder()
                    .symbol(symbol)
                    .shakeVal(res[0].longValue())
                    .incRate(incRate)
                    .maxPrice(BigDecimal.valueOf(res[2]))
                    .minPrice(BigDecimal.valueOf(res[1]))
                    .meanVariance(meanVariance)
                    .build());
        }

        /**
         *  主要关注 shockVal 和 meanVariance指标
         *  一般来说、前者具备更高优先级、数值越大、越震荡、但走势是多样的、
         *  后者作为辅助参考、shockVal在同一范围内、meanVariance越小、代表震荡点位越密集、单边走势势能越低
         *  即具备更高的套利价值【shockVal越大&&meanVariance越小】
         */
        return shakeModels.stream()
                // 均值方差小于125 * (假设振幅上限为10%、局部区间取的5%、则最优方差为 2.5^2 * 20=125)
                .filter(e -> e.getMeanVariance() < 125 * period.time / 1440 / 1000 / 60)
                .sorted((e1, e2) -> {
                    Double v1 = e1.getShakeVal() / 50;
                    Double v2 = e2.getShakeVal() / 50;
                    int res = v2.intValue() - v1.intValue();
                    if (res == 0) {
                        Double m1 = e1.getMeanVariance();
                        Double m2 = e1.getMeanVariance();
                        return m1.intValue() - m2.intValue();
                    }
                    return res;
                })
                .limit(10)
                .collect(Collectors.toList());
    }


    /**
     * 传入币种和周期获取kline分析数据
     *
     * @return data[0] shakeVal,
     * data[1] lowP,
     * data[2] highP,
     * data[3] meanVariance
     */
    private Double[] doKlineAnalyze(String symbol, IntervalEnum period) {
        try {
            // 先更新kline
            klineService.updateKline(symbol);

            List<KlineForBa> klines = klineService.getKline(symbol, period.time);

            // 最高价、最低价、均价
            double maxPrice = klines.stream().max(Comparator.comparing(KlineForBa::getMaxPrice)).get().getMaxPrice().doubleValue();
            double minPrice = klines.stream().min(Comparator.comparing(KlineForBa::getMinPrice)).get().getMinPrice().doubleValue();
            double avgPrice = klines.stream().map(e -> e.getOpenPrice().add(e.getClosePrice()).doubleValue()).collect(Collectors.averagingDouble(e -> e)) / 2;

            /**
             * 均价的千分之一作为分布曲线最小单位
             */
            double scalePrice = avgPrice / 1000;

            /**
             * 数组长度定义
             */
            int len = (int) ((maxPrice - minPrice) / scalePrice) + 1;
            int[] priceCountArr = new int[len];
            for (KlineForBa kline : klines) {
                double openPrice = kline.getOpenPrice().doubleValue();
                double closePrice = kline.getClosePrice().doubleValue();
                double lowPrice = Math.min(openPrice, closePrice);
                double highPrice = Math.max(openPrice, closePrice);

                // 撒点、计算离minP的价格距离
                int li = (int) ((lowPrice - minPrice) / scalePrice);
                int ri = (int) ((highPrice - minPrice) / scalePrice);
                priceCountArr[li + 1 == len ? li : li + 1]++;
                priceCountArr[ri + 1 == len ? ri : ri + 1]--;
            }

            // 总点数统计
            int totalShakePoint = priceCountArr[0];
            for (int i = 1; i < priceCountArr.length; i++) {
                priceCountArr[i] += priceCountArr[i - 1];
                totalShakePoint += priceCountArr[i];
            }
            /**
             * 将得到的点位分布曲线向中收缩10%、定位震荡区间、
             */
            float sparseLimit = 0.1F;
            // 至少在两端应该去除的点数和
            int sparseCount = (int) (totalShakePoint * sparseLimit);
            // 双指针记录去除双端点数后的位置、双指针之间即为密集点位区间
            int left = 1, right = priceCountArr.length - 1;
            int tmpCount = 0;
            while (left < right) {
                if (priceCountArr[left] < priceCountArr[right]) {
                    tmpCount += priceCountArr[left++];
                } else {
                    tmpCount += priceCountArr[right--];
                }
                if (tmpCount > sparseCount) {
                    break;
                }
            }
            // 定义已经确定的分析数据
            Double[] res = {1D * totalShakePoint, (minPrice + scalePrice * left), (minPrice + scalePrice * right), 0.0};
            int size = klines.size();
            // 单边行情分析
            /**
             * 获取其均值行情、和震荡幅度、
             *  比较首尾差值、如果接近振幅、则代表可能单边行情、
             *  如果首尾相近、检查是否有山顶和谷底、如果有则代表山峰行情
             *
             *  获取整体均值、
             *  在获取局部均值如5%区间和整体均值的差值比率、(区分正负)、(差值比率将在[0%,100%]、0即为等于均值、100则代表严重偏离均值)
             *  最后如果是单边上涨行情、将得到类似 [-30%,-20%,-10%,5%,15%,50%]的区间
             *  如果是震荡行情、将得到类似 [4%,1%,-2%,0%,-1%,2%]相等趋近于0的的区间
             */
            // 整体平均价格
            double overallAvgPrice = klines.stream()
                    .collect(Collectors.averagingDouble(e -> e.getOpenPrice().add(e.getClosePrice()).doubleValue())) / 2;
            // 以5%为最小区间分割、得到20个小区间的均值集合
            List<Double> avgList = new ArrayList<>();
            for (int i = 0; i < 20; i++) {
                int from = size * (i * 5) / 100;
                int to = size * (i * 5) / 100 + size * 5 / 100;
                double avgP = 0;
                while (from < to) {
                    KlineForBa kline = klines.get(from);
                    avgP += kline.getOpenPrice().add(kline.getClosePrice()).doubleValue() / 2;
                    from++;
                }
                avgP /= (size * 5 / 100);
                if (!Double.isNaN(avgP)) {
                    avgList.add(avgP);
                }
            }

            /**
             * 得到各小区间的方差并求和
             */
            Double meanVariance = avgList.stream().map(avgP ->
                    BigDecimal.valueOf(Math.pow(((avgP - overallAvgPrice) / overallAvgPrice * 100), 2))
                            .setScale(2, RoundingMode.DOWN).doubleValue()
            ).collect(Collectors.summingDouble(e -> e));
            // 方差值
            res[3] = meanVariance;
            return res;
        } catch (RuntimeException e) {
            log.error("klineAnalyze,err:" + e.getMessage(), e);
            return null;
        }
    }
}