package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.springframework.stereotype.Service;

/**
 * Holds references to all of the segment files that have been downloaded
 * to the server.
 */
@Service
public class HlsSegmentFileStore {
	
	// Key is the hashCode of the remote url
	private final HashMap<Integer, HlsSegmentFile> segments = new HashMap<>();

	/**
	 * Get a segment that is/was located at the specified url.
	 * If the segment does not already exist locally it will
	 * be downloaded.
	 * @param remoteUrl
	 * @return
	 */
	public HlsSegmentFile getSegment(URL remoteUrl) {
		int hashCode = remoteUrl.hashCode();
		if (segments.containsKey(hashCode)) {
			return segments.get(hashCode);
		}
		// TODO download segment and store somewhere
		File localFile = null; // TODO
		HlsSegmentFile newSegment = new HlsSegmentFile(remoteUrl, localFile);
		segments.put(hashCode, newSegment);
		return newSegment;
	}
	
}
