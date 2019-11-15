package com.github.bcgov.keycloak.soam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AbstractOIDCProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAccessTokenMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.OIDCIDTokenMapper;
import org.keycloak.protocol.oidc.mappers.UserInfoTokenMapper;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;


/**
 * SOAM Protocol Mapper
 * Will be used to set Education specific claims
 * for our client applications
 * 
 * @author Marco Villeneuve
 *
 */
public class SoamProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

	private static Logger logger = Logger.getLogger(SoamProtocolMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, SoamProtocolMapper.class);
    }

    public static final String PROVIDER_ID = "oidc-soam-mapper";


    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Soam Protocol Mapper";
    }

    @Override
    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    @Override
    public String getHelpText() { 
        return "Map SOAM claims";
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
    	//Inject callout from here, using the GUID as our key
    	logger.info("Protocol Mapper - User GUID is: " + token.getOtherClaims().get("bceid_guid"));
    	token.getOtherClaims().put("given_name", "Working Test Claim");
    }

    public static ProtocolMapperModel create(String name,
                                      String tokenClaimName,
                                      boolean consentRequired, String consentText,
                                      boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConsentRequired(consentRequired);
        mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        config.put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, tokenClaimName);
        if (accessToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken) config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);
        
        return mapper;
    }


}
