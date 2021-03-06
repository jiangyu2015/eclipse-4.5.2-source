/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.actions;
 
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberScopeManager;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceModelParticipant;
import org.eclipse.team.internal.ccvs.ui.mappings.WorkspaceSubscriberContext;
import org.eclipse.team.internal.ccvs.ui.subscriber.WorkspaceSynchronizeParticipant;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.*;

/**
 * Action to initiate a CVS workspace synchronize
 */
public class SyncAction extends WorkspaceTraversalAction {
	
	public void execute(IAction action) throws InvocationTargetException {
		// First, see if there is a single file selected
		if (isOpenEditorForSingleFile()) {
			IFile file = getSelectedFile();
			if (file != null && isOKToShowSingleFile(file)) {
				showSingleFileComparison(getShell(), CVSProviderPlugin.getPlugin().getCVSWorkspaceSubscriber(), file, getTargetPage());
				return;
			}
		}
		if (isShowModelSync()) {
			ResourceMapping[] mappings = getCVSResourceMappings();
			if (mappings.length == 0)
				return;
			SubscriberScopeManager manager = WorkspaceSubscriberContext.createWorkspaceScopeManager(mappings, true, CommitAction.isIncludeChangeSets(getShell(), CVSUIMessages.SyncAction_1));
			WorkspaceSubscriberContext context = WorkspaceSubscriberContext.createContext(manager, ISynchronizationContext.THREE_WAY);
			WorkspaceModelParticipant participant = new WorkspaceModelParticipant(context);
			TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			participant.run(getTargetPart());
		} else {
	        IResource[] resources = getResourcesToCompare(getWorkspaceSubscriber());
			if (resources == null || resources.length == 0) return;
			// First check if there is an existing matching participant
			WorkspaceSynchronizeParticipant participant = (WorkspaceSynchronizeParticipant)SubscriberParticipant.getMatchingParticipant(WorkspaceSynchronizeParticipant.ID, resources);
			// If there isn't, create one and add to the manager
			if (participant == null) {
                ISynchronizeScope scope;
                if (includesAllCVSProjects(resources)) {
                    scope = new WorkspaceScope();
                } else {
                    IWorkingSet[] sets = getSelectedWorkingSets();            
                    if (sets != null) {
                        scope = new WorkingSetScope(sets);
                    } else {
                        scope = new ResourceScope(resources);
                    }
                }
                participant = new WorkspaceSynchronizeParticipant(scope);
				TeamUI.getSynchronizeManager().addSynchronizeParticipants(new ISynchronizeParticipant[] {participant});
			}
			participant.refresh(resources, getTargetPart().getSite());
		}
	}

