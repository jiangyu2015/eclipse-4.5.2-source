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
package org.eclipse.team.internal.ccvs.ui.actions;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.mapping.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialogWithToggle;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.mapping.provider.SynchronizationScopeManager;
import org.eclipse.team.internal.ccvs.core.mapping.CVSActiveChangeSetCollector;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.wizards.CommitWizard;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.PlatformUI;

/**
 * Action for checking in files to a CVS provider.
 * Prompts the user for a release comment.
 */
public class CommitAction extends WorkspaceTraversalAction {
	
	private final class CommitScopeManager extends SynchronizationScopeManager {
		private boolean includeChangeSets;

		private CommitScopeManager(ResourceMapping[] mappings, ResourceMappingContext context, boolean models) {
			super("", mappings, context, models); //$NON-NLS-1$
			includeChangeSets = isIncludeChangeSets(getShell(), CVSUIMessages.CommitAction_2);
		}

		protected ResourceTraversal[] adjustInputTraversals(ResourceTraversal[] traversals) {
			if (includeChangeSets)
				return ((CVSActiveChangeSetCollector)CVSUIPlugin.getPlugin().getChangeSetManager()).adjustInputTraversals(traversals);
			return super.adjustInputTraversals(traversals);
		}
	}

	/*
	 * @see CVSAction#execute(IAction)
	 */
	public void execute(IAction action) throws InvocationTargetException, InterruptedException {
		final ResourceTraversal[][] traversals = new ResourceTraversal[][] { null };
		PlatformUI.getWorkbench().getProgressService().busyCursorWhile(new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
	            try {
	            	monitor.beginTask(CVSUIMessages.CommitAction_0, 100);
	                traversals[0] = getTraversals(Policy.subMonitorFor(monitor, 80));
	            } catch (CoreException e) {
	                throw new InvocationTargetException(e);
	            } finally {
	            	monitor.done();
	            }
            }
        });
        run(new IRunnableWithProgress() {
            public void run(IProgressMonitor monitor)
                    throws InvocationTargetException, InterruptedException {
	            try {
	            	CommitWizard.run(getTargetPart(), getShell(), traversals[0]);
	            } catch (CoreException e) {
	                throw new InvocationTargetException(e);
	            }
            }
        }, false, PROGRESS_BUSYCURSOR);
	}
    
	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getErrorTitle()
	 */
	protected String getErrorTitle() {
		return CVSUIMessages.CommitAction_commitFailed; 
	}

	/**
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForUnmanagedResources()
	 */
	protected boolean isEnabledForUnmanagedResources() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction#isEnabledForNonExistantResources()
	 */
	protected boolean isEnabledForNonExistantResources() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.actions.CVSAction#getId()
	 */
	public String getId() {
		return ICVSUIConstants.CMD_COMMIT;
	}
	
	protected SynchronizationScopeManager getScopeManager() {
		return new CommitScopeManager(getCVSResourceMappings(), getResourceMappingContext(), true);
	}

	public static boolean isIncludeChangeSets(final Shell shell, final String message) {
		if (CVSUIPlugin.getPlugin().getChangeSetManager().getSets().length == 0)
			return false;
		final IPreferenceStore store = CVSUIPlugin.getPlugin().getPreferenceStore();
		final String option = store.getString(ICVSUIConstants.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT);
		if (option.equals(MessageDialogWithToggle.ALWAYS))
			return true; // no, always switch
		
		if (option.equals(MessageDialogWithToggle.NEVER))
			return false; // no, never switch
		
	    // Ask the user whether to switch
		final int[] result = new int[] { 0 };
		Utils.syncExec(new Runnable() {
			public void run() {
				final MessageDialogWithToggle m = MessageDialogWithToggle.openYesNoQuestion(
						shell,
						CVSUIMessages.CommitAction_1, 
						message, 
						CVSUIMessages.ShowAnnotationOperation_4,   
						false /* toggle state */,
						store,
						ICVSUIConstants.PREF_INCLUDE_CHANGE_SETS_IN_COMMIT);
				
				result[0] = m.getReturnCode();
			}
		}, shell);
		
		switch (result[0]) {
		// yes
		case IDialogConstants.YES_ID:
		case IDialogConstants.OK_ID :
		    return true;
		// no
		case IDialogConstants.NO_ID :
		    return false;
		}
		return false;
	}
}
