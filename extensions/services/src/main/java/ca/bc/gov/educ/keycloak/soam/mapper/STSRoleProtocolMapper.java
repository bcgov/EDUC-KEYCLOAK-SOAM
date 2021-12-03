package ca.bc.gov.educ.keycloak.soam.mapper;

import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;
import ca.bc.gov.educ.keycloak.soam.utils.ExpiringConcurrentHashMap;
import ca.bc.gov.educ.keycloak.soam.utils.ExpiringConcurrentHashMapListener;
import org.jboss.logging.Logger;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.utils.RoleResolveUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * STS Role Protocol Mapper
 * Will be used to set STS specific roles
 *
 * @author Marco Villeneuve
 */
public class STSRoleProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {
  private static final List<ProviderConfigProperty> configProperties = new ArrayList();
  public static final String PROVIDER_ID = "oidc-sts-role-mapper";
  private static Logger logger = Logger.getLogger(STSRoleProtocolMapper.class);

  public STSRoleProtocolMapper() {
  }

  //Create hashmap with 30 second expiry.
  private ExpiringConcurrentHashMap<String, List<String>> loginDetailCache = new ExpiringConcurrentHashMap<>(30000, new ExpiringConcurrentHashMapListener<String, List<String>>() {

    @Override
    public void notifyOnAdd(String key, List<String> value) {
      logger.debug("Adding STS roles to SOAM cache, key: " + key);
    }

    @Override
    public void notifyOnRemoval(String key, List<String> value) {
      logger.debug("Removing STS roles from SOAM cache, key: " + key);
      logger.debug("Current cache size on this node: " + loginDetailCache.size());
    }
  });

  public List<ProviderConfigProperty> getConfigProperties() {
    return configProperties;
  }

  public String getId() {
    return "oidc-sts-role-mapper";
  }

  public String getDisplayType() {
    return "STS Role Mapper";
  }

  public String getDisplayCategory() {
    return "Token mapper";
  }

  public String getHelpText() {
    return "Get STS roles into the access token.";
  }

  public int getPriority() {
    return 20;
  }

  public AccessToken transformAccessToken(AccessToken token, ProtocolMapperModel mappingModel, KeycloakSession session, UserSessionModel userSession, ClientSessionContext clientSessionCtx) {
    String accountType = userSession.getUser().getFirstAttribute("account_type");
    logger.debug("STS Protocol Mapper - User Account Type is: " + accountType);

    if(accountType == null) {
      //This is a client credential call
    }else {
      String userGUID = userSession.getUser().getFirstAttribute("user_guid");
      logger.debug("User GUID is: " + userGUID);

      List<String> roles = fetchSTSRoles(userGUID);
      AccessToken.Access access;

      access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, true);
      for (String role : roles) {
        access.addRole(role);
        logger.debug("Role added: " + role);
      }
    }

    return token;
  }

  private List<String> fetchSTSRoles(String userGUID) {
    if (loginDetailCache.containsKey(userGUID)) {
      return loginDetailCache.get(userGUID);
    }
    logger.debug("SOAM Fetching STS roles for GUID " + userGUID);
    List<String> roles = RestUtils.getInstance().getSTSRoles(userGUID);
    loginDetailCache.put(userGUID, roles);

    return roles;
  }

  public static ProtocolMapperModel create(String name) {
    String mapperId = "oidc-sts-role-mapper";
    ProtocolMapperModel mapper = new ProtocolMapperModel();
    mapper.setName(name);
    mapper.setProtocolMapper(mapperId);
    mapper.setProtocol("openid-connect");
    return mapper;
  }

}
