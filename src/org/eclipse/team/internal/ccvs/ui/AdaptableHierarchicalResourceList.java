/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * This class acts as an adaptable list that will return the resources in the
 * hierarchy indicated by their paths
 */
public class AdaptableHierarchicalResourceList extends AdaptableResourceList {
	private IContainer root;

	/**
	 * Constructor for AdaptableHierarchicalResourceList.
	 * @param resources
	 */
	public AdaptableHierarchicalResourceList(IContainer root, IResource[] resources) {
		super(resources);
		this.root = root;
	}

	/**
	 * @see org.eclipse.ui.model.IWorkbenchAdapter#getChildren(java.lang.Object)
	 */
	public Object[] getChildren(Object o) {
		return getChildenFor(root);
	}

	private IResource[] getChildenFor(IContainer parent) {
		Set children = new HashSet();
		IPath parentPath = parent.getFullPath();
		for (int i = 0; i < resources.length; i++) {
			IResource resource = resources[i];
			IPath resourcePath = resource.getFullPath();
			if (parent instanceof IWorkspaceRoot) {
				children.add(((IWorkspaceRoot)parent).getProject(resourcePath.segment(0)));
			} else if (parentPath.isPrefixOf(resourcePath)) {
				IPath parentRelativePath = resourcePath.removeFirstSegments(parentPath.segmentCount());
				if (parentRelativePath.segmentCount() == 1) {
					children.add(resource);
				} else if (parentRelativePath.segmentCount() > 1) {
					children.add(parent.getFolder(new Path(null, parentRelativePath.segment(0))));
				}
			}
		}
		return (IResource[]) children.toArray(new IResource[children.size()]);
	}
	
	/**
	 * Returns a content provider for <code>IResource</code>s that returns 
	 * only children of the given resource type.
	 */
	public ITreeContentProvider getTreeContentProvider() {
		return new WorkbenchContentProvider() {
			public Object[] getChildren(Object o) {
				if (o instanceof IContainer) {
					return getChildenFor((IContainer) o);
				} else {
					return super.getChildren(o);
				}
			}
		};
	}
	
	public void setResources(IResource[] resources) {
		this.resources = resources;
	}

	/**
	 * Sets the root.
	 * @param root The root to set
	 */
	public void setRoot(IContainer root) {
		this.root = root;
	}

}
