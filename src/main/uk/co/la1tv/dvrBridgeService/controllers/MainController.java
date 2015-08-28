package uk.co.la1tv.dvrBridgeService.controllers;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.co.la1tv.dvrBridgeService.handlers.IRequestHandler;
import uk.co.la1tv.dvrBridgeService.handlers.RequestHandlers;
import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;

@RestController
public class MainController {
	
	private final RequestHandlers requestHandlers;
	
	@Autowired
	public MainController(RequestHandlers requestHandlers) {
		this.requestHandlers = requestHandlers;
	}
	
	@RequestMapping(value = "/dvrBridgeService", method = RequestMethod.POST)
	public Object handlePost(HttpServletRequest request, @RequestParam("type") String type, @RequestParam("id") String streamId) {
		
		IRequestHandler handler = requestHandlers.getRequestHandlerForType(type);
		if (handler == null) {
			throw(new InternalServerErrorException("Unknown type."));
		}
		
		if (streamId == null || streamId.length() > 100) {
			throw(new InternalServerErrorException("Invalid id."));
		}
		
		Map<String, String[]> requestParameters = request.getParameterMap();
		return handler.handle(streamId, requestParameters);
	}
	
	
}
