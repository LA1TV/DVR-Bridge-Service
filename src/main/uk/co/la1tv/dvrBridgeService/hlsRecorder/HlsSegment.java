package uk.co.la1tv.dvrBridgeService.hlsRecorder;

/**
 * Represents a hls segment.
 */
public class HlsSegment {
	
	public final int duration; // segment duration in seconds
	
	public HlsSegment(int duration) {
		this.duration = duration;
	}
	
	public int getDuration() {
		return duration;
	}
}
