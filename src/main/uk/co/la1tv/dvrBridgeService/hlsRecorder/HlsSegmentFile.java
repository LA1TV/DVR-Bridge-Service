package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.URL;

/**
 * Represents a hls segment file.
 */
public class HlsSegmentFile {
	
	public final URL remoteUrl; // the url that this segment was located at
	public final File localFile; // the local location
	
	public HlsSegmentFile(URL remoteUrl, File localFile) {
		this.remoteUrl = remoteUrl;
		this.localFile = localFile;
	}
	
	public URL getRemoteUrl() {
		return remoteUrl;
	}

	public File getFile() {
		return localFile;
	}
	
}