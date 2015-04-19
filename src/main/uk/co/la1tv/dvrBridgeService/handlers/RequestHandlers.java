package uk.co.la1tv.dvrBridgeService.handlers;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RequestHandlers {
	
	private final Set<IRequestHandler> requestHandlers;
	
	@Autowired
	// tasks is automatically populated with all beans that are of type RequestHandler by spring
	public RequestHandlers(Set<IRequestHandler> requestHandlers) {
		this.requestHandlers = requestHandlers;
	}
	
	public IRequestHandler getRequestHandlerForType(String requestType) {
		synchronized(requestHandlers) {
			for(IRequestHandler handler : requestHandlers) {
				if (handler.getType().equals(requestType)) {
					return handler;
				}
			}
		}
		return null;
	}

	public IRequestHandler[] getHandlers() {
		return requestHandlers.toArray(new IRequestHandler[requestHandlers.size()]);
	}
	
}
