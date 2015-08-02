package uk.co.la1tv.dvrBridgeService.handlers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;
import uk.co.la1tv.dvrBridgeService.streamManager.ISiteStream;
import uk.co.la1tv.dvrBridgeService.streamManager.MasterStreamManager;

@Component
public class StopRequestHandler implements IRequestHandler {

	@Autowired
	private MasterStreamManager streamManager;
	
	@Override
	public String getType() {
		return "STOP";
	}

	@Override
	public Object handle(long streamId, Map<String, String[]> requestParameters) {
		ISiteStream stream = streamManager.getStream(streamId);
		if (stream == null || !stream.stopCapture()) {
			throw(new InternalServerErrorException("Unable to stop the capture for some reason."));
		}
		return null;
	}

}
