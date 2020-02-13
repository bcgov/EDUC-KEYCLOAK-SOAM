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
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
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
        
        Map<String, Object> otherClaims = token.getOtherClaims();
		for(String s: otherClaims.keySet()) {
    		logger.info("Key: " + s + " Value: " + otherClaims.get(s));
		}
        
		String accountType = (String)otherClaims.get("account_type");
		
		if(accountType == null) {
			throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
		}
		
		String username = null;
		
		switch (accountType) {
		case "bceid":
			logger.info("SOAM: Account type bceid found");
			username = (String)otherClaims.get("bceid_guid");
			if(username == null) {
				throw new SoamRuntimeException("No bceid_guid value was found in token");
			}
			createOrUpdateUser(username, accountType, "BASIC", null);
			break;
		case "bcsc":
			logger.info("SOAM: Account type bcsc found");
			username = (String)otherClaims.get("bcsc_did");
			if(username == null) {
				throw new SoamRuntimeException("No bcsc_did value was found in token");
			}
				
			SoamServicesCard servicesCard = new SoamServicesCard();
			servicesCard.setBirthDate((String)otherClaims.get("birthdate"));
			servicesCard.setCity((String)otherClaims.get("city"));
			servicesCard.setCountry((String)otherClaims.get("country"));
			servicesCard.setDid((String)otherClaims.get("bcsc_did"));
			servicesCard.setEmail((String)otherClaims.get("email"));
			servicesCard.setGender((String)otherClaims.get("gender"));
			servicesCard.setGivenName((String)otherClaims.get("given_name"));
			servicesCard.setGivenNames((String)otherClaims.get("given_names"));
			servicesCard.setIdentityAssuranceLevel((String)otherClaims.get("identity_assurance_level"));
			servicesCard.setPostalCode((String)otherClaims.get("postal_code"));
			servicesCard.setProvince((String)otherClaims.get("province"));
			servicesCard.setStreetAddress((String)otherClaims.get("streetAddress"));
			servicesCard.setSurname((String)otherClaims.get("family_name"));
			servicesCard.setUserDisplayName((String)otherClaims.get("name"));
			createOrUpdateUser(username, accountType, "BCSC", servicesCard);
			break;
		case "idir": 
			logger.info("SOAM: Account type idir found");
			username = (String)otherClaims.get("idir_guid");
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
            
            if(accountType.equals("bceid")) {
	           federatedUser.setSingleAttribute("display_name", (String)otherClaims.get("display_name"));
            }else if(accountType.equals("idir")) {
 	           federatedUser.setSingleAttribute("idir_username", ((String)otherClaims.get("preferred_username")).replaceFirst("@idir", "").toUpperCase());
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

    protected void createOrUpdateUser(String guid, String accountType, String credType, SoamServicesCard servicesCard) {
    	logger.info("SOAM: createOrUpdateUser");
    	logger.info("SOAM: performing login for " + accountType + " user: " + guid);
    	
    	try {
			RestUtils.getInstance().performLogin(credType, guid, guid, servicesCard);
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
