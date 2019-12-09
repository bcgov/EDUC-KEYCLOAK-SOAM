package ca.bc.gov.educ.keycloak.soam.authenticator;

import javax.json.JsonReader;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
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
			logger.info("SOAM Post: inside authenticate");
			
	        if (context.getAuthenticationSession().getAuthNote(BROKER_REGISTERED_NEW_USER) != null) {
	        	context.setUser(context.getUser());
				context.success();
	            return;
	        }
	        
			String accountType = context.getUser().getFirstAttribute("account_type");
			
			if(accountType == null) {
				throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
			}
			
			String username = null;
			
			switch (accountType) {
			case "bceid":
				logger.info("SOAM Post: Account type bceid found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No bceid_guid value was found in token");
				}
				updateBasicUser(username, accountType);
				break;
			case "bcsc":
				logger.info("SOAM Post: Account type bcsc found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No bcsc_did value was found in token");
				}
				break;
			case "idir":
				logger.info("SOAM Post: Account type idir found");
				username = context.getUser().getUsername();
				if(username == null) {
					throw new SoamRuntimeException("No idir_guid value was found in token");
				}
				break; 
			default:
				throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
			}
			
//			String stringSerialCtx = context.getAuthenticationSession().getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
//			SerializedBrokeredIdentityContext serializedCtx = JsonSerialization.readValue(stringSerialCtx, SerializedBrokeredIdentityContext.class);
//			BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), context.getAuthenticationSession());
//			JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
//			
//	        logger.info("JWT token is: " + token);
//	        
//			for(String s: token.getOtherClaims().keySet()) {
//        		logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
//			}
			
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
    
    protected void updateBasicUser(String guid, String accountType) {
    	logger.info("SOAM Post: updateBasicUser");
    	logger.info("SOAM Post: performing login for " + accountType + " user: " + guid);
    	
    	try {
			RestUtils.getInstance().performLogin("BASIC", guid, guid);
		} catch (Exception e) {
			logger.error("Exception occurred within SOAM while processing login" + e.getMessage());
			throw new SoamRuntimeException("Exception occurred within SOAM while processing login, check downstream logs for digital ID API service");
		}
    }
 
    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM Post: inside returning authenticateImpl");
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
