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
			accountType = ((List<String>)brokerContext.getContextData().get("user.attributes.account_type")).get(0);
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
			servicesCard.setBirthDate(((List<String>)brokerContext.getContextData().get("user.attributes.birthdate")).get(0));
			servicesCard.setCity(((List<String>)brokerContext.getContextData().get("user.attributes.locality")).get(0));
			servicesCard.setCountry(((List<String>)brokerContext.getContextData().get("user.attributes.country")).get(0));
			servicesCard.setDid(((List<String>)brokerContext.getContextData().get("user.attributes.sub")).get(0));
			servicesCard.setEmail(((List<String>)brokerContext.getContextData().get("user.attributes.email")).get(0));
			servicesCard.setGender(((List<String>)brokerContext.getContextData().get("user.attributes.gender")).get(0));
			servicesCard.setGivenName(((List<String>)brokerContext.getContextData().get("user.attributes.given_name")).get(0));
			servicesCard.setGivenNames(((List<String>)brokerContext.getContextData().get("user.attributes.given_names")).get(0));
			servicesCard.setIdentityAssuranceLevel(((List<String>)brokerContext.getContextData().get("user.attributes.identity_assurance_level")).get(0));
			servicesCard.setPostalCode(((List<String>)brokerContext.getContextData().get("user.attributes.postal_code")).get(0));
			servicesCard.setProvince(((List<String>)brokerContext.getContextData().get("user.attributes.region")).get(0));
			servicesCard.setStreetAddress(((List<String>)brokerContext.getContextData().get("user.attributes.street_address")).get(0));
			servicesCard.setSurname(((List<String>)brokerContext.getContextData().get("user.attributes.family_name")).get(0));
			servicesCard.setUserDisplayName(((List<String>)brokerContext.getContextData().get("user.attributes.display_name")).get(0));
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
