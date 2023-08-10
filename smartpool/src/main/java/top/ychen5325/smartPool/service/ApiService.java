package top.ychen5325.smartPool.service;/**
 *
 */

import top.ychen5325.smartPool.common.CallResult;
import top.ychen5325.smartPool.job.SmartPoolJob;
import top.ychen5325.smartPool.model.SymbolShake;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

import java.util.List;

/**
 * @author yyy
 * @tg t.me/ychen5325
 */
@RestController()
public class ApiService {

	@Resource
	private SmartPoolJob smartPoolJob;

	@GetMapping("/list")
	public CallResult<List<SymbolShake>> list(@RequestParam("interval") String interval,
											  @RequestParam("top") Integer top) {
		return smartPoolJob.list(interval, top);
	}
}
