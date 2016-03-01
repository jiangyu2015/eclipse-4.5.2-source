/*******************************************************************************
* Copyright (c) 2009 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
******************************************************************************/
package org.eclipse.ecf.core.util;

import org.eclipse.ecf.core.IContainerManager;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Service tracker customized to handle tracking the ECF container manager service (singleton).
 * @since 3.1
 *
 */
public class ContainerManagerTracker extends ServiceTracker {

	public ContainerManagerTracker(BundleContext context) {
		super(context, IContainerManager.class.getName(), null);
	}

	public IContainerManager getContainerManager() {
		return (IContainerManager) getService();
	}
}
