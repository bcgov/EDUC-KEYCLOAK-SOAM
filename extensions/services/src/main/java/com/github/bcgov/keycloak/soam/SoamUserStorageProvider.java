package com.github.bcgov.keycloak.soam;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

import com.github.bcgov.keycloak.soam.model.SoamUserModel;

public class SoamUserStorageProvider implements UserStorageProvider,UserLookupProvider, UserQueryProvider, UserRegistrationProvider {

	private static Logger logger = Logger.getLogger(SoamUserStorageProvider.class);
	
	public SoamUserStorageProvider() {
		logger.info("SOAM: Inside SoamUserStorageProvider");
	}

	@Override
	public void close() {
		logger.info("SOAM: Inside close");
	}

	@Override
	public int getUsersCount(RealmModel realm) {
		logger.info("SOAM: Inside getUsersCount");
		return 1;
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm) {
		logger.info("SOAM: Inside getUsers 1");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
		logger.info("SOAM: Inside getUsers 2");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm) {
		logger.info("SOAM: Inside searchForUser 1");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
		logger.info("SOAM: Inside searchForUser 2");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
		logger.info("SOAM: Inside searchForUser 3");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
			int maxResults) {
		logger.info("SOAM: Inside searchForUser 4");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
		logger.info("SOAM: Inside getGroupMembers");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
		logger.info("SOAM: Inside getGroupMembers 2");
		List<UserModel> list = new ArrayList<UserModel>();
		list.add(getTestUserModel());
		return list;
	}

	@Override
	public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
		logger.info("SOAM: Inside searchForUserByUserAttribute");
		//List<UserModel> list = new ArrayList<UserModel>();
		//list.add(getTestUserModel());
		return null;
	}

	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		logger.info("SOAM: Inside getUserById");
		return getTestUserModel();
	}

	@Override
	public UserModel getUserByUsername(String username, RealmModel realm) {
		logger.info("SOAM: Inside getUserByUsername");
		return getTestUserModel();
	}

	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		logger.info("SOAM: Inside getUserByEmail");
		return getTestUserModel();
	}
	
	private UserModel getTestUserModel() {
		SoamUserModel model = new SoamUserModel();
		model.setSingleAttribute("MARCOWASHERE", "THEBOYSINTHEHOUSE");
		model.setEmail("asdfasdf@someplace.com");
		model.setFirstName("Eric");
		model.setLastName("Sermon");
		model.setUsername("edub");
		Date date = new Date();
		model.setCreatedTimestamp(date.getTime());
		model.setFederationLink("123456GUID");
		model.setEnabled(true);
		model.setEmailVerified(true);
		return model;
	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		logger.info("SOAM: Inside addUser");
		return getTestUserModel();
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		logger.info("SOAM: Inside removeUser");
		return true;
	}

}
