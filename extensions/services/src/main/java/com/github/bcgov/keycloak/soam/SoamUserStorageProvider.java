package com.github.bcgov.keycloak.soam;

import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;

public class SoamUserStorageProvider implements UserStorageProvider,UserLookupProvider, UserQueryProvider {

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
		return 0;
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm) {
		logger.info("SOAM: Inside getUsers 1");
		return null;
	}

	@Override
	public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
		logger.info("SOAM: Inside getUsers 2");
		return null;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm) {
		logger.info("SOAM: Inside searchForUser 1");
		return null;
	}

	@Override
	public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
		logger.info("SOAM: Inside searchForUser 2");
		return null;
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
		logger.info("SOAM: Inside searchForUser 3");
		return null;
	}

	@Override
	public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult,
			int maxResults) {
		logger.info("SOAM: Inside searchForUser 4");
		logger.info("Params for search 4: ");
		for(String s: params.keySet()) {
			logger.info("Key: " + s + " Value: " + params.get(s) + "\n");	
		}
		return null;
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
		logger.info("SOAM: Inside getGroupMembers");
		return null;
	}

	@Override
	public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
		logger.info("SOAM: Inside getGroupMembers 2");
		return null;
	}

	@Override
	public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
		logger.info("SOAM: Inside searchForUserByUserAttribute");
		return null;
	}

	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		logger.info("SOAM: Inside getUserById");
		return null;
	}

	@Override
	public UserModel getUserByUsername(String username, RealmModel realm) {
		logger.info("SOAM: Inside getUserByUsername");
		return null;
	}

	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		logger.info("SOAM: Inside getUserByEmail");
		return null;
	}

}
