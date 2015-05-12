package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.exceptions.IncompletePlaylistException;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.exceptions.PlaylistRequestException;

/**
 * An object that represents a hls playlist recording.
 */
@Component
@Scope("prototype")
public class HlsPlaylistCapture {

	private static Logger logger = Logger.getLogger(HlsPlaylistCapture.class);
	
	private final Object lock = new Object();
	private final Object playlistGenerationLock = new Object();
	
	@Value("${m3u8Parser.nodePath}")
	private String nodePath;
	
	@Value("${m3u8Parser.applicationJsPath}")
	private String m3u8ParserApplicationPath;
	
	@Value("${app.playlistUpdateInterval}")
	private int playlistUpdateInterval;
	
	@Autowired
	private HlsSegmentFileStore hlsSegmentFileStore;
	
	private final HlsPlaylist playlist;
	private HlsPlaylistCaptureState captureState = HlsPlaylistCaptureState.NOT_STARTED;
	private Long captureStartTime = null; // start time in unix time in milliseconds
	// the segments that have been downloaded in order
	private ArrayList<HlsSegment> segments = new ArrayList<>();
	// the maximum length that a segment can be (milliseconds)
	// retrieved from the playlist
	private Float segmentTargetDuration = null;
	private final Timer updateTimer = new Timer();
	private IPlaylistUpdatedListener playlistUpdatedListener = null;
	private ICaptureStateChangeListener captureStateChangeListener = null;
	private String generatedPlaylistContent = null;
	
	/**
	 * Create a new object which represents a capture file for a playlist.
	 * @param playlist The playlist to generate a capture from.
	 */
	public HlsPlaylistCapture(HlsPlaylist playlist) {
		this.playlist = playlist;
	}
	
	/**
	 * Register a listener to be informed when the generated playlist changes.
	 * This callback will be in a new thread. (More info in the interface)
	 * @param playlistUpdatedListener
	 */
	public void setPlaylistUpdatedListener(IPlaylistUpdatedListener playlistUpdatedListener) {
		this.playlistUpdatedListener = playlistUpdatedListener;
	}
	
	/**
	 * Register a listener to be informed when the capture state changes.
	 * This callback will be in a new thread. (More info in the interface)
	 * @param stateChangeListener
	 */
	public void setStateChangeListener(ICaptureStateChangeListener stateChangeListener) {
		this.captureStateChangeListener = stateChangeListener;
	}
	
	private void updateCaptureState(HlsPlaylistCaptureState newState) {
		synchronized(lock) {
			captureState = newState;
			callCaptureStateChangedCallback(newState);
		}
	}
	
	/**
	 * Start capturing. This operation can only be performed once.
	 * False is returned if the capture could not be started for some reason.
	 */
	public boolean startCapture() {
		synchronized(lock) {
			if (captureState != HlsPlaylistCaptureState.NOT_STARTED) {
				throw(new RuntimeException("Invalid capture state."));
			}
			try {
				retrievePlaylistMetadata();
			} catch (PlaylistRequestException e) {
				logger.warn("An error occurred retrieving the playlist so capture could not be started.");
				return false;
			}
			updateCaptureState(HlsPlaylistCaptureState.CAPTURING);
			captureStartTime = System.currentTimeMillis();
			updateTimer.schedule(new UpdateTimerTask(), 0, playlistUpdateInterval);
			generatePlaylistContent();
			return true;
		}
	}
	
	/**
	 * Stop capturing. This operation can only be performed once.
	 */
	public void stopCapture() {
		synchronized(lock) {
			if (captureState != HlsPlaylistCaptureState.CAPTURING) {
				throw(new RuntimeException("Invalid capture state."));
			}
			updateTimer.cancel();
			updateTimer.purge();
			updateCaptureState(HlsPlaylistCaptureState.STOPPED);
			generatePlaylistContent();
		}
	}
	
	/**
	 * Delete the capture. This operation can only be performed once,
	 * and must be after the capture has stopped.
	 */
	public void deleteCapture() {
		synchronized(lock) {
			if (captureState != HlsPlaylistCaptureState.STOPPED) {
				throw(new RuntimeException("Invalid capture state."));
			}
			
			for (HlsSegment segment : segments) {
				// release all the files. This allows the HlsSegmentFileStore to delete them
				HlsSegmentFileProxy segmentFile = segment.getSegmentFile();
				if (!segmentFile.isReleased()) {
					// could be possible for 2 items in the remote playlist to be the same file
					// e.g an advert segment repeated several times
					segmentFile.release();
				}
			}
			updateCaptureState(HlsPlaylistCaptureState.DELETED);
		}
	}
	
	/**
	 * Get the unix timestamp (seconds) when the capture started.
	 * @return the unix timestamp (seconds)
	 */
	public long getCaptureStartTime() {
		synchronized(lock) {
			if (captureState == HlsPlaylistCaptureState.NOT_STARTED) {
				throw(new RuntimeException("Capture not started yet."));
			}
			return captureStartTime;
		}
	}
	
