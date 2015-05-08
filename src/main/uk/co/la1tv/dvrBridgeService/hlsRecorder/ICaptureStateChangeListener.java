package uk.co.la1tv.dvrBridgeService.hlsRecorder;

public interface ICaptureStateChangeListener {
	
	/**
	 * Called whenever the state of the capture has changed.
	 * This is called in a separate thread. The new state is passed in
	 * as a parameter and this is the state as it it was when the callback
	 * was triggered. It is possible for the state to have changed
	 * between this method getting called and after the callback thread was
	 * started. If this is the case the callback will be called again with these
	 * changes. The callback will get called once for every state change.
	 * @param newState
	 */
	void onStateChange(HlsPlaylistCaptureState newState);
}
