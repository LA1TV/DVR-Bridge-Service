package uk.co.la1tv.dvrBridgeService.streamManager;

import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylist;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCaptureState;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsVariantPlaylist;
import uk.co.la1tv.dvrBridgeService.servableFiles.ServableFile;
import uk.co.la1tv.dvrBridgeService.servableFiles.ServableFileGenerator;

/**
 * Represents a variant stream on the site.
 * A variant stream is essentially a group of streams.
 * The capture can be start, stopped and removed from here.
 * Operations must occur in that order and can only occur once.
 * Each operation is applied to each of the streams in the variant playlist.
 */
@Component
@Scope("prototype")
public class VariantSiteStream implements ISiteStream {

	private static Logger logger = Logger.getLogger(VariantSiteStream.class);
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private ServableFileGenerator fileGenerator;
	
	
	private final Object lock = new Object();
	
	// unique id for this stream provided by site
	private final long siteVariantStreamId;
	private final URL sourceVariantPlaylistUrl;
	private HlsVariantPlaylist sourceVariantPlaylist;
	private HashMap<HlsPlaylist, SiteStream> siteStreams = null;
	private ServableFile generatedVariantPlaylistFile = null;
	private boolean requestedRemove = false;
	private ISiteStreamCaptureRemovedListener captureRemovedListener = null;
	private HlsPlaylistCaptureState captureState = HlsPlaylistCaptureState.NOT_STARTED;
	
	public VariantSiteStream(long id, URL sourcePlaylistUrl) {
		this.siteVariantStreamId = id;
		this.sourceVariantPlaylistUrl = sourcePlaylistUrl;
	}
	

	@PostConstruct
	private void onPostConstruct() {
		sourceVariantPlaylist = (HlsVariantPlaylist) context.getBean("HlsVariantPlaylist", sourceVariantPlaylistUrl);
		createSiteStreams();
	}
	
