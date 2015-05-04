package uk.co.la1tv.dvrBridgeService.hlsRecorder;

/**
 * Represents a hls segment.
 */
public class HlsSegment {

	private final HlsSegmentFile segmentFile;
	private final int sequenceNumber;
	private final float duration; // duration of segment
	private final boolean discontinuityFlag;

	public HlsSegment(HlsSegmentFile segmentFile, int sequenceNumber, float duration, boolean discontinuityFlag) {
		this.segmentFile = segmentFile;
		this.sequenceNumber = sequenceNumber;
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
	 * Get this segment's sequence number.
	 * @return
	 */
	public int getSequenceNumber() {
		return sequenceNumber;
	}
	
	/**
	 * Get the duration of the segment in milliseconds.
	 * @return
	 */
	public float getDuration() {
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
