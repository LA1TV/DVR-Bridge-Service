package uk.co.la1tv.dvrBridgeService.handlers;

import org.springframework.stereotype.Component;

@Component
public class GetUrlRequestHandler implements IRequestHandler {

	@Override
	public String getType() {
		return "GET_URL";
	}

	@Override
	public Object handle(long streamId) {
		// TODO Auto-generated method stub
		return null;
	}

}