	private static boolean isShowModelSync() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_ENABLE_MODEL_SYNC);
	}
	
	private static boolean isOpenEditorForSingleFile() {
		return CVSUIPlugin.getPlugin().getPreferenceStore().getBoolean(ICVSUIConstants.PREF_OPEN_COMPARE_EDITOR_FOR_SINGLE_FILE);
	}

	private IWorkingSet[] getSelectedWorkingSets() {
        ResourceMapping[] mappings = getCVSResourceMappings();
        List sets = new ArrayList();
        for (int i = 0; i < mappings.length; i++) {
            ResourceMapping mapping = mappings[i];
            if (mapping.getModelObject() instanceof IWorkingSet) {
                IWorkingSet set = (IWorkingSet) mapping.getModelObject();
                sets.add(set);
            } else {
                return null;
            }
        }
        if (sets.isEmpty())
            return null;
        return (IWorkingSet[]) sets.toArray(new IWorkingSet[sets.size()]);
    }

    private boolean includesAllCVSProjects(IResource[] resources) {
        // First, make sure all the selected thinsg are projects
        for (int i = 0; i < resources.length; i++) {
            IResource resource = resources[i];
            if (resource.getType() != IResource.PROJECT)
                return false;
        }
        IProject[] cvsProjects = getAllCVSProjects();
        return cvsProjects.length == resources.length;
    }

    private IProject[] getAllCVSProjects() {
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        Set cvsProjects = new HashSet();
        for (int i = 0; i < projects.length; i++) {
            IProject project = projects[i];
            if (RepositoryProvider.isShared(project) && RepositoryProvider.getProvider(project, CVSProviderPlugin.getTypeId()) != null) {
                cvsProjects.add(project);
            }
        }
        return (IProject[]) cvsProjects.toArray(new IProject[cvsProjects.size()]);
    }

    /**
     * Return whether it is OK to open the selected file directly in a compare editor.
     * It is not OK to show the single file if the file is part of a logical model element
     * that spans files.
     * @param file the file
     * @return whether it is OK to open the selected file directly in a compare editor
     */
    public static boolean isOKToShowSingleFile(IFile file) {
		if (!isShowModelSync())
			return true;
		IModelProviderDescriptor[] descriptors = ModelProvider.getModelProviderDescriptors();
		for (int i = 0; i < descriptors.length; i++) {
			IModelProviderDescriptor descriptor = descriptors[i];
			try {
				IResource[] resources = descriptor.getMatchingResources(new IResource[] { file });
				if (resources.length > 0) {
					ModelProvider provider = descriptor.getModelProvider();
					// Technically, we should us the remote context to determine if multiple resources are involved.
					// However, we do not have a progress monitor so we'll just use a local context since,
					// it is unlikely that a model element will consist of one file locally but multiple files remotely
					ResourceMapping[] mappings = provider.getMappings(file, ResourceMappingContext.LOCAL_CONTEXT, null);
					for (int j = 0; j < mappings.length; j++) {
						ResourceMapping mapping = mappings[j];
						ResourceTraversal[] traversals = mapping.getTraversals(ResourceMappingContext.LOCAL_CONTEXT, null);
						for (int k = 0; k < traversals.length; k++) {
							ResourceTraversal traversal = traversals[k];
							IResource[] tResources = traversal.getResources();
							for (int index = 0; index < tResources.length; index++) {
								IResource tr = tResources[index];
								if (!tr.equals(file))
									return false;
							}
						}
					}
				}
			} catch (CoreException e) {
				CVSUIPlugin.log(e);
			}
		}
		return true;
	}
    
    /**
	 * Refresh the subscriber directly and show the resulting synchronization state in a compare editor. If there
	 * is no difference the user is prompted.
	 * 
	 * @param resources the file to refresh and compare
	 */
	public static void showSingleFileComparison(final Shell shell, final Subscriber subscriber, final IResource resource, final IWorkbenchPage page) {
		try {
			PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {	
						subscriber.refresh(new IResource[]{resource}, IResource.DEPTH_ZERO, monitor);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				}
			});
			final SyncInfo info = subscriber.getSyncInfo(resource);
			if (info == null) return;
			shell.getDisplay().syncExec(new Runnable() {
				public void run() {
					if (info.getKind() == SyncInfo.IN_SYNC) {
						MessageDialog.openInformation(shell, CVSUIMessages.SyncAction_noChangesTitle, CVSUIMessages.SyncAction_noChangesMessage); // 
					} else {
						SyncInfoCompareInput input = new SyncInfoCompareInput(subscriber.getName(), info);
                        OpenInCompareAction.openCompareEditor(input, page);
					}
				}
			});
		} catch (InvocationTargetException e) {
			Utils.handle(e);
		} catch (InterruptedException e) {
		} catch (TeamException e) {
			Utils.handle(e);
		}
	}

	public static boolean isSingleFile(IResource[] resources) {
		return resources.length == 1 && resources[0].getType() == IResource.FILE;
	}
	
	/**
	 * Enable for resources that are managed (using super) or whose parent is a
	 * CVS folder.
	 * 
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForCVSResource(org.eclipse.team.internal.ccvs.core.ICVSResource)
	 */
	protected boolean isEnabledForCVSResource(ICVSResource cvsResource) throws CVSException {
		return (super.isEnabledForCVSResource(cvsResource) || (cvsResource.getParent().isCVSFolder() && !cvsResource.isIgnored()));
	}
	
	public String getId() {
		return ICVSUIConstants.CMD_SYNCHRONIZE;
	}

	
	public boolean isEnabled() {
		if(super.isEnabled()){
			return true;
		}
		IWorkingSet[] sets = getSelectedWorkingSets();
		// empty selection will not be considered
		if(sets == null || sets.length == 0){
			return false;
		}
		
		Set projects = getProjects(sets);
		
		boolean existsProjectToSynchronize = false;
		for (Iterator it = projects.iterator(); it.hasNext();) {
			IProject project = (IProject) it.next();
			RepositoryProvider provider = RepositoryProvider.getProvider(project);
			if(provider != null){
				existsProjectToSynchronize = true;
				//we handle only CVS repositories
				if(!CVSProviderPlugin.getTypeId().equals(provider.getID())){
					return false;
				}
			}
		}
		
		return existsProjectToSynchronize;
	}

	private Set getProjects(IWorkingSet[] sets) {
		Set projects = new HashSet();
		
		if(sets == null) 
			return projects;
		
		for (int i = 0; i < sets.length; i++) {
			IAdaptable ad[] = sets[i].getElements();
			if (ad != null) {
				for (int j = 0; j < ad.length; j++) {
					IResource resource = (IResource) ad[j]
							.getAdapter(IResource.class);
					if (resource != null) {
						projects.add(resource.getProject());
					}
				}
			}
		}
		
		return projects;
	}
}
