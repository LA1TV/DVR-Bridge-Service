package uk.co.la1tv.dvrBridgeService.handlers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;
import uk.co.la1tv.dvrBridgeService.streamManager.SiteStream;
import uk.co.la1tv.dvrBridgeService.streamManager.StreamManager;

@Component
public class PingHandler implements IRequestHandler {

	@Autowired
	private StreamManager streamManager;
	
	@Override
	public String getType() {
		return "PING";
	}

	@Override
	public Object handle(long streamId, Map<String, String[]> requestParameters) {
		SiteStream stream = streamManager.getStream(streamId);
		if (stream == null || !stream.hasCapture()) {
			throw(new InternalServerErrorException("Unable find stream or stream doesn't have capture."));
		}
		return null;
	}

}
