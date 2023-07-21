package ca.bc.gov.educ.keycloak.tenant.authenticator;

import ca.bc.gov.educ.keycloak.tenant.exception.TenantRuntimeException;
import ca.bc.gov.educ.keycloak.tenant.rest.TenantRestUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

import java.util.List;
import java.util.Map;

/**
 * SOAM First Time login authenticator
 * This class will handle the callouts to our API
 *
 * @author Marco Villeneuve
 */
public class TenantFirstTimeLoginAuthenticator extends AbstractIdpAuthenticator {

  private static Logger logger = Logger.getLogger(TenantFirstTimeLoginAuthenticator.class);


  @Override
  protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
  }



  @Override
  protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    logger.debug("Tenant: inside first time authenticateImpl");

    if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
      context.attempted();
      return;
    }

    Map<String, Object> brokerClaims = brokerContext.getContextData();
    for (String s : brokerClaims.keySet()) {
      logger.debug("Context Key: " + s + " Value: " + brokerClaims.get(s));
    }

    JsonWebToken token = (JsonWebToken) brokerContext.getContextData().get("VALIDATED_ID_TOKEN");

    Map<String, Object> otherClaims = token.getOtherClaims();
    for (String s : otherClaims.keySet()) {
      logger.debug("VALIDATED_ID_TOKEN Key: " + s + " Value: " + otherClaims.get(s));
    }

    String tenantID = (String) otherClaims.get("tenantID");
    String clientID = context.getAuthenticationSession().getClient().getClientId();

//    TenantRestUtils.getInstance().checkForValidTenant(clientID, tenantID);

    context.success();
  }

  @Override
  public boolean requiresUser() {
    return false;
  }

  @Override
  public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;
  }

}
