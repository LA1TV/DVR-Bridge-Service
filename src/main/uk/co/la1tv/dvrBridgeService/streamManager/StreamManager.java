package uk.co.la1tv.dvrBridgeService.streamManager;

import java.net.URL;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Manages the streams that are controlled from the web server
 */
@Service
public class StreamManager {
	
	private static Logger logger = Logger.getLogger(StreamManager.class);
	
	@Autowired
	private ApplicationContext context;
	
	private final HashMap<Long, SiteStream> siteStreams = new HashMap<>();
	
	/**
	 * Creates a stream and returns a reference to it. The capture will be started.
	 * If the stream already exists the capture will be restarted.
	 * Returns null if there was an error.
	 * @param id
	 * @param remoteHlsPlaylistUrl
	 * @return
	 */
	public SiteStream createStream(final long id, URL remoteHlsPlaylistUrl) {
		synchronized(siteStreams) {
			SiteStream siteStream = siteStreams.get(id);
			if (siteStream != null) {
				// already exists. remove the capture and then remove from hashmap
				// when onCaptureRemoved() called it will have no effect
				if (!siteStream.removeCapture()) {
					logger.error("A capture that already existed could not be deleted for some reason.");
					return null;
				}
				siteStreams.remove(id);
			}
			
			final SiteStream newSiteStream = context.getBean(SiteStream.class, id, remoteHlsPlaylistUrl);
			if (!newSiteStream.startCapture()) {
				logger.warn("An error occurred when trying to start a stream capture.");
				return null;
			}
			newSiteStream.setCaptureRemovedListener(new ISiteStreamCaptureRemovedListener() {
				@Override
				public void onCaptureRemoved() {
					synchronized(siteStreams) {
						// remove streams when their captures are removed
						siteStreams.remove(id, newSiteStream);
					}
				}
			});
			siteStreams.put(id, newSiteStream);
			return newSiteStream;
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
