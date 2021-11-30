package ca.bc.gov.educ.keycloak.soam.mapper;

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
 * STS Role Protocol Mapper Will be used to set STS specific roles
 *
 * @author Marco Villeneuve
 */
public class STSRoleProtocolMapper extends AbstractOIDCProtocolMapper implements OIDCAccessTokenMapper {
  private static final List<ProviderConfigProperty> configProperties = new ArrayList();
  public static final String PROVIDER_ID = "oidc-sts-role-mapper";

  public STSRoleProtocolMapper() {
  }

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
    String role = "TESTERMARCO";
    AccessToken.Access access;

    access = RoleResolveUtil.getResolvedRealmRoles(session, clientSessionCtx, true);
    access.addRole(role);

    return token;
  }

  public static ProtocolMapperModel create(String name, String role) {
    String mapperId = "oidc-sts-role-mapper";
    ProtocolMapperModel mapper = new ProtocolMapperModel();
    mapper.setName(name);
    mapper.setProtocolMapper(mapperId);
    mapper.setProtocol("openid-connect");
    Map<String, String> config = new HashMap();
    config.put("role", role);
    mapper.setConfig(config);
    return mapper;
  }

  static {
    ProviderConfigProperty property = new ProviderConfigProperty();
    property.setName("role");
    property.setLabel("Role");
    property.setHelpText("Role you want added to the token.  Click 'Select Role' button to browse roles, or just type it in the textbox.  To specify an application role the syntax is appname.approle, i.e. myapp.myrole");
    property.setType("Role");
    configProperties.add(property);
  }

}
