/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.util.*;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.internal.build.site.BuildTimeFeature;
import org.osgi.framework.Version;

public class AssemblyInformation implements IPDEBuildConstants {
	// List all the features and plugins to assemble sorted on a per config basis 
	//	key: string[] representing the tuple of a config 
	// value: (AssemblyLevelConfigInfo) representing the info for the given config
	private final Map<Config, AssemblyLevelConfigInfo> assembleInformation = new HashMap<Config, AssemblyLevelConfigInfo>(8);
	private final Map<String, BundleDescription> bundleMap = new HashMap<String, BundleDescription>();
	private final Map<String, BuildTimeFeature> rootMap = new HashMap<String, BuildTimeFeature>();

	public AssemblyInformation() {
		// Initialize the content of the assembly information with the configurations 
		for (Iterator<Config> iter = AbstractScriptGenerator.getConfigInfos().iterator(); iter.hasNext();) {
			assembleInformation.put(iter.next(), new AssemblyLevelConfigInfo());
		}
	}

	public void addFeature(Config config, BuildTimeFeature feature) {
		AssemblyLevelConfigInfo entry = assembleInformation.get(config);
		entry.addFeature(feature);
	}

	public void removeFeature(Config config, BuildTimeFeature feature) {
		AssemblyLevelConfigInfo entry = assembleInformation.get(config);
		entry.removeFeature(feature);
	}

	public void addPlugin(Config config, BundleDescription plugin) {
		AssemblyLevelConfigInfo entry = assembleInformation.get(config);
		entry.addPlugin(plugin);

		String id = plugin.getSymbolicName();
		BundleDescription existing = bundleMap.get(id);
		if (existing == null || existing.getVersion().compareTo(plugin.getVersion()) < 0)
			bundleMap.put(id, plugin);
		bundleMap.put(id + '_' + plugin.getVersion().toString(), plugin);
	}

	public BundleDescription getPlugin(String id, String version) {
		if (version != null && !GENERIC_VERSION_NUMBER.equals(version))
			return bundleMap.get(id + '_' + version);
		return bundleMap.get(id);
	}

	public BuildTimeFeature getRootProvider(String id, String version) {
		if (version != null && !GENERIC_VERSION_NUMBER.equals(version))
			return rootMap.get(id + '_' + version);
		return rootMap.get(id);
	}

	public Collection<BundleDescription> getPlugins(Config config) {
		return assembleInformation.get(config).getPlugins();
	}

	public Set<BundleDescription> getAllPlugins() {
		Collection<AssemblyLevelConfigInfo> pluginsByConfig = assembleInformation.values();
		Set<BundleDescription> result = new LinkedHashSet<BundleDescription>();
		for (Iterator<AssemblyLevelConfigInfo> iter = pluginsByConfig.iterator(); iter.hasNext();) {
			Collection<BundleDescription> allPlugins = iter.next().getPlugins();
			result.addAll(allPlugins);
		}
		return result;
	}

	public Collection<BundleDescription> getBinaryPlugins(Config config) {
		Collection<BundleDescription> allPlugins = getPlugins(config);
		Set<BundleDescription> result = new LinkedHashSet<BundleDescription>(allPlugins.size());
		for (Iterator<BundleDescription> iter = allPlugins.iterator(); iter.hasNext();) {
			BundleDescription bundle = iter.next();
			Properties bundleProperties = ((Properties) bundle.getUserObject());
			if (bundleProperties == null || bundleProperties.get(IS_COMPILED) == null || Boolean.FALSE == bundleProperties.get(IS_COMPILED))
				result.add(bundle);
		}
		return result;
	}

	public Collection<BundleDescription> getCompiledPlugins(Config config) {
		Collection<BundleDescription> allPlugins = getPlugins(config);
		Set<BundleDescription> result = new LinkedHashSet<BundleDescription>(allPlugins.size());
		for (Iterator<BundleDescription> iter = allPlugins.iterator(); iter.hasNext();) {
			BundleDescription bundle = iter.next();
			Properties bundleProperties = ((Properties) bundle.getUserObject());
			if (bundleProperties != null && Boolean.TRUE == bundleProperties.get(IS_COMPILED))
				result.add(bundle);
		}
		return result;
	}

