/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationScope;
import org.eclipse.team.core.mapping.ISynchronizationScopeManager;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ui.mapping.BuildScopeOperation;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Performs a CVS operation on multiple repository providers
 */
public abstract class RepositoryProviderOperation extends CVSOperation {

	/**
	 * Flag to indicate whether models are to be consulted when building 
	 * the scope. This is provided for testing purposes and is not expected 
	 * to be used otherwise.
	 */
	public static boolean consultModelsWhenBuildingScope = true;
	
	private ISynchronizationScopeManager manager;
	private final ResourceMapping[] selectedMappings;
	
    /**
     * Interface that is available to subclasses which identifies
     * the depth for various resources. The files will be included
     * in whichever group (deep or shallow) has resources.
     */
    public interface ICVSTraversal {
        IResource[] getShallowResources();
        IResource[] getDeepResources();
        IResource[] getNontraversedFolders();
    }
    
	/*
	 * A map entry for a provider that divides the traversals to be performed by depth.
	 * There are really only 
	 */
	private static class TraversalMapEntry implements ICVSTraversal {
	    // The provider for this entry
	    RepositoryProvider provider;
	    // Files are always shallow
	    List files = new ArrayList();
	    // Not sure what to do with zero depth folders but we'll record them
	    List zeroFolders = new ArrayList();
	    // Non-recursive folder (-l)
	    List shallowFolders = new ArrayList();
	    // Recursive folders (-R)
	    List deepFolders = new ArrayList();
        public TraversalMapEntry(RepositoryProvider provider) {
            this.provider = provider;
        }
        /**
         * Add the resources from the traversals to the entry
         * @param traversals the traversals
         */
        public void add(ResourceTraversal[] traversals) {
            for (int i = 0; i < traversals.length; i++) {
                ResourceTraversal traversal = traversals[i];
                add(traversal);
            }
        }
	    /**
	     * Add the resources from the traversal to the entry
	     * @param traversal the traversal
	     */
	    public void add(ResourceTraversal traversal) {
	        IResource[] resources = traversal.getResources();
	        for (int i = 0; i < resources.length; i++) {
                IResource resource = resources[i];
                if (resource.getProject().equals(provider.getProject())) {
	                if (resource.getType() == IResource.FILE) {
	                    files.add(resource);
	                } else {
				        switch (traversal.getDepth()) {
			            case IResource.DEPTH_ZERO:
			                zeroFolders.add(resource);
			                break;
			            case IResource.DEPTH_ONE:
			                shallowFolders.add(resource);
			                break;
			            case IResource.DEPTH_INFINITE:
			                deepFolders.add(resource);
			                break;
			            default:
			                deepFolders.add(resource);
			            }
	                }
                }
            }
	    }
	    /**
	     * Return the resources that can be included in a shallow operation.
	     * Include files with the shallow resources if there are shallow folders
	     * or if there are no shallow or deep folders.
	     * @return the resources that can be included in a shallow operation
	     */
	    public IResource[] getShallowResources() {
	        if (shallowFolders.isEmpty() && deepFolders.isEmpty() && !files.isEmpty()) {
	            return (IResource[]) files.toArray(new IResource[files.size()]);
	        }
	        if (!shallowFolders.isEmpty()) {
	            if (files.isEmpty()) {
	                return (IResource[]) shallowFolders.toArray(new IResource[shallowFolders.size()]);
	            }
	            List result = new ArrayList();
	            result.addAll(shallowFolders);
	            result.addAll(files);
	            return (IResource[]) result.toArray(new IResource[result.size()]);
	        }
	        return new IResource[0];
	    }
	    /**
	     * Return the resources to be included in a deep operation.
	     * If there are no shallow folders, this will include any files.
	     * @return
	     */
	    public IResource[] getDeepResources() {
	        if (deepFolders.isEmpty())
	            return new IResource[0];
	        if (!shallowFolders.isEmpty())
	            return (IResource[]) deepFolders.toArray(new IResource[deepFolders.size()]);
            List result = new ArrayList();
            result.addAll(deepFolders);
            result.addAll(files);
            return (IResource[]) result.toArray(new IResource[result.size()]);
	    }
	    /**
	     * Return the folders that are depth zero
	     */
	    public IResource[] getNontraversedFolders() {
	        return (IResource[]) zeroFolders.toArray(new IResource[zeroFolders.size()]);
	    }
	}

    
    /**
     * Convert the provided resources to one or more resource mappers
     * that traverse the elements deeply. The model element of the resource
     * mappers will be an IStructuredSelection.
     * @param resources the resources
     * @return a resource mappers that traverses the resources
     */
    public static ResourceMapping[] asResourceMappers(final IResource[] resources) {
        return asResourceMappers(resources, IResource.DEPTH_INFINITE);
    }
    
