/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.site;

import java.io.PrintWriter;
import java.util.Locale;
import java.util.Vector;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.*;
import org.w3c.dom.*;

public class StatsInfo extends SiteObject implements IStatsInfo {

	private static final long serialVersionUID = 1L;
	final static String INDENT = "   "; //$NON-NLS-1$

	public static final String P_URL = "url"; //$NON-NLS-1$
	private String fURL;
	private Vector<ISiteObject> featureArtifacts = new Vector<ISiteObject>();
	private Vector<ISiteObject> bundleArtifacts = new Vector<ISiteObject>();

	public StatsInfo() {
		super();
	}

	public void setURL(String url) throws CoreException {
		String old = fURL;
		fURL = url;
		ensureModelEditable();
		firePropertyChanged(P_URL, old, fURL);
	}

	public String getURL() {
		return fURL;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#addFeatureArtifacts(org.eclipse.pde.internal.core.isite.ISiteFeature[])
	 */
	public void addFeatureArtifacts(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteFeature feature = newFeatures[i];
			((SiteFeature) feature).setInTheModel(true);
			featureArtifacts.add(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#addBundleArtifacts(org.eclipse.pde.internal.core.isite.ISiteBundle[])
	 */
	public void addBundleArtifacts(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newBundles.length; i++) {
			ISiteBundle bundle = newBundles[i];
			((SiteBundle) bundle).setInTheModel(true);
			bundleArtifacts.add(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.INSERT);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#removeFeatureArtifacts(org.eclipse.pde.internal.core.isite.ISiteFeature[])
	 */
	public void removeFeatureArtifacts(ISiteFeature[] newFeatures) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newFeatures.length; i++) {
			ISiteFeature feature = newFeatures[i];
			((SiteFeature) feature).setInTheModel(false);
			featureArtifacts.remove(newFeatures[i]);
		}
		fireStructureChanged(newFeatures, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#removeBundleArtifacts(org.eclipse.pde.internal.core.isite.ISiteBundle[])
	 */
	public void removeBundleArtifacts(ISiteBundle[] newBundles) throws CoreException {
		ensureModelEditable();
		for (int i = 0; i < newBundles.length; i++) {
			ISiteBundle bundle = newBundles[i];
			((SiteBundle) bundle).setInTheModel(false);
			bundleArtifacts.remove(bundle);
		}
		fireStructureChanged(newBundles, IModelChangedEvent.REMOVE);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#getFeatureArtifacts()
	 */
	public ISiteFeature[] getFeatureArtifacts() {
		return featureArtifacts.toArray(new ISiteFeature[featureArtifacts.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.IStatsInfo#getBundleArtifacts()
	 */
	public ISiteBundle[] getBundleArtifacts() {
		return bundleArtifacts.toArray(new ISiteBundle[bundleArtifacts.size()]);
	}

	public void parse(Node node) {
		if (node.getNodeType() == Node.ELEMENT_NODE) {
			Element element = (Element) node;
			fURL = element.getAttribute("location"); //$NON-NLS-1$
			NodeList children = node.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE) {
					parseChild(child);
				}
			}
		}
	}

	protected void parseChild(Node child) {
		String tag = child.getNodeName().toLowerCase(Locale.ENGLISH);
		if (tag.equals("feature")) { //$NON-NLS-1$
			ISiteFeature feature = getModel().getFactory().createFeature();
			((SiteFeature) feature).parse(child);
			((SiteFeature) feature).setInTheModel(true);
			featureArtifacts.add(feature);
		} else if (tag.equals("bundle")) { //$NON-NLS-1$
			ISiteBundle bundle = getModel().getFactory().createBundle();
			((SiteBundle) bundle).parse(child);
			((SiteBundle) bundle).setInTheModel(true);
			bundleArtifacts.add(bundle);
		}
	}

	public void write(String indent, PrintWriter writer) {
		if (isURLDefined()) {
			writer.print(indent + "<stats location=\"" + fURL + "\""); //$NON-NLS-1$ //$NON-NLS-2$
			writer.println(">"); //$NON-NLS-1$
			String indent2 = indent + INDENT;
			// features
			for (int i = 0; i < featureArtifacts.size(); i++) {
				IWritable writable = featureArtifacts.get(i);
				writable.write(indent2, writer);
			}
			// bundles
			for (int i = 0; i < bundleArtifacts.size(); i++) {
				IWritable writable = bundleArtifacts.get(i);
				writable.write(indent2, writer);
			}
			writer.println(indent + "</stats>"); //$NON-NLS-1$
		}
	}

	private boolean isURLDefined() {
		return fURL != null && fURL.length() > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.isite.ISiteObject#isValid()
	 */
	public boolean isValid() {
		for (int i = 0; i < featureArtifacts.size(); i++) {
			ISiteFeature feature = (ISiteFeature) featureArtifacts.get(i);
			if (!feature.isValid())
				return false;
		}
		for (int i = 0; i < bundleArtifacts.size(); i++) {
			ISiteBundle bundle = (ISiteBundle) bundleArtifacts.get(i);
			if (!bundle.isValid())
				return false;
		}
		return isURLDefined();
	}

}
