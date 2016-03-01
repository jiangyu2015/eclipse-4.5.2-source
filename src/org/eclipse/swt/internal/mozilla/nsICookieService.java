/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Mozilla Communicator client code, released March 31, 1998.
 *
 * The Initial Developer of the Original Code is
 * Netscape Communications Corporation.
 * Portions created by Netscape are Copyright (C) 1998-1999
 * Netscape Communications Corporation.  All Rights Reserved.
 *
 * Contributor(s):
 *
 * IBM
 * -  Binding to permit interfacing between Mozilla and SWT
 * -  Copyright (C) 2003, 2012 IBM Corp.  All Rights Reserved.
 *
 * ***** END LICENSE BLOCK ***** */
package org.eclipse.swt.internal.mozilla;


public class nsICookieService extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 5;

	static final String NS_ICOOKIESERVICE_IID_STR = "011c3190-1434-11d6-a618-0010a401eb10";
	static final String NS_ICOOKIESERVICE_1_9_IID_STR = "2aaa897a-293c-4d2b-a657-8c9b7136996d";

	public nsICookieService(int /*long*/ address) {
		super(address);
	}

	static {
		IIDStore.RegisterIID(nsICookieService.class, MozillaVersion.VERSION_BASE, new nsID(NS_ICOOKIESERVICE_IID_STR));
		IIDStore.RegisterIID(nsICookieService.class, MozillaVersion.VERSION_XR1_9, new nsID(NS_ICOOKIESERVICE_1_9_IID_STR));
	}

	public int GetCookieString(int /*long*/ aURI, int /*long*/ aChannel, int /*long*/[] _retval) {
		return XPCOM.VtblCall(this.getMethodIndex("getCookieString"), getAddress(), aURI, aChannel, _retval);
	}

	public int SetCookieString(int /*long*/ aURI, int /*long*/ aPrompt, byte[] aCookie, int /*long*/ aChannel) {
		return XPCOM.VtblCall(this.getMethodIndex("setCookieString"), getAddress(), aURI, aPrompt, aCookie, aChannel);
	}
}
