package com.github.bcgov.keycloak.soam;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
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
		return new AbstractUserAdapter(session, realm, model) {
			@Override
			public String getUsername() {
				return username;
			}
		};
	}

	@Override
	public UserModel getUserById(String id, RealmModel realm) {
		StorageId storageId = new StorageId(id);
		String username = storageId.getExternalId();
		return getUserByUsername(username, realm);
	}

	@Override
	public UserModel getUserByEmail(String email, RealmModel realm) {
		return null;
	}

	@Override
	public boolean isConfiguredFor(RealmModel realm, UserModel user, String credentialType) {
		String password = properties.get(user.getUsername());
		return credentialType.equals(CredentialModel.PASSWORD) && password != null;
	}

	@Override
	public boolean supportsCredentialType(String credentialType) {
		return credentialType.equals(CredentialModel.PASSWORD);
	}

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!supportsCredentialType(input.getType()) || !(input instanceof UserCredentialModel)) return false;

        UserCredentialModel cred = (UserCredentialModel)input;
        String password = properties.get(user.getUsername());
        if (password == null || UNSET_PASSWORD.equals(password)) return false;
        return password.equals(cred.getValue());
    }

    @Override
    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
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
        if (!credentialType.equals(CredentialModel.PASSWORD)) return;
        synchronized (properties) {
            properties.put(user.getUsername(), UNSET_PASSWORD);
            save();
        }

    }

    @Override
    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {

        return disableableTypes;
    }

	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public UserModel addUser(RealmModel realm, String username) {
		synchronized (properties) {
			properties.put(username, UNSET_PASSWORD);
			save();
		}
		return createAdapter(realm, username);
	}

	@Override
	public boolean removeUser(RealmModel realm, UserModel user) {
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
        return properties.size();
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm) {
        return getUsers(realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> getUsers(RealmModel realm, int firstResult, int maxResults) {
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
        return searchForUser(search, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(String search, RealmModel realm, int firstResult, int maxResults) {
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
        return searchForUser(params, realm, 0, Integer.MAX_VALUE);
    }

    @Override
    public List<UserModel> searchForUser(Map<String, String> params, RealmModel realm, int firstResult, int maxResults) {
        // only support searching by username
        String usernameSearchString = params.get("username");
        if (usernameSearchString == null) return Collections.EMPTY_LIST;
        return searchForUser(usernameSearchString, realm, firstResult, maxResults);
    }
    
    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group, int firstResult, int maxResults) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> getGroupMembers(RealmModel realm, GroupModel group) {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<UserModel> searchForUserByUserAttribute(String attrName, String attrValue, RealmModel realm) {
        return Collections.EMPTY_LIST;
    }
}