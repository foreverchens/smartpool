package icu.smartpool;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import icu.smartpool.job.SmartPoolJob;
import lombok.extern.slf4j.Slf4j;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
public class SmartPoolApplication {

	public static void main(String[] args) {
		System.out.println("\n\n\n--SMART-POOL SUCCESS--\n\n\n");
		ch.qos.logback.classic.Logger root = (Logger) LoggerFactory.getLogger(
				org.slf4j.Logger.ROOT_LOGGER_NAME);
		root.setLevel(Level.INFO);
		SmartPoolJob.run();
	}
}