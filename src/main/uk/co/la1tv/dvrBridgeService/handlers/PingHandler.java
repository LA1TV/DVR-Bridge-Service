package uk.co.la1tv.dvrBridgeService.handlers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;
import uk.co.la1tv.dvrBridgeService.streamManager.ISiteStream;
import uk.co.la1tv.dvrBridgeService.streamManager.MasterStreamManager;

@Component
public class PingHandler implements IRequestHandler {

	@Autowired
	private MasterStreamManager streamManager;
	
	@Override
	public String getType() {
		return "PING";
	}

	@Override
	public Object handle(String streamId, Map<String, String[]> requestParameters) {
		ISiteStream stream = streamManager.getStream(streamId);
		if (stream == null || !stream.hasCapture()) {
			throw(new InternalServerErrorException("Unable find stream or stream doesn't have capture."));
		}
		stream.registerActivity();
		return null;
	}

}
