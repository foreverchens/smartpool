package top.ychen5325.smartPool.service;

import top.ychen5325.smartPool.common.HttpConstant;

import com.alibaba.fastjson.JSONObject;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 币种相关信息提供服务
 *
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


    /**
     * 初始化所有币种、基本认为其不可变
     */
    @PostConstruct
    private void init() {
        symbols = restTemplate.getForObject(HttpConstant.listSymbolsUrl, JSONObject.class)
                .getJSONArray("symbols")
                .stream()
                .map(e -> ((Map<String, String>) e).get("symbol"))
                .collect(Collectors.toList());
    }

    /**
     * 获取支持合约交易的币种列表
     *
     * @return
     */
    public List<String> listContractSymbol() {
        return symbols;
    }
}