	/**
	 * Get the current capture state.
	 * @return
	 */
	public HlsPlaylistCaptureState getCaptureState() {
		return captureState;
	}
	
	
	/**
	 * Get the contents of the playlist file that represents this capture
	 */
	public String getPlaylistContent() {
		if (captureState == HlsPlaylistCaptureState.NOT_STARTED) {
			throw(new RuntimeException("Capture not started yet."));
		}
		else if (captureState == HlsPlaylistCaptureState.DELETED) {
			throw(new RuntimeException("This capture has been deleted."));
		}
		return generatedPlaylistContent;
	}
	
	
	/**
	 * Generate the playlist content that can be retrieved with getPlaylistContent
	 */
	private void generatePlaylistContent() {
		synchronized(playlistGenerationLock) {
			String contents = "";
			contents += "#EXTM3U\n";
			contents += "#EXT-X-PLAYLIST-TYPE:EVENT\n";
			contents += "#EXT-X-TARGETDURATION:"+segmentTargetDuration+"\n";
			contents += "#EXT-X-MEDIA-SEQUENCE:0\n";
			
			// segments might be in the array that haven't actually downloaded yet (or where their download has failed)
			boolean allSegmentsDownloaded = true;
			synchronized(lock) {
				for(HlsSegment segment : segments) {
					HlsSegmentFileProxy segmentFile = segment.getSegmentFile();
					// TODO it's failing here
					HlsSegmentFileState state = segmentFile.getState();
					if (state == HlsSegmentFileState.DOWNLOAD_FAILED) {
						// can never put anything else in this playlist, because will always be
						// missing this chunk
						break;
					}
					else if (state != HlsSegmentFileState.DOWNLOADED) {
						// can't get any more segments until this one has the file downloaded.
						allSegmentsDownloaded = false;
						break;
					}
					
					if (segment.getDiscontinuityFlag()) {
						contents += "#EXT-X-DISCONTINUITY\n";
					}
					contents += "#EXTINF:"+segment.getDuration()+",\n";
					contents += segmentFile.getFileUrl().toExternalForm()+"\n";
				}
			}
			
			if (captureState == HlsPlaylistCaptureState.STOPPED && allSegmentsDownloaded) {
				// recording has finished, and has all segments, so mark event as finished
				contents += "#EXT-X-ENDLIST\n";
			}
			
			if (generatedPlaylistContent != null && contents.equals(generatedPlaylistContent)) {
				// no change
				return;
			}
			generatedPlaylistContent = contents;
			callPlaylistUpdatedCallback(generatedPlaylistContent);
		}
	}
	
	/**
	 * Get any necessary metadata about the playlist.
	 * e.g the segmentTargetDuration
	 * @throws PlaylistRequestException 
	 */
	private void retrievePlaylistMetadata() throws PlaylistRequestException {
		JSONObject info = getPlaylistInfo();
		JSONObject properties = (JSONObject) info.get("properties");
		Object targetDuration = properties.get("targetDuration");
		if (targetDuration == null) {
			throw(new IncompletePlaylistException());
		}
		String durationStr = String.valueOf(targetDuration);
		float duration = Float.valueOf(durationStr);
		segmentTargetDuration = duration;
	}
	
