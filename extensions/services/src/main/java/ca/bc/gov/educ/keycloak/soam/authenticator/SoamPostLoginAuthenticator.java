package ca.bc.gov.educ.keycloak.soam.authenticator;

import java.util.Map;

import javax.json.JsonReader;

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

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;


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
	        
			String accountType = context.getUser().getFirstAttribute("account_type");
			
			if(accountType == null) {
				throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
			}
			
			String stringSerialCtx = context.getAuthenticationSession().getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
			SerializedBrokeredIdentityContext serializedCtx = JsonSerialization.readValue(stringSerialCtx, SerializedBrokeredIdentityContext.class);
			BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), context.getAuthenticationSession());
			JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
				        
	        Map<String, Object> otherClaims = token.getOtherClaims();
			for(String s: otherClaims.keySet()) {
	    		logger.debug("Key: " + s + " Value: " + otherClaims.get(s));
			}
			
			String username = null;
			
			switch (accountType) {
			case "bceid":
				logger.debug("SOAM Post: Account type bceid found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No bceid_guid value was found in token");
				}
				updateUserInfo(username, accountType, "BASIC", null);
				break;
			case "bcsc":
				logger.debug("SOAM Post: Account type bcsc found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No bcsc_did value was found in token");
				}
				SoamServicesCard servicesCard = new SoamServicesCard();
				servicesCard.setBirthDate((String)otherClaims.get("birthdate"));
				servicesCard.setCity((String)otherClaims.get("locality"));
				servicesCard.setCountry((String)otherClaims.get("country"));
				servicesCard.setDid((String)otherClaims.get("bcsc_did"));
				servicesCard.setEmail((String)otherClaims.get("email"));
				servicesCard.setGender((String)otherClaims.get("gender"));
				servicesCard.setGivenName((String)otherClaims.get("given_name"));
				servicesCard.setGivenNames((String)otherClaims.get("given_names"));
				servicesCard.setIdentityAssuranceLevel((String)otherClaims.get("identity_assurance_level"));
				servicesCard.setPostalCode((String)otherClaims.get("postal_code"));
				servicesCard.setProvince((String)otherClaims.get("region"));
				servicesCard.setStreetAddress((String)otherClaims.get("street_address"));
				servicesCard.setSurname((String)otherClaims.get("family_name"));
				servicesCard.setUserDisplayName((String)otherClaims.get("name"));
				updateUserInfo(username, accountType, "BCSC", servicesCard);
				break;
			case "idir":
				logger.debug("SOAM Post: Account type idir found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No idir_guid value was found in token");
				}
				break; 
			default:
				throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
			}
			
			//UserModel existingUser = context.getSession().users().getUserByUsername(context.getUser().getUsername(), context.getRealm());
			
			context.setUser(context.getUser());
			context.success();
		} catch (Exception e) {
			throw new SoamRuntimeException(e);
		} finally {
			if(reader != null) {
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
