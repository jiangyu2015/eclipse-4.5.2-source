/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.ui.memory;

import java.math.BigInteger;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.MemoryByte;

/**
 * A memory rendering that can be repositioned.  Reposition behavior is rendering
 * specific.  Typically, reposition means that the rendering should move its 
 * cursor/current selection to the given address. However, clients may define 
 * its reposition behavior that is suitable for the rendering.
 * <p>
 * Clients may implement this interface.
 * </p>
 * @since 3.3
 *
 */
public interface IRepositionableMemoryRendering extends IMemoryRendering{
	
	/**
	 * Returns the currently selected address of this rendering or <code>null</code> if none
	 * @return the currently selected address of this rendering or <code>null</code> if none
	 */
	public BigInteger getSelectedAddress();
	
	/**
	 * Returns the currently selected content as <code>MemoryByte</code> array.  
	 * Returns an empty array if there is no selection.
	 * @return the currently selected as <code>MemoryByte</code> array or empty if there is
	 * no selection.
	 */
	public MemoryByte[] getSelectedAsBytes();

	/**
	 * Position the rendering to the given address.
	 * 
	 * @param address the address to go to
	 * @throws DebugException when there is a problem repositioning the rendering to the 
	 * address
	 */
	public void goToAddress(BigInteger address) throws DebugException ;
}
