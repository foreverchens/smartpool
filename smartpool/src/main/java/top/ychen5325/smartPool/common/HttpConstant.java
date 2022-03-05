package top.ychen5325.smartPool.common;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
public interface HttpConstant {
    String kLinesUrl = "https://api.binance.com/api/v3/klines?symbol=%s&interval=%s";
    String listSymbolsUrl = "https://fapi.binance.com/fapi/v1/exchangeInfo";
}
