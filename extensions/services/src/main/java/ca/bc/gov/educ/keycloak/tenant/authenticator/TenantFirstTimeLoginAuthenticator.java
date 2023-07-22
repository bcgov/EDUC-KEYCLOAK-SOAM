package ca.bc.gov.educ.keycloak.tenant.authenticator;

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.rest.SoamRestUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

import java.util.Map;

public class TenantFirstTimeLoginAuthenticator extends AbstractIdpAuthenticator {

  private static Logger logger = Logger.getLogger(ca.bc.gov.educ.keycloak.soam.authenticator.SoamFirstTimeLoginAuthenticator.class);


  @Override
  protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
  }

  @Override
  protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    logger.debug("Tenant: inside authenticateImpl");
    KeycloakSession session = context.getSession();
    RealmModel realm = context.getRealm();

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

    String accountType = (String) otherClaims.get("account_type");

    String username = "TEST";

    logger.debug("Tenant: Existing " + accountType + " user found with username: " + username);
    context.success();
  }

  protected void createOrUpdateUser(String guid, String accountType, String credType, SoamServicesCard servicesCard) {
    logger.debug("Tenant: createOrUpdateUser");
    logger.debug("Tenant: performing login for " + accountType + " user: " + guid);

    try {
      SoamRestUtils.getInstance().performLogin(credType, guid, guid, servicesCard);
    } catch (Exception e) {
      logger.error("Exception occurred within SOAM while processing login" + e.getMessage());
      throw new SoamRuntimeException("Exception occurred within SOAM while processing login, check downstream logs for SOAM API service");
    }
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
