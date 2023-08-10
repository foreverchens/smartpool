package top.ychen5325.smartPool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class Kline {

    /**
     * [
     * 1499040000000,      // 开盘时间
     * "0.01634790",       // 开盘价
     * "0.80000000",       // 最高价
     * "0.01575800",       // 最低价
     * "0.01577100",       // 收盘价(当前K线未结束的即为最新价)
     * "148976.11427815",  // 成交量
     * 1499644799999,      // 收盘时间
     * "2434.19055334",    // 成交额
     * 308,                // 成交笔数
     * "1756.87402397",    // 主动买入成交量
     * "28.46694368",      // 主动买入成交额
     * "17928899.62484339" // 请忽略该参数
     * ]
     */
    Long id;
    Long openTime;
    Long closeTime;
    BigDecimal openPrice;
    BigDecimal closePrice;
    BigDecimal maxPrice;
    BigDecimal minPrice;
    BigDecimal txAmount;
}
