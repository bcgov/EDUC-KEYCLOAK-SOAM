package ca.bc.gov.educ.keycloak.soam.properties;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * Class holds all application properties
 * 
 * @author Marco Villeneuve
 *
 */
@Configuration
@ComponentScan("ca.bc.gov.educ.keycloak.soam")
@PropertySource("classpath:application.properties")
public class ApplicationProperties {

	@Value("${soamURL}")
	private String soamURL;

	@Value("${tokenURL}")
	private String tokenURL;

	@Value("${clientID}")
	private String clientID;

	@Value("${clientSecret}")
	private String clientSecret;

	public String getSoamURL() {
		return soamURL;
	}

	public void setSoamURL(String soamURL) {
		this.soamURL = soamURL;
	}

	public String getTokenURL() {
		return tokenURL;
	}

	public void setTokenURL(String tokenURL) {
		this.tokenURL = tokenURL;
	}

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getClientSecret() {
		return clientSecret;
	}

	public void setClientSecret(String clientSecret) {
		this.clientSecret = clientSecret;
	}

}
