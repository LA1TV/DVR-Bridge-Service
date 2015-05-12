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

/**
 * Represents a stream on the site.
 */
@Component
@Scope("prototype")
public class SiteStream {

	private static Logger logger = Logger.getLogger(SiteStream.class);
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private ServableFileGenerator fileGenerator;
	
	// unique id for this stream provided by site
	private final long siteStreamId;
	private final URL sourcePlaylistUrl;
	private HlsPlaylist hlsPlaylist = null;
	private HlsPlaylistCapture capture = null;
	private ServableFile generatedPlaylistFile = null;
	private ISiteStreamCaptureRemovedListener captureRemovedListener = null;
	
	public SiteStream(long id, URL sourcePlaylistUrl) {
		this.siteStreamId = id;
		this.sourcePlaylistUrl = sourcePlaylistUrl;
	}
	
	@PostConstruct
	private void onPostConstruct() {
		hlsPlaylist = context.getBean(HlsPlaylist.class, sourcePlaylistUrl);
		generateHlsPlaylistCapture();
	}
	
	public void setCaptureRemovedListener(ISiteStreamCaptureRemovedListener captureRemovedListener) {
		this.captureRemovedListener = captureRemovedListener;
	}
	
	/**
	 * Generate a new capture. If there is already a capture delete it,
	 * then setup a new one.
	 */
	private synchronized void generateHlsPlaylistCapture() {
		if (capture != null) {
			switch(capture.getCaptureState()) {
			case NOT_STARTED:
				// no need to create a new one
				return;
			case CAPTURING:
				capture.stopCapture();
			case STOPPED:
				capture.deleteCapture();
			case DELETED:
			default:
				break;
			}
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
			}
			
		});
		capture.setPlaylistUpdatedListener(playlistFileGenerator);
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
		try {
			generateHlsPlaylistCapture();
			return capture.startCapture();
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Stop the capture for this item.
	 * Returns true on success or false on a failure.
	 */
	public boolean stopCapture() {
		try {
			capture.stopCapture();
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Remove the capture for this item from the server.
	 * Returns true on success or false on a failure.
	 */
	public boolean removeCapture() {
		try {
			capture.deleteCapture();
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
