package top.ychen5325.smartPool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import okhttp3.OkHttpClient;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@EnableScheduling
@SpringBootApplication
public class SmartPoolApplication {


	public static void main(String[] args) {
		SpringApplication.run(SmartPoolApplication.class, args);
		System.out.println("\n\n\n--SMARTPOOL SUCCESS--\n\n\n");
	}

	@Bean
	public OkHttpClient okHttpClient() {
		return new OkHttpClient();
	}
}