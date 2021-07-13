package top.ychen5325.smartPool.server;

import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.common.CallResult;
import top.ychen5325.smartPool.common.IntervalEnum;
import top.ychen5325.smartPool.common.UrlConfig;
import top.ychen5325.smartPool.model.SymbolShock;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 网格策略快速模拟跑盘
 * 数据仅供参考。
 *
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 * @apiNote 对机枪池的推荐进行历史回测
 */
@Slf4j
@Component
public class BackTestService {

    @Resource
    public RestTemplate restTemplate;

    /**
     * 每格价格和支配资金
     */
    BigDecimal gridPrice;
    BigDecimal gridBalance;
    BigDecimal usdtBalance = BigDecimal.valueOf(100);
    BigDecimal nextPayPrice;
    BigDecimal nextSellPrice;

    BigDecimal monthRate;

    List<Double> prices = new ArrayList<>();
    int index = 0;
    /**
     * 所有未卖出买单的loverId&buyQty
     */
    Stack<String> qtyStack = new Stack<>();


    private void clear() {
        index = 0;
        prices.clear();
        monthRate = BigDecimal.ZERO;
        nextSellPrice = null;
        nextPayPrice = null;
        usdtBalance = BigDecimal.valueOf(100);
        gridBalance = null;
        gridPrice = null;
        qtyStack.clear();
    }

    /**
     * 对币种在指定周期内进行历史行情回测、最后返回其回测理论月化率
     *
     * @param symbol
     * @param highP     震荡上限价格
     * @param lowP      震荡下限价格
     * @param incRate   振幅
     * @param startTime 回测开始时间
     * @param endTime   回测结束时间
     * @return 其周期内理论月化率
     */
    public BigDecimal startRobot(String symbol, BigDecimal highP, BigDecimal lowP, BigDecimal incRate, Long startTime, Long endTime) {
        // 1、先获取kline
        if (!initKlines(symbol, startTime, endTime)) {
            log.error("{}k线失败", symbol);
            return null;
        }
        // 2、初始化机器人
        initRobot(highP, lowP, incRate);
        // 3、模拟跑盘
        patrol();
        // 4、返回月化
        return monthRate;
    }

    private void initRobot(BigDecimal highP, BigDecimal lowP, BigDecimal incRate) {
        highP = highP.multiply(
                incRate.add(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(100), 8, RoundingMode.DOWN)
        );
        lowP = lowP.multiply(
                BigDecimal.valueOf(100).subtract(incRate)
                        .divide(BigDecimal.valueOf(100)));

        BigDecimal gridCount = incRate.multiply(BigDecimal.valueOf(9));

        gridPrice = (highP.subtract(lowP).divide(gridCount, 8, RoundingMode.DOWN));
        gridBalance = usdtBalance.divide(gridCount, 8, RoundingMode.DOWN);
        BigDecimal curP = BigDecimal.valueOf(prices.get(index));
        nextPayPrice = curP.subtract(gridPrice);
        nextSellPrice = curP.add(gridPrice);

        int unFilledCount = (highP.subtract(curP).divide(gridPrice, 8, BigDecimal.ROUND_DOWN)).intValue();
        // 可使用的资金、下单、获取实际购买数量、实际花费的资金和手续费
        double useFunds = gridBalance.doubleValue() * unFilledCount;
        double planQty = useFunds / curP.doubleValue();

        usdtBalance = usdtBalance.subtract(BigDecimal.valueOf(useFunds));

        for (int i = 0; i < unFilledCount; i++) {
            qtyStack.push(RandomUtil.randomLong(0, Long.MAX_VALUE) + "".concat("&").concat(String.valueOf(planQty / unFilledCount)));
        }
    }

    /**
     * 获取回测周期内对k线列表、
     */
    private boolean initKlines(String symbol, Long startTime, Long endTime) {
        int count = 0;
        do {
            try {
                clear();
                monthRate = BigDecimal.ZERO;
                CallResult result = restTemplate.getForObject(
                        String.format(UrlConfig.klineListUrl, symbol, startTime, endTime), CallResult.class);
                prices = (List<Double>) result.getData();
                return true;
            } catch (Exception e) {
                log.info("{}k线获取失败:{}", symbol, e.getMessage());
            }
        } while (count++ < 3);
        return false;
    }

