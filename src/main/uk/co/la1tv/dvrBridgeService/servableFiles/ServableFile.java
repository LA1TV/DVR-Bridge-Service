package uk.co.la1tv.dvrBridgeService.servableFiles;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ServableFile extends File {

	private static final long serialVersionUID = 3301874952415408890L;

	@Value("${app.webDirectoryBaseUrl}")
	private String baseUrlStr;
	private URL baseUrl = null;
	
	@Value("${app.serverId}")
	private String serverIdStr;
	
	public ServableFile(File parent, String child) {
		super(parent, child);
	}
	
	@PostConstruct
	private void onPostConstruct() {
		try {
			// each server has a subdirectory set to its id
			baseUrl = new URL(baseUrlStr+"/"+serverIdStr);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			throw(new RuntimeException("Invalid base url."));
		}
	}
	
	/**
	 * Get the url to this file on the web server.
	 * @return
	 */
	public URL getUrl() {
		try {
			return new URL(baseUrl, getName());
		} catch (MalformedURLException e) {
			// shouldn't happen!
			e.printStackTrace();
			throw(new RuntimeException("Unable to build file url."));
		}
	}

}
