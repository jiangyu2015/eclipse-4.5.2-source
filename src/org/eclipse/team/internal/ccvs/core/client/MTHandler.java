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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.CVSException;

public class MTHandler extends ResponseHandler {

	private String nextLine;
	private boolean isLineAvailable;
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.internal.ccvs.core.client.ResponseHandler#getInstance()
	 */
	ResponseHandler getInstance() {
		return new MTHandler();
	}

	/**
	 * @see ResponseHandler#getResponseID()
	 */
	public String getResponseID() {
		return "MT"; //$NON-NLS-1$
	}

	/**
	 * @see ResponseHandler#handle(Session, String, IProgressMonitor)
	 */
	public void handle(Session session, String argument, IProgressMonitor monitor)
		throws CVSException {
		
		// If there was a line available from before, clear it
		if (isLineAvailable()) {
			startNextLine();
		}
		
		if (argument.charAt(0) == '+') {
			// Reset any previously accumulated text
			startNextLine();
		} else if (argument.charAt(0) == '-') {
			// Mark the line as available in case there was no trailing newline
			if (nextLine != null) {
				isLineAvailable = true;
			}
		} else {
			// Extract the tag and text from the line
			String tag;
			String text;
			int spaceIndex = argument.indexOf(' ');
			if (spaceIndex == -1) {
				tag = argument;
				text = null;
			} else {
				tag = argument.substring(0, spaceIndex);
				text = argument.substring(spaceIndex + 1);
			}
			
			// Accumulate the line and indicate if its available for use
			if (tag.equals("newline")) { //$NON-NLS-1$
				isLineAvailable = true;
			} else if (text != null) {
				// Reset the previous line if required
				if (isLineAvailable()) {
					startNextLine();
				}
				// Accumulate the line
				if (nextLine == null) {
					nextLine = text;
				} else {
					// The text from the sevrver contains spaces when appropriate so just append
					nextLine = nextLine + text;
				}
			}
		}
	}
	
	/**
	 * Check if there is a line available. If there is, it should be fetched with
	 * getLine() immediatly before the next MT response is processed.
	 */
	public boolean isLineAvailable() {
		return isLineAvailable;
	}
	
	/**
	 * Get the available line. This purges the line from the handler
	 */
	public String getLine() {
		return nextLine;
	}
	
	private void startNextLine() {
		isLineAvailable = false;
		nextLine = null;
	}
}
