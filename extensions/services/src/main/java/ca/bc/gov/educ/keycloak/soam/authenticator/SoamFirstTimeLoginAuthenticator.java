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

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
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
        
		String accountType = (String)token.getOtherClaims().get("account_type");
		
		if(accountType == null) {
			throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
		}
		
		String username = null;
		
		switch (accountType) {
		case "bceid":
			logger.info("SOAM: Account type bceid found");
			username = (String)token.getOtherClaims().get("bceid_guid");
			if(username == null) {
				throw new SoamRuntimeException("No bceid_guid value was found in token");
			}
			createOrUpdateBasicUser(username, accountType);
			break;
		case "bcsc":
			logger.info("SOAM: Account type bcsc found");
			username = (String)token.getOtherClaims().get("bcsc_did");
			if(username == null) {
				throw new SoamRuntimeException("No bcsc_did value was found in token");
			}
			break;
		case "idir":
			logger.info("SOAM: Account type idir found");
			username = (String)token.getOtherClaims().get("idir_guid");
			if(username == null) {
				throw new SoamRuntimeException("No idir_guid value was found in token");
			}
			break; 
		default:
			throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
		}
        
        if(context.getSession().users().getUserByUsername(username, realm) == null) {
            logger.infof("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, brokerContext.getIdpConfig().getAlias()); 

            UserModel federatedUser = session.users().addUser(realm, username);
            federatedUser.setEnabled(true);
            federatedUser.setSingleAttribute("first_name", brokerContext.getFirstName());
            federatedUser.setSingleAttribute("last_name", brokerContext.getLastName());
            federatedUser.setSingleAttribute("email_address", brokerContext.getEmail());
            
            if(accountType.equals("bceid")) {
	            federatedUser.setSingleAttribute("display_name", brokerContext.getFirstName() + " " + brokerContext.getLastName());
	            //federatedUser.setSingleAttribute("middle_names", "FIX WHEN CAP SERVICE IS IN");
            }
            
            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else {
        	logger.info("SOAM: Existing " + accountType + " user found with username: " + username);
        	UserModel existingUser = context.getSession().users().getUserByUsername(username, realm);
        	context.setUser(existingUser);
        	context.success();
        } 
    }

    protected void createOrUpdateBasicUser(String guid, String accountType) {
    	logger.info("SOAM: createOrUpdateBasicUser");
    	logger.info("SOAM: performing login for " + accountType + " user: " + guid);
    	
    	try {
			RestUtils.getInstance().performLogin("BASIC", guid, guid);
		} catch (Exception e) {
			logger.error("Exception occurred within SOAM while processing login" + e.getMessage());
			throw new SoamRuntimeException("Exception occurred within SOAM while processing login, check downstream logs for digital ID API service");
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
