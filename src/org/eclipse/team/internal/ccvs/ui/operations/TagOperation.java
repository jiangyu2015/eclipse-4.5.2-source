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
package org.eclipse.team.internal.ccvs.ui.operations;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.*;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.actions.TagAction;
import org.eclipse.team.internal.ccvs.ui.tags.TagSource;
import org.eclipse.ui.IWorkbenchPart;

public class TagOperation extends RepositoryProviderOperation implements ITagOperation {

	private Set localOptions = new HashSet();
	private CVSTag tag;

	public TagOperation(IWorkbenchPart part, ResourceMapping[] mappers) {
		super(part, mappers);
	}

	public CVSTag getTag() {
		return tag;
	}

	public void setTag(CVSTag tag) {
		this.tag = tag;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#execute(org.eclipse.team.internal.ccvs.core.CVSTeamProvider, org.eclipse.core.resources.IResource[], org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void execute(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor monitor) throws CVSException, InterruptedException {
		IStatus status = tag(provider, resources, recurse, monitor);
		collectStatus(status);
	}

	/**
	 * Override to dislay the number of tag operations that succeeded
	 */
	protected String getErrorMessage(IStatus[] problems, int operationCount) {
		// We accumulated 1 status per resource above.
		if(operationCount == 1) {
			return CVSUIMessages.TagAction_tagProblemsMessage; 
		} else {
			return NLS.bind(CVSUIMessages.TagAction_tagProblemsMessageMultiple, new String[] { Integer.toString(operationCount - problems.length), Integer.toString(problems.length) });
		}
	}
	
	/** 
	 * Tag the resources in the CVS repository with the given tag.
	 * 
	 * The returned IStatus will be a status containing any errors or warnings.
	 * If the returned IStatus is a multi-status, the code indicates the severity.
	 * Possible codes are:
	 *    CVSStatus.OK - Nothing to report
	 *    CVSStatus.SERVER_ERROR - The server reported an error
	 *    any other code - warning messages received from the server
	 * @param recurse 
	 */
	public IStatus tag(CVSTeamProvider provider, IResource[] resources, boolean recurse, IProgressMonitor progress) throws CVSException {
						
		LocalOption[] commandOptions = (LocalOption[])localOptions.toArray(new LocalOption[localOptions.size()]);
        if (recurse) {
            commandOptions = Command.DO_NOT_RECURSE.removeFrom(commandOptions);
        } else {
            commandOptions = Command.RECURSE.removeFrom(commandOptions);
            commandOptions = Command.DO_NOT_RECURSE.addTo(commandOptions);
        }
				
		// Build the arguments list
		String[] arguments = getStringArguments(resources);

		// Execute the command
		CVSWorkspaceRoot root = provider.getCVSWorkspaceRoot();
		Session s = new Session(root.getRemoteLocation(), root.getLocalRoot());
		progress.beginTask(null, 100);
		try {
			// Opening the session takes 20% of the time
			s.open(Policy.subMonitorFor(progress, 20), true /* open for modification */);
			return Command.TAG.execute(s,
				Command.NO_GLOBAL_OPTIONS,
				commandOptions,
				tag,
				arguments,
				null,
				Policy.subMonitorFor(progress, 80));
		} finally {
			s.close();
			progress.done();
		}
	}
	
	public void addLocalOption(LocalOption option)  {
		localOptions.add(option);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#moveTag()
	 */
	public void moveTag() {
		addLocalOption(Tag.FORCE_REASSIGNMENT);	
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#recurse()
	 */
	public void doNotRecurse() {
		addLocalOption(Command.DO_NOT_RECURSE);
	}

	protected  String getTaskName() {
		return CVSUIMessages.TagFromWorkspace_taskName; 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.RepositoryProviderOperation#getTaskName(org.eclipse.team.internal.ccvs.core.CVSTeamProvider)
	 */
	protected String getTaskName(CVSTeamProvider provider) {
		return NLS.bind(CVSUIMessages.TagOperation_0, new String[] { provider.getProject().getName() }); 
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.CVSOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		super.execute(monitor);
		if (!errorsOccurred()) {
			try {
				TagAction.broadcastTagChange(getCVSResources(), getTag());
			} catch (InvocationTargetException e) {
				throw CVSException.wrapException(e);
			}
		}
	}

    private ICVSResource[] getCVSResources() {
        IResource[] resources = getTraversalRoots();
        ICVSResource[] cvsResources = new ICVSResource[resources.length];
        for (int i = 0; i < resources.length; i++) {
            cvsResources[i] = CVSWorkspaceRoot.getCVSResourceFor(resources[i]);
        }
        return cvsResources;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#getTagSource()
     */
    public TagSource getTagSource() {
       return TagSource.create(getProjects());
    }

    private IProject[] getProjects() {
		ResourceMapping[] mappings = getSelectedMappings();
		Set projects = new HashSet();
		for (int i = 0; i < mappings.length; i++) {
			ResourceMapping mapping = mappings[i];
			projects.addAll(Arrays.asList(mapping.getProjects()));
		}
		return (IProject[]) projects.toArray(new IProject[projects.size()]);
	}

	protected boolean isReportableError(IStatus status) {
        return super.isReportableError(status)
        	|| status.getCode() == CVSStatus.TAG_ALREADY_EXISTS;
    }

    /* (non-Javadoc)
     * @see org.eclipse.team.internal.ccvs.ui.operations.ITagOperation#isEmpty()
     */
    public boolean isEmpty() {
        return getSelectedMappings().length == 0;
    }
}
