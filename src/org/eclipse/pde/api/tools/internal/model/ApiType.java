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
package org.eclipse.pde.api.tools.internal.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.internal.builder.Reference;
import org.eclipse.pde.api.tools.internal.builder.ReferenceExtractor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.builder.IReference;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IMemberDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiElement;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiField;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiMethod;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiType;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiTypeRoot;
import org.eclipse.pde.api.tools.internal.util.Signatures;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

import com.ibm.icu.text.MessageFormat;

/**
 * Base implementation of {@link IApiType}
 * 
 * @since 1.0.0
 * @noextend This class is not intended to be sub-classed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class ApiType extends ApiMember implements IApiType {

	private String fSuperclassName;
	private String[] fSuperInterfaceNames;
	private String fEnclosingTypeName;
	private String fSimpleName;

	private static final IApiMethod[] EMPTY_METHODS = new IApiMethod[0];
	private static final IApiField[] EMPTY_FIELDS = new IApiField[0];
	private static final IApiType[] EMPTY_TYPES = new IApiType[0];

	/*
	 * Use to tag fEnclosingMethodName and fEnclosingMethodSignature when there
	 * is no enclosing method but the EnclosingMethodAttribute is set (anonymous
	 * type in a field initializer).
	 */
	private static final String NO_ENCLOSING_METHOD = Util.EMPTY_STRING;

	/**
	 * Maps field name to field element.
	 */
	private Map<String, ApiField> fFields;
	/**
	 * Maps method name/signature pair to method element.
	 */
	private LinkedHashMap<MethodKey, ApiMethod> fMethods;

	/**
	 * Map of member type names to class file (or null until resolved)
	 */
	private Map<String, IApiTypeRoot> fMemberTypes;

	/**
	 * Cached descriptor
	 */
	private IReferenceTypeDescriptor fHandle;

	/**
	 * Cached superclass or <code>null</code>
	 */
	private IApiType fSuperclass;

	/**
	 * Cached super interfaces or <code>null</code>
	 */
	private IApiType[] fSuperInterfaces;

	/**
	 * The storage this type structure originated from
	 */
	private IApiTypeRoot fStorage;

	/**
	 * The signature of the enclosing method if this is a local type
	 */
	private String fEnclosingMethodSignature = null;

	/**
	 * The name of the method that encloses this type if it is local
	 */
	private String fEnclosingMethodName = null;

	/**
	 * If this is an anonymous class or not
	 */
	private boolean fAnonymous = false;

	/**
	 * If this is a local type or not (class defined in a method)
	 */
	private boolean fLocal = false;

	/**
	 * If this is a member type or not (class defined in a method)
	 */
	private boolean fMemberType = false;

	/**
	 * cached enclosing type once it has been successfully calculated
	 */
	private IApiType fEnclosingType = null;

	/**
	 * The method that encloses this type
	 */
	private IApiMethod fEnclosingMethod = null;

	/**
	 * Creates an API type. Note that if an API component is not specified, then
	 * some operations will not be available (navigating super types, member
	 * types, etc).
	 * 
	 * @param parent the parent {@link IApiElement} or <code>null</code> if none
	 * @param name the name of the type
	 * @param signature the signature of the type
	 * @param genericSig the generic signature of the type
	 * @param flags the flags for the type
	 * @param enclosingName
	 * @param storage the storage this content was generated from
	 */
	public ApiType(IApiElement parent, String name, String signature, String genericSig, int flags, String enclosingName, IApiTypeRoot storage) {
		super(parent, name, signature, genericSig, IApiElement.TYPE, flags);
		fEnclosingTypeName = enclosingName;
		fStorage = storage;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#
	 * extractReferences(int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public List<IReference> extractReferences(int referenceMask, IProgressMonitor monitor) throws CoreException {
		HashSet<Reference> references = new HashSet<Reference>();
		ReferenceExtractor extractor = new ReferenceExtractor(this, references, referenceMask);
		ClassReader reader = new ClassReader(((AbstractApiTypeRoot) fStorage).getContents());
		reader.accept(extractor, ClassReader.SKIP_FRAMES);
		return new LinkedList<IReference>(references);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getField
	 * (java.lang.String)
	 */
	@Override
	public IApiField getField(String name) {
		if (fFields != null) {
			return fFields.get(name);
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getFields()
	 */
	@Override
	public IApiField[] getFields() {
		if (fFields != null) {
			return fFields.values().toArray(new IApiField[fFields.size()]);
		}
		return EMPTY_FIELDS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#
	 * getPackageName()
	 */
	@Override
	public String getPackageName() {
		return getName().substring(0, getName().lastIndexOf('.'));
	}

	/**
	 * Used when building a type structure.
	 * 
	 * @param name method name
	 * @param signature method signature
	 * @param genericSig
	 * @param modifiers method modifiers
	 * @param exceptions names of thrown exceptions
	 */
	public ApiMethod addMethod(String name, String signature, String genericSig, int modifiers, String[] exceptions) {
		if (fMethods == null) {
			fMethods = new LinkedHashMap<MethodKey, ApiMethod>();
		}
		ApiMethod method = new ApiMethod(this, name, signature, genericSig, modifiers, exceptions);
		fMethods.put(new MethodKey(getName(), name, signature, true), method);
		return method;
	}

	/**
	 * Used when building a type structure.
	 * 
	 * @param name field name
	 * @param signature field signature
	 * @param genericSig
	 * @param modifiers field modifiers
	 * @param value constant value or <code>null</code> if none
	 */
	public ApiField addField(String name, String signature, String genericSig, int modifiers, Object value) {
		if (fFields == null) {
			fFields = new HashMap<String, ApiField>();
		}
		ApiField field = new ApiField(this, name, signature, genericSig, modifiers, value);
		fFields.put(name, field);
		return field;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMethod
	 * (java.lang.String, java.lang.String)
	 */
	@Override
	public IApiMethod getMethod(String name, String signature) {
		if (fMethods != null) {
			return fMethods.get(new MethodKey(getName(), name, signature, true));
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMethods
	 * ()
	 */
	@Override
	public IApiMethod[] getMethods() {
		if (fMethods != null) {
			return fMethods.values().toArray(new IApiMethod[fMethods.size()]);
		}
		return EMPTY_METHODS;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#
	 * getSuperInterfaceNames()
	 */
	@Override
	public String[] getSuperInterfaceNames() {
		return fSuperInterfaceNames;
	}

	public void setSuperInterfaceNames(String[] names) {
		fSuperInterfaceNames = names;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#
	 * getSuperInterfaces()
	 */
	@Override
	public IApiType[] getSuperInterfaces() throws CoreException {
		String[] names = getSuperInterfaceNames();
		if (names == null) {
			return EMPTY_TYPES;
		}
		if (fSuperInterfaces == null) {
			IApiType[] interfaces = new IApiType[names.length];
			for (int i = 0; i < interfaces.length; i++) {
				interfaces[i] = resolveType(names[i]);
				if (interfaces[i] == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ApiPlugin.REPORT_RESOLUTION_ERRORS, MessageFormat.format(Messages.ApiType_0, new Object[] {
							names[i], getName() }), null));
				}
			}
			fSuperInterfaces = interfaces;
		}
		return fSuperInterfaces;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSuperclass
	 * ()
	 */
	@Override
	public IApiType getSuperclass() throws CoreException {
		String name = getSuperclassName();
		if (name == null) {
			return null;
		}
		if (fSuperclass == null) {
			fSuperclass = resolveType(name);
			if (fSuperclass == null) {
				throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, ApiPlugin.REPORT_RESOLUTION_ERRORS, MessageFormat.format(Messages.ApiType_1, new Object[] {
						name, getName() }), null));
			}
		}
		return fSuperclass;
	}

	/**
	 * Resolves and returns the specified fully qualified type name or
	 * <code>null</code> if none.
	 * 
	 * @param qName qualified name
	 * @return type or <code>null</code>
	 * @throws CoreException if unable to resolve
	 */
	private IApiType resolveType(String qName) throws CoreException {
		if (getApiComponent() == null) {
			requiresApiComponent();
		}
		String packageName = Signatures.getPackageName(qName);
		IApiComponent[] components = getApiComponent().getBaseline().resolvePackage(getApiComponent(), packageName);
		IApiTypeRoot result = Util.getClassFile(components, qName);
		if (result != null) {
			return result.getStructure();
		}
		return null;
	}

	/**
	 * Throws an exception due to the fact an API component was not provided
	 * when this type was created and is now required to perform navigation or
	 * resolution.
	 * 
	 * @throws CoreException
	 */
	private void requiresApiComponent() throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, Messages.ApiType_2));
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#
	 * getSuperclassName()
	 */
	@Override
	public String getSuperclassName() {
		return fSuperclassName;
	}

	public void setSuperclassName(String superName) {
		fSuperclassName = superName;
	}

	public void setSimpleName(String simpleName) {
		fSimpleName = simpleName;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isAnnotation
	 * ()
	 */
	@Override
	public boolean isAnnotation() {
		return (getModifiers() & Opcodes.ACC_ANNOTATION) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isAnonymous
	 * ()
	 */
	@Override
	public boolean isAnonymous() {
		return fAnonymous;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isLocal()
	 */
	@Override
	public boolean isLocal() {
		return fLocal;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getTypeRoot
	 * ()
	 */
	@Override
	public IApiTypeRoot getTypeRoot() {
		return fStorage;
	}

	/**
	 * Used when building a type structure.
	 */
	public void setAnonymous() {
		fAnonymous = true;
	}

	/**
	 * Used when building a type structure.
	 */
	public void setMemberType() {
		fMemberType = true;
	}

	/**
	 * Used when building a type structure for pre-1.5 sources
	 */
	public void setLocal() {
		fLocal = true;
	}

	/**
	 * Sets the signature of the method that encloses this local type
	 * 
	 * @param signature the signature of the method.
	 * @see org.eclipse.jdt.core.Signature for more information
	 */
	public void setEnclosingMethodInfo(String name, String signature) {
		if (name != null) {
			fEnclosingMethodName = name;
		} else {
			fEnclosingMethodName = NO_ENCLOSING_METHOD;
		}
		if (signature != null) {
			fEnclosingMethodSignature = signature;
		} else {
			fEnclosingMethodSignature = NO_ENCLOSING_METHOD;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.model.IApiType#
	 * getEnclosingMethod()
	 */
	@Override
	public IApiMethod getEnclosingMethod() {
		if (fEnclosingMethod == null) {
			try {
				IApiType enclosingType = getEnclosingType();
				if (fEnclosingMethodName != null) {
					if (fEnclosingMethodName != NO_ENCLOSING_METHOD) {
						fEnclosingMethod = enclosingType.getMethod(fEnclosingMethodName, fEnclosingMethodSignature);
					}
				} else {
					TypeStructureBuilder.setEnclosingMethod(enclosingType, this);
					if (fEnclosingMethodName != null) {
						fEnclosingMethod = enclosingType.getMethod(fEnclosingMethodName, fEnclosingMethodSignature);
					} else {
						// this prevents from trying to retrieve again the
						// enclosing method when there is none
						fEnclosingMethodName = NO_ENCLOSING_METHOD;
					}
				}
			} catch (CoreException ce) {
			}
		}
		return fEnclosingMethod;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isClass()
	 */
	@Override
	public boolean isClass() {
		return (getModifiers() & (Opcodes.ACC_ANNOTATION | Opcodes.ACC_ENUM | Opcodes.ACC_INTERFACE)) == 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isEnum()
	 */
	@Override
	public boolean isEnum() {
		return (getModifiers() & Opcodes.ACC_ENUM) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isInterface
	 * ()
	 */
	@Override
	public boolean isInterface() {
		return (getModifiers() & Opcodes.ACC_INTERFACE) != 0;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#isMemberType
	 * ()
	 */
	@Override
	public boolean isMemberType() {
		return fMemberType;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiMember#getHandle
	 * ()
	 */
	@Override
	public IMemberDescriptor getHandle() {
		if (fHandle == null) {
			fHandle = Util.getType(getName());
		}
		return fHandle;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof IApiType) {
			IApiType type = (IApiType) obj;
			if (getApiComponent() == null) {
				return type.getApiComponent() == null && getName().equals(type.getName());
			}
			return getApiComponent().equals(type.getApiComponent()) && getName().equals(type.getName());
		}
		return false;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		IApiComponent component = getApiComponent();
		if (component == null) {
			return getName().hashCode();
		}
		return component.hashCode() + getName().hashCode();
	}

	/**
	 * Used when building a type structure.
	 * 
	 * @param name member type name
	 * @param modifiers
	 */
	public void addMemberType(String name, int modifiers) {
		if (fMemberTypes == null) {
			fMemberTypes = new HashMap<String, IApiTypeRoot>();
		}
		int index = name.lastIndexOf('$');
		String simpleName = name.substring(index + 1);
		fMemberTypes.put(simpleName, null);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMemberType
	 * (java.lang.String)
	 */
	@Override
	public IApiType getMemberType(String simpleName) throws CoreException {
		if (fMemberTypes == null) {
			return null;
		}
		if (getApiComponent() == null) {
			requiresApiComponent();
		}
		if (fMemberTypes.containsKey(simpleName)) {
			IApiTypeRoot file = fMemberTypes.get(simpleName);
			if (file == null) {
				// resolve
				StringBuffer qName = new StringBuffer();
				qName.append(getName());
				qName.append('$');
				qName.append(simpleName);
				file = getApiComponent().findTypeRoot(qName.toString());
				if (file == null) {
					throw new CoreException(new Status(IStatus.ERROR, ApiPlugin.PLUGIN_ID, MessageFormat.format(Messages.ApiType_3, new Object[] {
							simpleName, getName() })));
				}
				fMemberTypes.put(simpleName, file);
			}
			return file.getStructure();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getMemberTypes
	 * ()
	 */
	@Override
	public IApiType[] getMemberTypes() throws CoreException {
		if (fMemberTypes == null) {
			return EMPTY_TYPES;
		}
		IApiType[] members = new IApiType[fMemberTypes.size()];
		Iterator<String> iterator = fMemberTypes.keySet().iterator();
		int index = 0;
		while (iterator.hasNext()) {
			String name = iterator.next();
			members[index] = getMemberType(name);
			index++;
		}
		return members;
	}

	/**
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Type : access(") //$NON-NLS-1$
		.append(getModifiers()).append(") ") //$NON-NLS-1$
		.append(getName());
		if (getSuperclassName() != null) {
			buffer.append(" superclass: ") //$NON-NLS-1$
			.append(getSuperclassName());
		}
		if (getSuperInterfaceNames() != null) {
			buffer.append(" interfaces : "); //$NON-NLS-1$
			if (getSuperInterfaceNames().length > 0) {
				for (int i = 0; i < getSuperInterfaceNames().length; i++) {
					if (i > 0) {
						buffer.append(',');
					}
					buffer.append(getSuperInterfaceNames()[i]);
				}
			} else {
				buffer.append("none"); //$NON-NLS-1$
			}
		}
		buffer.append(';').append(Util.LINE_DELIMITER);
		if (getGenericSignature() != null) {
			buffer.append(" Signature : ") //$NON-NLS-1$
			.append(getGenericSignature()).append(Util.LINE_DELIMITER);
		}
		buffer.append(Util.LINE_DELIMITER).append("Methods : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		IApiMethod[] methods = getMethods();
		for (int i = 0; i < methods.length; i++) {
			buffer.append(methods[i]);
		}
		buffer.append(Util.LINE_DELIMITER).append("Fields : ").append(Util.LINE_DELIMITER); //$NON-NLS-1$
		IApiField[] fields = getFields();
		for (int i = 0; i < fields.length; i++) {
			buffer.append(fields[i]);
		}
		return String.valueOf(buffer);
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.provisional.model.IApiType#getSimpleName
	 * ()
	 */
	@Override
	public String getSimpleName() {
		if (this.isAnonymous()) {
			return null;
		}
		if (this.isLocal() || this.isMemberType()) {
			return this.fSimpleName;
		}
		String name = getName();
		int index = name.lastIndexOf('.');
		if (index != -1) {
			return name.substring(index + 1);
		}
		return name;
	}

	/*
	 * (non-Javadoc)
	 * @see
	 * org.eclipse.pde.api.tools.internal.model.ApiMember#getEnclosingType()
	 */
	@Override
	public IApiType getEnclosingType() throws CoreException {
		if (fEnclosingType != null) {
			return fEnclosingType;
		}
		if (fEnclosingTypeName != null) {
			IApiTypeRoot root = getApiComponent().findTypeRoot(processEnclosingTypeName());
			if (root != null) {
				fEnclosingType = root.getStructure();
			}
		}
		return fEnclosingType;
	}

	private String processEnclosingTypeName() {
		if (isLocal() || isAnonymous()) {
			int idx = fEnclosingTypeName.lastIndexOf('$');
			if (Character.isDigit(fEnclosingTypeName.charAt(idx + 1))) {
				return fEnclosingTypeName.substring(0, idx);
			}
		}
		return fEnclosingTypeName;
	}
}
