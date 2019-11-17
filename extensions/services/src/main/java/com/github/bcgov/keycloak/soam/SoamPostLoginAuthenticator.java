package com.github.bcgov.keycloak.soam;

import java.io.ByteArrayInputStream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.apache.commons.codec.binary.Base64;
import org.jboss.logging.Logger;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.authenticators.broker.AbstractIdpAuthenticator;
import org.keycloak.authentication.authenticators.broker.util.PostBrokerLoginConstants;
import org.keycloak.authentication.authenticators.broker.util.SerializedBrokeredIdentityContext;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.JsonWebToken;

import com.google.gson.Gson;


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
			
			logger.info("context.getUser(): " + context.getUser());
			logger.info("context.getSession(): " + context.getSession());
			logger.info("User GUID: " + context.getUser().getFirstAttribute("GUID"));
			
			
			String brokeredIdentityContext = context.getAuthenticationSession().getAuthNote(PostBrokerLoginConstants.PBL_BROKERED_IDENTITY_CONTEXT);
			logger.info("brokeredIdentityContext: " + brokeredIdentityContext);
			Gson gson = new Gson();

			SerializedBrokeredIdentityContext serializedCtx = gson.fromJson(brokeredIdentityContext, SerializedBrokeredIdentityContext.class);
			BrokeredIdentityContext brokerContext = serializedCtx.deserialize(context.getSession(), context.getAuthenticationSession());
			JsonWebToken token = (JsonWebToken)brokerContext.getContextData().get("VALIDATED_ID_TOKEN");
			
			for(String s: token.getOtherClaims().keySet()) {
        		logger.info("Key: " + s + " Value: " + token.getOtherClaims().get(s));
			}
			
			UserModel existingUser = context.getSession().users().getUserByUsername(context.getUser().getUsername(), context.getRealm());
			
			context.setUser(existingUser);
			context.success();
		} finally {
			if(reader != null) {
				reader.close();
			}
		}
    }
    
    private void testDecodeJWT(String jwtToken){
    	logger.info("------------ Decode JWT ------------");
        String[] split_string = jwtToken.split("\\.");
        String base64EncodedHeader = split_string[0];
        String base64EncodedBody = split_string[1];
        String base64EncodedSignature = split_string[2];

        logger.info("~~~~~~~~~ JWT Header ~~~~~~~");
        Base64 base64Url = new Base64(true);
        String header = new String(base64Url.decode(base64EncodedHeader));
        System.out.println("JWT Header : " + header);


        logger.info("~~~~~~~~~ JWT Body ~~~~~~~");
        String body = new String(base64Url.decode(base64EncodedBody));
        logger.info("JWT Body : "+body);        
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
