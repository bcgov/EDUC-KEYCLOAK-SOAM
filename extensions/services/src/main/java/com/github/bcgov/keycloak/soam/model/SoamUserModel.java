package com.github.bcgov.keycloak.soam.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;

public class SoamUserModel implements UserModel {

	public SoamUserModel() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Set<RoleModel> getRealmRoleMappings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<RoleModel> getClientRoleMappings(ClientModel app) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasRole(RoleModel role) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void grantRole(RoleModel role) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<RoleModel> getRoleMappings() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void deleteRoleMapping(RoleModel role) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUsername() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setUsername(String username) {
		// TODO Auto-generated method stub

	}

	@Override
	public Long getCreatedTimestamp() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCreatedTimestamp(Long timestamp) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEnabled(boolean enabled) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSingleAttribute(String name, String value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAttribute(String name, List<String> values) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeAttribute(String name) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getFirstAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getAttribute(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, List<String>> getAttributes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<String> getRequiredActions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addRequiredAction(String action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRequiredAction(String action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addRequiredAction(RequiredAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeRequiredAction(RequiredAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getFirstName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFirstName(String firstName) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getLastName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setLastName(String lastName) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getEmail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setEmail(String email) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isEmailVerified() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setEmailVerified(boolean verified) {
		// TODO Auto-generated method stub

	}

	@Override
	public Set<GroupModel> getGroups() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void joinGroup(GroupModel group) {
		// TODO Auto-generated method stub

	}

	@Override
	public void leaveGroup(GroupModel group) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isMemberOf(GroupModel group) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getFederationLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setFederationLink(String link) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getServiceAccountClientLink() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setServiceAccountClientLink(String clientInternalId) {
		// TODO Auto-generated method stub

	}

}
