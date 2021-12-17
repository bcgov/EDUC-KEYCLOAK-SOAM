package ca.bc.gov.educ.keycloak.soam.model;

import java.util.Date;
import java.util.UUID;

public class SoamServicesCard {
	private UUID servicesCardInfoID;
	private String did;
	private String userDisplayName;
	private String givenName;
	private String givenNames;
	private String surname;
	private String birthDate;
	private String gender;
	private String email;
	private String identityAssuranceLevel;
	private String postalCode;
	private String createUser;
	private Date createDate;
	private String updateUser;
	private Date updateDate;

	public String getIdentityAssuranceLevel() {
		return identityAssuranceLevel;
	}

	public void setIdentityAssuranceLevel(String identityAssuranceLevel) {
		this.identityAssuranceLevel = identityAssuranceLevel;
	}

	public UUID getServicesCardInfoID() {
		return servicesCardInfoID;
	}

	public void setServicesCardInfoID(UUID servicesCardInfoID) {
		this.servicesCardInfoID = servicesCardInfoID;
	}

	public String getDid() {
		return did;
	}

	public void setDid(String did) {
		this.did = did;
	}

	public String getUserDisplayName() {
		return userDisplayName;
	}

	public void setUserDisplayName(String userDisplayName) {
		this.userDisplayName = userDisplayName;
	}

	public String getGivenName() {
		return givenName;
	}

	public void setGivenName(String givenName) {
		this.givenName = givenName;
	}

	public String getGivenNames() {
		return givenNames;
	}

	public void setGivenNames(String givenNames) {
		this.givenNames = givenNames;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public String getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(String birthDate) {
		this.birthDate = birthDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getCreateUser() {
		return createUser;
	}

	public void setCreateUser(String createUser) {
		this.createUser = createUser;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public String getUpdateUser() {
		return updateUser;
	}

	public void setUpdateUser(String updateUser) {
		this.updateUser = updateUser;
	}

	public Date getUpdateDate() {
		return updateDate;
	}

	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

}
