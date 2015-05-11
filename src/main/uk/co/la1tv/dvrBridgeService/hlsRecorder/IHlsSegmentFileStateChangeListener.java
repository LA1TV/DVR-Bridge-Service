package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface IHlsSegmentFileStateChangeListener {
	
	/**
	 * Called when the state of the segment file changes.
	 * The state is the state when the callbacks were triggered.
	 * The state may change in between some of the callbacks but this will
	 * just result in all of the callbacks getting called multiple times
	 * with the different state. Each listener will get the same number of
	 * callbacks with the same state values.
	 * @param state
	 */
	void onStateChange(HlsSegmentFileState state);
	
}
