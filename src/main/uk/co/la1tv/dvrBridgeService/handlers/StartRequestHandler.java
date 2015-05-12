package uk.co.la1tv.dvrBridgeService.handlers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;
import uk.co.la1tv.dvrBridgeService.streamManager.SiteStream;
import uk.co.la1tv.dvrBridgeService.streamManager.StreamManager;

@Component
public class StartRequestHandler implements IRequestHandler {

	@Autowired
	private StreamManager streamManager;
	
	@Override
	public String getType() {
		return "START";
	}

	@Override
	public Object handle(long streamId, Map<String, String[]> requestParameters) {
		String[] tmp = requestParameters.get("hlsPlaylistUrl");
		if (tmp == null) {
			throw(new InternalServerErrorException("\"hlsPlaylistUrl\" parameter is missing from the request url and is required."));
		}
		
		// the url of the remote playlist
		String hlsPlaylistUrlStr = requestParameters.get("hlsPlaylistUrl")[0];
		URL hlsPlaylistUrl;
		try {
			hlsPlaylistUrl = new URL(hlsPlaylistUrlStr);
		} catch (MalformedURLException e) {
			throw(new InternalServerErrorException("The provided hls playlist url is invalid."));
		}
		SiteStream stream = streamManager.createStream(streamId, hlsPlaylistUrl);
		if (stream == null) {
			throw(new InternalServerErrorException("Unable to start capture for some reason."));
		}
		return null;
	}

}
