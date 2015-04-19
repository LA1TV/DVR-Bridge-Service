package uk.co.la1tv.dvrBridgeService.handlers;

import org.springframework.stereotype.Component;

@Component
public class StartRequestHandler implements IRequestHandler {

	@Override
	public String getType() {
		return "START";
	}

	@Override
	public Object handle() {
		// TODO Auto-generated method stub
		return null;
	}

}
