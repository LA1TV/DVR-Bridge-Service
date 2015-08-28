package uk.co.la1tv.dvrBridgeService.handlers;

import java.util.Map;

public interface IRequestHandler {
	
	// get the string that represents the type of request that this handler handles
	public String getType();
	
	// handle the request and return what should be returned to the user
	// request parameters is the data that has been provided in the query string (and post data)
	public Object handle(String streamId, Map<String, String[]> requestParameters);
}
