package uk.co.la1tv.dvrBridgeService;


import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;

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
    }
}
