package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.awt.Dimension;
import java.net.URL;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Represents a playlist file.
 */
@Component("HlsPlaylist")
@Scope("prototype")
public class HlsPlaylist {
	
	private final URL playlistUrl;
	private final Integer bandwidth;
	private final String codecs;
	private final Dimension resolution;
	
	/**
	 * Create an instance which represents a specific playlist file.
	 * @param playlistUrl
	 */
	public HlsPlaylist(URL playlistUrl) {
		this(playlistUrl, null, null, null);
	}
	
	public HlsPlaylist(URL playlistUrl, Integer bandwidth, String codecs, Dimension resolution) {
		this.playlistUrl = playlistUrl;
		this.bandwidth = bandwidth;
		this.codecs = codecs;
		this.resolution = resolution;
	}
	
	/**
	 * Get the playlists url.
	 * @return
	 */
	public URL getUrl() {
		return playlistUrl;
	}
	
	public Integer getBandwidth() {
		return bandwidth;
	}
	
	public String getCodecs() {
		return codecs;
	}
	
	public Dimension getResolution() {
		return resolution;
	}
	
}
