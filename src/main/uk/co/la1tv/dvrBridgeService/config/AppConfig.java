package uk.co.la1tv.dvrBridgeService.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource({
	"file:application.properties",
	"file:local.properties" // this should be excluded from version control
})
public class AppConfig {

}
