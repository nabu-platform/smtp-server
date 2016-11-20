package be.nabu.libs.smtp.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;

import javax.net.ssl.SSLContext;

import be.nabu.libs.authentication.api.Authenticator;
import be.nabu.libs.nio.api.ExceptionFormatter;
import be.nabu.libs.nio.api.KeepAliveDecider;
import be.nabu.libs.nio.api.MessageFormatter;
import be.nabu.libs.nio.api.MessageFormatterFactory;
import be.nabu.libs.nio.api.NIOServer;
import be.nabu.libs.nio.api.Pipeline;
import be.nabu.libs.nio.api.PipelineFactory;
import be.nabu.libs.nio.impl.MessagePipelineImpl;
import be.nabu.libs.smtp.server.api.MailValidator;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.io.SSLServerMode;
import be.nabu.utils.io.api.ByteBuffer;
import be.nabu.utils.io.api.ReadableContainer;
import be.nabu.utils.mime.api.Part;

public class SMTPPipelineFactory implements PipelineFactory {

	private String serverName;
	private Authenticator authenticator;
	private SSLContext context;
	private SSLServerMode sslServerMode;
	private MailValidator validator;

	public SMTPPipelineFactory(String serverName, MailValidator validator, Authenticator authenticator, SSLContext context, SSLServerMode sslServerMode) {
		this.serverName = serverName;
		this.validator = validator;
		this.authenticator = authenticator;
		this.context = context;
		this.sslServerMode = sslServerMode;
	}
	
	@Override
	public Pipeline newPipeline(NIOServer server, SelectionKey key) throws IOException {
		SMTPMessageParserFactory requestParserFactory = new SMTPMessageParserFactory(serverName, "defaultRealm", validator, authenticator, context, sslServerMode);
		
		MessagePipelineImpl<Part, String> pipeline = new MessagePipelineImpl<Part, String>(
			server,
			key,
			requestParserFactory,
			new MessageFormatterFactory<String>() {
				@Override
				public MessageFormatter<String> newMessageFormatter() {
					return new MessageFormatter<String>() {
						@Override
						public ReadableContainer<ByteBuffer> format(String message) {
							return IOUtils.wrap((message + "\n").getBytes(), true);
						}
					};
				}
			},
			new SMTPMessageProcessFactory(),
			new KeepAliveDecider<String>() {
				@Override
				public boolean keepConnectionAlive(String response) {
					return response != null && !response.startsWith("221 ");
				}
			},
			new ExceptionFormatter<Part, String>() {
				@Override
				public String format(Part request, Exception e) {
					return "500 " + e.getMessage();
				}
			}
		);
		
		requestParserFactory.setPipeline(pipeline);
		
		// send back the initial message
		pipeline.getResponseQueue().offer("220 " + serverName);
		
//		pipeline.setReadTimeout(readTimeout);
//		pipeline.setWriteTimeout(writeTimeout);
//		pipeline.setRequestLimit(requestLimit);
//		pipeline.setResponseLimit(responseLimit);
		return pipeline;
	}

	public String getServerName() {
		return serverName;
	}

	public Authenticator getAuthenticator() {
		return authenticator;
	}

}
