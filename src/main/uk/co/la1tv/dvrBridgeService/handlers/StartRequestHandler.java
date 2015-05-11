package uk.co.la1tv.dvrBridgeService.handlers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylist;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCapture;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCaptureState;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.ICaptureStateChangeListener;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.IPlaylistUpdatedListener;
import uk.co.la1tv.dvrBridgeService.httpExceptions.InternalServerErrorException;

@Component
public class StartRequestHandler implements IRequestHandler {

	@Autowired
	private ApplicationContext context;
	
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
		
		HlsPlaylist hlsPlaylist = context.getBean(HlsPlaylist.class, hlsPlaylistUrl);
		HlsPlaylistCapture hlsPlaylistCapture = context.getBean(HlsPlaylistCapture.class, hlsPlaylist);
		
		
		// TODO temp, remove
		hlsPlaylistCapture.setPlaylistUpdatedListener(new IPlaylistUpdatedListener() {
			
			private int count = 0;
			
			@Override
			public void onPlaylistUpdated(String playlistContent) {
				System.out.println("playlist update");
			}
			
		});
		
		hlsPlaylistCapture.setStateChangeListener(new ICaptureStateChangeListener() {

			@Override
			public void onStateChange(HlsPlaylistCaptureState newState) {
				System.out.println("STATE CHANGED! "+newState.toString());
			}
			
		});
		
		
		hlsPlaylistCapture.startCapture();
		
		return null;
	}

}
