package ca.bc.gov.educ.keycloak.tenant.exception;

public class TenantRuntimeException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TenantRuntimeException() {

	}

	public TenantRuntimeException(String message) {
		super(message);
	}

	public TenantRuntimeException(Throwable cause) {
		super(cause);
	}

	public TenantRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public TenantRuntimeException(String message, Throwable cause, boolean enableSuppression,
								  boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
