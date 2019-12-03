package ca.bc.gov.educ.keycloak.soam.authenticator;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;

/**
 * SOAM First Time login authenticator
 * This class will handle the callouts to our API
 * 
 * @author Marco Villeneuve
 *
 */
public class SoamFirstTimeLoginAuthenticator extends AbstractIdpAuthenticator { 

    private static Logger logger = Logger.getLogger(SoamFirstTimeLoginAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    }

    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM: inside authenticateImpl");
        KeycloakSession session = context.getSession();
        RealmModel realm = context.getRealm();

        if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
            context.attempted();
            return;
        }
        
        JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
        
        logger.info("JWT token is: " + token);
        
		for(String s: token.getOtherClaims().keySet()) {
    		logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
		}
        
        //Username will be a generated GUID when the DB is setup
        String username = (String)token.getOtherClaims().get("bceid_guid");
        
        if(username == null) {
        	throw new RuntimeException("No BCeID guid was found in token");
        }

        createOrUpdateBasicUser(username);
        
        //Temporary change
        if(context.getSession().users().getUserByUsername(username, realm) == null) {
        //if (!userExists) {
            logger.infof("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, brokerContext.getIdpConfig().getAlias());

            UserModel federatedUser = session.users().addUser(realm, username);
            federatedUser.setEnabled(true);
            //federatedUser.setEmail(brokerContext.getEmail());
            //federatedUser.setFirstName(brokerContext.getFirstName());
            //federatedUser.setLastName(brokerContext.getLastName());
            //This random value will be the actual BCeID GUID when the DB is ready
            federatedUser.setSingleAttribute("first_name", brokerContext.getFirstName());
            federatedUser.setSingleAttribute("last_name", brokerContext.getLastName());
            federatedUser.setSingleAttribute("email_address", brokerContext.getEmail());
            federatedUser.setSingleAttribute("display_name", (String)token.getOtherClaims().get("display_name"));
            federatedUser.setSingleAttribute("middle_names", "FIX WHEN CAP SERVICE IS IN");

            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            //userRegisteredSuccess(context, federatedUser, serializedCtx, brokerContext);

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else {
        	UserModel existingUser = context.getSession().users().getUserByUsername(username, realm);
        	context.setUser(existingUser);
        	context.success();
        } 
    }

    protected void createOrUpdateBasicUser(String guid) {
    	logger.info("SOAM: inside checkExistingUser");
    	logger.info("SOAM: performing login: " + guid);
    	
    	RestUtils.getInstance().performLogin("BASIC", guid, guid);
    }

//    // Empty method by default. This exists, so subclass can override and add callback after new user is registered through social
//    protected void userRegisteredSuccess(AuthenticationFlowContext context, UserModel registeredUser, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
//
//    }
    
    @Override
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
