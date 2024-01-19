package icu.smartpool.model;

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
    Long openT;
    BigDecimal openP;
    BigDecimal closeP;
    BigDecimal highP;
    BigDecimal lowP;
}
