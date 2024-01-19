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
	int score;
	String period;
	String amplitude;
	String lowP;
	String highP;
	String variance;
}
