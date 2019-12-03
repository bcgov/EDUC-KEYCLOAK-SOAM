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

	private String soamApiURL;
	private String tokenURL;
	private String clientID;
	private String clientSecret;

	public ApplicationProperties() {
		logger.info("SOAM: Building application properties");
		soamApiURL = System.getenv().getOrDefault("soamApiURL", "MissingSoamURL");
		tokenURL = System.getenv().getOrDefault("tokenURL", "MissingSoamTokenURL");
		clientID = System.getenv().getOrDefault("clientID", "MissingSoamClientID");
		clientSecret = System.getenv().getOrDefault("clientSecret", "MissingSoamClientSecret");
	}

	public String getSoamApiURL() {
		return soamApiURL;
	} 

	public String getTokenURL() {
		return tokenURL;
	}

	public String getClientID() {
		return clientID;
	}

	public String getClientSecret() {
		return clientSecret;
	}

}
