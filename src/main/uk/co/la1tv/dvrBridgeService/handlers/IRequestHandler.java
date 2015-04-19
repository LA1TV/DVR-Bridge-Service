package uk.co.la1tv.dvrBridgeService.handlers;

public interface IRequestHandler {
	
	// get the string that represents the type of request that this handler handles
	public String getType();
	
	// handle the request and return what should be returned to the user
	public Object handle(long streamId);
}
