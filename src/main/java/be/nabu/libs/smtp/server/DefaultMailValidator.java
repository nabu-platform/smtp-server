package be.nabu.libs.smtp.server;

import java.net.SocketAddress;
import java.util.List;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.smtp.server.api.MailValidator;

public class DefaultMailValidator implements MailValidator {

	@Override
	public long getMaxLineSize() {
		return 8192;
	}

	@Override
	public long getMaxDataSize() {
		return 1024l*1024*10;
	}

	@Override
	public boolean acceptConnection(String localServerName, String realm, String remoteServerName, SocketAddress remoteAddress) {
		return true;
	}

	@Override
	public boolean acceptMail(String localServerName, String realm, String remoteServerName, String from, List<String> to, Token token) {
		return true;
	}

}
