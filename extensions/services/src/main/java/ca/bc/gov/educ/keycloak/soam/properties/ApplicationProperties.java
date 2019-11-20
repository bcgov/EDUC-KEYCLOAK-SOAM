package ca.bc.gov.educ.keycloak.soam.properties;

import org.jboss.logging.Logger;

/**
 * Class holds all application properties
 * 
 * @author Marco Villeneuve
 *
 */
public class ApplicationProperties {
	
	private static Logger logger = Logger.getLogger(ApplicationProperties.class);

	public String getSoamURL() {
		logger.info("Fetching SOAM URL: " + System.getenv().getOrDefault("soamURL", "hello world"));
		return System.getenv().getOrDefault("soamURL", "hello world");
	}

	public String getTokenURL() {
		return System.getenv().getOrDefault("tokenURL", "testtoken");
	}

	public String getClientID() {
		return System.getenv().getOrDefault("clientID", "testclient");
	}

	public String getClientSecret() {
		return System.getenv().getOrDefault("clientSecret", "testsecret");
	}

}
