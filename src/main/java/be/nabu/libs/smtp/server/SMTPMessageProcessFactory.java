package be.nabu.libs.smtp.server;

import be.nabu.libs.events.api.EventDispatcher;
import be.nabu.libs.nio.api.ExceptionFormatter;
import be.nabu.libs.nio.api.MessageProcessor;
import be.nabu.libs.nio.api.MessageProcessorFactory;
import be.nabu.libs.nio.impl.EventDrivenMessageProcessor;
import be.nabu.utils.mime.api.Part;

public class SMTPMessageProcessFactory implements MessageProcessorFactory<Part, String> {

	private EventDispatcher dispatcher;
	private ExceptionFormatter<Part, String> exceptionFormatter;
	
	public SMTPMessageProcessFactory(EventDispatcher dispatcher, ExceptionFormatter<Part, String> exceptionFormatter) {
		this.dispatcher = dispatcher;
		this.exceptionFormatter = exceptionFormatter;
	}
	
	@Override
	public MessageProcessor<Part, String> newProcessor(Part request) {
		return new EventDrivenMessageProcessor<Part, String>(Part.class, String.class, dispatcher, exceptionFormatter, false);
	}

}
