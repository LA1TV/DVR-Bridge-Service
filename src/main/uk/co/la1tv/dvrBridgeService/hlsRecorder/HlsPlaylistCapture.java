package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;

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
	
	private final HlsPlaylist playlist;
	private int captureState = 0; // 0=not started, 1=capturing, 2=stopped
	private Long captureStartTime = null; // start time in unix time in milliseconds
	private long captureDuration = 0; // the number of milliseconds currently captured
	// the segments that have been downloaded in order
	private ArrayList<HlsSegment> segments = new ArrayList<>();
	// the maximum length that a segment can be (milliseconds)
	// retrieved from the playlist
	private Integer segmentTargetDuration = null;
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
	 */
	public void startCapture() {
		synchronized(lock) {
			if (captureState != 0) {
				throw(new RuntimeException("Invalid capture state."));
			}
			captureState = 1;
			captureStartTime = System.currentTimeMillis();
			retrievePlaylistMetadata(); // TODO handle error
			updateTimer.schedule(new UpdateTimerTask(), 0, playlistUpdateInterval);
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
	 * @return the unix timestamp (seconds)
	 */
	public long getCaptureDuration() {
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
		contents += "#EXT-X-TARGETDURATION:"+segmentTargetDuration/1000+"\n";
		contents += "#EXT-X-MEDIA-SEQUENCE:0\n";
		
		for(HlsSegment segment : segments) {
			if (segment.getDiscontinuityFlag()) {
				contents += "#EXT-X-DISCONTINUITY\n";
			}
			contents += "#EXTINF:"+segment.getDuration()/1000+",\n";
			contents += "[URL]\n"; // TODO
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
	 */
	private void retrievePlaylistMetadata() {
		JSONObject info = getPlaylistInfo();
		if (info == null) {
			// TODO
			return;
		}
		//segmentTargetDuration = (Integer) info.get("something");
		
	}
	
	/**
	 * Make request to get playlist, parse it, and return info.
	 * Returns null if there was an error.
	 * @return
	 */
	private JSONObject getPlaylistInfo() {
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
			return null;
		}
	    if (exitVal != 0) {
			logger.warn("Error trying to retrieve playlist information.");
			return null;
		}
		String playlistInfoJsonString = outputStream.toString();
		JSONObject playlistInfo = null;
		try {
			playlistInfo = (JSONObject) JSONValue.parseWithException(playlistInfoJsonString);
		} catch (ParseException e) {
			e.printStackTrace();
			logger.warn("Error trying to retrieve playlist information.");
			return null;
		}
		System.out.println(playlistInfo.toJSONString());
		return playlistInfo;
	}
	
	/**
	 * Responsible for retrieving new segments as they become available.
	 */
	private class UpdateTimerTask extends TimerTask {

		@Override
		public void run() {
			synchronized(lock) {
				Long lastSequenceNumber = !segments.isEmpty() ? segments.get(segments.size()-1).getSequenceNumber() : null;
				// the next sequence number will always be one more than the last one as per the specification
				// if we don't have any segments yet then we will just grab the first segment in the file and
				// get its sequence number.
				Long nextSequenceNumber = lastSequenceNumber != null ? lastSequenceNumber+1 : null;
				// TODO use the node app to get the json data from the playlist file, and then
				// get any new segments using the segment file store, and create an entry for segments.
				getPlaylistInfo();
			}
			
		}
	}
	
}
