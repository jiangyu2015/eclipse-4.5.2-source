/*******************************************************************************
 * Copyright (c) 2009, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.swt.internal.ole.win32;

public class IServiceProvider extends IUnknown
{
public IServiceProvider(int /*long*/ address) {
	super(address);
}
public int QueryService(GUID iid1, GUID iid2, int /*long*/ ppvObject[]) {
	return COM.VtblCall(3, address, iid1, iid2, ppvObject);
}
}
