package ca.bc.gov.educ.keycloak.tenant.rest;

import ca.bc.gov.educ.keycloak.common.properties.ApplicationProperties;
import ca.bc.gov.educ.keycloak.tenant.model.TenantAccess;
import org.jboss.logging.Logger;
import org.jboss.logging.MDC;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.token.grant.client.ClientCredentialsResourceDetails;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

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

  public TenantAccess checkForValidTenant(String clientID, String tenantID) {
    String url = props.getSoamApiURL() + "/tenant";
    final String correlationID = logAndGetCorrelationID(tenantID, url, HttpMethod.GET.toString());
    RestTemplate restTemplate = getRestTemplate(null);
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    headers.add("correlationID", correlationID);
    Map<String, String> params = new HashMap<>();
    params.put("clientID", clientID);
    params.put("tenantID", tenantID);
    try {
      return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>("parameters", headers), TenantAccess.class, params).getBody();
    } catch (final HttpClientErrorException e) {
      throw new RuntimeException("Could not complete checkForValidTenant call: " + e.getMessage());
    }
  }

  private String logAndGetCorrelationID(String tenantID, String url, String httpMethod) {
    final String correlationID = UUID.randomUUID().toString();
    MDC.put("correlation_id", correlationID);
    MDC.put("tenant_id", tenantID);
    MDC.put("client_http_request_url", url);
    MDC.put("client_http_request_method", httpMethod);
    logger.info("correlation id for tenant ID=" + tenantID + " is=" + correlationID);
    MDC.clear();
    return correlationID;
  }
}
