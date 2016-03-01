/*******************************************************************************
 * Copyright (c) 2014 Rapicorp Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Rapicorp Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.product;

import java.io.PrintWriter;
import org.eclipse.pde.internal.core.iproduct.ICSSInfo;
import org.eclipse.pde.internal.core.iproduct.IProductModel;
import org.w3c.dom.*;

public class CSSInfo extends ProductObject implements ICSSInfo {

	private static final long serialVersionUID = 1L;
	private String fFilePath;

	public CSSInfo(IProductModel model) {
		super(model);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.ICSSInfo#setFilePath(java.lang.String)
	 */
	public void setFilePath(String text) {
		String old = fFilePath;
		fFilePath = text;
		if (isEditable()) {
			firePropertyChanged(P_CSSFILEPATH, old, fFilePath);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.ICSSInfo#getFilePath()
	 */
	public String getFilePath() {
		return fFilePath;
	}


	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.product.ProductObject#write(java.lang.String, java.io.PrintWriter)
	 */
	public void write(String indent, PrintWriter writer) {
		writer.println(indent + "<cssInfo>"); //$NON-NLS-1$
		if (fFilePath != null && fFilePath.length() > 0) {
			writer.println(indent + "   <file path=\"" + getWritableString(fFilePath.trim()) + "\"/>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		writer.println(indent + "</cssInfo>"); //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.iproduct.IProductObject#parse(org.w3c.dom.Node)
	 */
	public void parse(Node node) {
		NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (child.getNodeName().equals("file")) { //$NON-NLS-1$
					fFilePath = ((Element) child).getAttribute("path"); //$NON-NLS-1$
				}
			}
		}
	}
}
