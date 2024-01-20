package icu.smartpool.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Data
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class ShakeScore {
	String symbol;

	/**
	 * 最终点位密度得分、正相关总点数、负相关振幅
	 */
	int score;

	/**
	 * 区间的振幅
	 */
	double amplitude;

	String lowP;

	String highP;

	String variance;
}
