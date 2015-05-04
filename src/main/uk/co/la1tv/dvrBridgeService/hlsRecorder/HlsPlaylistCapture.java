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
	
	@Value("${m3u8Parser.nodePath}")
	private String nodePath;
	
	@Value("${m3u8Parser.applicationJsPath}")
	private String m3u8ParserApplicationPath;
	
	@Value("${app.playlistUpdateInterval}")
	private int playlistUpdateInterval;
	
	@Autowired
	private HlsSegmentFileStore hlsSegmentFileStore;
	
	private final HlsPlaylist playlist;
	private int captureState = 0; // 0=not started, 1=capturing, 2=stopped
	private Long captureStartTime = null; // start time in unix time in milliseconds
	private double captureDuration = 0; // the number of seconds currently captured
	// the segments that have been downloaded in order
	private ArrayList<HlsSegment> segments = new ArrayList<>();
	// the maximum length that a segment can be (milliseconds)
	// retrieved from the playlist
	private Float segmentTargetDuration = null;
	private final Timer updateTimer = new Timer();
	
	/**
	 * Create a new object which represents a capture file for a playlist.
	 * @param playlist The playlist to generate a capture from.
	 */
	public HlsPlaylistCapture(HlsPlaylist playlist) {
		this.playlist = playlist;
	}
	
	/**
	 * Start capturing. This operation can only be performed once.
	 * False is returned if the capture could not be started for some reason.
	 */
	public boolean startCapture() {
		synchronized(lock) {
			if (captureState != 0) {
				throw(new RuntimeException("Invalid capture state."));
			}
			try {
				retrievePlaylistMetadata();
			} catch (PlaylistRequestException e) {
				logger.warn("An error occurred retrieving the playlist so capture could not be started.");
				return false;
			}
			captureState = 1;
			captureStartTime = System.currentTimeMillis();
			updateTimer.schedule(new UpdateTimerTask(), 0, playlistUpdateInterval);
			return true;
		}
	}
	
	/**
	 * Stop capturing. This operation can only be performed once.
	 */
	public void stopCapture() {
		synchronized(lock) {
			if (captureState != 1) {
				throw(new RuntimeException("Invalid capture state."));
			}
			updateTimer.cancel();
			updateTimer.purge();
			captureState = 2;
		}
	}
	
	/**
	 * Get the unix timestamp (seconds) when the capture started.
	 * @return the unix timestamp (seconds)
	 */
	public long getCaptureStartTime() {
		synchronized(lock) {
			if (captureState == 0) {
				throw(new RuntimeException("Capture not started yet."));
			}
			return captureStartTime;
		}
	}
	
	/**
	 * Get the duration of the capture (seconds). This is dynamic and will
	 * update if a capture is currently in progress.
	 * @return
	 */
	public double getCaptureDuration() {
		synchronized(lock) {
			if (captureState == 0) {
				throw(new RuntimeException("Capture not started yet."));
			}
			return captureDuration;
		}
	}
	
	/**
	 * Determine if the stream is currently being captured.
	 * @return
	 */
	public boolean isCapturing() {
		return captureState == 1;
	}
	
	
	/**
	 * Get the contents of the playlist file that represents this capture
	 */
	public String generatePlaylistFile() {
		if (captureState == 0) {
			throw(new RuntimeException("Capture not started yet."));
		}
		
		String contents = "";
		contents += "#EXTM3U\n";
		contents += "#EXT-X-PLAYLIST-TYPE:EVENT\n";
		contents += "#EXT-X-TARGETDURATION:"+segmentTargetDuration+"\n";
		contents += "#EXT-X-MEDIA-SEQUENCE:0\n";
		
		for(HlsSegment segment : segments) {
			
			HlsSegmentFile segmentFile = segment.getSegmentFile();
			if (!segmentFile.isAvailable()) {
				// can't get any more segments until this one has the file downloaded.
				break;
			}
			
			if (segment.getDiscontinuityFlag()) {
				contents += "#EXT-X-DISCONTINUITY\n";
			}
			contents += "#EXTINF:"+segment.getDuration()+",\n";
			contents += segmentFile.getFileUrl().toExternalForm()+"\n";
		}
		
		if (captureState == 2) {
			// recording has finished so mark event as finished
			contents += "#EXT-X-ENDLIST\n";
		}
		return contents;
	}
	
	/**
	 * Get any necessary metadata about the playlist.
	 * e.g the segmentTargetDuration
	 * @throws PlaylistRequestException 
	 */
	private void retrievePlaylistMetadata() throws PlaylistRequestException {
		JSONObject info = getPlaylistInfo();
		JSONObject properties = (JSONObject) info.get("properties");
		String durationStr = String.valueOf(properties.get("targetDuration"));
		if (durationStr == null) {
			throw(new IncompletePlaylistException());
		}
		float duration =Float.valueOf(durationStr);
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
	
	/**
	 * Responsible for retrieving new segments as they become available.
	 */
	private class UpdateTimerTask extends TimerTask {

		@Override
		public void run() {
			synchronized(lock) {
				if (captureState != 1) {
					return;
				}
				
				Integer lastSequenceNumber = !segments.isEmpty() ? segments.get(segments.size()-1).getSequenceNumber() : null;
				// the next sequence number will always be one more than the last one as per the specification
				// if we don't have any segments yet then we will just grab the first segment in the file and
				// get its sequence number.
				Integer nextSequenceNumber = lastSequenceNumber != null ? lastSequenceNumber+1 : null;
				JSONObject playlistInfo = null;
				try {
					playlistInfo = getPlaylistInfo();
				} catch (PlaylistRequestException e) {
					logger.warn("Error retrieveing playlist so capture stopped.");
					stopCapture();
				}
				
				JSONObject properties = (JSONObject) playlistInfo.get("properties");
				int firstSequenceNumber = Integer.valueOf(String.valueOf(properties.get("mediaSequence")));
				
				JSONArray items = extractPlaylistItems(playlistInfo);
				int seqNum = firstSequenceNumber;
				for(int i=0; i<items.size(); i++) {
					if (nextSequenceNumber == null || seqNum >= nextSequenceNumber) {
						// this is a new item
						JSONObject item = (JSONObject) items.get(i);
						JSONObject itemProperties = (JSONObject) item.get("properties");
						float duration = Float.parseFloat(String.valueOf(itemProperties.get("duration")));
						boolean discontinuityFlag = itemProperties.get("discontinuity") != null;
						URL segmentUrl = null;
						try {
							segmentUrl = new URL(playlist.getUrl(), String.valueOf(itemProperties.get("uri")));
						} catch (MalformedURLException e) {
							throw(new IncompletePlaylistException());
						}
						System.out.println(segments.size());
						segments.add(new HlsSegment(hlsSegmentFileStore.getSegment(segmentUrl), seqNum, duration, discontinuityFlag));
					}
					seqNum++;
				}
				System.out.println(generatePlaylistFile());
			}
			
		}
	}
	
}
