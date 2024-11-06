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
