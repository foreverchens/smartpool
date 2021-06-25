package top.ychen5325.smartPool.common;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
public class UrlConfig {
    public static String kLinesUrl = "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s";
    public static String listSymbolsUrl = "https://fapi.binance.com/fapi/v1/exchangeInfo";
    public static String klineListUrl = "http://127.0.0.1:9001/list/%s/%s/%s";//http://www.ychen5325.top/api/v1/kline/list/%s/%s/%s";
}
