package be.nabu.libs.smtp.server;

import javax.net.ssl.SSLContext;

import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.nio.api.MessageParser;
import be.nabu.libs.nio.api.MessageParserFactory;
import be.nabu.libs.nio.api.MessagePipeline;
import be.nabu.libs.smtp.server.api.MailValidator;
import be.nabu.utils.io.SSLServerMode;
import be.nabu.utils.mime.api.Part;

public class SMTPMessageParserFactory implements MessageParserFactory<Part> {

	private String serverName;
	private MessagePipeline<Part, String> pipeline;
	private String remoteName;
	private boolean enablePipelining;
	private Authenticator authenticator;
	private SSLServerMode sslServerMode;
	private SSLContext context;
	private String realm;
	private MailValidator validator;
	
	public SMTPMessageParserFactory(String serverName, String realm, MailValidator validator, Authenticator authenticator, SSLContext context, SSLServerMode sslServerMode) {
		this.serverName = serverName;
		this.realm = realm;
		this.validator = validator;
		this.authenticator = authenticator;
		this.context = context;
		this.sslServerMode = sslServerMode;
	}

	@Override
	public MessageParser<Part> newMessageParser() {
		return new SMTPMessageParser(this, pipeline);
	}

	public MessagePipeline<Part, String> getPipeline() {
		return pipeline;
	}

	public void setPipeline(MessagePipeline<Part, String> pipeline) {
		this.pipeline = pipeline;
	}

	public String getRemoteName() {
		return remoteName;
	}

	public void setRemoteName(String remoteName) {
		this.remoteName = remoteName;
	}

	public String getServerName() {
		return serverName;
	}

	public boolean isEnablePipelining() {
		return enablePipelining;
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

	public SSLServerMode getSslServerMode() {
		return sslServerMode;
	}

	public SSLContext getContext() {
		return context;
	}

	public String getRealm() {
		return realm;
	}

	public MailValidator getValidator() {
		if (validator == null) {
			validator = new DefaultMailValidator();
		}
		return validator;
	}
	
}
