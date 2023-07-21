package ca.bc.gov.educ.keycloak.tenant.rest;

import ca.bc.gov.educ.keycloak.common.properties.ApplicationProperties;
import ca.bc.gov.educ.keycloak.tenant.model.TenantResponse;
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

import java.util.List;
import java.util.UUID;

/**
 * This class is used for REST calls
 *
 * @author Marco Villeneuve
 */
public class TenantRestUtils {

  private static Logger logger = Logger.getLogger(TenantRestUtils.class);

  private static TenantRestUtils tenantRestUtilsInstance;

  private static ApplicationProperties props;

  private TenantRestUtils() {
    props = new ApplicationProperties();
  }

  public static TenantRestUtils getInstance() {
    if (tenantRestUtilsInstance == null) {
      tenantRestUtilsInstance = new TenantRestUtils();
    }
    return tenantRestUtilsInstance;
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

  public void checkForValidTenant(String clientID, String tenantID) {
    String url = props.getSoamApiURL() + "/valid-tenant";
    final String correlationID = logAndGetCorrelationID(tenantID + ":" + clientID, url, HttpMethod.POST.toString());
    RestTemplate restTemplate = getRestTemplate(null);
    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
    headers.add("correlationID", correlationID);
    MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
    map.add("clientID", clientID);
    map.add("tenantID", tenantID);

    HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<MultiValueMap<String, String>>(map, headers);

    try {
      logger.debug("Calling checkForValidTenant with client ID: " + clientID + " and Tenant ID: " + tenantID);
      restTemplate.postForEntity(url, request, TenantResponse.class);
    } catch (final HttpClientErrorException e) {
      throw new RuntimeException("Could not complete valid tenant check call: " + e.getMessage());
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
