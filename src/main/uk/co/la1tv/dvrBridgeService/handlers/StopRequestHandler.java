package uk.co.la1tv.dvrBridgeService.handlers;

import org.springframework.stereotype.Component;

@Component
public class StopRequestHandler implements IRequestHandler {

	@Override
	public String getType() {
		return "STOP";
	}

	@Override
	public Object handle(long streamId) {
		// TODO Auto-generated method stub
		return null;
	}

}
