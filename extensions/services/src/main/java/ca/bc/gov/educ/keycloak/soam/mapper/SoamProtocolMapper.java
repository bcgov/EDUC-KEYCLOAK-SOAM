package ca.bc.gov.educ.keycloak.soam.mapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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

import ca.bc.gov.educ.keycloak.soam.exception.SoamRuntimeException;
import ca.bc.gov.educ.keycloak.soam.model.SoamLoginEntity;
import ca.bc.gov.educ.keycloak.soam.model.SoamServicesCard;
import ca.bc.gov.educ.keycloak.soam.model.SoamStudent;
import ca.bc.gov.educ.keycloak.soam.rest.RestUtils;
import ca.bc.gov.educ.keycloak.soam.utils.ExpiringConcurrentHashMap;
import ca.bc.gov.educ.keycloak.soam.utils.ExpiringConcurrentHashMapListener;

/**
 * SOAM Protocol Mapper Will be used to set Education specific claims for our
 * client applications
 * 
 * @author Marco Villeneuve
 *
 */
public class SoamProtocolMapper extends AbstractOIDCProtocolMapper
		implements OIDCAccessTokenMapper, OIDCIDTokenMapper, UserInfoTokenMapper {

	private static Logger logger = Logger.getLogger(SoamProtocolMapper.class);
	private static final List<ProviderConfigProperty> configProperties = new ArrayList<ProviderConfigProperty>();

	static {
		// OIDCAttributeMapperHelper.addTokenClaimNameConfig(configProperties);
		OIDCAttributeMapperHelper.addIncludeInTokensConfig(configProperties, SoamProtocolMapper.class);
	}

	//Create hashmap with 30 second expiry. 
	private ExpiringConcurrentHashMap<String, SoamLoginEntity> loginDetailCache = new ExpiringConcurrentHashMap<>(30000, new ExpiringConcurrentHashMapListener<String, SoamLoginEntity>() {

		@Override
		public void notifyOnAdd(String key, SoamLoginEntity value) {
			logger.debug("Adding SoamLoginEntity to SOAM cache, key: " + key);
		}

		@Override
		public void notifyOnRemoval(String key, SoamLoginEntity value) {
			logger.debug("Removing SoamLoginEntity from SOAM cache, key: " + key);
			logger.debug("Current cache size on this node: " + loginDetailCache.size());
		}
	});
	
	public static final String PROVIDER_ID = "oidc-soam-mapper";

	public List<ProviderConfigProperty> getConfigProperties() {
		return configProperties;
	}

	public String getId() {
		return PROVIDER_ID;
	}

	public String getDisplayType() {
		return "Soam Protocol Mapper";
	}

	public String getDisplayCategory() {
		return TOKEN_MAPPER_CATEGORY;
	}

	public String getHelpText() {
		return "Map SOAM claims";
	}
	
	private SoamLoginEntity fetchSoamLoginEntity(String type, String userGUID) {
		if(loginDetailCache.containsKey(userGUID)) {
			return loginDetailCache.get(userGUID);
		}
		logger.debug("SOAM Fetching " + type + " Claims");
		SoamLoginEntity soamLoginEntity = RestUtils.getInstance().getSoamLoginEntity(type, userGUID);
		loginDetailCache.put(userGUID, soamLoginEntity);
		
		return soamLoginEntity;
	}
	
	protected void setClaim(IDToken token, ProtocolMapperModel mappingModel, UserSessionModel userSession) {
		String accountType = userSession.getUser().getFirstAttribute("account_type");
		
		logger.debug("Protocol Mapper - User Account Type is: " + accountType);
		
		if(accountType == null) {
			//This is a client credential call
		}else {
			String userGUID = userSession.getUser().getUsername();
			
			if(accountType.equals("bceid")){
				SoamLoginEntity soamLoginEntity = fetchSoamLoginEntity("BASIC", userGUID);
				token.getOtherClaims().put("accountType", "BCEID");
				
				setStandardSoamLoginClaims(token, soamLoginEntity, userSession);
			}else if(accountType.equals("bcsc")){
				SoamLoginEntity soamLoginEntity = fetchSoamLoginEntity("BCSC", userGUID);
				token.getOtherClaims().put("accountType", "BCSC");
				
				setStandardSoamLoginClaims(token, soamLoginEntity, userSession);	
			}
		}
	}

	private void setStandardSoamLoginClaims(IDToken token, SoamLoginEntity soamLoginEntity, UserSessionModel userSession) {
		//In this case we have a services card
		if(soamLoginEntity.getServiceCard() != null) {
			populateServicesCardClaims(token, soamLoginEntity);
		}
		
		else if(soamLoginEntity.getStudent() != null) {
			populateStudentClaims(token, soamLoginEntity);
		}

		//In this case we have a digital identity; someone that has logged in but does not have an associated student record
		else if(soamLoginEntity.getDigitalIdentityID() != null) {
			populateDigitalIDClaims(token, soamLoginEntity, userSession);
		}
		
		//This is an exception since we have no data at all
		else {
			throw new SoamRuntimeException("No student or digital ID data found in SoamLoginEntity");
		}
	}
	
	private void populateDigitalIDClaims(IDToken token, SoamLoginEntity soamLoginEntity, UserSessionModel userSession) {
		Map<String, Object> otherClaims = token.getOtherClaims();
		otherClaims.put("digitalIdentityID", soamLoginEntity.getDigitalIdentityID());
		otherClaims.put("displayName", userSession.getUser().getFirstAttribute("display_name"));
	}
	
	private void populateStudentClaims(IDToken token, SoamLoginEntity soamLoginEntity) {
		SoamStudent student = soamLoginEntity.getStudent();
		Map<String, Object> otherClaims = token.getOtherClaims();
		otherClaims.put("digitalIdentityID", soamLoginEntity.getDigitalIdentityID());
		otherClaims.put("studentID", student.getStudentID());
		otherClaims.put("legalFirstName", student.getLegalFirstName());
		otherClaims.put("legalMiddleNames", student.getLegalMiddleNames());
		otherClaims.put("legalLastName", student.getLegalLastName());
		otherClaims.put("dob", student.getDob());
		otherClaims.put("pen", student.getPen());
		otherClaims.put("sexCode", student.getSexCode());
		otherClaims.put("dataSourceCode", student.getDataSourceCode());
		otherClaims.put("usualFirstName", student.getUsualFirstName());
		otherClaims.put("usualMiddleNames", student.getUsualMiddleNames());
		otherClaims.put("usualLastName", student.getUsualLastName());
		otherClaims.put("email", student.getEmail());
		otherClaims.put("deceasedDate", student.getDeceasedDate());
		otherClaims.put("createUser", student.getCreateUser());
		otherClaims.put("createDate", student.getCreateDate());
		otherClaims.put("updateUser", student.getUpdateUser());
		otherClaims.put("updateDate", student.getUpdateDate());
		if(StringUtils.isNotEmpty(student.getLegalFirstName())){
			otherClaims.put("displayName", student.getLegalFirstName() + " " + soamLoginEntity.getStudent().getLegalLastName());
		}else{
			otherClaims.put("displayName", soamLoginEntity.getStudent().getLegalLastName());
		}
	}

	private void populateServicesCardClaims(IDToken token, SoamLoginEntity soamLoginEntity) {
		SoamServicesCard servicesCard = soamLoginEntity.getServiceCard();
		Map<String, Object> otherClaims = token.getOtherClaims();
		otherClaims.put("digitalIdentityID", soamLoginEntity.getDigitalIdentityID());
		otherClaims.put("birthDate", servicesCard.getBirthDate());
		otherClaims.put("city", servicesCard.getCity());
		otherClaims.put("country", servicesCard.getCountry());
		otherClaims.put("createDate", servicesCard.getCreateDate());
		otherClaims.put("createUser", servicesCard.getCreateUser());
		otherClaims.put("did", servicesCard.getDid());
		otherClaims.put("email", servicesCard.getEmail());
		otherClaims.put("gender", servicesCard.getGender());
		otherClaims.put("givenName", servicesCard.getGivenName());
		otherClaims.put("identityAssuranceLevel", servicesCard.getIdentityAssuranceLevel());
		otherClaims.put("givenNames", servicesCard.getGivenNames());
		otherClaims.put("postalCode", servicesCard.getPostalCode());
		otherClaims.put("province", servicesCard.getProvince());
		otherClaims.put("servicesCardInfoID", servicesCard.getServicesCardInfoID());
		otherClaims.put("streetAddress", servicesCard.getStreetAddress());
		otherClaims.put("surname", servicesCard.getSurname());
		otherClaims.put("updateDate", servicesCard.getUpdateDate());
		otherClaims.put("updateUser", servicesCard.getUpdateUser());
		otherClaims.put("displayName", servicesCard.getUserDisplayName());
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
