package uk.co.la1tv.dvrBridgeService.httpExceptions;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = org.springframework.http.HttpStatus.NOT_FOUND)
public final class ResourceNotFoundException extends RuntimeException {
	private static final long serialVersionUID = 2759125795227696645L;
	
	public ResourceNotFoundException(String msg) {
		super(msg);
	}
}
