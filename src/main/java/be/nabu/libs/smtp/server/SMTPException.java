package be.nabu.libs.smtp.server;

public class SMTPException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	
	private int code;

	public SMTPException(int code) {
		super();
		this.code = code;
	}

	public SMTPException(int code, String message, Throwable cause) {
		super(message, cause);
		this.code = code;
	}

	public SMTPException(int code, String message) {
		super(message);
		this.code = code;
	}

	public SMTPException(int code, Throwable cause) {
		super(cause);
		this.code = code;
	}

	public int getCode() {
		return code;
	}
	
}
