package top.ychen5325.smartPool.server;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.common.UrlConfig;
import top.ychen5325.smartPool.model.KlineForBa;
import top.ychen5325.smartPool.model.SymbolShock;

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
public class SmartPoolService {

    @Resource
    private RestTemplate restTemplate;

    /**
     * 获取支持合约交易的币种列表
     *
     * @return
     */
    public List<String> listContractSymbol() {
        JSONObject resp = restTemplate.getForObject(UrlConfig.listSymbolsUrl, JSONObject.class);
        List<String> symbols = resp.getJSONArray("symbols")
                .stream()
                .map(e -> ((Map<String, String>) e).get("symbol"))
                .collect(Collectors.toList());
        return symbols;
    }

    /**
     * 获取指定币种、k线数据
     *
     * @param symbol    币种
     * @param interval  时间间隔 [1m、3m、5m、15m、30m、1h、2h、4h、6h、8h、12h、 1d、3d、1w、1M]
     * @param startTime 开始时间戳[毫秒]
     * @param endTime   结束时间戳[毫秒]
     * @param limit     返回个数
     * @return
     */
    private List<KlineForBa> listKline(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        String reqUrl = String.format(UrlConfig.kLinesUrl, symbol, interval);
        if (startTime != null) {
            reqUrl = reqUrl.concat("&startTime=" + startTime);
        }
        if (endTime != null) {
            reqUrl = reqUrl.concat("&endTime=" + endTime);
        }
        if (limit != null) {
            reqUrl = reqUrl.concat("&limit=" + limit);
        }
        // log.info("list kline url is {}", reqUrl);

        String resp = restTemplate.getForObject(reqUrl, String.class);
        List<List> klines = JSON.parseArray(resp, List.class);

        // 结果集
        List<KlineForBa> result = new ArrayList<>(klines.size() * 2);

        for (List cur : klines) {
            List kline = cur;
            Long openTime = (long) kline.get(0);
            Long closeTime = (long) kline.get(6);
            String openPrice = kline.get(1).toString();
            String maxPrice = kline.get(2).toString();
            String minPrice = kline.get(3).toString();
            String closePrice = kline.get(4).toString();
            // 交易总额
            String txAmount = kline.get(7).toString();
            result.add(KlineForBa.builder()
                    .openTime(openTime)
                    .closeTime(closeTime)
                    .openPrice(new BigDecimal(openPrice))
                    .maxPrice(new BigDecimal(maxPrice))
                    .minPrice(new BigDecimal(minPrice))
                    .closePrice(new BigDecimal(closePrice))
                    .txAmount(new BigDecimal(txAmount))
                    .build());
        }
        return result;
    }