	/**
	 * Make request to get playlist, parse it, and return info.
	 * @return
	 * @throws PlaylistRequestException 
	 */
	private JSONObject getPlaylistInfo() throws PlaylistRequestException {
		String playlistUrl = playlist.getUrl().toExternalForm();
		
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		CommandLine commandLine = new CommandLine(FileHelper.format(nodePath));
		commandLine.addArgument(FileHelper.format(m3u8ParserApplicationPath));
		commandLine.addArgument(playlistUrl);
		DefaultExecutor exec = new DefaultExecutor();
		// handle the stdout stream, ignore error stream
		PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream, null);
		exec.setStreamHandler(streamHandler);
		int exitVal;
		try {
			exitVal = exec.execute(commandLine);
		} catch (IOException e1) {
			e1.printStackTrace();
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
	    if (exitVal != 0) {
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
		String playlistInfoJsonString = outputStream.toString();
		JSONObject playlistInfo = null;
		try {
			playlistInfo = (JSONObject) JSONValue.parseWithException(playlistInfoJsonString);
		} catch (ParseException e) {
			e.printStackTrace();
			logger.warn("Error trying to retrieve playlist information.");
			throw(new PlaylistRequestException());
		}
		return playlistInfo;
	}
	
	/**
	 * Extract the playlist items out of playlistInfo.
	 * Returns null if there was an error.
	 * @return
	 */
	private JSONArray extractPlaylistItems(JSONObject playlistInfo) {
		JSONObject items = (JSONObject) playlistInfo.get("items");
		JSONArray playlistItems = (JSONArray) items.get("PlaylistItem");
		return playlistItems;
	}
	
	private void callPlaylistUpdatedCallback(final String playlistContent) {
		// call the callback in a separate thread to prevent issues if actions are performed
		// in the callback that call other methods like stopCapture()
		if (playlistUpdatedListener != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					playlistUpdatedListener.onPlaylistUpdated(playlistContent);
				}
			}).start();
		}
	}
	
	private void callCaptureStateChangedCallback(final HlsPlaylistCaptureState captureState) {
		// call the callback in a separate thread to prevent issues if actions are performed
		// in the callback that call other methods like stopCapture()
		if (captureStateChangeListener != null) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					captureStateChangeListener.onStateChange(captureState);
				}
			}).start();
		}
	}
	
	/**
	 * Responsible for retrieving new segments as they become available.
	 */
	private class UpdateTimerTask extends TimerTask {

		@Override
		public void run() {
			synchronized(lock) {
				if (captureState != HlsPlaylistCaptureState.CAPTURING) {
					return;
				}
				
				int numSegments = segments.size();
				Integer lastSequenceNumber = numSegments > 0 ? segments.get(numSegments-1).getSequenceNumber() : null;
				// the next sequence number will always be one more than the last one as per the specification
				// if we don't have any segments yet then we will set this to null which will mean just the newest chunk
				// will be retrieved
				Integer nextSequenceNumber = lastSequenceNumber != null ? lastSequenceNumber+1 : null;
				JSONObject playlistInfo = null;
				try {
					playlistInfo = getPlaylistInfo();
				} catch (PlaylistRequestException e) {
					logger.warn("Error retrieving playlist so stopping capture.");
					stopCapture();
					return;
				}
				
				JSONObject properties = (JSONObject) playlistInfo.get("properties");
				int firstSequenceNumber = Integer.valueOf(String.valueOf(properties.get("mediaSequence")));
				
				JSONArray items = extractPlaylistItems(playlistInfo);
				// TODO check if not had a new chunk in a while and if that's the case stop the capture.
				// TODO also check if has the end tag and if it has end the capture immediately
				if (!items.isEmpty()) {
					if (nextSequenceNumber != null) {
						System.out.println(new Integer((int) Math.ceil(System.currentTimeMillis()/1000))+" Looking at playlist.");
						if (firstSequenceNumber > nextSequenceNumber) {
							// the next chunk we want has left the playlist already
							// stop the capture
							logger.warn("Next chunk has already left remote playlist so stopping capture.");
							stopCapture();
						}
						else {
							int seqNum = firstSequenceNumber;
							for(int i=0; i<items.size(); i++) {
								if (seqNum >= nextSequenceNumber) {
									// this is a new item
									addNewSegment((JSONObject) items.get(i), seqNum);
								}
								seqNum++;
							}
						}
					}
					else {
						// just add the newest segment
						addNewSegment((JSONObject) items.get(items.size()-1), firstSequenceNumber+items.size()-1);
					}
				}
			}
		}
	}
	
	private void addNewSegment(JSONObject item, int seqNum) {
		JSONObject itemProperties = (JSONObject) item.get("properties");
		float duration = Float.parseFloat(String.valueOf(itemProperties.get("duration")));
		boolean discontinuityFlag = itemProperties.get("discontinuity") != null;
		URL segmentUrl = null;
		try {
			segmentUrl = new URL(playlist.getUrl(), String.valueOf(itemProperties.get("uri")));
		} catch (MalformedURLException e) {
			throw(new IncompletePlaylistException());
		}
		HlsSegmentFileProxy hlsSegmentFile = hlsSegmentFileStore.getSegment(segmentUrl);
		hlsSegmentFile.registerStateChangeCallback(new HlsSegmentFileStateChangeHandler(hlsSegmentFile));
		synchronized(lock) {
			segments.add(new HlsSegment(hlsSegmentFile, seqNum, duration, discontinuityFlag));
		}
	}
	
	private class HlsSegmentFileStateChangeHandler implements IHlsSegmentFileStateChangeListener {
		
		// the HlsSegmentFile that this handler has been set up for
		private final HlsSegmentFileProxy hlsSegmentFile;
		private boolean handled = false;
		
		public HlsSegmentFileStateChangeHandler(HlsSegmentFileProxy hlsSegmentFile) {
			this.hlsSegmentFile = hlsSegmentFile;
			handleStateChange(hlsSegmentFile.getState());
		}
		
		private synchronized void handleStateChange(HlsSegmentFileState state) {
			if (handled) {
				// this can happen if the event is fired just as this is already being
				// handled from the constructor
				return;
			}
			
			synchronized(lock) {
				if (captureState == HlsPlaylistCaptureState.DELETED) {
					// if the capture has gone into the deleted state then the
					// file will have already been released.
					return;
				}
				
				if (state == HlsSegmentFileState.DOWNLOADED) {
					// regenerate the playlist content.
					generatePlaylistContent();
				}
				else if (state == HlsSegmentFileState.DOWNLOAD_FAILED) {
					logger.warn("Error downloading playlist chunk so stopping capture.");
					if (captureState != HlsPlaylistCaptureState.STOPPED) {
						// capture hasn't already been stopped by an earlier failure
						stopCapture();
					}
				}
				else {
					return;
				}

				// don't care about any more state changes so remove handler
				// so can be garbage collected
				hlsSegmentFile.unregisterStateChangeCallback(this);
				handled = true;
			}
		}
		
		@Override
		public void onStateChange(HlsSegmentFileState state) {
			handleStateChange(state);
		}
		
	}
	
}
