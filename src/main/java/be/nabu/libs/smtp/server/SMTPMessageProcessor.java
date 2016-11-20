package be.nabu.libs.smtp.server;

import java.io.IOException;

import be.nabu.libs.nio.api.MessageProcessor;
import be.nabu.libs.nio.api.SecurityContext;
import be.nabu.libs.nio.api.SourceContext;
import be.nabu.utils.io.IOUtils;
import be.nabu.utils.mime.api.ContentPart;
import be.nabu.utils.mime.api.Header;
import be.nabu.utils.mime.api.Part;

public class SMTPMessageProcessor implements MessageProcessor<Part, String> {

	@Override
	public String process(SecurityContext securityContext, SourceContext sourceContext, Part request) {
		System.out.println("RECEIVE " + request.getContentType());
		for (Header header : request.getHeaders()) {
			System.out.println(header.toString());
		}
		if (request instanceof ContentPart) {
			try {
				byte[] bytes = IOUtils.toBytes(((ContentPart) request).getReadable());
				System.out.println(new String(bytes));
			}
			catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	@Override
	public Class<Part> getRequestClass() {
		return Part.class;
	}

	@Override
	public Class<String> getResponseClass() {
		return String.class;
	}

}
