package uk.co.la1tv.dvrBridgeService.streamManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylist;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCapture;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCaptureState;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.ICaptureStateChangeListener;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.IPlaylistUpdatedListener;
import uk.co.la1tv.dvrBridgeService.servableFiles.ServableFile;
import uk.co.la1tv.dvrBridgeService.servableFiles.ServableFileGenerator;

// TODO need to register pings and if haven't had one in more than a certain amount of time delete the capture


/**
 * Represents a stream on the site.
 * The capture can be start, stopped and removed from here.
 * Operations must occur in that order and can only occur once.
 */
@Component
@Scope("prototype")
public class SiteStream {

	private static Logger logger = Logger.getLogger(SiteStream.class);
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private ServableFileGenerator fileGenerator;
	
	private final Object lock = new Object();
	
	// unique id for this stream provided by site
	private final long siteStreamId;
	private final URL sourcePlaylistUrl;
	private HlsPlaylist hlsPlaylist = null;
	private HlsPlaylistCapture capture = null;
	private ServableFile generatedPlaylistFile = null;
	private ISiteStreamCaptureRemovedListener captureRemovedListener = null;
	private boolean requestedStop = false;
	private long lastActivity = System.currentTimeMillis();
	
	public SiteStream(long id, URL sourcePlaylistUrl) {
		this.siteStreamId = id;
		this.sourcePlaylistUrl = sourcePlaylistUrl;
	}
	
	@PostConstruct
	private void onPostConstruct() {
		hlsPlaylist = context.getBean(HlsPlaylist.class, sourcePlaylistUrl);
	}

	/**
	 * Set the listener to be informed when the capture is deleted.
	 * @param captureRemovedListener
	 */
	public void setCaptureRemovedListener(ISiteStreamCaptureRemovedListener captureRemovedListener) {
		this.captureRemovedListener = captureRemovedListener;
	}
	
	/**
	 * Determine if this stream has a capture.
	 * This may be a capture in progress or a finished capture.
	 * @return
	 */
	public boolean hasCapture() {
		synchronized(lock) {
			HlsPlaylistCaptureState state = capture.getCaptureState();
			return capture != null && (state == HlsPlaylistCaptureState.CAPTURING || state == HlsPlaylistCaptureState.STOPPED);
		}
	}
	
	/**
	 * Determine if the capture has been deleted.
	 * @return
	 */
	public boolean captureDeleted() {
		synchronized(lock) {
			return capture != null && capture.getCaptureState() == HlsPlaylistCaptureState.DELETED;
		}
	}
	
	/**
	 * Register activity. If the configurable timeout passes inbetween calls to this then
	 * the capture will be deleted.
	 */
	public void registerActivity() {
		lastActivity = System.currentTimeMillis();
	}
	
	/**
	 * Generate a new capture.
	 */
	private void generateHlsPlaylistCapture() {
		synchronized(lock) {
			if (capture != null) {
				throw(new RuntimeException("HlsPlaylistCapture object already exists."));
			}
			ServableFile file = fileGenerator.generateServableFile("m3u8");
			generatedPlaylistFile = file;
			PlaylistFileGenerator playlistFileGenerator = new PlaylistFileGenerator(file);
			capture = context.getBean(HlsPlaylistCapture.class, hlsPlaylist);
			capture.setStateChangeListener(new ICaptureStateChangeListener() {
	
				@Override
				public void onStateChange(HlsPlaylistCaptureState newState) {
					if (newState == HlsPlaylistCaptureState.DELETED) {
						// delete the generated playlist file and call the capture removed callback
						generatedPlaylistFile.delete();
						if (captureRemovedListener != null) {
							captureRemovedListener.onCaptureRemoved();
						}
					}
					else if (newState == HlsPlaylistCaptureState.STOPPED) {
						if (!requestedStop) {
							// this is a stop due to an error.
							// delete the capture
							try {
								capture.deleteCapture();
							}
							catch(Exception e) {
								e.printStackTrace();
								logger.warn("Error trying to delete capture after it was stopped unexpectadly.");
							}
						}
					}
				}
				
			});
			capture.setPlaylistUpdatedListener(playlistFileGenerator);
		}
	}
	
	/**
	 * Remove the HlsPlaylistCapture object and remove listeners so it can be garbage collected.
	 */
	private void removeHlsPlaylistCapture() {
		synchronized(lock) {
			if (capture == null) {
				throw(new RuntimeException("HlsPlaylistCapture object does not exist."));
			}
			capture.setStateChangeListener(null);
			capture.setPlaylistUpdatedListener(null);
			capture = null;
		}
	}
	
	/**
	 * Get the id that the site has assigned to this stream.
	 * @return
	 */
	public long getSiteStreamId() {
		return siteStreamId;
	}
	
	/**
	 * Start a capture for this item.
	 * Returns true on success or false on a failure.
	 */
	public boolean startCapture() {
		generateHlsPlaylistCapture();
		boolean success = false;
		try {
			success = capture.startCapture();
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		if (!success) {
			removeHlsPlaylistCapture();
		}
		return success;
	}
	
	/**
	 * Stop the capture for this item.
	 * Returns true on success or false on a failure.
	 */
	public boolean stopCapture() {
		return stopCaptureImpl();
	}
	
	private synchronized boolean stopCaptureImpl() {
		try {
			requestedStop = true;
			capture.stopCapture();
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			requestedStop = false;
			return false;
		}
	}
	
	/**
	 * Remove the capture for this item from the server,
	 * and stop the recording if one is taking place.
	 * Returns true on success or false on a failure.
	 */
	public boolean removeCapture() {
		try {
			if (capture.getCaptureState() == HlsPlaylistCaptureState.CAPTURING) {
				// currently capturing.
				// stop capture first
				if (!stopCaptureImpl()) {
					return false;
				}
			}
			capture.deleteCapture();
			removeHlsPlaylistCapture();
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Get the url to the playlist file for this capture.
	 * Returns null if the playlist file is not available.
	 * @return
	 */
	public URL getPlaylistUrl() {
		HlsPlaylistCaptureState state = capture.getCaptureState();
		if (state != HlsPlaylistCaptureState.CAPTURING && state != HlsPlaylistCaptureState.STOPPED) {
			return null;
		}
		return generatedPlaylistFile.getUrl();
	}
	
	private class PlaylistFileGenerator implements IPlaylistUpdatedListener {
		
		private final File file;
		
		public PlaylistFileGenerator(File file) {
			this.file = file;
		}
		
		@Override
		public void onPlaylistUpdated(String playlistContent) {
			try {
				Files.write(Paths.get(file.getAbsolutePath()), playlistContent.getBytes(), StandardOpenOption.CREATE);
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Error when trying to write generated playlist file.");
			}
		}
		
	}
}
