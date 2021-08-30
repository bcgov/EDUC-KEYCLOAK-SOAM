package ca.bc.gov.educ.keycloak.soam.authenticator;

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.JsonWebToken;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SOAM First Time login authenticator
 * This class will handle the callouts to our API
 * 
 * @author Marco Villeneuve
 *
 */
public class SoamFirstTimeSAMLLoginAuthenticator extends AbstractIdpAuthenticator {

    private static Logger logger = Logger.getLogger(SoamFirstTimeSAMLLoginAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    } 
 
    @Override
    protected void authenticateImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
		logger.debug("SOAM: inside SAML authenticateImpl");
		KeycloakSession session = context.getSession();
		RealmModel realm = context.getRealm();

		if (context.getAuthenticationSession().getAuthNote(EXISTING_USER_INFO) != null) {
			context.attempted();
			return;
		}

		Map<String, Object> brokerClaims = brokerContext.getContextData();
		for(String s: brokerClaims.keySet()) {
			logger.info("Context Key: " + s + " Value: " + brokerClaims.get(s));
		}

		AssertionType assertion = (AssertionType)brokerContext.getContextData().get("SAML_ASSERTION");

		String username = null;
		String userGUID = null;
		String displayName = null;

		Set<AttributeStatementType> otherClaims = assertion.getAttributeStatements();
		for(AttributeStatementType s: otherClaims) {
			logger.info("SAML Assertion Claims:");
			for(AttributeStatementType.ASTChoiceType type: s.getAttributes()) {
				String name = type.getAttribute().getName();
				logger.info("SAML_ASSERTION Key: " + name + " Value: " + type.getAttribute().getAttributeValue());

				if(name.equalsIgnoreCase("useridentifier")){
					userGUID = (String)type.getAttribute().getAttributeValue().get(0);
				}else if(name.equalsIgnoreCase("user_name")){
					username = (String)type.getAttribute().getAttributeValue().get(0);
				}else if(name.equalsIgnoreCase("SMGOV_USERDISPLAYNAME")){
					displayName = (String)type.getAttribute().getAttributeValue().get(0);
				}
			}
		}

		String accountType = "bceid";

		switch (accountType) {
		case "bceid":
			logger.debug("SOAM: Account type bceid found");
			if(userGUID == null) {
				throw new SoamRuntimeException("No bceid_guid value was found in token");
			}
			createOrUpdateUser(userGUID, accountType, "BASIC");
			break;
		case "idir":
			logger.debug("SOAM: Account type idir found");
			if(userGUID == null) {
				throw new SoamRuntimeException("No idir_guid value was found in token");
			}
			break;
		default:
			throw new SoamRuntimeException("Account type is not bcsc, bceid or idir, check IDP mappers");
		}

        if(context.getSession().users().getUserByUsername(userGUID, realm) == null) {
            logger.debugf("No duplication detected. Creating account for user '%s' and linking with identity provider '%s' .",
					userGUID, brokerContext.getIdpConfig().getAlias());

            UserModel federatedUser = session.users().addUser(realm, userGUID);
            federatedUser.setEnabled(true);

            if(accountType.equals("bceid")) {
	           federatedUser.setSingleAttribute("display_name", displayName);
	           federatedUser.setSingleAttribute("bceid_userid", userGUID);
	           federatedUser.setLastName(username + "@bceid");
            }else if(accountType.equals("idir")) {
 	           federatedUser.setSingleAttribute("idir_username", userGUID);
 	           federatedUser.setSingleAttribute("display_name", displayName);
   	           //federatedUser.setFirstName(((String)otherClaims.get("given_name")));
  	           //federatedUser.setLastName(((String)otherClaims.get("family_name")));
            }

            for (Map.Entry<String, List<String>> attr : serializedCtx.getAttributes().entrySet()) {
                federatedUser.setAttribute(attr.getKey(), attr.getValue());
            }

            context.setUser(federatedUser);
            context.getAuthenticationSession().setAuthNote(BROKER_REGISTERED_NEW_USER, "true");
            context.success();
        } else {
        	logger.debug("SOAM: Existing " + accountType + " user found with username: " + userGUID);
        	UserModel existingUser = context.getSession().users().getUserByUsername(userGUID, realm);
        	context.setUser(existingUser);
        	context.success();
        }
    }

    protected void createOrUpdateUser(String guid, String accountType, String credType) {
    	logger.debug("SOAM: createOrUpdateUser");
    	logger.debug("SOAM: performing login for " + accountType + " user: " + guid);
    	
    	try {
			RestUtils.getInstance().performLogin(credType, guid, guid, null);
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
