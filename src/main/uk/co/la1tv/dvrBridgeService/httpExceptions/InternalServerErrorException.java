package uk.co.la1tv.dvrBridgeService.httpExceptions;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR)
public final class InternalServerErrorException extends RuntimeException {
	private static final long serialVersionUID = 8212727435464776628L;
	
	public InternalServerErrorException(String msg) {
		super(msg);
	}
	
}
