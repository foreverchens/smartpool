package top.ychen5325.smartPool.common;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
public enum IntervalEnum {
    /**
     * [1m、3m、5m、15m、30m、1h、2h、4h、6h、8h、12h、 1d、3d、1w、1M]
     */
    m1("1m", 1), m3("3m", 3), m5("5m", 5), m30("30m", 30),
    h1("1h", 60), h2("h2", 120), h4("h4", 240), h6("h6", 360), h8("h8", 480), h12("h12", 720),
    d1("1d", 1440), d3("3d", 1440 * 3), d5("5d", 1440 * 5),
    w1("1w", 1440 * 7), M1("1M", 1440 * 30);

    public String interval;
    public Long time;

    IntervalEnum(String interval, int time) {
        Long baseTime = 1000 * 60L;
        this.interval = interval;
        this.time = baseTime * time;
    }

}
