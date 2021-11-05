package ca.bc.gov.educ.keycloak.soam.authenticator;

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.properties.ApplicationProperties;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import javax.json.JsonReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class SoamPostSAMLLoginAuthenticator extends AbstractIdpAuthenticator {

  private static Logger logger = Logger.getLogger(SoamPostSAMLLoginAuthenticator.class);


  @Override
  protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
  }

  @Override
  public void authenticate(AuthenticationFlowContext context) {
    JsonReader reader = null;
    try {
      logger.debug("SOAM Post: inside authenticate");

      if (context.getAuthenticationSession().getAuthNote(BROKER_REGISTERED_NEW_USER) != null) {
        context.setUser(context.getUser());
        context.success();
        return;
      }

      String stringSerialCtx = context.getAuthenticationSession().getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
      SerializedBrokeredIdentityContext serializedCtx = JsonSerialization.readValue(stringSerialCtx, SerializedBrokeredIdentityContext.class);
      BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), context.getAuthenticationSession());

      Map<String, Object> brokerClaims = brokerContext.getContextData();
      for (String s : brokerClaims.keySet()) {
        logger.debug("Context Key: " + s + " Value: " + brokerClaims.get(s));
      }

      String accountType = context.getUser().getFirstAttribute("account_type");

      if (accountType == null) {
        throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
      }

      AssertionType assertion = (AssertionType) brokerContext.getContextData().get("SAML_ASSERTION");

      String userGUID = null;
      String displayName = null;
      String email = null;
      String usernameFromToken = null;

      Set<AttributeStatementType> otherClaims = assertion.getAttributeStatements();
      for (AttributeStatementType s : otherClaims) {
        logger.info("SAML Assertion Claims:");
        for (AttributeStatementType.ASTChoiceType type : s.getAttributes()) {
          String name = type.getAttribute().getName();
          logger.info("SAML_ASSERTION Key: " + name + " Value: " + type.getAttribute().getAttributeValue());

          if (name.equalsIgnoreCase("useridentifier")) {
            userGUID = (String) type.getAttribute().getAttributeValue().get(0);
          } else if (name.equalsIgnoreCase("user_name")) {
            usernameFromToken = (String) type.getAttribute().getAttributeValue().get(0);
          } else if (name.equalsIgnoreCase("SMGOV_USERDISPLAYNAME")) {
            displayName = (String) type.getAttribute().getAttributeValue().get(0);
          } else if (name.equalsIgnoreCase("Email")) {
            email = (String) type.getAttribute().getAttributeValue().get(0);
          }
        }
      }

      String username = (String) ((ArrayList) brokerClaims.get("user.attributes.username")).get(0);
      UserModel existingUser = context.getUser();

      switch (accountType) {
        case "bceid":
          logger.debug("SOAM Post: Account type bceid found");
          existingUser.setSingleAttribute("display_name", displayName);
          existingUser.setSingleAttribute("bceid_userid", userGUID);
          existingUser.setSingleAttribute("user_guid", userGUID);

          if (userGUID == null) {
            throw new SoamRuntimeException("No bceid_guid value was found in token");
          }
          updateUserInfo(userGUID, accountType, "BASIC", null);
          break;
        case "idir":
          logger.debug("SOAM Post: Account type idir found");
          existingUser.setSingleAttribute("idir_username", usernameFromToken);
          existingUser.setSingleAttribute("idir_guid", userGUID);
          existingUser.setSingleAttribute("user_guid", userGUID);
          existingUser.setSingleAttribute("display_name", displayName);

          if (userGUID == null) {
            throw new SoamRuntimeException("No idir_guid value was found in token");
          }
          break;
        default:
          throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
      }

      context.setUser(context.getUser());
      context.success();
    } catch (Exception e) {
      throw new SoamRuntimeException(e);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  protected void updateUserInfo(String guid, String accountType, String credType, SoamServicesCard servicesCard) {
    logger.debug("SOAM: createOrUpdateUser");
    logger.debug("SOAM: performing login for " + accountType + " user: " + guid);

    try {
      RestUtils.getInstance().performLogin(credType, guid, guid, servicesCard);
    } catch (Exception e) {
      logger.error("Exception occurred within SOAM while processing login" + e.getMessage());
      throw new SoamRuntimeException("Exception occurred within SOAM while processing login, check downstream logs for SOAM API service");
    }
  }

  @Override
  protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    logger.debug("SOAM Post: inside returning authenticateImpl");
    //Not used for Post Login Authenticator
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
