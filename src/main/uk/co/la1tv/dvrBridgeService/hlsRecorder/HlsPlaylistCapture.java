package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.util.ArrayList;

/**
 * An object that represents a hls playlist recording.
 */
public class HlsPlaylistCapture {

	private final Object lock = new Object();
	
	private final HlsPlaylist playlist;
	private int captureState = 0; // 0=not started, 1=capturing, 2=stopped
	private Long captureStartTime = null; // start time in unix time in milliseconds
	private long captureDuration = 0; // the number of milliseconds currently captured
	// the segments that have been downloaded in order
	private ArrayList<HlsSegment> segments = new ArrayList<>();
	
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
			// TODO
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
			captureState = 2;
			// TODO
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
	 * Rounds the time that is provided to a time that is on a segment
	 * boundary.
	 * @param time The input time.
	 * @param mode The rounding mode.
	 * @return A time that sits at a segment boundary.
	 */
	public long roundTimeToSegment(long time, SegmentTimeRoundMode mode) {
		if (time < 0) {
			throw(new RuntimeException("Invalid time."));
		}
		synchronized(lock) {
			if (captureState == 0) {
				return 0;
			}
			long timeUpTo = 0;
			for (int i=0; i<segments.size(); i++) {
				HlsSegment segment = segments.get(i);
				long nextSegmentTime = timeUpTo += segment.getDuration();
				if (timeUpTo == time || ((mode == SegmentTimeRoundMode.PREVIOUS_SEGMENT && nextSegmentTime > time))) {
					return timeUpTo;
				}
				else if (mode == SegmentTimeRoundMode.NEXT_SEGMENT && timeUpTo > time) {
					return nextSegmentTime;
				}
				else if (mode == SegmentTimeRoundMode.CLOSEST_SEGMENT && nextSegmentTime < time && timeUpTo > time) {
					return time - timeUpTo < nextSegmentTime - time ? timeUpTo : nextSegmentTime;
				}
				timeUpTo = nextSegmentTime;
			}
			return timeUpTo;
		}
	}
	
	/**
	 * Get the contents of the playlist file that represents this capture
	 * from a specific time (milliseconds) into the recording and for the specific
	 * duration (milliseconds). The start time must be a time that maps to the start of 
	 * a segment, and the duration must be a duration that ends at the end of
	 * a segment. Look at roundTimeToSegment
	 * 
	 * A duration of -1 means the playlist should represent the entire recording.
	 */
	public void generatePlaylistFile(long startTime, long duration) {
		if (startTime < 0 || startTime > captureDuration) {
			throw(new RuntimeException("Invalid start time."));
		}
		else if (duration < -1 || duration > captureDuration) {
			throw(new RuntimeException("Invalid duration."));
		}
		// TODO
	}
	
}
