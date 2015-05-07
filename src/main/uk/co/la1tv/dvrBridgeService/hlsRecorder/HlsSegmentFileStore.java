package uk.co.la1tv.dvrBridgeService.hlsRecorder;

import java.io.File;
import java.net.URL;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import uk.co.la1tv.dvrBridgeService.helpers.FileHelper;

/**
 * Holds references to all of the segment files that have been downloaded
 * to the server.
 */
@Service
public class HlsSegmentFileStore {
	
	@Autowired
	private ApplicationContext context;
	
	@Autowired
	private HlsFileGenerator hlsFileGenerator;
	
	// Key is the url object
	private final HashMap<URL, HlsSegmentFile> segments = new HashMap<>();

	/**
	 * Get a segment that is/was located at the specified url.
	 * If the segment does not already exist locally it will
	 * be downloaded.
	 * @param remoteUrl
	 * @return
	 */
	public HlsSegmentFile getSegment(URL remoteUrl) {
		if (segments.containsKey(remoteUrl)) {
			return segments.get(remoteUrl);
		}
		File localFile = hlsFileGenerator.generateFile(FileHelper.getExtension(remoteUrl.getFile()));
		
		HlsSegmentFile newSegment = context.getBean(HlsSegmentFile.class, remoteUrl, localFile);
		segments.put(remoteUrl, newSegment);
		return newSegment;
	}
	
}
