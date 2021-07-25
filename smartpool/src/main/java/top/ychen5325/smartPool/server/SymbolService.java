package top.ychen5325.smartPool.server;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONObject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import top.ychen5325.smartPool.common.UrlConfig;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author yyy
 * @wx ychen5325
 * @email yangyouyuhd@163.com
 */
@Slf4j
@Data
@Component
public class SymbolService {

    @Resource
    private RestTemplate restTemplate;

    private List<String> symbols;

    // @PostConstruct
    public void init() {
        symbols = Arrays.asList("CTKUSDT");
    }

    /**
     * 获取支持合约交易的币种列表
     *
     * @return
     */
    public List<String> listContractSymbol() {
        if (CollectionUtil.isNotEmpty(symbols)) {
            return symbols;
        }

        int i = 1;
        do {
            try {
                JSONObject resp = restTemplate.getForObject(UrlConfig.listSymbolsUrl, JSONObject.class);
                symbols = resp.getJSONArray("symbols")
                        .stream()
                        .map(e -> ((Map<String, String>) e).get("symbol"))
                        .collect(Collectors.toList());
                if (CollectionUtil.isNotEmpty(symbols)) {
                    break;
                }
            } catch (Exception ex) {
            }
        } while (i++ < 3);
        return CollectionUtil.isEmpty(symbols) ? Collections.EMPTY_LIST : symbols;
    }
}
