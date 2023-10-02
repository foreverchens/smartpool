package top.ychen5325.smartPool.service;/**
 *
 */

import top.ychen5325.smartPool.common.UrlEnum;
import top.ychen5325.smartPool.model.Kline;
import top.ychen5325.smartPool.model.KlineParam;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpStatus;

import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
@Component
public class CzClient {
	/**
	 * 币列表的过期时间、设置十天更新一次
	 */
	private long expireTime;
	private List<String> symbols;

	@Resource
	private OkHttpClient okHttpClient;


	@PostConstruct
	private void init() {
		expireTime = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 10);
		symbols = listSymbol();
	}

	/**
	 * 非幂等的CZ API接口...
	 */
	public List<String> listSymbol() {
		if (CollectionUtil.isNotEmpty(symbols) && System.currentTimeMillis() < expireTime) {
			return symbols;
		}
		Request request = new Request.Builder().url(UrlEnum.listSymbol).get().build();
		List<String> symbols = new ArrayList<>();
		try (Response response = okHttpClient.newCall(request).execute()) {
			assert response.body() != null;
			if (response.code() != HttpStatus.HTTP_OK) {
				throw new RuntimeException(response.body().string());
			}
			JSONArray symbolsJsonArr = JSON.parseObject(response.body().string()).getJSONArray("symbols");
			for (int i = 0; i < symbolsJsonArr.size(); i++) {
				JSONObject jsonObj = symbolsJsonArr.getJSONObject(i);
				if (symbolFilter(jsonObj)) {
					continue;
				}
				symbols.add(jsonObj.getString("symbol"));
			}
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
		expireTime = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 10);
		return symbols;
	}

	/**
	 * 无法通过api获取kline
	 * 其他低价值标的
	 */
	private static boolean symbolFilter(JSONObject jsonObj) {
		String status = jsonObj.getString("status");
		if (!"TRADING".equalsIgnoreCase(status)) {
			return true;
		}
		if (!"USDT".equalsIgnoreCase(jsonObj.getString("quoteAsset"))) {
			return true;
		}
		if (!"COIN".equalsIgnoreCase(jsonObj.getString("underlyingType"))) {
			return true;
		}
		List<String> underlyingSubType = jsonObj.getObject("underlyingSubType", new TypeReference<List<String>>() {});
		if (CollectionUtil.isNotEmpty(underlyingSubType)) {
			if ("INDEX".equalsIgnoreCase(underlyingSubType.get(0))) {
				return true;
			}
			if ("MEME".equalsIgnoreCase(underlyingSubType.get(0))) {
				return true;
			}
		}
		String symbol = jsonObj.getString("symbol");
		if (symbol.contains("_") || "LUNA2USDT".equalsIgnoreCase(symbol) || "BLURUSDT".equalsIgnoreCase(symbol)) {
			return true;
		}
		if (symbol.contains("1000") || "DODOXUSDT".equalsIgnoreCase(symbol)) {
			return true;
		}
		// 最近一个月上线的不要
		long onboardDate = jsonObj.getLongValue("onboardDate");
		if (onboardDate > System.currentTimeMillis() - (1000L * 60 * 60 * 24 * 30)) {
			return true;
		}
		return false;
	}

	public List<Kline> listKline(KlineParam param) {
		if (Strings.isBlank(param.getSymbol()) || Strings.isBlank(param.getInterval())) {
			throw new RuntimeException("symbol || interval is blank");
		}
		String url = UrlEnum.kLine + paramFill(param);
		Request request = new Request.Builder().url(url).get().build();
		try (Response response = okHttpClient.newCall(request).execute()) {
			assert response.body() != null;
			if (response.code() != HttpStatus.HTTP_OK) {
				throw new RuntimeException(JSON.toJSONString(param) + response.body().string());
			}
			List<List> klineList = JSON.parseObject(response.body().string(), new TypeReference<List<List>>() {});
			List<Kline> result = new ArrayList<>(klineList.size() * 2);
			for (List kline : klineList) {
				Long openTime = (long) kline.get(0);
				Long closeTime = (long) kline.get(6);
				String openPrice = kline.get(1).toString();
				String maxPrice = kline.get(2).toString();
				String minPrice = kline.get(3).toString();
				String closePrice = kline.get(4).toString();
				// 交易总额
				String txAmount = kline.get(7).toString();
				result.add(Kline.builder().openTime(openTime).closeTime(closeTime).openPrice(new BigDecimal(openPrice)).maxPrice(new BigDecimal(maxPrice)).minPrice(new BigDecimal(minPrice))
								.closePrice(new BigDecimal(closePrice)).txAmount(new BigDecimal(txAmount)).build());
			}
			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	private String paramFill(KlineParam param) {
		StringBuilder sb = new StringBuilder("?");
		Field[] fields = param.getClass().getDeclaredFields();
		for (Field field : fields) {
			field.setAccessible(true);
			String name = field.getName();
			Object val;
			try {
				val = field.get(param);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
			if (Objects.isNull(val)) {
				continue;
			}
			sb.append(name);
			sb.append("=");
			sb.append(val);
			sb.append("&");
		}
		return sb.deleteCharAt(sb.length() - 1).toString();
	}

	public static void main(String[] args) {
		CzClient czClient = new CzClient();
		czClient.okHttpClient = new OkHttpClient();
		czClient.init();
	}
}
