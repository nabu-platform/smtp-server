/*
* Copyright (C) 2016 Alexander Verbruggen
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

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
		
		ExceptionFormatter<Part, String> exceptionFormatter = new ExceptionFormatter<Part, String>() {
			@Override
			public String format(Part request, Exception e) {
				System.out.println("FORMATTING EXCEPTION: " + e.getMessage());
				return "500 " + e.getMessage();
			}
		};
		
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
							return IOUtils.wrap((message + "\r\n").getBytes(), true);
						}
					};
				}
			},
			new SMTPMessageProcessFactory(server.getDispatcher(), exceptionFormatter),
			new KeepAliveDecider<String>() {
				@Override
				public boolean keepConnectionAlive(String response) {
					return response != null && !response.startsWith("221 ");
				}
			},
			exceptionFormatter
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
