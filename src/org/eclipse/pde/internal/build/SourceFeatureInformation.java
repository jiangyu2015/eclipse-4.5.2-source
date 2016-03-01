/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;

public class SourceFeatureInformation implements IPDEBuildConstants {
	// Key : a source bundle 
	// Value : the list of plugins that needs to get copied into the given source bundle
	// This list will be used to build the content of the fragment that contains 
	// config specific code
	private final Map<String, Set<BundleDescription>> sourceFeatureInformation = new HashMap<String, Set<BundleDescription>>(8);

	public SourceFeatureInformation() {
		//empty
	}

	public void addElementEntry(String bundle, BundleDescription plugin) {
		Set<BundleDescription> entry = sourceFeatureInformation.get(bundle);
		if (entry == null) {
			entry = new HashSet<BundleDescription>();
			sourceFeatureInformation.put(bundle, entry);
		}
		entry.add(plugin);
	}

	public Map<String, Set<BundleDescription>> getElementEntries() {
		return sourceFeatureInformation;
	}
}