	private boolean createSiteStreams() {
		HlsPlaylist[] playlists = sourceVariantPlaylist.getPlaylists();
		if (playlists == null) {
			// can be null if there was an error requesting the playlists from the server
			return false;
		}
		siteStreams = new HashMap<>();
		synchronized(siteStreams) {
			for(HlsPlaylist playlist : playlists) {
				siteStreams.put(playlist, context.getBean(SiteStream.class, siteVariantStreamId, playlist.getUrl()));
			}
		}
		return true;
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
			return captureState == HlsPlaylistCaptureState.CAPTURING || captureState == HlsPlaylistCaptureState.STOPPED;
		}
	}
	
	/**
	 * Determine if the capture has been deleted.
	 * @return
	 */
	public boolean captureDeleted() {
		synchronized(lock) {
			return captureState == HlsPlaylistCaptureState.DELETED;
		}
	}
	
	/**
	 * Register activity. If the configurable timeout passes inbetween calls to this then
	 * the capture will be deleted.
	 */
	public void registerActivity() {
		synchronized(siteStreams) {
			if (siteStreams == null) {
				return;
			}
			for(SiteStream siteStream : siteStreams.values()) {
				siteStream.registerActivity();
			}
		}
	}
	
	private void addSiteStreamRemoveListeners() {
		synchronized(siteStreams) {
			for (SiteStream siteStream : siteStreams.values()) {
				siteStream.setCaptureRemovedListener(new ISiteStreamCaptureRemovedListener() {
	
					@Override
					public void onCaptureRemoved() {
						if (!requestedRemove) {
							// this is a remove due to an error.
							// delete the capture
							if (!removeCapture()) {
								logger.warn("Error trying to delete variant playlist capture after one of it's playlists stopped unexpectedly.");
							}
						}
					}
				});
			}
		}
	}
	
	private void removeSiteStreamRemoveListeners() {
		synchronized(siteStreams) {
			for (SiteStream siteStream : siteStreams.values()) {
				siteStream.setCaptureRemovedListener(null);
			}
		}
	}
	
	private boolean writeVariantPlaylistFile(ServableFile file) throws IOException {
		HlsPlaylist[] playlists = sourceVariantPlaylist.getPlaylists();
		if (playlists == null) {
			// null if there was an error getting the information from the server
			throw(new RuntimeException("The variant playlist is returning null for it's playlists."));
		}
		String contents = "";
		contents += "#EXTM3U\n";
		contents += "#EXT-X-VERSION:3\n";
		for (HlsPlaylist playlist : playlists) {
			Dimension resolution = playlist.getResolution();
			contents += "#EXT-X-STREAM-INF:BANDWIDTH="+playlist.getBandwidth()+",CODECS=\""+playlist.getCodecs()+"\",RESOLUTION="+Math.round(resolution.getWidth())+"x"+Math.round(resolution.getHeight())+"\n";
			URL generatedPlaylistUrl = siteStreams.get(playlist).getPlaylistUrl();
			if (generatedPlaylistUrl == null) {
				logger.error("Unable to retrieve generated playlist url so can't generate variant playlist.");
				return false;
			}
			contents += generatedPlaylistUrl.toExternalForm()+"\n";
		}
		Files.write(Paths.get(file.getAbsolutePath()), contents.getBytes(), StandardOpenOption.CREATE);
		return true;
	}
	
	
	/**
	 * Get the id that the site has assigned to this stream.
	 * @return
	 */
	public long getSiteStreamId() {
		return siteVariantStreamId;
	}
	
	/**
	 * Start a capture for this item.
	 * Returns true on success or false on a failure.
	 */
	public boolean startCapture() {
		synchronized(lock) {
			
			if (captureState != HlsPlaylistCaptureState.NOT_STARTED) {
				return false;
			}
			
			if (siteStreams == null) {
				return false;
			}
			
			addSiteStreamRemoveListeners();
			
			// attempt to start all captures
			SiteStream siteStreamWithError = null;
			synchronized(siteStreams) {
				for (SiteStream siteStream : siteStreams.values()) {
					if (!siteStream.startCapture()) {
						siteStreamWithError = siteStream;
						break;
					}
				}
			}
			
			if (siteStreamWithError != null) {
				// there was an error starting a capture.
				// attempt to remove all the ones that started
				synchronized(siteStreams) {
					for (SiteStream siteStream : siteStreams.values()) {
						if (siteStream == siteStreamWithError) {
							break;
						}
						if (!siteStream.removeCapture()) {
							logger.error("There was an removing a capture that was being removed because a different capture failed to start.");
						}
					}
				}
				removeSiteStreamRemoveListeners();
				return false;
			}
			
			ServableFile file = fileGenerator.generateServableFile("m3u8");
			try {
				if (writeVariantPlaylistFile(file)) {
					generatedVariantPlaylistFile = file;
				}
			} catch (IOException e) {
				e.printStackTrace();
				logger.error("Error when trying to write generated variant playlist file.");
			}
			if (generatedVariantPlaylistFile == null) {
				return false;
			}
			captureState = HlsPlaylistCaptureState.CAPTURING;
			return true;
		}
	}
	
	/**
	 * Stop the capture for this item.
	 * Returns true on success or false on a failure.
	 */
	public boolean stopCapture() {
		synchronized(lock) {
			if (captureState != HlsPlaylistCaptureState.CAPTURING) {
				return false;
			}
			return stopCaptureImpl();
		}
	}
	
	private synchronized boolean stopCaptureImpl() {
		try {
			boolean errorOccurredStoppingACapture = false;
			synchronized(siteStreams) {
				for (SiteStream siteStream : siteStreams.values()) {
					if (!siteStream.stopCapture()) {
						logger.warn("An error occurred when trying to stop a capture belonging to a variant playlist.");
						errorOccurredStoppingACapture = true;
						break;
					}
				}
				
				if (errorOccurredStoppingACapture) {
					removeCaptureImpl();
					return false;
				}
			}
			captureState = HlsPlaylistCaptureState.STOPPED;
			return true;
		}
		catch(Exception e) {
			e.printStackTrace();
			return false;
		}
	}
	
	/**
	 * Remove the captures for this item from the server,
	 * and stop the recording if one is taking place.
	 * Returns true if the captures should not exist after this call completes.
	 */
	public boolean removeCapture() {
		synchronized(lock) {
			if (captureState == HlsPlaylistCaptureState.DELETED) {
				return true;
			}
			
			if (captureState != HlsPlaylistCaptureState.CAPTURING && captureState != HlsPlaylistCaptureState.STOPPED) {
				return false;
			}
			try {
				if (captureState == HlsPlaylistCaptureState.CAPTURING) {
					// currently capturing.
					// stop capture first
					if (!stopCaptureImpl()) {
						return false;
					}
				}
				removeCaptureImpl();
				captureState = HlsPlaylistCaptureState.DELETED;
				return true;
			}
			catch(Exception e) {
				e.printStackTrace();
				return false;
			}
		}
	}
	
	private void removeCaptureImpl() {
		// attempt to remove all captures
		synchronized(siteStreams) {
			requestedRemove = true;
			try {
				for (SiteStream siteStream : siteStreams.values()) {
					if (!siteStream.removeCapture()) {
						logger.error("There was an error removing a capture that belongs to a variant playlist.");
					}
				}
			}
			finally {
				requestedRemove = false;
			}
		}
		removeSiteStreamRemoveListeners();
		generatedVariantPlaylistFile.delete();
		captureState = HlsPlaylistCaptureState.DELETED;
		if (captureRemovedListener != null) {
			captureRemovedListener.onCaptureRemoved();
		}
	}
	
	/**
	 * Get the url to the variant playlist file for this capture.
	 * Returns null if the playlist file is not available.
	 * @return
	 */
	public URL getPlaylistUrl() {
		synchronized(lock) {
			if (captureState != HlsPlaylistCaptureState.CAPTURING && captureState != HlsPlaylistCaptureState.STOPPED) {
				return null;
			}
			return generatedVariantPlaylistFile.getUrl();
		}
	}
	

}
