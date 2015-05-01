package uk.co.la1tv.dvrBridgeService.hlsRecorder;

/**
 * Represents a hls segment.
 */
public class HlsSegment {

	private final HlsSegmentFile segmentFile;
	private final int duration; // duration of segment in milliseconds
	private final boolean discontinuityFlag;

	public HlsSegment(HlsSegmentFile segmentFile, int duration, boolean discontinuityFlag) {
		this.segmentFile = segmentFile;
		this.duration = duration;
		this.discontinuityFlag = discontinuityFlag;
	}
	
	/**
	 * Get the reference to the segment file.
	 * @return
	 */
	public HlsSegmentFile getSegmentFile() {
		return segmentFile;
	}
	
	/**
	 * Get the duration of the segment in milliseconds.
	 * @return
	 */
	public int getDuration() {
		return duration;
	}
	
	/**
	 * Determine if the segment that preceded this is of a different format meaning the
	 * discontinuity line should be appended before this in the playlist.
	 * @return
	 */
	public boolean getDiscontinuityFlag() {
		return discontinuityFlag;
	}
	
}