    /**
     * Convert the provided resources to one or more resource mappers
     * that traverse the elements deeply. The model element of the resource
     * mappers will be an IStructuredSelection.
     * @param resources the resources
     * @return a resource mappers that traverses the resources
     */
    public static ResourceMapping[] asResourceMappers(final IResource[] resources, int depth) {
        return WorkspaceResourceMapper.asResourceMappers(resources, depth);
    }
    
	public RepositoryProviderOperation(IWorkbenchPart part, final IResource[] resources) {
		this(part, asResourceMappers(resources));
	}

    public RepositoryProviderOperation(IWorkbenchPart part, ResourceMapping[] selectedMappings) {
        super(part);
		this.selectedMappings = selectedMappings;
    }

    /* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		try {
            monitor.beginTask(null, 100);
            buildScope(monitor);
            Map table = getProviderTraversalMapping(Policy.subMonitorFor(monitor, 30));
            execute(table, Policy.subMonitorFor(monitor, 30));
        } catch (CoreException e) {
            throw CVSException.wrapException(e);
        } finally {
            monitor.done();
        }
	}
	
	protected void endOperation() throws CVSException {
    	if (manager != null) {
    		manager.dispose();
    		manager = null;
    	}
		super.endOperation();
	}

    public ISynchronizationScope buildScope(IProgressMonitor monitor) throws InterruptedException, CVSException {
    	if (manager == null) {
    		manager = createScopeManager(consultModelsWhenBuildingScope && consultModelsForMappings());
    		BuildScopeOperation op = new BuildScopeOperation(getPart(), manager);
			try {
				op.run(monitor);
			} catch (InvocationTargetException e) {
				throw CVSException.wrapException(e);
			}
    	}
    	return manager.getScope();
	}

    /**
     * Create the scope manager to be used by this operation.
     * @param consultModels whether models should be consulted to include additional mappings
     * @return a scope manager
     */
	protected SynchronizationScopeManager createScopeManager(boolean consultModels) {
		return new SynchronizationScopeManager(getJobName(), getSelectedMappings(), getResourceMappingContext(), consultModels);
	}

	private void execute(Map providerTraversal, IProgressMonitor monitor) throws CVSException, InterruptedException {
        Set keySet = providerTraversal.keySet();
        monitor.beginTask(null, keySet.size() * 1000);
        Iterator iterator = keySet.iterator();
        while (iterator.hasNext()) {
        	CVSTeamProvider provider = (CVSTeamProvider)iterator.next();
        	monitor.setTaskName(getTaskName(provider));
        	TraversalMapEntry entry = (TraversalMapEntry)providerTraversal.get(provider);
        	execute(provider, entry, Policy.subMonitorFor(monitor, 1000));
        }
    }

