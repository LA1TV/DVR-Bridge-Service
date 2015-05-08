package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.net.URL;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Represents a playlist file that contains the stream fragment urls.
 */
@Component
@Scope("prototype")
public class HlsPlaylist {
	
	private final URL playlistUrl;
	
	/**
	 * Create an instance which represents a specific playlist file.
	 * @param playlistUrl
	 */
	public HlsPlaylist(URL playlistUrl) {
		this.playlistUrl = playlistUrl;
	}
	
	/**
	 * Get the playlists url.
	 * @return
	 */
	public URL getUrl() {
		return playlistUrl;
	}
	
}
