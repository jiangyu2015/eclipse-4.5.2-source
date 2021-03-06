/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.ui.IWorkbenchPart;

public class ClipboardDiffOperation extends DiffOperation {

	private static final Object DESTINATION_CLIPBOARD = CVSUIMessages.ClipboardDiffOperation_Clipboard;

    final ByteArrayOutputStream os = new ByteArrayOutputStream();

	public ClipboardDiffOperation(IWorkbenchPart part, ResourceMapping[] mappings, LocalOption[] options, boolean isMultiPatch, boolean includeFullPathInformation, IPath patchRoot) {
		super(part, mappings, options, isMultiPatch, includeFullPathInformation, patchRoot, DESTINATION_CLIPBOARD);
	}

	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
	    super.execute(monitor);
	    
	    if (os.size() == 0 ||
	    	(!patchHasContents && !patchHasNewFiles)) {
            reportEmptyDiff();
        } else {
            copyToClipboard(os);
        }
     }
	
	 private void copyToClipboard(final ByteArrayOutputStream baos) {
        getShell().getDisplay().syncExec(new Runnable() {
        	public void run() {
        		TextTransfer plainTextTransfer = TextTransfer.getInstance();
        		Clipboard clipboard = new Clipboard(getShell().getDisplay());		
        		clipboard.setContents(
        			new String[]{baos.toString()}, 
        			new Transfer[]{plainTextTransfer});	
        		clipboard.dispose();
        	}
        });
	 }

	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.ui.operations.DiffOperation#openStream()
	 */
	protected PrintStream openStream() {
		return new PrintStream(os);
	}
}
