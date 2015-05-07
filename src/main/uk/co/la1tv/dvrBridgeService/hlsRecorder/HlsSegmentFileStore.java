package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;

/**
 * Holds references to all of the segment files that have been downloaded
 * to the server.
 */
@Service
public class HlsSegmentFileStore {
	
	private static Logger logger = Logger.getLogger(HlsSegmentFileStore.class);
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private HlsFileGenerator hlsFileGenerator;
	
	// Key is the url object
	private final HashMap<URL, HlsSegmentFile> segments = new HashMap<>();
	
	private final Timer cleanupTimer = new Timer();
	
	@PostConstruct
	private void onPostConstruct() {
		cleanupTimer.schedule(new CleanupTask(), 10000, 10000);
	}
	
	@PreDestroy
	private void onPreDestroy() {
		cleanupTimer.cancel();
		cleanupTimer.purge();
	}

	/**
	 * Get a segment that is/was located at the specified url.
	 * If the segment does not already exist locally it will
	 * be downloaded.
	 * @param remoteUrl
	 * @return
	 */
	public HlsSegmentFileProxy getSegment(URL remoteUrl) {
		synchronized(segments) {
			if (segments.containsKey(remoteUrl)) {
				return createProxy(segments.get(remoteUrl));
			}
			File localFile = hlsFileGenerator.generateFile(FileHelper.getExtension(remoteUrl.getFile()));
			HlsSegmentFile newSegment = context.getBean(HlsSegmentFile.class, remoteUrl, localFile);
			segments.put(remoteUrl, newSegment);
			HlsSegmentFileProxy newSegmentProxy = createProxy(newSegment);
			return newSegmentProxy;
		}
	}
	
	private HlsSegmentFileProxy createProxy(HlsSegmentFile segmentFile) {
		return new HlsSegmentFileProxy(segmentFile);
	}
	
	private class CleanupTask extends TimerTask {

		@Override
		public void run() {
			synchronized(segments) {
				// check for any files that no longer have any proxies pointing at
				// them and delete the source files and remove them from the array
				HashMap<URL, HlsSegmentFile> clone = new HashMap<>(segments);
				for (URL key : clone.keySet()) {
					HlsSegmentFile segmentFile = segments.get(key);
					if (segmentFile.getNumProxiesAccessingFile() == 0) {
						// no one is accessing this file so remove it if possible
						HlsSegmentFileState state = segmentFile.getState();
						if (state != HlsSegmentFileState.DOWNLOADED && state != HlsSegmentFileState.DOWNLOAD_FAILED) {
							// file must be in one of these 2 states before it can be removed
							// from the segments array
							continue;
						}
						
						if (state == HlsSegmentFileState.DOWNLOADED) {
							if (!segmentFile.deleteFile()) {
								// file failed to delete for some reason
								logger.warn("Failed to delete file "+segmentFile.getFile().getAbsolutePath());
								continue;
							}
							logger.debug("Deleted file "+segmentFile.getFile().getAbsolutePath());
						}
						
						// can now safely be removed from the segments array and will be garbage collected
						segments.remove(key);
					}
				}
			}
		}
		
	}
	
}
