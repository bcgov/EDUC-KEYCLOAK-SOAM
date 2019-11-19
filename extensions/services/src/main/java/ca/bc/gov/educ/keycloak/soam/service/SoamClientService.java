package ca.bc.gov.educ.keycloak.soam.service;

import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableOAuth2Client;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("deprecation")
@EnableOAuth2Client
@Configuration
@ComponentScan({"ca.bc.gov.educ.keycloak.soam","ca.bc.gov.educ.keycloak.soam.mapper","ca.bc.gov.educ.keycloak.soam.service"})
public class SoamClientService {
	private static Logger logger = Logger.getLogger(SoamClientService.class);
	
    @Value("${token.url}")
    private String tokenURL;
	
    @Value("${soamAPI.url}")
    private String soamApiURL;
    
    @Value("${client.id}")
    private String clientID;
    
    @Value("${client.secret}")
    private String clientSecret;

    @Bean
    public RestTemplate oAuthRestTemplate() {
        ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
        resourceDetails.setId("1");
        resourceDetails.setClientId(clientID);
        resourceDetails.setClientSecret(clientSecret);
        resourceDetails.setAccessTokenUri(tokenURL);

        /*

        When using @EnableOAuth2Client spring creates a OAuth2ClientContext for us:

        "The OAuth2ClientContext is placed (for you) in session scope to keep the state for different users separate.
        Without that you would have to manage the equivalent data structure yourself on the server,
        mapping incoming requests to users, and associating each user with a separate instance of the OAuth2ClientContext."
        (http://projects.spring.io/spring-security-oauth/docs/oauth2.html#client-configuration)

        Internally the SessionScope works with a threadlocal to store variables, hence a new thread cannot access those.
        Therefore we can not use @Async

        Solution: create a new OAuth2ClientContext that has no scope.
        *Note: this is only safe when using client_credentials as OAuth grant type!

         */

//            OAuth2RestTemplate restTemplate = new      OAuth2RestTemplate(resourceDetails, oauth2ClientContext);
        OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());

        return restTemplate;
    }
    
    public String login() {
    	logger.info("Soam API URL: " + soamApiURL);
        return oAuthRestTemplate().getForObject(soamApiURL, String.class);
    }

}