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


public class nsICancelable extends nsISupports {

	static final int LAST_METHOD_ID = nsISupports.LAST_METHOD_ID + 1;

	static final String NS_ICANCELABLE_IID_STR = "d94ac0a0-bb18-46b8-844e-84159064b0bd";

	static {
		IIDStore.RegisterIID(nsICancelable.class, MozillaVersion.VERSION_BASE, new nsID(NS_ICANCELABLE_IID_STR));
	}

	public nsICancelable(int /*long*/ address) {
		super(address);
	}

	public int Cancel(int aReason) {
		return XPCOM.VtblCall(this.getMethodIndex("cancel"), getAddress(), aReason);
	}
}