    /**
     * 模拟跑盘
     */
    private void patrol() {
        while (true) {
            if (index == prices.size()) {
                return;
            }
            BigDecimal realPrice = BigDecimal.valueOf(prices.get(index++));
            if (realPrice.compareTo(nextPayPrice) < 1) {
                if (usdtBalance.compareTo(gridBalance) > -1) {
                    // 执行买入
                    usdtBalance = usdtBalance.subtract(gridBalance);

                    nextPayPrice = realPrice.subtract(gridPrice);
                    nextSellPrice = realPrice.add(gridPrice);

                    qtyStack.push(RandomUtil.randomLong(0, Long.MAX_VALUE) + "".concat("&").concat(String.valueOf(gridBalance.doubleValue() / realPrice.doubleValue())));
                }
            } else if (realPrice.compareTo(nextSellPrice) > -1) {
                if (!qtyStack.isEmpty()) {
                    // 获取栈顶节点
                    String curNode = qtyStack.pop();
                    String[] split = curNode.split("&");
                    String exeQty = split[1];

                    BigDecimal income = new BigDecimal(exeQty).multiply(realPrice);

                    monthRate = monthRate.add(income.subtract(gridBalance));

                    usdtBalance = usdtBalance.add(income);

                    nextPayPrice = realPrice.subtract(gridPrice);
                    nextSellPrice = realPrice.add(gridPrice);
                }
            }
        }
    }

    /**
     * 入口函数
     * 对机枪池计算币种震荡数据进行指定周期内回测、并返回其理论月化、
     *
     * @return ["symbol \t monthRate"]
     */
    public List<String> backTestHandler(IntervalEnum intervalEnum, List<SymbolShock> symbolShocks) {
        try {
            log.info("【回测池】周期:{},开始。。。。", intervalEnum.toString());
            List<String> backTestPool = new ArrayList<>();
            Long time = intervalEnum.time;
            for (SymbolShock symbolShock : symbolShocks) {
                log.info("【回测池】币种:{}。。。", symbolShock.getSymbol());
                try {
                    BigDecimal monthRate = this.startRobot(
                            symbolShock.getSymbol(),
                            symbolShock.getMaxPrice(),
                            symbolShock.getMinPrice(),
                            symbolShock.getIncRate(),
                            System.currentTimeMillis() - time,
                            System.currentTimeMillis()
                    );
                    if (Objects.isNull(monthRate)) {
                        continue;
                    }
                    backTestPool.add(
                            symbolShock.getSymbol()
                                    .concat("\t" + monthRate.multiply(BigDecimal.valueOf(30))
                                            .setScale(2, RoundingMode.DOWN)
                                            .toPlainString()));
                } catch (Exception e) {
                    log.error("{},msg:{}", symbolShock.getSymbol(), e.getMessage());
                    e.printStackTrace();
                }
            }
            // 降序、截取20
            return backTestPool.stream().sorted((a, b) -> {
                Double a1 = Double.valueOf(a.split("\t")[1]);
                Double b1 = Double.valueOf(b.split("\t")[1]);
                return b1.compareTo(a1);
            }).limit(20).collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        CallResult<List<Map>> result = new RestTemplate().getForObject("http://www.ychen5325.top/api/v1/smart/list/shock/d1/101", CallResult.class);
        List<SymbolShock> shockModels = result.getData().stream().map(e -> JSON.parseObject(JSON.toJSONString(e), SymbolShock.class)).collect(Collectors.toList());
        List<String> res = new ArrayList<>();
        shockModels.forEach(e -> {
            BackTestService service = new BackTestService();
            service.restTemplate = new RestTemplate();
            res.add(e.getSymbol().concat("\t" +
                    service.startRobot(e.getSymbol(),
                            e.getMaxPrice(),
                            e.getMinPrice(),
                            e.getIncRate(),
                            1618566404000L, 1618652804000L).multiply(BigDecimal.valueOf(30))
                            .setScale(2, RoundingMode.DOWN).toPlainString()));
        });
        res.sort((a, b) -> {
            Double a1 = Double.valueOf(a.split("\t")[1]);
            Double b1 = Double.valueOf(b.split("\t")[1]);
            return a1 > b1 ? -1 : 1;
        });
        res.forEach(e -> System.out.println(e));

    }
}
