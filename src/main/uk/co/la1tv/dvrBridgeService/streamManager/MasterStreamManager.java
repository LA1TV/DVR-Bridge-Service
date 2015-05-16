package uk.co.la1tv.dvrBridgeService.streamManager;

import java.net.URL;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.helpers.M3u8ParserHelper;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.exceptions.PlaylistRequestException;

/**
 * Provides methods to manage variant and standard hls streams.
 * Communicates with the StreamManager and VariantStreamManager
 */
@Service
public class MasterStreamManager {
	
	@Autowired
	private VariantStreamManager variantStreamManager;
	@Autowired
	private StreamManager streamManager;
	@Autowired
	private M3u8ParserHelper m3u8ParserHelper;
	
	/**
	 * Creates a stream and returns a reference to it. The capture will be started.
	 * If the stream already exists the capture will be restarted.
	 * This determines where the remote playlist is a variant playlist or standard hls
	 * playlist and creates either a VariantSiteStream or SiteStream accordingly.
	 * Returns null if there was an error.
	 * @param id
	 * @param remoteHlsPlaylistUrl
	 * @return
	 */
	public ISiteStream createStream(final long id, URL remoteHlsPlaylistUrl) {
		try {
			if (m3u8ParserHelper.isVariantPlaylist(remoteHlsPlaylistUrl)) {
				return variantStreamManager.createStream(id, remoteHlsPlaylistUrl);
			}
			else {
				return streamManager.createStream(id, remoteHlsPlaylistUrl);
			}
		} catch (PlaylistRequestException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	
	/**
	 * Get a reference to the stream with the corresponding id.
	 * If the stream doesn't exist null is returned.
	 * This may be a VariantSiteStream or SiteStream.
	 * @param id
	 * @return
	 */
	public ISiteStream getStream(long id) {
		ISiteStream siteStream = variantStreamManager.getStream(id);
		if (siteStream != null) {
			return siteStream;
		}
		return streamManager.getStream(id);
	}
}
