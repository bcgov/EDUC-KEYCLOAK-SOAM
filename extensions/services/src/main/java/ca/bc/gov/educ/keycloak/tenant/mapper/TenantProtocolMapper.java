package ca.bc.gov.educ.keycloak.tenant.mapper;

import ca.bc.gov.educ.keycloak.common.utils.ExpiringConcurrentHashMap;
import ca.bc.gov.educ.keycloak.common.utils.ExpiringConcurrentHashMapListener;
import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.tenant.model.TenantAccessEntity;
import ca.bc.gov.educ.keycloak.tenant.rest.TenantRestUtils;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenant Protocol Mapper Will be used to set Tenant valid attribute
 *
 * @author Marco Villeneuve
 */
public class TenantProtocolMapper extends AbstractOIDCProtocolMapper
        implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

    private static Logger logger = Logger.getLogger(TenantProtocolMapper.class);
    private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

    static {
        // OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
        OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, TenantProtocolMapper.class);
    }

    //Create hashmap with 30 second expiry.
    private ExpiringConcurrentHashMap<String, TenantAccessEntity> loginDetailCache = new ExpiringConcurrentHashMap<>(30000, new ExpiringConcurrentHashMapListener<String, TenantAccessEntity>() {

        @Override
        public void notifyOnAdd(String key, TenantAccessEntity value) {
            logger.debug("Adding TenantAccessEntity to Tenant cache, key: " + key);
        }

        @Override
        public void notifyOnRemoval(String key, TenantAccessEntity value) {
            logger.debug("Removing TenantAccessEntity from Tenant cache, key: " + key);
            logger.debug("Current cache size on this node: " + loginDetailCache.size());
        }
    });

    public static final String PROVIDER_ID = "oidc-tenant-mapper";

    public List<ProviderConfigProperty> getConfigProperties() {
        return configProperties;
    }

    public String getId() {
        return PROVIDER_ID;
    }

    public String getDisplayType() {
        return "Tenant Protocol Mapper";
    }

    public String getDisplayCategory() {
        return TOKEN_MAPPER_CATEGORY;
    }

    public String getHelpText() {
        return "Map Tenant claims";
    }

    private TenantAccessEntity fetchTenantAccessEntity(String clientID, String tenantID) {
        if (loginDetailCache.containsKey(tenantID)) {
            return loginDetailCache.get(tenantID);
        }
        logger.debug("Tenant Access Fetching by Tenant ID: " + tenantID);
        TenantAccessEntity tenantAccessEntity = TenantRestUtils.getInstance().checkForValidTenant(clientID, tenantID);
        loginDetailCache.put(tenantID, tenantAccessEntity);

        return tenantAccessEntity;
    }

    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
        logger.debug("Tenant Mapper - setClaim method");

//        String clientID = clientSessionCtx.getClientSession().getClient().getClientId();
//
//        logger.debug("Client ID is: " + clientID);

        Map<String, List<String>> attributes = userSession.getUser().getAttributes();
        for (String s : attributes.keySet()) {
            logger.debug("User Key: " + s);
            for (String val : attributes.get(s)) {
                logger.debug("Value: " + val);
            }
        }

        Map<String, Object> otherClaims = token.getOtherClaims();
        for (String s : otherClaims.keySet()) {
            logger.debug("Protocol Mapper ID Token Key: " + s + " Value: " + otherClaims.get(s));
        }

        String tenantID = userSession.getUser().getFirstAttribute("tenant_id");
        logger.debug("Tenant ID is: " + tenantID);

	}

    @Override
    protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession, KeycloakSession keycloakSession, ClientSessionContext clientSessionCtx) {
        logger.debug("Tenant Mapper - setClaim large method");

        String clientID = clientSessionCtx.getClientSession().getClient().getClientId();

        logger.debug("Client ID is: " + clientID);

        Map<String, List<String>> attributes = userSession.getUser().getAttributes();
        for (String s : attributes.keySet()) {
            logger.debug("User Key: " + s);
            for (String val : attributes.get(s)) {
                logger.debug("Value: " + val);
            }
        }

        Map<String, Object> otherClaims = token.getOtherClaims();
        for (String s : otherClaims.keySet()) {
            logger.debug("Protocol Mapper ID Token Key: " + s + " Value: " + otherClaims.get(s));
        }

        String tenantID = userSession.getUser().getFirstAttribute("tenant_id");
        logger.debug("Tenant ID is: " + tenantID);
//        TenantAccessEntity entity = fetchTenantAccessEntity(clientID, tenantID);
//
//        token.getOtherClaims().put("isValidTenant", entity.getIsValid());
    }

    public static ProtocolMapperModel create(String name, String tokenClaimName, boolean consentRequired,
                                             String consentText, boolean accessToken, boolean idToken) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(PROVIDER_ID);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
//		mapper.setConsentRequired(consentRequired);
//		mapper.setConsentText(consentText);
        Map<String, String> config = new HashMap<String, String>();
        if (accessToken)
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        if (idToken)
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ID_TOKEN, "true");
        mapper.setConfig(config);

        return mapper;
    }

}
