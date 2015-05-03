package uk.co.la1tv.dvrBridgeService.helpers;

import java.io.InputStream;

public interface StreamMonitor extends Runnable {
	
	public void setStream(InputStream stream);
	
}
