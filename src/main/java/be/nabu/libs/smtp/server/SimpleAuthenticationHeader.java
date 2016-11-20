package be.nabu.libs.smtp.server;

import be.nabu.libs.authentication.api.Token;
import be.nabu.libs.http.api.server.AuthenticationHeader;

public class SimpleAuthenticationHeader implements AuthenticationHeader {

	private Token token;

	public SimpleAuthenticationHeader(Token token) {
		this.token = token;
	}
	
	@Override
	public String getName() {
		return "X-Remote-User";
	}

	@Override
	public String getValue() {
		return token.getName();
	}

	@Override
	public String[] getComments() {
		return new String[0];
	}
	
	@Override
	public Token getToken() {
		return token;
	}
}
