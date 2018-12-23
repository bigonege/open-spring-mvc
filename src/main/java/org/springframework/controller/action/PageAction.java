
package org.springframework.controller.action;

import org.springframework.annotation.Autowired;
import org.springframework.annotation.Controller;
import org.springframework.annotation.RequestMapping;
import org.springframework.annotation.RequestParam;
import org.springframework.controller.service.IQueryService;
import org.springframework.webmvc.ModelAndView;

import java.util.HashMap;
import java.util.Map;

/**
 * 公布接口url
 */

@Controller
@RequestMapping("/")
public class PageAction {

	@Autowired
	IQueryService queryService;
	@RequestMapping("/first.html")
	public ModelAndView query(@RequestParam("teacher") String teacher){
		String result = queryService.query(teacher);
		Map<String,Object> model = new HashMap<String,Object>();
		model.put("teacher", teacher);
		model.put("data", result);
		model.put("token", "123456");
		return new ModelAndView("first.html",model);
	}
	
}
