/*******************************************************************************
 * Copyright (c) 2008, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.pde.api.tools.internal.model.MethodKey;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;

/**
 * Detects references to 'no reference' fields.
 * 
 * @since 1.1
 */
public class IllegalFieldReferenceDetector extends AbstractProblemDetector {

	/**
	 * Map of {@link org.eclipse.pde.api.tools.internal.model.MethodKey} to
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor}
	 */
	private Map<MethodKey, IFieldDescriptor> fIllegalFields = new HashMap<MethodKey, IFieldDescriptor>();

	private Map<String, String> fIllegalTypes = new HashMap<String, String>();

	/**
	 * Map of
	 * {@link org.eclipse.pde.api.tools.internal.provisional.descriptors.IFieldDescriptor}
	 * to associated component IDs
	 */
	private Map<IFieldDescriptor, String> fFieldComponents = new HashMap<IFieldDescriptor, String>();

	/**
	 * Adds the given field as not to be referenced
	 * 
	 * @param field a field that is marked no reference
	 * @param componentId the component the type is located in
	 */
	void addIllegalField(IFieldDescriptor field, String componentId) {
		fIllegalFields.put(new MethodKey(field.getEnclosingType().getQualifiedName(), field.getName(), null, true), field);
		fFieldComponents.put(field, componentId);
	}

	/**
	 * Adds an {@link IReferenceTypeDescriptor} that is reference-restricted
	 * 
	 * @param type the {@link IReferenceTypeDescriptor} that is restricted
	 * @param componentid the id of the API the reference type comes from
	 * 
	 * @since 1.0.400
	 */
	void addIllegalType(IReferenceTypeDescriptor type, String componentid) {
		fIllegalTypes.put(type.getQualifiedName(), componentid);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector
	 * #considerReference(org.eclipse.pde.api.tools.internal.provisional.model.
	 * IReference)
	 */
	@Override
	public boolean considerReference(IReference reference) {
		MethodKey key = new MethodKey(reference.getReferencedTypeName(), reference.getReferencedMemberName(), reference.getReferencedSignature(), true);
		if ((super.considerReference(reference) && fIllegalFields.containsKey(key)) || isEnclosedBy(reference.getReferencedTypeName(), fIllegalTypes.keySet())) {
			retainReference(reference);
			return true;
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.search.IApiProblemDetector
	 * #getReferenceKinds()
	 */
	@Override
	public int getReferenceKinds() {
		return IReference.REF_GETFIELD | IReference.REF_GETSTATIC | IReference.REF_PUTFIELD | IReference.REF_PUTSTATIC;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getProblemKind()
	 */
	@Override
	protected int getProblemKind() {
		return IApiProblem.ILLEGAL_REFERENCE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getSeverityKey()
	 */
	@Override
	protected String getSeverityKey() {
		return IApiProblemTypes.ILLEGAL_REFERENCE;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getProblemFlags
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected int getProblemFlags(IReference reference) {
		return IApiProblem.FIELD;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getElementType
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected int getElementType(IReference reference) {
		return IElementDescriptor.FIELD;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getMessageArgs
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected String[] getMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getResolvedReference();
		return new String[] {
				getSimpleTypeName(field),
				getSimpleTypeName(reference.getMember()), field.getName() };
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getQualifiedMessageArgs
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected String[] getQualifiedMessageArgs(IReference reference) throws CoreException {
		IApiField field = (IApiField) reference.getResolvedReference();
		return new String[] {
				getQualifiedTypeName(field),
				getQualifiedTypeName(reference.getMember()), field.getName() };
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#
	 * getSourceRange(org.eclipse.jdt.core.IType,
	 * org.eclipse.jface.text.IDocument,
	 * org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected Position getSourceRange(IType type, IDocument document, IReference reference) throws CoreException, BadLocationException {
		return getFieldNameRange((IApiField) reference.getResolvedReference(), document, reference);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.search.AbstractProblemDetector#isProblem
	 * (org.eclipse.pde.api.tools.internal.provisional.model.IReference)
	 */
	@Override
	protected boolean isProblem(IReference reference) {
		if (!super.isProblem(reference)) {
			return false;
		}
		String componentId = fFieldComponents.get(reference.getResolvedReference().getHandle());
		if (componentId != null) {
			return isReferenceFromComponent(reference, componentId);
		}
		// try to find the enclosing type that might be restricted
		StringTokenizer tokenizer = new StringTokenizer(reference.getReferencedTypeName(), "$"); //$NON-NLS-1$
		while (tokenizer.hasMoreTokens()) {
			componentId = fIllegalTypes.get(tokenizer.nextToken());
			if (componentId != null) {
				break;
			}
		}
		return isReferenceFromComponent(reference, componentId);
	}
}
