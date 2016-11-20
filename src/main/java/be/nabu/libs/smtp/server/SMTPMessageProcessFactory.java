package be.nabu.libs.smtp.server;

import be.nabu.libs.nio.api.MessageProcessor;
import be.nabu.libs.nio.api.MessageProcessorFactory;
import be.nabu.utils.mime.api.Part;

public class SMTPMessageProcessFactory implements MessageProcessorFactory<Part, String> {

	@Override
	public MessageProcessor<Part, String> newProcessor(Part request) {
		return new SMTPMessageProcessor();
	}

}