    /**
     * Execute the operation on the given set of traversals
     * @param provider
     * @param entry
     * @param subMonitor
     * @throws CVSException
     * @throws InterruptedException
     */
    protected void execute(CVSTeamProvider provider, ICVSTraversal entry, IProgressMonitor monitor) throws CVSException, InterruptedException {
        IResource[] deepResources = entry.getDeepResources();
        IResource[] shallowResources = entry.getShallowResources();
        IResource[] nontraversedFolders = entry.getNontraversedFolders();
        try {
	        monitor.beginTask(getTaskName(provider), (deepResources.length > 0 ? 100 : 0) + (shallowResources.length > 0 ? 100 : 0) + (nontraversedFolders.length > 0 ? 10 : 0));
	        if (deepResources.length == 0 && shallowResources.length == 0 && nontraversedFolders.length == 0)
	        	return;
	        final ISchedulingRule rule = getSchedulingRule(provider);
	        try {
	        	Job.getJobManager().beginRule(rule, monitor);
	            if (deepResources.length > 0)
	                execute(provider, deepResources, true /* recurse */, Policy.subMonitorFor(monitor, 100));
	            if (shallowResources.length > 0)
	                execute(provider, shallowResources, false /* recurse */, Policy.subMonitorFor(monitor, 100));
	            if (nontraversedFolders.length > 0) {
	                handleNontraversedFolders(provider, nontraversedFolders, Policy.subMonitorFor(monitor, 10));
	            }
	        } finally {
	        	Job.getJobManager().endRule(rule);
	        }
        } finally {
            monitor.done();
        }
    }
	
	/**
     * Handle any non-traversed (depth-zero) folders that were in the logical modle that primed this operation.
     * @param provider the repository provider associated with the project containing the folders
     * @param nontraversedFolders the folders
     * @param monitor a progress monitor
     */
    protected void handleNontraversedFolders(CVSTeamProvider provider, IResource[] nontraversedFolders, IProgressMonitor monitor) throws CVSException {
        // Default is do nothing
    }

    /**
	 * Return the taskname to be shown in the progress monitor while operating
	 * on the given provider.
	 * @param provider the provider being processed
	 * @return the taskname to be shown in the progress monitor
	 */
	protected abstract String getTaskName(CVSTeamProvider provider);

	/**
	 * Retgurn the scheduling rule to be obtained before work
	 * begins on the given provider. By default, it is the provider's project.
	 * This can be changed by subclasses.
	 * @param provider
	 * @return
	 */
	protected ISchedulingRule getSchedulingRule(CVSTeamProvider provider) {
		return provider.getProject();
	}

