package uk.co.la1tv.dvrBridgeService.streamManager;

import java.net.URL;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

/**
 * Manages the variant streams which are essentially groups of streams.
 * Generates the variant playlist and handles each of the streams
 */
@Service
public class VariantStreamManager {
	
	private static Logger logger = Logger.getLogger(VariantStreamManager.class);
	
	@Autowired
	private ApplicationContext context;
	
	// key is the id of the variant stream, and value is the array of SiteStream's it contains
	private final HashMap<Long, VariantSiteStream> variantSiteStreams = new HashMap<>();
	
	/**
	 * Creates each of the streams in the variant playlist and returns a reference to the object
	 * which represents the variant stream. The captures will be started.
	 * If the stream already exists the captures will be restarted.
	 * Returns null if there was an error.
	 * @param id
	 * @param remoteHlsVariantPlaylistUrl
	 * @return
	 */
	public VariantSiteStream createStream(final long id, URL remoteHlsVariantPlaylistUrl) {
		synchronized(variantSiteStreams) {
			VariantSiteStream variantSiteStream = variantSiteStreams.get(id);
			if (variantSiteStream != null) {
				// already exists. remove the capture and then remove from hashmap
				// when onCaptureRemoved() called it will have no effect
				if (!variantSiteStream.removeCapture()) {
					logger.error("A capture that already existed could not be deleted for some reason.");
					return null;
				}
				// make sure it can be garbage collected
				variantSiteStream.setCaptureRemovedListener(null);
				variantSiteStreams.remove(id);
			}
			
			final VariantSiteStream newVariantSiteStream = context.getBean(VariantSiteStream.class, id, remoteHlsVariantPlaylistUrl);
			if (!newVariantSiteStream.startCapture()) {
				logger.warn("An error occurred when trying to start a variant stream capture.");
				return null;
			}
			newVariantSiteStream.setCaptureRemovedListener(new ISiteStreamCaptureRemovedListener() {
				@Override
				public void onCaptureRemoved() {
					synchronized(variantSiteStreams) {
						// remove variant stream when it's capture is removed
						variantSiteStreams.remove(id, newVariantSiteStream);
					}
				}
			});
			variantSiteStreams.put(id, newVariantSiteStream);
			return newVariantSiteStream;
		}
	}
	
	/**
	 * Get a reference to the stream with the corresponding id.
	 * If the stream doesn't exist null is returned.
	 * @param id
	 * @return
	 */
	public VariantSiteStream getStream(long id) {
		synchronized(variantSiteStreams) {
			return variantSiteStreams.get(id);
		}
	}
}
