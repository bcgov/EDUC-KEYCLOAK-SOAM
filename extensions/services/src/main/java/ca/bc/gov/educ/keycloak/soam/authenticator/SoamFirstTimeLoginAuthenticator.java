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
    	logger.debug("SOAM: inside authenticateImpl");
        KeycloakSession session = context.getSession(); 
        RealmModel realm = context.getRealm();

        if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
            context.attempted();
            return;
        }

		Map<String, Object> brokerClaims = brokerContext.getContextData();
		for(String s: brokerClaims.keySet()) {
			logger.debug("Context Key: " + s + " Value: " + brokerClaims.get(s));
		}

        JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
        
        Map<String, Object> otherClaims = token.getOtherClaims();
		for(String s: otherClaims.keySet()) {
    		logger.debug("VALIDATED_ID_TOKEN Key: " + s + " Value: " + otherClaims.get(s));
		}

		String accountType = (String)otherClaims.get("account_type");

		//This is added for BCSC - direct IDP
		if(accountType == null){
			accountType = (String)brokerContext.getContextData().get("user.attributes.account_type");
		}
		
		if(accountType == null) {
			throw new SoamRuntimeException("Account type is null; account type should always be available, check the IDP mappers for the hardcoded attribute");
		}
		
		String username = null;
		
		switch (accountType) {
		case "bceid":
			logger.debug("SOAM: Account type bceid found");
			username = (String)otherClaims.get("bceid_guid");
			if(username == null) {
				throw new SoamRuntimeException("No bceid_guid value was found in token");
			}
			createOrUpdateUser(username, accountType, "BASIC", null);
			break;
		case "bcsc":
			logger.debug("SOAM: Account type bcsc found");
			username = (String)otherClaims.get("bcsc_did");
			if(username == null) {
				throw new SoamRuntimeException("No bcsc_did value was found in token");
			}
				
			SoamServicesCard servicesCard = new SoamServicesCard();
			servicesCard.setBirthDate((String)brokerContext.getContextData().get("birthdate"));
			servicesCard.setCity((String)brokerContext.getContextData().get("locality"));
			servicesCard.setCountry((String)brokerContext.getContextData().get("country"));
			servicesCard.setDid((String)brokerContext.getContextData().get("sub"));
			servicesCard.setEmail((String)brokerContext.getContextData().get("email"));
			servicesCard.setGender((String)brokerContext.getContextData().get("gender"));
			servicesCard.setGivenName((String)brokerContext.getContextData().get("given_name"));
			servicesCard.setGivenNames((String)brokerContext.getContextData().get("given_names"));
			servicesCard.setIdentityAssuranceLevel((String)brokerContext.getContextData().get("identity_assurance_level"));
			servicesCard.setPostalCode((String)brokerContext.getContextData().get("postal_code"));
			servicesCard.setProvince((String)brokerContext.getContextData().get("region"));
			servicesCard.setStreetAddress((String)brokerContext.getContextData().get("street_address"));
			servicesCard.setSurname((String)brokerContext.getContextData().get("family_name"));
			servicesCard.setUserDisplayName((String)brokerContext.getContextData().get("display_name"));
			createOrUpdateUser(username, accountType, "BCSC", servicesCard);
			break; 
		case "idir": 
			logger.debug("SOAM: Account type idir found");
			username = (String)otherClaims.get("idir_guid");
			if(username == null) {
				throw new SoamRuntimeException("No idir_guid value was found in token");
			}
			break; 
		default:
			throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
		}
        
        if(context.getSession().users().getUserByUsername(username, realm) == null) {
            logger.debugf("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
                    username, brokerContext.getIdpConfig().getAlias()); 

            UserModel federatedUser = session.users().addUser(realm, username);
            federatedUser.setEnabled(true);
            
            if(accountType.equals("bceid")) {
	           federatedUser.setSingleAttribute("display_name", (String)otherClaims.get("display_name"));
            }else if(accountType.equals("idir")) {
 	           federatedUser.setSingleAttribute("idir_username", ((String)otherClaims.get("preferred_username")).replaceFirst("@idir", "").toUpperCase());
 	           federatedUser.setSingleAttribute("display_name", ((String)otherClaims.get("name")));
   	           federatedUser.setFirstName(((String)otherClaims.get("given_name"))); 
  	           federatedUser.setLastName(((String)otherClaims.get("family_name"))); 
            }
            
            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else {
        	logger.debug("SOAM: Existing " + accountType + " user found with username: " + username);
        	UserModel existingUser = context.getSession().users().getUserByUsername(username, realm);
        	context.setUser(existingUser);
        	context.success();
        } 
    }

    protected void createOrUpdateUser(String guid, String accountType, String credType, SoamServicesCard servicesCard) {
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
    public boolean requiresUser() {
        return false;
    }

    @Override
    public boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
        return true;
    }

}
