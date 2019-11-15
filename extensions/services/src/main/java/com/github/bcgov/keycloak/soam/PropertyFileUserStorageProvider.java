package com.github.bcgov.keycloak.soam;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.UserModelDelegate;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;

public class PropertyFileUserStorageProvider implements UserStorageProvider, UserLookupProvider,
		UserRegistrationProvider, CredentialInputValidator, CredentialInputUpdater, UserQueryProvider {
	protected KeycloakSession session;
	public static final String UNSET_PASSWORD = "#$!-UNSET-PASSWORD";
	protected HashMap<String, String> properties;
	protected ComponentModel model;
	// map of loaded users in this transaction
	protected Map<String, UserModel> loadedUsers = new HashMap<>();
    private static final Set<String> disableableTypes = new HashSet<>();
    private static Logger logger = Logger.getLogger(PropertyFileUserStorageProvider.class);
    
    static {
        disableableTypes.add(CredentialModel.PASSWORD);
    }


	public PropertyFileUserStorageProvider(KeycloakSession session, ComponentModel model, HashMap<String, String> properties) {
		this.session = session;
		this.model = model;
		this.properties = properties;
	}

	@Override
	public UserModel getUserByUsername(String username, RealmModel realm) {
		logger.info("SOAM User Storage: inside getUserByUsername");
		UserModel adapter = loadedUsers.get(username);
		if (adapter == null) {
			String password = properties.get(username);
			if (password != null) {
				adapter = createAdapter(realm, username);
				loadedUsers.put(username, adapter);
			}
		}
		return adapter;
	}

    protected UserModel createAdapter(RealmModel realm, String username) {
    	logger.info("SOAM User Storage: inside createAdapter");
        UserModel local = session.userLocalStorage().getUserByUsername(username, realm);
        if (local == null) {
            local = session.userLocalStorage().addUser(realm, username);
            local.setFederationLink(model.getId());
        }
        return new UserModelDelegate(local) {
            @Override
            public void setUsername(String username) {
                String pw = (String)properties.remove(username);
                if (pw != null) {
                    properties.put(username, pw);
                    save();
                }
                super.setUsername(username);
            }
        };
    }
	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		logger.info("SOAM User Storage: inside getUserById");
		StorageId storageId = new StorageId(id);
		String username = storageId.getExternalId();
		return getUserByUsername(username, realm);
	}

	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		logger.info("SOAM User Storage: inside getUserByEmail");
		return null;
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		logger.info("SOAM User Storage: inside isConfiguredFor");
		String password = properties.get(user.getUsername());
		return credentialType.equals(CredentialModel.PASSWORD) && password != null;
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		logger.info("SOAM User Storage: inside supportsCredentialType");
		return credentialType.equals(CredentialModel.PASSWORD);
	}

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    	logger.info("SOAM User Storage: inside isValid");
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        String password = properties.get(user.getUsername());
        if (password == null || UNSET_PASSWORD.equals(password)) return false;
        return password.equals(cred.getValue());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
    	logger.info("SOAM User Storage: inside updateCredential");
        if (!(input instanceof UserCredentialModel)) return false;
        if (!input.getType().equals(CredentialModel.PASSWORD)) return false;
        UserCredentialModel cred = (UserCredentialModel)input;
        synchronized (properties) {
            properties.put(user.getUsername(), cred.getValue());
            save();
        }
        return true;
    }

    @Override
    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {
    	logger.info("SOAM User Storage: inside disableCredentialType");
        if (!credentialType.equals(CredentialModel.PASSWORD)) return;
        synchronized (properties) {
            properties.put(user.getUsername(), UNSET_PASSWORD);
            save();
        }

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
    	logger.info("SOAM User Storage: inside getDisableableCredentialTypes");
        return disableableTypes;
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		logger.info("SOAM User Storage: inside addUser");
		synchronized (properties) {
			properties.put(username, UNSET_PASSWORD);
			save();
		}
		return createAdapter(realm, username);
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
		logger.info("SOAM User Storage: inside removeUser");
		synchronized (properties) {
			if (properties.remove(user.getUsername()) == null)
				return false;
			save();
			return true;
		}
	}

	public void save() {
//		URL url = getClass().getResource("/opt/eap/users.properties");
//		//String path = model.getConfig().getFirst("path");
//		//path = EnvUtil.replace(path);
//		try {
//			FileOutputStream fos = new FileOutputStream(url.toURI().getPath());
//			properties.store(fos, "");
//			fos.close();
//		} catch (IOException | URISyntaxException e) {
//			throw new RuntimeException(e);
//		}
	}
	
    @Override
    public int getUsersCount(RealmModel realm) {
    	logger.info("SOAM User Storage: inside getUsersCount");
        return properties.size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
    	logger.info("SOAM User Storage: inside getUsers");
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
    	logger.info("SOAM User Storage: inside getUsers2");
        List<UserModel> users = new LinkedList<>();
        int i = 0;
        for (Object obj : properties.keySet()) {
            if (i++ < firstResult) continue;
            String username = (String)obj;
            UserModel user = getUserByUsername(username, realm);
            users.add(user);
            if (users.size() >= maxResults) break;
        }
        return users;
    }
    
    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm) {
    	logger.info("SOAM User Storage: inside searchForUser4");
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
    	logger.info("SOAM User Storage: inside searchForUser");
        List<UserModel> users = new LinkedList<>();
        int i = 0;
        for (Object obj : properties.keySet()) {
            String username = (String)obj;
            if (!username.contains(search)) continue;
            if (i++ < firstResult) continue;
            UserModel user = getUserByUsername(username, realm);
            users.add(user);
            if (users.size() >= maxResults) break;
        }
        return users;
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm) {
    	logger.info("SOAM User Storage: inside searchForUser2");
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
    	logger.info("SOAM User Storage: inside searchForUser3");
        // only support searching by username
        String usernameSearchString = params.get("username");
        if (usernameSearchString == null) return Collections.EMPTY_LIST;
        return searchForUser(usernameSearchString, realm, firstResult, maxResults);
    }
    
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
    	logger.info("SOAM User Storage: inside getGroupMembers");
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
    	logger.info("SOAM User Storage: inside getGroupMembers");
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
    	logger.info("SOAM User Storage: inside searchForUserByUserAttribute");
        return Collections.EMPTY_LIST;
    }
}