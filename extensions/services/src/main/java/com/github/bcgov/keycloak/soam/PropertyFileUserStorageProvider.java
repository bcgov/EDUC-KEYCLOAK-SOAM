package com.github.bcgov.keycloak.soam;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputUpdater;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapter;
import org.keycloak.storage.user.UserLookupProvider;

public class PropertyFileUserStorageProvider implements
        UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater
{
	 protected KeycloakSession session;
	    protected Properties properties;
	    protected ComponentModel model;
	    // map of loaded users in this transaction
	    protected Map<String, UserModel> loadedUsers = new HashMap<>();

	    public PropertyFileUserStorageProvider(KeycloakSession session, ComponentModel model, Properties properties) {
	        this.session = session;
	        this.model = model;
	        this.properties = properties;
	    }
	    
	    @Override
	    public UserModel getUserByUsername(String username, RealmModel realm) {
	        UserModel adapter = loadedUsers.get(username);
	        if (adapter == null) {
	            String password = properties.getProperty(username);
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
	        String password = properties.getProperty(user.getUsername());
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
	        String password = properties.getProperty(user.getUsername());
	        if (password == null) return false;
	        return password.equals(cred.getValue());
	    }
	    
	    @Override
	    public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
	        if (input.getType().equals(CredentialModel.PASSWORD)) throw new ReadOnlyException("user is read only for this update");

	        return false;
	    }

	    @Override
	    public void disableCredentialType(RealmModel realm, UserModel user, String credentialType) {

	    }

	    @Override
	    public Set<String> getDisableableCredentialTypes(RealmModel realm, UserModel user) {
	        return Collections.EMPTY_SET;
	    }

		@Override
		public void close() {
			// TODO Auto-generated method stub
			
		}
}