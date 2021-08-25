package top.ychen5325.smartPool.server;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSON;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.common.UrlConfig;
import top.ychen5325.smartPool.model.KlineForBa;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 维护所有币种最近周期的k线数据、并提供各种get函数
 *
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Data
@Component
public class KlineService {

    private Map<String, KlineForBa[]> klineCache;
    private Map<String, Integer> indexCache;

    // 三天前开始
    private Long beforeTime = 1000 * 60 * 60 * 24 * 3L;

    // 最长维护五天
    private int longestMin = 5 * 24 * 60;

    @Resource
    private RestTemplate restTemplate;
    @Resource
    private SymbolService symbolService;

    /**
     * 获取所有币种、初始化kline起始点、
     */
    @PostConstruct
    public void init() {
        List<String> symbols = symbolService.listContractSymbol();
        klineCache = new HashMap<>(symbols.size() * 3 / 2);
        indexCache = new HashMap<>(symbols.size() * 3 / 2);
        // 初始化最近时间kline
        Long initTime = System.currentTimeMillis() / 1000 / 60 * 1000 * 60 - beforeTime;
        for (String symbol : symbols) {
            KlineForBa[] klines = new KlineForBa[longestMin];
            klines[0] = KlineForBa.builder().openTime(initTime).build();
            klineCache.put(symbol, klines);
            indexCache.put(symbol, 0);
        }
    }

    /**
     * 更新kline缓存
     */
    public void updateKline(String symbol) {
        // 指针和kline
        KlineForBa[] klineArr = klineCache.get(symbol);
        int index = indexCache.get(symbol);
        // 获取最新的kline开盘时间
        KlineForBa lastKline = klineArr[index];
        long lastOpenTime = lastKline.getOpenTime();

        Long curTime = System.currentTimeMillis() / 1000 / 60 * 1000 * 60;
        while (lastOpenTime < curTime) {
            // 如果时间差小于12个小时、则全量获取、如果大于12个小时、则获取720条1m级别k线
            List<KlineForBa> klineList = listKline(symbol, "1m", lastOpenTime, curTime, 720);
            if (CollectionUtil.isEmpty(klineList)) {
                break;
            }
            // 先回走一步、后续在前进一步、把其指向的kline更新一下
            index = (index + klineArr.length - 1) % klineArr.length;
            for (KlineForBa kline : klineList) {
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
    public List<KlineForBa> getKline(String symbol, long period) {
        // 获取kline和指针
        KlineForBa[] klineArr = klineCache.get(symbol);
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
            return Collections.emptyList();
        }
        int startIndex = (curIndex + klineArr.length - timeLen) % klineArr.length;
        // 顺序遍历返回
        List<KlineForBa> result = new ArrayList<>(timeLen * 3 / 2);
        if ((startIndex = startIndex < 0 ? -startIndex : startIndex) > curIndex) {
            curIndex += klineArr.length;
        }
        while (startIndex < curIndex) {
            KlineForBa cacheKline = klineArr[startIndex % klineArr.length];
            KlineForBa val = new KlineForBa();
            if (cacheKline == null) {
                System.out.println(startIndex);
            }
            BeanUtils.copyProperties(cacheKline, val);
            result.add(val);
            startIndex++;
        }
        return result;
    }


    /**
     * 获取指定币种、k线数据
     *
     * @param symbol    币种
     * @param interval  时间间隔 [1m、3m、5m、15m、30m、1h、2h、4h、6h、8h、12h、 1d、3d、1w、1M]
     * @param startTime 开始时间戳[毫秒]
     * @param endTime   结束时间戳[毫秒]
     * @param limit     返回个数
     * @return klineList
     */
    private List<KlineForBa> listKline(String symbol, String interval, Long startTime, Long endTime, Integer limit) {
        try {
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
        } catch (Exception ex) {
            log.error("listKline.ex:", ex.getMessage(), ex);
            return Collections.EMPTY_LIST;
        }
    }
}


