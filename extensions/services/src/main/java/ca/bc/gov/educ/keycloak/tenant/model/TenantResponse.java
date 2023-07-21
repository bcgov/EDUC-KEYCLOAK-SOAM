package ca.bc.gov.educ.keycloak.tenant.model;

public class TenantResponse {

	private String clientID;
	private String tenantID;
	private boolean valid;

	public String getClientID() {
		return clientID;
	}

	public void setClientID(String clientID) {
		this.clientID = clientID;
	}

	public String getTenantID() {
		return tenantID;
	}

	public void setTenantID(String tenantID) {
		this.tenantID = tenantID;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}
}
