package donmez.yusuf.shortener.servlet;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import donmez.yusuf.shortener.services.MySQLService;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/")
public class RestServlet {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(RestServlet.class);
	  
	// private static int counter = 0;
	// private static Date firstCall = Calendar.getInstance().getTime();
	
	@RequestMapping(path = "/", method = {RequestMethod.GET, RequestMethod.POST} )
	@ResponseBody
	public String notFound( HttpServletRequest httpAdapter, HttpServletResponse httpResp)
	{
		System.out.println("root / called");
		httpResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		return "Not Found";
	}

	@RequestMapping(path = "/rest/v2/health", method = RequestMethod.GET)
	@ResponseBody
	public String health( HttpServletRequest httpAdapter, HttpServletResponse httpResp)
	{
		return "OK";
	}
	
	
	@RequestMapping(path = "/{keyword}", method = RequestMethod.GET)
	@ResponseBody
	public void get(@PathVariable String keyword, HttpServletRequest httpAdapter, HttpServletResponse httpResp)
	{
		
		try {
			
			String sql = String.format("select url from ShortUrl where keyword='%s';",keyword);
			// System.out.println(sql);
	
			List<Map> list = MySQLService.getInstance().executeDynSelectQuery(sql);
	
			for (Map map : list) {
				// System.out.printf("url: %s \n", map.get("url"));
				httpResp.setHeader("Location", map.get("url").toString());
				httpResp.setStatus(302);
				return;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("SQL Exception ", e.fillInStackTrace());
			
			httpResp.setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
		httpResp.setStatus(HttpServletResponse.SC_OK);
	}
	
	

	@RequestMapping(path = "/rest/v2/short", method = RequestMethod.GET)
	@ResponseBody
	public String updateUser(@RequestParam("url") String url, HttpServletRequest httpAdapter, HttpServletResponse httpResp)
	{
		JSONObject responseJson = new JSONObject("{\"success\":true}");

		String keyword = null;
		List<Map> list = null;
		int counter = 0;

		do {
			keyword = UUID.randomUUID().toString().substring(0, 4);
			String existQuery = String.format("select id from ShortUrl where keyword='%s';", keyword);
			list = MySQLService.getInstance().executeDynSelectQuery(existQuery);
			System.out.printf("%s - %s \n",counter++, keyword);
			if(counter>10){
				responseJson.put("error_message", "can not find keyword");
				responseJson.put("success", false);
				httpResp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				return responseJson.toString();
			}
		} while (list.size() > 0);
		
		try {				
			MapSqlParameterSource params = new MapSqlParameterSource();
			params.addValue("keyword", keyword);
			params.addValue("url", url);
			// params.addValue("timestamp",Calendar.getInstance().getTime());
			
			String query = MySQLService.getInstance().createInsertStatement("ShortUrl", params);
			
			System.out.printf("update query:  %s \n", query);
			
			long id = MySQLService.getInstance().executeInsertQuery(query, params);
			responseJson.put("keyword", params.getValue("keyword"));

		} catch (Exception e) {
			e.printStackTrace();
			LOGGER.error("SQL Exception ", e.fillInStackTrace());
			responseJson.put("error_message", e.toString());
			responseJson.put("success", false);
			httpResp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return responseJson.toString();
		}
		
		httpResp.setContentType("application/json");
		httpResp.setStatus(HttpServletResponse.SC_OK);
		return responseJson.toString();
	}

}