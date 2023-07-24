package ca.bc.gov.educ.keycloak.soam.authenticator;

import ca.bc.gov.educ.keycloak.common.properties.ApplicationProperties;
import ca.bc.gov.educ.keycloak.common.utils.CommonUtils;
import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.rest.SoamRestUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.util.JsonSerialization;

import javax.json.JsonReader;
import java.util.List;
import java.util.Map;


public class SoamPostLoginAuthenticator extends AbstractIdpAuthenticator {

  private static Logger logger = Logger.getLogger(SoamPostLoginAuthenticator.class);


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

      //This is added for BCSC - direct IDP
      if (accountType == null) {
        accountType = (String) brokerContext.getContextData().get("user.attributes.account_type");
      }

      if (accountType == null) {
        throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
      }

      JsonWebToken token = (JsonWebToken) brokerContext.getContextData().get("VALIDATED_ID_TOKEN");

      Map<String, Object> otherClaims = token.getOtherClaims();
      logger.debug(ApplicationProperties.mapper.writeValueAsString(otherClaims));
      UserModel existingUser = context.getUser();
      String user_guid = null;

      switch (accountType) {
        case "entra":
          logger.debug("SOAM Post: Account type entra found");
          user_guid = (String) otherClaims.get("entra_user_id");
          existingUser.setSingleAttribute("user_guid", user_guid);
          if (user_guid == null) {
            throw new SoamRuntimeException("No entra_user_id value was found in token");
          }
          updateUserInfo(user_guid, accountType, "ENTRA", null);
          break;
        case "bceidbasic":
          logger.debug("SOAM Post: Account type basic bceid found");
          user_guid = (String) otherClaims.get("bceid_user_guid");
          existingUser.setSingleAttribute("user_guid", user_guid);
          if (user_guid == null) {
            throw new SoamRuntimeException("No bceid_user_guid value was found in token");
          }
          updateUserInfo(user_guid, accountType, "BASIC", null);
          break;
        case "bcsc":
          logger.debug("SOAM Post: Account type bcsc found");
          user_guid = ((List<String>) brokerContext.getContextData().get("user.attributes.did")).get(0);
          existingUser.setSingleAttribute("user_did", user_guid);
          if (user_guid == null) {
            throw new SoamRuntimeException("No bcsc_did value was found in token");
          }
          SoamServicesCard servicesCard = new SoamServicesCard();
          servicesCard.setBirthDate(CommonUtils.getValueForAttribute("user.attributes.birthdate", brokerContext));
          servicesCard.setDid(CommonUtils.getValueForAttribute("user.attributes.did", brokerContext));
          servicesCard.setEmail(CommonUtils.getValueForAttribute("user.attributes.emailAddress", brokerContext));
          servicesCard.setGender(CommonUtils.getValueForAttribute("user.attributes.gender", brokerContext));
          servicesCard.setGivenName(CommonUtils.getValueForAttribute("user.attributes.given_name", brokerContext));
          servicesCard.setGivenNames(CommonUtils.getValueForAttribute("user.attributes.given_names", brokerContext));
          servicesCard.setIdentityAssuranceLevel(CommonUtils.getValueForAttribute("user.attributes.identity_assurance_level", brokerContext));
          servicesCard.setPostalCode(CommonUtils.getValueForAttribute("user.attributes.postal_code", brokerContext));
          servicesCard.setSurname(CommonUtils.getValueForAttribute("user.attributes.family_name", brokerContext));
          servicesCard.setUserDisplayName(CommonUtils.getValueForAttribute("user.attributes.display_name", brokerContext));
          updateUserInfo(user_guid, accountType, "BCSC", servicesCard);
          break;
        default:
          throw new SoamRuntimeException("Account type is not bcsc or bceid, check IDP mappers");
      }

      context.setUser(existingUser);
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
      SoamRestUtils.getInstance().performLogin(credType, guid, guid, servicesCard);
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