	/*
	 * Helper method. Return a Map mapping provider to a list of resources
	 * shared with that provider.
	 */
	Map getProviderTraversalMapping(IProgressMonitor monitor) throws CoreException {
		Map result = new HashMap();
		ResourceMapping[] mappings = getScope().getMappings();
        for (int j = 0; j < mappings.length; j++) {
            ResourceMapping mapping = mappings[j];
            IProject[] projects = mapping.getProjects();
            ResourceTraversal[] traversals = getScope().getTraversals(mapping);
            for (int k = 0; k < projects.length; k++) {
                IProject project = projects[k];
                RepositoryProvider provider = RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId());
                if (provider != null) {
                    TraversalMapEntry entry = (TraversalMapEntry)result.get(provider);
                    if (entry == null) {
                        entry = new TraversalMapEntry(provider);
                        result.put(provider, entry);
                    }
                    entry.add(traversals);
                } 
            }
        }
		return result;
	}

    /**
     * Return the resource mapping context that is to be used by this operation.
     * By default, <code>null</code> is returned but subclasses may override
     * to provide a specific context.
     * @return the resource mapping context for this operation
     */
	protected ResourceMappingContext getResourceMappingContext() {
        return ResourceMappingContext.LOCAL_CONTEXT;
    }

    /**
	 * Execute the operation on the resources for the given provider.
	 * @param provider the provider for the project that contains the resources
	 * @param resources the resources to be operated on
	 * @param recurse whether the operation is deep or shallow
	 * @param monitor a progress monitor
	 * @throws CVSException
	 * @throws InterruptedException
	 */
	protected abstract void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException;

    /**
     * Return the local options for this operation including the 
     * option to provide the requested traversal.
     * @param recurse deep or shallow
     * @return the local options for the operation
     */
    protected LocalOption[] getLocalOptions(boolean recurse) {
        if (!recurse) {
            return new LocalOption[] { Command.DO_NOT_RECURSE };
        }
        return Command.NO_LOCAL_OPTIONS;
    }
    
	protected ICVSResource[] getCVSArguments(IResource[] resources) {
		ICVSResource[] cvsResources = new ICVSResource[resources.length];
		for (int i = 0; i < cvsResources.length; i++) {
			cvsResources[i] = CVSWorkspaceRoot.getCVSResourceFor(resources[i]);
		}
		return cvsResources;
	}
	
	/*
	 * Get the arguments to be passed to a commit or update
	 */
	protected String[] getStringArguments(IResource[] resources) throws CVSException {
		List arguments = new ArrayList(resources.length);
		for (int i=0;i<resources.length;i++) {
			IPath cvsPath = resources[i].getFullPath().removeFirstSegments(1);
			if (cvsPath.segmentCount() == 0) {
				arguments.add(Session.CURRENT_LOCAL_FOLDER);
			} else {
				arguments.add(cvsPath.toString());
			}
		}
		return (String[])arguments.toArray(new String[arguments.size()]);
	}
	
	protected ICVSRepositoryLocation getRemoteLocation(CVSTeamProvider provider) throws CVSException {
		CVSWorkspaceRoot workspaceRoot = provider.getCVSWorkspaceRoot();
		return workspaceRoot.getRemoteLocation();
	}
	
	protected ICVSFolder getLocalRoot(CVSTeamProvider provider) throws CVSException {
		CVSWorkspaceRoot workspaceRoot = provider.getCVSWorkspaceRoot();
		return workspaceRoot.getLocalRoot();
	}

	/**
	 * Update the workspace subscriber for an update operation performed on the 
	 * given resources. After an update, the remote tree is flushed in order
	 * to ensure that stale incoming additions are removed. This need only
	 * be done for folders. At the time of writing, all update operations
	 * are deep so the flush is deep as well.
	 * @param provider the provider (project) for all the given resources
	 * @param resources the resources that were updated
	 * @param recurse 
	 * @param monitor a progress monitor
	 */
	protected void updateWorkspaceSubscriber(CVSTeamProvider provider, ICVSResource[] resources, boolean recurse, IProgressMonitor monitor) {
		CVSWorkspaceSubscriber s = CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber();
		monitor.beginTask(null, 100 * resources.length);
		for (int i = 0; i < resources.length; i++) {
			ICVSResource resource = resources[i];
			if (resource.isFolder()) {
				try {
					s.updateRemote(provider, (ICVSFolder)resource, recurse, Policy.subMonitorFor(monitor, 100));
				} catch (TeamException e) {
					// Just log the error and continue
					CVSUIPlugin.log(e);
				}
			} else {
				monitor.worked(100);
			}
		}
	}
	
	/* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#isKeepOneProgressServiceEntry()
     */
    public boolean isKeepOneProgressServiceEntry() {
        // Keep the last repository provider operation in the progress service
        return true;
    }
    
    /* (non-Javadoc)
     * @see org.eclipse.team.ui.TeamOperation#getGotoAction()
     */
    protected IAction getGotoAction() {
        return getShowConsoleAction();
    }
    
    /**
     * Return the root resources for all the traversals of this operation.
     * This method may only be invoked after {@link #buildScope(IProgressMonitor) }.
     * @return the root resources for all the traversals of this operation
     * @throws CoreException 
     */
    protected IResource[] getTraversalRoots() {
        List result = new ArrayList();
        ResourceTraversal[] traversals = getTraversals();
        for (int i = 0; i < traversals.length; i++) {
            ResourceTraversal traversal = traversals[i];
            result.addAll(Arrays.asList(traversal.getResources()));
        }
        return (IResource[]) result.toArray(new IResource[result.size()]);
    }
    
    /**
     * Return the traversals that will be used by this operation.
     * This method can only be called after {@link #buildScope(IProgressMonitor) }.
     * @return the traversals that will be used by this operation
     * @throws CoreException
     */
    public ResourceTraversal[] getTraversals() {
        return getScope().getTraversals();
    }
    
    public boolean consultModelsForMappings() {
    	return true;
    }

	public ResourceMapping[] getSelectedMappings() {
		return selectedMappings;
	}

	public ISynchronizationScope getScope() {
		return manager.getScope();
	}

	public ISynchronizationScopeManager getScopeManager() {
		return manager;
	}
}
