/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.views.navigator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.MoveProjectAction;
import org.eclipse.ui.actions.MoveResourceAction;

/**
 * The ResourceNavigatorMoveAction is a resource move that aso updates the navigator
 * to show the result of the move.
 * It also delegates to MoveProjectAction as needed.
 *
 * @since 2.0
 * @deprecated as of 3.5, use the Common Navigator Framework classes instead
 */
@Deprecated
public class ResourceNavigatorMoveAction extends MoveResourceAction {
    private StructuredViewer viewer;

    private MoveProjectAction moveProjectAction;

    /**
     * Create a ResourceNavigatorMoveAction and use the supplied viewer to update the UI.
     * @param shell Shell
     * @param structureViewer StructuredViewer
     */
    public ResourceNavigatorMoveAction(Shell shell,
            StructuredViewer structureViewer) {
        super(shell);
        PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
                INavigatorHelpContextIds.RESOURCE_NAVIGATOR_MOVE_ACTION);
        this.viewer = structureViewer;
        this.moveProjectAction = new MoveProjectAction(shell);
    }

    /* (non-Javadoc)
     * Method declared on IAction.
     */
    @Override
	public void run() {
        if (moveProjectAction.isEnabled()) {
            moveProjectAction.run();
            return;
        }

        super.run();
        List destinations = getDestinations();
        if (destinations != null && destinations.isEmpty() == false) {
            IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
            List resources = new ArrayList();
            Iterator iterator = destinations.iterator();

            while (iterator.hasNext()) {
                IResource newResource = root
                        .findMember((IPath) iterator.next());
                if (newResource != null) {
					resources.add(newResource);
				}
            }

            this.viewer.setSelection(new StructuredSelection(resources), true);
        }

    }

    @Override
	protected boolean updateSelection(IStructuredSelection selection) {
        moveProjectAction.selectionChanged(selection);
        return super.updateSelection(selection)
                || moveProjectAction.isEnabled();
    }

}
