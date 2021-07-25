package top.ychen5325.smartpool.service;

import cn.hutool.core.util.ObjectUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.model.KlineForBa;
import top.ychen5325.smartPool.server.KlineService;
import top.ychen5325.smartPool.server.SymbolService;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
//@SpringBootTest
//@RunWith(SpringRunner.class)
public class KlineServiceTest {

    KlineService klineService;
    SymbolService symbolService;

    SimpleDateFormat yMdHms;

    @Before
    public void before() {
        yMdHms = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        symbolService = new SymbolService();
        symbolService.setSymbols(Arrays.asList("BTCUSDT"));
        symbolService.setRestTemplate(new RestTemplate());

        klineService = new KlineService();
        klineService.setRestTemplate(new RestTemplate());
        klineService.setSymbolService(symbolService);
    }


    /**
     * 测试拉取最近10分钟kline更新
     */
    @Test
    public void tenMinTest() {
        String symbol = "BTCUSDT";
        // 设置从10分钟前开始拉取
        // 60min
        klineService.setLongestMin(60);
        // 10分钟前
        klineService.setBeforeTime(1000 * 60 * 10L);
        klineService.init();


        klineService.updateKline(symbol);
        KlineForBa[] klineList = klineService.getKlineCache().get(symbol);
        Integer index = klineService.getIndexCache().get(symbol);
        System.out.println("index=" + index);
        for (int i = 0; i < klineList.length; i++) {
            KlineForBa kline = klineList[i];
            if (ObjectUtil.isNotEmpty(kline)) {
                System.out.println("openTime = " + yMdHms.format(kline.getOpenTime()));
            }
        }
    }


    /**
     * 连续更新两次测试并且超过最大可存储区间
     */
    @Test
    public void twoUpTest() throws InterruptedException {
        String symbol = "BTCUSDT";
        // 设置从10分钟前开始拉取
        // 12min、先拉取10分钟、睡3分钟、在拉取达到覆盖条件
        klineService.setLongestMin(12);
        // 10分钟前
        klineService.setBeforeTime(1000 * 60 * 10L);
        klineService.init();

        int count = 2;
        while (count-- > 0) {
            klineService.updateKline(symbol);
            KlineForBa[] klineList = klineService.getKlineCache().get(symbol);
            Integer index = klineService.getIndexCache().get(symbol);
            System.out.println("index=" + index);
            for (int i = 0; i < klineList.length; i++) {
                KlineForBa kline = klineList[i];
                if (ObjectUtil.isNotEmpty(kline)) {
                    System.out.println("openTime = " + yMdHms.format(kline.getOpenTime()));
                }
            }
            TimeUnit.MINUTES.sleep(3);
        }
    }

    @Test
    public void getKlineTest() throws InterruptedException {
        String symbol = "BTCUSDT";
        long threeMin = 1000 * 60 * 3;
        // 数组长度为10
        klineService.setLongestMin(10);
        // 8分钟前
        klineService.setBeforeTime(1000 * 60 * 8L);
        // 先初始化并更新最近8分钟的kline
        klineService.init();

        while (true) {
            // 获取最近三分钟kline为
            klineService.updateKline(symbol);
            List<KlineForBa> klineList = klineService.getKline(symbol, threeMin);
            System.out.println("当前时间：" + yMdHms.format(System.currentTimeMillis()));
            for (KlineForBa kline : klineList) {
                System.out.println(yMdHms.format(kline.getOpenTime()));
            }
            System.out.println("sleep 3 min");
            // 睡3分钟
            TimeUnit.MINUTES.sleep(3);
        }
    }
}
