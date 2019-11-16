package com.github.bcgov.keycloak.soam;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.sessions.AuthenticationSessionModel;


public class SoamPostLoginAuthenticator extends AbstractIdpAuthenticator implements IdentityProvider{

    private static Logger logger = Logger.getLogger(SoamPostLoginAuthenticator.class);


    @Override
    protected void actionImpl(AuthenticationFlowContext context, SerializedBrokeredIdentityContext serializedCtx, BrokeredIdentityContext brokerContext) {
    	logger.info("SOAM Post: inside actionImpl");
        
    }
    
    @Override
    public void authenticate(AuthenticationFlowContext context) {
    	logger.info("SOAM Post: inside authenticate");
        
        logger.info("context.getUser(): " + context.getUser());
        logger.info("context.getSession(): " + context.getSession());
        
        if(context.getUser()!=null) {
        	logger.info("User GUID: " + context.getUser().getFirstAttribute("GUID"));
        }
        
        
       
        
//        JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
//        
//        for(String s: token.getOtherClaims().keySet()) {
//        	logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
//        }
        
        UserModel existingUser = context.getSession().users().getUserByUsername(context.getUser().getUsername(), context.getRealm());
        
        
        context.setUser(existingUser);
        context.success();
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

	@Override
	public void preprocessFederatedIdentity(KeycloakSession session, RealmModel realm,
			BrokeredIdentityContext context) {
	
	}

	@Override
	public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
		logger.info("SOAM Post: inside authenticationFinished");
		logger.info("Token is: " + (JsonWebToken)context.getContextData().get("VALIDATED_ID_TOKEN"));
		
	}

	@Override
	public void importNewUser(KeycloakSession session, RealmModel realm, UserModel user,
			BrokeredIdentityContext context) {
	}

	@Override
	public void updateBrokeredUser(KeycloakSession session, RealmModel realm, UserModel user,
			BrokeredIdentityContext context) {
	}

	@Override
	public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
		return null;
	}

	@Override
	public Response performLogin(AuthenticationRequest request) {
		return null;
	}

	@Override
	public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
		return null;
	}

	@Override
	public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo,
			RealmModel realm) {
	}

	@Override
	public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession,
			UriInfo uriInfo, RealmModel realm) {
		return null;
	}

	@Override
	public Response export(UriInfo uriInfo, RealmModel realm, String format) {
		return null;
	}

	@Override
	public IdentityProviderDataMarshaller getMarshaller() {
		return null;
	}

}
