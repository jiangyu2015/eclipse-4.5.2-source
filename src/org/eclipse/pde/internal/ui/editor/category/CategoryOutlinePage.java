/******************************************************************************* 
* Copyright (c) 2009, 2013 EclipseSource and others. All rights reserved. This
* program and the accompanying materials are made available under the terms of
* the Eclipse Public License v1.0 which accompanies this distribution, and is
* available at http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*   EclipseSource - initial API and implementation
*   IBM Corporation - ongoing enhancements
******************************************************************************/
package org.eclipse.pde.internal.ui.editor.category;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.pde.core.IWritable;
import org.eclipse.pde.internal.core.isite.*;
import org.eclipse.pde.internal.ui.editor.*;

public class CategoryOutlinePage extends FormOutlinePage {
	private LabelProvider fLabelProvider;

	/**
	 * @param editor
	 */
	public CategoryOutlinePage(PDEFormEditor editor) {
		super(editor);
	}

	protected Object[] getChildren(Object parent) {
		if (parent instanceof PDEFormPage) {
			PDEFormPage page = (PDEFormPage) parent;
			ISiteModel model = (ISiteModel) page.getModel();
			if (model.isValid()) {
				ISite site = model.getSite();
				if (page.getId().equals(IUsPage.PAGE_ID)) {
					ArrayList<IWritable> result = new ArrayList<IWritable>();
					ISiteCategoryDefinition[] catDefs = site.getCategoryDefinitions();
					for (int i = 0; i < catDefs.length; i++) {
						result.add(catDefs[i]);
					}
					ISiteFeature[] features = site.getFeatures();
					for (int i = 0; i < features.length; i++) {
						if (features[i].getCategories().length == 0)
							result.add(new SiteFeatureAdapter(null, features[i]));
					}
					ISiteBundle[] bundles = site.getBundles();
					for (int i = 0; i < bundles.length; i++) {
						if (bundles[i].getCategories().length == 0) {
							result.add(new SiteBundleAdapter(null, bundles[i]));
						}
					}
					return result.toArray();
				}
			}
		}
		if (parent instanceof ISiteCategoryDefinition) {
			ISiteCategoryDefinition catDef = (ISiteCategoryDefinition) parent;
			ISiteModel model = catDef.getModel();
			if (model.isValid()) {
				ISite site = model.getSite();
				ISiteFeature[] features = site.getFeatures();
				HashSet<IWritable> result = new HashSet<IWritable>();
				for (int i = 0; i < features.length; i++) {
					ISiteCategory[] cats = features[i].getCategories();
					for (int j = 0; j < cats.length; j++) {
						if (cats[j].getDefinition() != null && cats[j].getDefinition().equals(catDef)) {
							result.add(new SiteFeatureAdapter(cats[j].getName(), features[i]));
						}
					}
				}
				ISiteBundle[] bundles = site.getBundles();
				for (int i = 0; i < bundles.length; i++) {
					ISiteCategory[] cats = bundles[i].getCategories();
					for (int j = 0; j < cats.length; j++) {
						if (cats[j].getDefinition() != null && cats[j].getDefinition().equals(catDef)) {
							result.add(new SiteBundleAdapter(cats[j].getName(), bundles[i]));
						}
					}
				}
				return result.toArray();
			}
		}
		return new Object[0];
	}

	protected String getParentPageId(Object item) {
		String pageId = null;
		if (item instanceof ISiteCategoryDefinition || item instanceof SiteFeatureAdapter || item instanceof SiteBundleAdapter)
			pageId = IUsPage.PAGE_ID;
		if (pageId != null)
			return pageId;
		return super.getParentPageId(item);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#createLabelProvider()
	 */
	public ILabelProvider createLabelProvider() {
		fLabelProvider = new CategoryLabelProvider();
		return fLabelProvider;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.editor.FormOutlinePage#dispose()
	 */
	public void dispose() {
		super.dispose();
		if (fLabelProvider != null)
			fLabelProvider.dispose();
	}
}
