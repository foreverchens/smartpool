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
public class H1Kline {
	Long openT;
	BigDecimal lowP;
	BigDecimal highP;
	int[] dataArr;
}
