package be.nabu.libs.smtp.server.api;

import java.net.SocketAddress;
import java.util.List;

import be.nabu.libs.authentication.api.Token;

public interface MailValidator {
	public long getMaxLineSize();
	public long getMaxDataSize();
	public boolean acceptConnection(String localServerName, String realm, String remoteServerName, SocketAddress remoteAddress);
	public boolean acceptMail(String localServerName, String realm, String remoteServerName, String from, List<String> to, Token token);
}
