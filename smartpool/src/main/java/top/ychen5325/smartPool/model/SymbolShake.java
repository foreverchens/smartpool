package top.ychen5325.smartPool.model;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */

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
public class SymbolShake {

    String symbol;
    /**
     * 根据算法计算得到的震频值
     */
    double shakeVal;

    /**
     * 震荡区间的振幅绝对值
     */
    BigDecimal incRate;

    /**
     * 震荡区间
     */
    BigDecimal maxPrice;
    BigDecimal minPrice;

    /**
     * 局部均值方差
     */
    double meanVariance;
}

