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
