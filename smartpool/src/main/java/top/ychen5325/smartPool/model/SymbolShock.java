package top.ychen5325.smartPool.model;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */

import lombok.*;

import java.math.BigDecimal;

/**
 * 记载某个币种的震荡属性
 *
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class SymbolShock {

    String symbol;
    /**
     * 根据算法计算得到的震频值
     */
    double ShockVal;

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
     * 最后得到的推荐指数 尚未成熟
     */
    double recomRatio;

    /**
     * 局部均值方差
     */
    double meanVariance;
}