	public Set<BundleDescription> getAllCompiledPlugins() {
		Collection<AssemblyLevelConfigInfo> pluginsByConfig = assembleInformation.values();
		Set<BundleDescription> result = new LinkedHashSet<BundleDescription>();
		for (Iterator<AssemblyLevelConfigInfo> iter2 = pluginsByConfig.iterator(); iter2.hasNext();) {
			Collection<BundleDescription> allPlugins = iter2.next().getPlugins();
			for (Iterator<BundleDescription> iter = allPlugins.iterator(); iter.hasNext();) {
				BundleDescription bundle = iter.next();
				if (!Utils.isBinary(bundle)) {
					result.add(bundle);
				}
			}
		}
		return result;
	}

	public Collection<BuildTimeFeature> getCompiledFeatures(Config config) {
		Collection<BuildTimeFeature> allFeatures = getFeatures(config);
		ArrayList<BuildTimeFeature> result = new ArrayList<BuildTimeFeature>(allFeatures.size());
		for (Iterator<BuildTimeFeature> iter = allFeatures.iterator(); iter.hasNext();) {
			BuildTimeFeature tmp = iter.next();
			if (!tmp.isBinary())
				result.add(tmp);
		}
		return result;
	}

	public Collection<BuildTimeFeature> getBinaryFeatures(Config config) {
		Collection<BuildTimeFeature> allFeatures = getFeatures(config);
		ArrayList<BuildTimeFeature> result = new ArrayList<BuildTimeFeature>(allFeatures.size());
		for (Iterator<BuildTimeFeature> iter = allFeatures.iterator(); iter.hasNext();) {
			BuildTimeFeature tmp = iter.next();
			if (tmp.isBinary())
				result.add(tmp);
		}
		return result;
	}

	public ArrayList<BuildTimeFeature> getFeatures(Config config) {
		return assembleInformation.get(config).getFeatures();
	}

	public boolean copyRootFile(Config config) {
		return assembleInformation.get(config).hasRootFile();
	}

	public Collection<BuildTimeFeature> getRootFileProviders(Config config) {
		return assembleInformation.get(config).getRootFileProvider();
	}

	public void addRootFileProvider(Config config, BuildTimeFeature feature) {
		assembleInformation.get(config).addRootFileProvider(feature);

		String id = feature.getId();
		BuildTimeFeature existing = rootMap.get(id);
		if (existing == null || new Version(existing.getVersion()).compareTo(new Version(feature.getVersion())) < 0)
			rootMap.put(id, feature);
		rootMap.put(id + '_' + feature.getVersion(), feature);
	}

	// All the information that will go into the assemble file for a specific info
	protected static class AssemblyLevelConfigInfo {
		// the plugins that are contained into this config
		private final Collection<BundleDescription> plugins = new LinkedHashSet<BundleDescription>(20);
		// the features that are contained into this config
		private final ArrayList<BuildTimeFeature> features = new ArrayList<BuildTimeFeature>(7);
		// indicate whether root files needs to be copied and where they are coming from
		private final LinkedList<BuildTimeFeature> rootFileProviders = new LinkedList<BuildTimeFeature>();

		public void addRootFileProvider(BuildTimeFeature feature) {
			if (rootFileProviders.contains(feature))
				return;
			for (Iterator<BuildTimeFeature> iter = rootFileProviders.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = iter.next();
				if (feature == featureDescriptor)
					return;
				if (feature.getId().equals(featureDescriptor.getId()) && feature.getVersion().equals(featureDescriptor.getVersion()))
					return;
			}
			rootFileProviders.add(feature);
		}

		public Collection<BuildTimeFeature> getRootFileProvider() {
			return rootFileProviders;
		}

		public boolean hasRootFile() {
			return rootFileProviders.size() > 0;
		}

		public ArrayList<BuildTimeFeature> getFeatures() {
			return features;
		}

		public Collection<BundleDescription> getPlugins() {
			return plugins;
		}

		public void addFeature(BuildTimeFeature feature) {
			for (Iterator<BuildTimeFeature> iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = iter.next();
				if (feature.getId().equals(featureDescriptor.getId()) && (feature).getVersion().equals(featureDescriptor.getVersion()))
					return;
			}
			features.add(feature);
		}

		public void addPlugin(BundleDescription plugin) {
			plugins.add(plugin);
		}

		public void removeFeature(BuildTimeFeature feature) {
			for (Iterator<BuildTimeFeature> iter = features.iterator(); iter.hasNext();) {
				BuildTimeFeature featureDescriptor = iter.next();
				if (feature.getId().equals(featureDescriptor.getId()) && feature.getVersion().equals(featureDescriptor.getVersion())) {
					features.remove(featureDescriptor);
					return;
				}
			}
		}
	}
}
