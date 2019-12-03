package ca.bc.gov.educ.keycloak.soam.rest;

import java.util.Collections;
import java.util.List;

import org.jboss.logging.Logger;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.soam.properties.ApplicationProperties;

/**
 * This class is used for REST calls
 * 
 * @author Marco Villeneuve
 *
 */
public class RestUtils {
	
	private static Logger logger = Logger.getLogger(RestUtils.class);
	
	private static RestUtils restUtilsInstance;
	
	private static ApplicationProperties props;

	private RestUtils() {
		props = new ApplicationProperties();
	}
	
	public static RestUtils getInstance() {
		if(restUtilsInstance == null) {
			restUtilsInstance = new RestUtils();
		}
		return restUtilsInstance;
	}

	public RestTemplate getRestTemplate(List<String> scopes) {
		logger.debug("Calling get token method");
		ClientCredentialsResourceDetails resourceDetails = new ClientCredentialsResourceDetails();
		resourceDetails.setClientId(props.getClientID());
		resourceDetails.setClientSecret(props.getClientSecret());
		resourceDetails.setAccessTokenUri(props.getTokenURL());
		if(scopes != null) {
			resourceDetails.setScope(scopes);
		}
		return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());
	}

    public SoamLoginEntity performLogin(String identifierType, String identifierValue, String userID) {
		RestTemplate restTemplate = getRestTemplate(null);
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map= new LinkedMultiValueMap<String, String>();
		map.add("identifierType", identifierType);
		map.add("identifierValue", identifierValue);
		map.add("userID", userID);
		
		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);
	
		ResponseEntity<SoamLoginEntity> response;
		try {
			response = restTemplate.postForEntity(props.getSoamApiURL() + "/login",request, SoamLoginEntity.class);
			return response.getBody();
		} catch (final HttpClientErrorException e) {
			
			//ADD ERROR LOGIC!
			e.printStackTrace();
		}
		
        return null;
    }
    
    
    public SoamLoginEntity getSoamLoginEntity(String identifierType, String identifierValue) {
		RestTemplate restTemplate = getRestTemplate(null);
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
		
		try {
			return restTemplate.exchange(props.getSoamApiURL() + "/" + identifierType + "/" + identifierValue, HttpMethod.GET, new HttpEntity<>("parameters", headers), SoamLoginEntity.class).getBody();
		} catch (final HttpClientErrorException e) {
			
			//ADD ERROR LOGIC!
			e.printStackTrace();
		}
		
        return null;
    }
}
