package uk.co.la1tv.dvrBridgeService.handlers;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;
import uk.co.la1tv.dvrBridgeService.streamManager.ISiteStream;
import uk.co.la1tv.dvrBridgeService.streamManager.MasterStreamManager;

@Component
public class GetUrlRequestHandler implements IRequestHandler {

	@Autowired
	private MasterStreamManager streamManager;
	
	@Override
	public String getType() {
		return "GET_URL";
	}

	@Override
	public Object handle(long streamId, Map<String, String[]> requestParameters) {
		ISiteStream stream = streamManager.getStream(streamId);
		if (stream == null) {
			throw(new InternalServerErrorException("Unable to retrieve url for some reason."));
		}
		URL url = stream.getPlaylistUrl();
		if (url == null) {
			throw(new InternalServerErrorException("Unable to retrieve url for some reason."));
		}
		HashMap<String, String> response = new HashMap<>();
		response.put("url", url.toExternalForm());
		return response;
	}

}
