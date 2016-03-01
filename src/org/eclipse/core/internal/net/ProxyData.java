/*******************************************************************************
 * Copyright (c) 2007, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.net;

import org.eclipse.core.net.proxy.IProxyData;

public class ProxyData implements IProxyData {

	private String type;
	private String host;
	private int port;
	private String user;
	private String password;
	private boolean requiresAuthentication;
	private String source;
	private boolean dynamic = false;

	public ProxyData(String type, String host, int port,
			boolean requiresAuthentication, String source) {
		this.type = type;
		this.host = host;
		this.port = port;
		this.requiresAuthentication = requiresAuthentication;
		this.source = source;
	}

	public ProxyData(String type) {
		this.type = type;
	}

	public String getHost() {
		return host;
	}

	public String getPassword() {
		return password;
	}

	public int getPort() {
		return port;
	}

	public String getType() {
		return type;
	}

	public String getUserId() {
		return user;
	}
	
	public void setType(String type) {
		this.type = type;
	}

	public void setHost(String host) {
		if (host != null && host.length() == 0)
			host = null;
		this.host = host;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUserid(String userid) {
		this.user = userid;
		requiresAuthentication = userid != null;
	}

	public boolean isRequiresAuthentication() {
		return requiresAuthentication;
	}

	public void disable() {
		host = null;
		port = -1;
		user = null;
		password = null;
		requiresAuthentication = false;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isDynamic() {
		return dynamic;
	}

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	public String toString() {
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("type: "); //$NON-NLS-1$
		stringBuffer.append(type);
		stringBuffer.append(" host: "); //$NON-NLS-1$
		stringBuffer.append(host);
		stringBuffer.append(" port: "); //$NON-NLS-1$
		stringBuffer.append(port);
		stringBuffer.append(" user: "); //$NON-NLS-1$
		stringBuffer.append(user);
		stringBuffer.append(" password: "); //$NON-NLS-1$
		stringBuffer.append(password);
		stringBuffer.append(" reqAuth: "); //$NON-NLS-1$
		stringBuffer.append(requiresAuthentication);
		stringBuffer.append(" source: "); //$NON-NLS-1$
		stringBuffer.append(source);
		stringBuffer.append(" dynamic: "); //$NON-NLS-1$
		stringBuffer.append(dynamic);
		return stringBuffer.toString();
	}

	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (dynamic ? 1231 : 1237);
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result
				+ ((password == null) ? 0 : password.hashCode());
		result = prime * result + port;
		result = prime * result + (requiresAuthentication ? 1231 : 1237);
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		result = prime * result + ((user == null) ? 0 : user.hashCode());
		return result;
	}

	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ProxyData other = (ProxyData) obj;
		if (dynamic != other.dynamic)
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		} else if (!host.equals(other.host))
			return false;
		if (password == null) {
			if (other.password != null)
				return false;
		} else if (!password.equals(other.password))
			return false;
		if (port != other.port)
			return false;
		if (requiresAuthentication != other.requiresAuthentication)
			return false;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		} else if (!type.equals(other.type))
			return false;
		if (user == null) {
			if (other.user != null)
				return false;
		} else if (!user.equals(other.user))
			return false;
		return true;
	}

}