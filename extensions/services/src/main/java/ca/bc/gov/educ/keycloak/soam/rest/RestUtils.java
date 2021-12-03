package ca.bc.gov.educ.keycloak.soam.rest;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.properties.ApplicationProperties;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
public class RestUtils {

  private static Logger logger = Logger.getLogger(RestUtils.class);

  private static RestUtils restUtilsInstance;

  private static ApplicationProperties props;

  private RestUtils() {
    props = new ApplicationProperties();
  }

  public static RestUtils getInstance() {
    if (restUtilsInstance == null) {
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
    if (scopes != null) {
      resourceDetails.setScope(scopes);
    }
    return new OAuth2RestTemplate(resourceDetails, new DefaultOAuth2ClientContext());
  }

  public void performLogin(String identifierType, String identifierValue, String userID, SoamServicesCard servicesCard) {
    String url = props.getSoamApiURL() + "/login";
    final String correlationID = logAndGetCorrelationID(identifierValue, url, HttpMethod.POST.toString());
    RestTemplate restTemplate = getRestTemplate(null);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("correlationID", correlationID);
    MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
    map.add("identifierType", identifierType);
    map.add("identifierValue", identifierValue);
    map.add("userID", userID);

    if (servicesCard != null) {
      map.add("birthDate", servicesCard.getBirthDate());
      map.add("city", servicesCard.getCity());
      map.add("country", servicesCard.getCountry());
      map.add("did", servicesCard.getDid());
      map.add("email", servicesCard.getEmail());
      map.add("gender", servicesCard.getGender());
      map.add("givenName", servicesCard.getGivenName());
      map.add("givenNames", servicesCard.getGivenNames());
      map.add("identityAssuranceLevel", servicesCard.getIdentityAssuranceLevel());
      map.add("postalCode", servicesCard.getPostalCode());
      map.add("province", servicesCard.getProvince());
      map.add("streetAddress", servicesCard.getStreetAddress());
      map.add("surname", servicesCard.getSurname());
      map.add("userDisplayName", servicesCard.getUserDisplayName());
    }

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

    try {
      restTemplate.postForEntity(url, request, SoamLoginEntity.class);
    } catch (final HttpClientErrorException e) {
      throw new RuntimeException("Could not complete login call: " + e.getMessage());
    }
  }

  public SoamLoginEntity getSoamLoginEntity(String identifierType, String identifierValue) {
    String url = props.getSoamApiURL() + "/" + identifierType + "/" + identifierValue;
    final String correlationID = logAndGetCorrelationID(identifierValue, url, HttpMethod.GET.toString());
    RestTemplate restTemplate = getRestTemplate(null);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.add("correlationID", correlationID);
    try {
      return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>("parameters", headers), SoamLoginEntity.class).getBody();
    } catch (final HttpClientErrorException e) {
      throw new RuntimeException("Could not complete getSoamLoginEntity call: " + e.getMessage());
    }
  }

  public List<String> getSTSRoles(String identifierValue) {
    String url = props.getSoamApiURL() + "/" + identifierValue + "/" + "sts-user-roles";
    final String correlationID = logAndGetCorrelationID(identifierValue, url, HttpMethod.GET.toString());
    RestTemplate restTemplate = getRestTemplate(null);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.add("correlationID", correlationID);
    try {
      return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>("parameters", headers), List.class).getBody();
    } catch (final HttpClientErrorException e) {
      throw new RuntimeException("Could not complete getSTSRoles call: " + e.getMessage());
    }
  }

  private String logAndGetCorrelationID(String identifierValue, String url, String httpMethod) {
    final String correlationID = UUID.randomUUID().toString();
    MDC.put("correlation_id", correlationID);
    MDC.put("user_guid", identifierValue);
    MDC.put("client_http_request_url", url);
    MDC.put("client_http_request_method", httpMethod);
    logger.info("correlation id for guid=" + identifierValue + " is=" + correlationID);
    MDC.clear();
    return correlationID;
  }
}
