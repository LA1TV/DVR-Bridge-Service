package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IHlsSegmentFileStateChangeCallback {
	
	/**
	 * Called when the state of the segment file changes.
	 * @param state
	 */
	void onStateChange(HlsSegmentFileState state);
	
}
