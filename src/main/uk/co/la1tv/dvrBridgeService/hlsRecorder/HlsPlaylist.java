package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.net.URL;

/**
 * Represents a playlist file that contains the stream fragment urls.
 */
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
