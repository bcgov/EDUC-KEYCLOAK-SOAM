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

import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;


public class SoamPostLoginAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(SoamPostLoginAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM Post: inside actionImpl");
        
    }
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
    	JsonReader reader = null;
    	try {
			logger.info("SOAM Post: inside authenticate");
			
	        if (context.getAuthenticationSession().getAuthNote(BROKER_REGISTERED_NEW_USER) != null) {
	            context.attempted();
	            return;
	        }
			
			//logger.info("User GUID: " + context.getUser().getFirstAttribute("GUID"));
			
//			String stringSerialCtx = context.getAuthenticationSession().getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
//			SerializedBrokeredIdentityContext serializedCtx = JsonSerialization.readValue(stringSerialCtx, SerializedBrokeredIdentityContext.class);
//			BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), context.getAuthenticationSession());
			//JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
			
//	        logger.info("JWT token is: " + token);
//	        
//			for(String s: token.getOtherClaims().keySet()) {
//        		logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
//			}
			
			//UserModel existingUser = context.getSession().users().getUserByUsername(context.getUser().getUsername(), context.getRealm());
			
			updateBasicUser(context.getUser().getUsername());
			context.setUser(context.getUser());
			context.success();
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			if(reader != null) {
				reader.close();
			}
		}
    }
    
    protected void updateBasicUser(String guid) {
    	logger.info("SOAM Post: inside updateBasicUser");
    	logger.info("SOAM Post: performing login: " + guid);
    	
    	RestUtils.getInstance().performLogin("BASIC", guid, guid);
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
