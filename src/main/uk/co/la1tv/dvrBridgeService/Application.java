package uk.co.la1tv.dvrBridgeService;


import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylist;
import uk.co.la1tv.dvrBridgeService.hlsRecorder.HlsPlaylistCapture;

@ComponentScan
@EnableAutoConfiguration
public class Application {
	private static Logger logger = Logger.getLogger(Application.class);
	
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    
    @Autowired
	private ApplicationContext context;
    
    @PostConstruct
    public void onPostConstruct() {
    	logger.info("Application loaded!");
    	
    	HlsPlaylistCapture a = null;
		try {
			a = context.getBean(HlsPlaylistCapture.class, new HlsPlaylist(new URL("https://devimages.apple.com.edgekey.net/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8")));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	a.startCapture();
    }
}
