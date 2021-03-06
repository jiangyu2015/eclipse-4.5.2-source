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
package org.eclipse.team.internal.ccvs.core;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.variants.*;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSResourceVariantFileRevision;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.core.mapping.ResourceVariantFileRevision;
import org.eclipse.team.internal.core.mapping.SyncInfoToDiffConverter;

/**
 * This class provides common functionality for three way synchronizing
 * for CVS.
 */
public abstract class CVSSyncTreeSubscriber extends ResourceVariantTreeSubscriber implements IAdaptable {
	
	public static final String SYNC_KEY_QUALIFIER = "org.eclipse.team.cvs"; //$NON-NLS-1$
	
	private IResourceVariantComparator comparisonCriteria;
	
	private QualifiedName id;
	private String name;
	private SyncInfoToDiffConverter converter = new CVSSyncInfoToDiffConverter();
	
	public class CVSSyncInfoToDiffConverter extends SyncInfoToDiffConverter {
		protected ResourceVariantFileRevision asFileRevision(IResourceVariant variant) {
			return new CVSResourceVariantFileRevision(variant);
		}
	}
	
	CVSSyncTreeSubscriber(QualifiedName id, String name) {
		this.id = id;
		this.name = name;
		this.comparisonCriteria = new CVSRevisionNumberCompareCriteria(isThreeWay());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getId()
	 */
	public QualifiedName getId() {
		return id;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getName()
	 */
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		if (!isSupervised(resource)) return null;
		if(resource.getType() == IResource.FILE || !isThreeWay()) {
			return super.getSyncInfo(resource);
		} else {
			// In CVS, folders do not have a base. Hence, the remote is used as the base.
			IResourceVariant remoteResource = getRemoteTree().getResourceVariant(resource);
			return getSyncInfo(resource, remoteResource, remoteResource);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.core.sync.ISyncTreeSubscriber#isSupervised(org.eclipse.core.resources.IResource)
	 */
	public boolean isSupervised(IResource resource) throws TeamException {
		try {
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject(), CVSProviderPlugin.getTypeId());
			if (provider == null) return false;
			// TODO: what happens for resources that don't exist?
			// TODO: is it proper to use ignored here?
			ICVSResource cvsThing = CVSWorkspaceRoot.getCVSResourceFor(resource);
			if (cvsThing.isIgnored()) {
				// An ignored resource could have an incoming addition (conflict)
				return getRemoteTree().hasResourceVariant(resource);
			}
			return true;
		} catch (TeamException e) {
			// If there is no resource in core this means there is no local and no remote
			// so the resource is not supervised.
			if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND || !resource.getProject().isAccessible()) {
				return false;
			}
			throw e;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.TeamSubscriber#getDefaultComparisonCriteria()
	 */
	public IResourceVariantComparator getResourceComparator() {
		return comparisonCriteria;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.core.subscribers.caches.ResourceVariantTreeSubscriber#getSyncInfo(org.eclipse.core.resources.IResource, org.eclipse.team.core.synchronize.IResourceVariant, org.eclipse.team.core.synchronize.IResourceVariant)
	 */
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote) throws TeamException {
		CVSSyncInfo info = new CVSSyncInfo(local, base, remote, this);
		info.init();
		return info;
	}
	
	/*
	 * Indicate whether file contents should be cached on a refresh
	 */
	protected  boolean getCacheFileContentsHint() {
		return false;
	}
	
	/*
	 * Indicate whether the subscriber is two-way or three-way
	 */
	protected boolean isThreeWay() {
		return true;
	}
	
	protected boolean rootsEqual(Subscriber other) {
		Set roots1 = new HashSet(Arrays.asList(other.roots()));
		Set roots2 = new HashSet(Arrays.asList(roots()));
		if(roots1.size() != roots2.size()) return false;
		return roots2.containsAll(roots1);
	}
	
	
	public IDiff getDiff(IResource resource) throws CoreException {
		SyncInfo info = getSyncInfo(resource);
		if (info == null || info.getKind() == SyncInfo.IN_SYNC)
			return null;
		return converter.getDeltaFor(info);
	}
	public Object getAdapter(Class adapter) {
		if (adapter == SyncInfoToDiffConverter.class) {
			return converter;
		}
		return Platform.getAdapterManager().getAdapter(this, adapter);
	}

}