    /**
     * @param symbol 交易对
     * @param period 周期
     */
    public Double[] shockAnalyze(String symbol, IntervalEnum period) {
        try {
            Long curTime = System.currentTimeMillis() / 1000 * 1000;
            Long time = period.time;
            List<KlineForBa> klines = new ArrayList<>();
            do {
                // 每次获取12 个小时的 1m k线数据
                klines.addAll(listKline(symbol, "1m", curTime - time, null, 720));
                time -= 43200000;
            } while (time > 0);

            // 最高价、最低价、中间价
            double maxPrice = klines.stream().max(Comparator.comparing(KlineForBa::getMaxPrice)).get().getMaxPrice().doubleValue();
            double minPrice = klines.stream().min(Comparator.comparing(KlineForBa::getMinPrice)).get().getMinPrice().doubleValue();


            // 均价
            double avgPrice = klines.stream().map(e -> e.getOpenPrice().add(e.getClosePrice()).doubleValue()).collect(Collectors.averagingDouble(e -> e)) / 2;

            /**
             * 最小价格百分比、即avgPrice的千分之1
             * 使用
             * 正向累加到 maxPrice、
             * 负向累减到 minPrice
             */
            double scalePrice = avgPrice / 1000;

            // 获取Y轴价格轴的刻度价格、一般币种的震荡幅度不会超过上下20%、暂取均价上下10%作为其震荡区间、并细分100份、此处可大数据优化、

            /**
             * 数组长度定义、
             * [avgP-x,avgP-x+1,avgP-1,avgP,avgP+1,avgP+x-1,avgP+x]
             */
            int len = (int) ((maxPrice - minPrice) / scalePrice) + 1;
            int[] priceCountArr = new int[len];
            for (KlineForBa kline : klines) {
                double openPrice = kline.getOpenPrice().doubleValue();
                double closePrice = kline.getClosePrice().doubleValue();
                double lowPrice = Math.min(openPrice, closePrice);
                double highPrice = Math.max(openPrice, closePrice);

                if (lowPrice >= avgPrice * (1 + ((maxPrice - avgPrice) / avgPrice) / 2) || highPrice <= avgPrice * (1 - ((avgPrice - minPrice) / avgPrice) / 2)) {
                    continue;
                }

                for (int i = 0; i < priceCountArr.length; i++) {
                    double curPrice = minPrice + (i * scalePrice);
                    if (lowPrice <= curPrice && highPrice >= curPrice) {
                        priceCountArr[i]++;
                    }
                }
            }



/*            // 最小价格尺度、
            double scalePrice = (maxPrice - minPrice) / 10000;

            *//**
             * 最小价格尺度固定为0.1、其他价格等比缩放到100、缩放比例为均价缩放比率
             *//*
            scalePrice = 0.1;
            // 均价
            double avgPrice = klines.stream().map(e -> e.getOpenPrice().add(e.getClosePrice()).doubleValue()).collect(Collectors.averagingDouble(e -> e)) / 2;
            double avgRate = 100 / avgPrice;


//          获取Y轴价格轴的刻度价格、一般币种的震荡幅度不会超过上下20%、暂取均价上下10%作为其震荡区间、并细分100份、此处可大数据优化、

            int len = (int) ((maxPrice - minPrice) * avgRate / scalePrice);
            int[] priceCountArr = new int[len];
            for (KlineForBa kline : klines) {
                double openPrice = kline.getOpenPrice().doubleValue();
                double closePrice = kline.getClosePrice().doubleValue();
                double lowPrice = Math.min(openPrice, closePrice) * avgRate;
                double highPrice = Math.max(openPrice, closePrice) * avgRate;
                for (int i = 0; i < priceCountArr.length; i++) {
                    double curPrice = minPrice * avgRate + (i * scalePrice);
                    if (lowPrice <= curPrice && highPrice >= curPrice) {
                        priceCountArr[i]++;
                    }
                }
            }*/
            int ShockVal = Arrays.stream(priceCountArr).sum();
//            log.info(" {} 震频值: {}", symbol, ShockVal);
            /**
             * 稀疏极限
             *     最后得到的正态分布曲线、我们将两端的点位和不超过总数10%的区间去除、剩下的即为震荡区间
             */
            float sparseLimit = 0.1F;
            int sparseCount = (int) (ShockVal * sparseLimit);
            int left = 0, right = priceCountArr.length - 1;
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
            /**
             * 套利次数分析
             */

//        log.info("包含90%点位区间:[{},{}],总点位数:{},对应价格区间为:[{},{}]", left, right, pointCount-tmpCount,minPrice + interval * left, minPrice + interval * right);
//        log.info(" {} 过去24h包含90%点位的震荡区间为:[{},{}]", symbol, minPrice + interval * left, minPrice + interval * right);
//            return String.format("%s当前所处震荡区间:[%s,%s]", symbol, minPrice + interval * left, minPrice + interval * right);
            Double[] res = {1D * ShockVal, (minPrice + scalePrice * left), (minPrice + scalePrice * right), 0.0};
//            Double[] res = {1D * ShockVal, (minPrice * avgRate + scalePrice * left) / avgRate, (minPrice * avgRate + scalePrice * right) / avgRate, 0.0};
            /**
             *  一下三类情况不适合做网格
             *  1、单边上涨行情
             *  2、单边下跌行情
             *  3、山峰和盆地行情
             */
            double maxP = res[2];
            double minP = res[1];
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
             *  如果是震荡行情、将得到类似 [4%,1%,-2%,0%,-1%,2%]的区间
             */
            // 整体平均价格
            double overallAvgPrice = klines.stream()
                    .collect(Collectors.averagingDouble(e -> e.getOpenPrice().add(e.getClosePrice()).doubleValue())) / 2;
            // 假设以5%为最小区间分割、则将得到区间为
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
                avgList.add(avgP);
            }

            /**
             * 均值方差、反映整体的震荡情况
             */
            Double avgVariance = avgList.stream().map(avgP ->
                    BigDecimal.valueOf(Math.pow(((avgP - overallAvgPrice) / overallAvgPrice * 100), 2))
                            .setScale(2, RoundingMode.DOWN).doubleValue()
            ).collect(Collectors.summingDouble(e -> e));
            // 方差值
            res[3] = avgVariance;
            /**
             * 单边行情：两边差值均较大、且符号相反、中间值接近均值、
             */
            /*Double leftAvg = avgList.get(0);
            Double rightAvg = avgList.get(avgList.size() - 1);
            if (Math.abs(rightAvg - leftAvg) > (maxP - minP) * 0.9) {
                // 左右区间平均值差值 大于 1/2倍震荡区间 则认为处于单边上涨或下跌行情
                log.error("{}处于单边行情", symbol);
                map.get("uni").add(symbol.concat("--"+leftAvg).concat("--"+rightAvg));
                return null;
            }*/
            return res;
        } catch (Exception e) {
            log.info(e.getMessage());
            return null;
        }
    }

    /**
     * 入口函数
     */
    public List<SymbolShock> shockAnalyzeHandler(List<String> symbols, IntervalEnum period) {
        log.info("周期:{},开始更新震荡池......", period.toString());
        List<SymbolShock> shockModels = new ArrayList<>(symbols.size() * 2);
        for (String symbol : symbols) {
            log.info("币种:{} start....", symbol);
            /**
             * res[0] 震频值
             * res[1] 震荡区间下限价格
             * res[2] 震荡区间上限价格
             */
            Double[] res = this.shockAnalyze(symbol, period);
            if (Objects.isNull(res)) {
                continue;
            }
            // 结果为 ${incRate}%
            BigDecimal incRate = BigDecimal.valueOf((res[2] - res[1]) * 100 / res[1]).setScale(3, RoundingMode.DOWN);
            double meanVariance = res[3];
            shockModels.add(SymbolShock.builder()
                    .symbol(symbol)
                    .ShockVal(res[0].longValue())
                    .incRate(incRate)
                    .maxPrice(BigDecimal.valueOf(res[2]))
                    .minPrice(BigDecimal.valueOf(res[1]))
                    .meanVariance(meanVariance)
                    .build());
        }

        /**
         *  shockVal 和 incRate 基本成反比、需要加权求和、
         *  shock 对其中位数求百分比、过滤出正值、
         *  incRate 对其中位数求百分比、过滤出正值
         */
        // 获取shockVal 和 incRate中位数
        shockModels.sort((a, b) -> (int) (a.getShockVal() - b.getShockVal()));
        double shockValMedian = shockModels.size() % 2 == 0
                ? (shockModels.get(shockModels.size() / 2).getShockVal() + shockModels.get((shockModels.size() - 1) / 2).getShockVal()) / 2
                : shockModels.get(shockModels.size() / 2).getShockVal();

        shockModels.sort(Comparator.comparing(SymbolShock::getIncRate));
        BigDecimal incRateMedian = shockModels.size() % 2 == 0
                ? (shockModels.get(shockModels.size() / 2).getIncRate().add(shockModels.get((shockModels.size() - 1) / 2).getIncRate())).divide(BigDecimal.valueOf(2), 6, RoundingMode.DOWN)
                : shockModels.get(shockModels.size() / 2).getIncRate();

        List<SymbolShock> result = shockModels.stream()
                // .filter(e -> e.getShockVal() >= shockValMedian /*&& e.getIncRate().compareTo(incRateMedian) > -1*/)

                // 均值方差小于125 * (假设振幅上限为10%、局部区间取的5%、则最优方差为 2.5^2 * 20=125)
                .filter(e -> e.getMeanVariance() < 125 * period.time / 1440 / 1000 / 60)
                .sorted((e1, e2) -> {
                    double e1ShockRate = e1.getShockVal() / shockValMedian - 1;
                    double e2ShockRate = e2.getShockVal() / shockValMedian - 1;
                    double e1IncRate = e1.getIncRate().divide(incRateMedian, 6, RoundingMode.DOWN).subtract(BigDecimal.ONE).doubleValue();
                    double e2IncRate = e2.getIncRate().divide(incRateMedian, 6, RoundingMode.DOWN).subtract(BigDecimal.ONE).doubleValue();
                    e1.setRecomRatio(e1IncRate + e1ShockRate);
                    e2.setRecomRatio(e2IncRate + e2ShockRate);
                    // return (e2ShockRate + e2IncRate) > (e1ShockRate + e1IncRate) ? 1 : -1;
                    return e2ShockRate > e1ShockRate ? 1 : -1;
                })
//                .limit(20)
                .collect(Collectors.toList());
        return result;
    }


    public static void main(String[] args) {
        List<Map<String, Double>> list = new ArrayList<>();
        for (int i = 0; i < 121; i++) {
            Map<String, Double> map = new HashMap<>();
            map.put("age", (double) i);
            list.add(map);
        }
        int size = list.size();
        // 单边行情分析
        Double leftAvg = list.subList(0, size / 10).stream().collect(Collectors.averagingDouble(a -> a.get("age")));
        Double rightAvg = list.subList((int) (size * 0.9), size).stream().collect(Collectors.averagingDouble(a -> a.get("age")));
        double midAvg = list.subList((int) (size * 0.4), (int) (size * 0.6)).stream().collect(Collectors.averagingDouble(a -> a.get("age")));
        log.info("{}-{}-{}", leftAvg, rightAvg, midAvg);
        log.info("{}\n{}\n{}", list.subList(0, size / 10), list.subList((int) (size * 0.9), size), list.subList((int) (size * 0.4), (int) (size * 0.6)));

    }


}