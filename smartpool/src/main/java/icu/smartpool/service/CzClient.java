package icu.smartpool.service;


import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.google.common.collect.Sets;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.http.HttpStatus;

import org.apache.commons.lang.StringUtils;

import icu.smartpool.common.Config;
import icu.smartpool.model.Kline;
import icu.smartpool.model.KlineParam;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@Slf4j
public class CzClient {
	private static long expireTime = System.currentTimeMillis() + (1000 * 60 * 60 * 24 * 10);

	private static List<String> symbols;

	private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder().build();

	private static final Set<String> SET = Sets.newHashSet("TUSDUSDT", "USDCUSDT", "USDPUSDT", "EURUSDT", "AEURUSDT",
														   "PAXGUSDT");

	public static List<String> listSymbol() {
		if (CollectionUtil.isNotEmpty(symbols) && System.currentTimeMillis() < expireTime) {
			return symbols;
		}
		symbols = new ArrayList<>();
		Request request = new Request.Builder().url(Config.LIST_SYMBOL).get().build();
		try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
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
		symbols.removeAll(SET);
		// symbols = Arrays.asList("BTCUSDT");
		return symbols;
	}

	public static List<Kline> listKline(KlineParam param) {
		if (StringUtils.isBlank(param.getSymbol()) || StringUtils.isBlank(param.getInterval())) {
			throw new RuntimeException("symbol || interval is blank");
		}
		String url = Config.KLINE + paramFill(param);
		Request request = new Request.Builder().url(url).get().build();
		try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
			assert response.body() != null;
			if (response.code() != HttpStatus.HTTP_OK) {
				throw new RuntimeException(JSON.toJSONString(param) + response.body().string());
			}
			List<List> klineList = JSON.parseObject(response.body().string(), new TypeReference<List<List>>() {});
			List<Kline> result = new ArrayList<>(klineList.size() * 2);
			for (List kline : klineList) {
				Long openTime = (long) kline.get(0);
				String openPrice = kline.get(1).toString();
				String maxPrice = kline.get(2).toString();
				String minPrice = kline.get(3).toString();
				String closePrice = kline.get(4).toString();
				result.add(Kline.builder()
								.openT(openTime)
								.openP(new BigDecimal(openPrice))
								.highP(new BigDecimal(maxPrice))
								.lowP(new BigDecimal(minPrice))
								.closeP(new BigDecimal(closePrice))
								.build());
			}
			return result;
		} catch (IOException ex) {
			throw new RuntimeException(ex.getMessage(), ex);
		}
	}

	private static boolean symbolFilter(JSONObject jsonObj) {
		String status = jsonObj.getString("status");
		// 交易中～
		if (!"TRADING".equalsIgnoreCase(status)) {
			return true;
		}
		// USDT交易对
		return !"USDT".equalsIgnoreCase(jsonObj.getString("quoteAsset"));
	}

	private static String paramFill(KlineParam param) {
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
}
