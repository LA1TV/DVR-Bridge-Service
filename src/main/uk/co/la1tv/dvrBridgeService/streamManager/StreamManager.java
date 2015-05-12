package uk.co.la1tv.dvrBridgeService.streamManager;

import java.net.URL;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Manages the streams that are controlled from the web server
 */
@Service
public class StreamManager {
	
	@Autowired
	private ApplicationContext context;
	
	private final HashMap<Long, SiteStream> siteStreams = new HashMap<>();
	
	/**
	 * Get reference to the stream with the corresponding id.
	 * A new stream object is created with the provided url if it doesn't exist.
	 * @param id
	 * @param remoteHlsPlaylistUrl
	 * @return
	 */
	public SiteStream getStream(final long id, URL remoteHlsPlaylistUrl) {
		synchronized(siteStreams) {
			SiteStream siteStream = siteStreams.get(id);
			if (siteStream == null) {
				siteStream = context.getBean(SiteStream.class, id, remoteHlsPlaylistUrl);
				siteStream.setCaptureRemovedListener(new ISiteStreamCaptureRemovedListener() {
					@Override
					public void onCaptureRemoved() {
						synchronized(siteStreams) {
							// remove streams when their captures are removed
							siteStreams.remove(id);
						}
					}
					
				});
				siteStreams.put(id, siteStream);
			}
			return siteStream;
		}
	}
	
	/**
	 * Get a reference to the stream with the corresponding id.
	 * If the stream doesn't exist null is returned.
	 * @param id
	 * @return
	 */
	public SiteStream getStream(long id) {
		synchronized(siteStreams) {
			return siteStreams.get(id);
		}
	}

}
