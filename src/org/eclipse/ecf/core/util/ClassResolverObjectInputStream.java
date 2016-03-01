/*******************************************************************************
 * Copyright (c) 2015 Composent, Inc. and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: Composent, Inc. - initial API and implementation
 ******************************************************************************/
package org.eclipse.ecf.core.util;

import java.io.*;
import java.util.HashMap;
import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 * @since 3.7
 */
public class ClassResolverObjectInputStream extends ObjectInputStream {

	public static final HashMap<String, Class<?>> primClasses = new HashMap<String, Class<?>>(8, 1.0F);

	static {
		primClasses.put("boolean", boolean.class); //$NON-NLS-1$
		primClasses.put("byte", byte.class); //$NON-NLS-1$
		primClasses.put("char", char.class); //$NON-NLS-1$
		primClasses.put("short", short.class); //$NON-NLS-1$
		primClasses.put("int", int.class); //$NON-NLS-1$
		primClasses.put("long", long.class); //$NON-NLS-1$
		primClasses.put("float", float.class); //$NON-NLS-1$
		primClasses.put("double", double.class); //$NON-NLS-1$
		primClasses.put("void", void.class); //$NON-NLS-1$
	}

	public static ObjectInputStream create(BundleContext ctxt, InputStream ins, String filter) throws IOException, SecurityException {
		if (ctxt == null)
			return new ObjectInputStream(ins);
		try {
			return new ClassResolverObjectInputStream(ctxt, ins, filter);
		} catch (InvalidSyntaxException e) {
			throw new IOException("Could not create ClassResolverObjectInputStream because of InvalidSyntaxException in filter=" + filter); //$NON-NLS-1$
		}
	}

	public static ObjectInputStream create(BundleContext ctxt, InputStream ins) throws IOException {
		return create(ctxt, ins, null);
	}

	private final BundleContext bundleContext;
	private ServiceTracker<IClassResolver, IClassResolver> classResolverST;
	private final Object trackerLock = new Object();
	private final Filter classResolverFilter;

	private Filter createClassResolverFilter(String classResolverFilterString) throws InvalidSyntaxException {
		String objectClassFilterString = "(" + Constants.OBJECTCLASS + "=" + IClassResolver.class.getName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (classResolverFilterString != null)
			objectClassFilterString = "(&" + objectClassFilterString + classResolverFilterString + ")"; //$NON-NLS-1$ //$NON-NLS-2$
		return this.bundleContext.createFilter(objectClassFilterString);
	}

	protected ClassResolverObjectInputStream(BundleContext ctxt, String classResolverFilter) throws IOException, SecurityException, InvalidSyntaxException {
		super();
		this.bundleContext = ctxt;
		this.classResolverFilter = createClassResolverFilter(classResolverFilter);
	}

	protected ClassResolverObjectInputStream(BundleContext ctxt) throws IOException, SecurityException, InvalidSyntaxException {
		this(ctxt, (String) null);
	}

	public ClassResolverObjectInputStream(BundleContext ctxt, InputStream ins, String classResolverFilter) throws IOException, SecurityException, InvalidSyntaxException {
		super(ins);
		this.bundleContext = ctxt;
		this.classResolverFilter = createClassResolverFilter(classResolverFilter);
	}

	public ClassResolverObjectInputStream(BundleContext ctxt, InputStream ins) throws IOException, SecurityException, InvalidSyntaxException {
		this(ctxt, ins, null);
	}

	protected BundleContext getContext() {
		return this.bundleContext;
	}

	private IClassResolver getClassResolver() {
		synchronized (trackerLock) {
			if (classResolverST == null) {
				classResolverST = new ServiceTracker<IClassResolver, IClassResolver>(this.bundleContext, classResolverFilter, null);
				classResolverST.open();
			}
		}
		return this.classResolverST.getService();
	}

	@SuppressWarnings("unused")
	@Override
	protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
		IClassResolver classResolver = getClassResolver();
		if (classResolver != null)
			return classResolver.resolveClass(desc);
		throw new ClassNotFoundException("Cannot deserialize class description=" + desc + " because no OSGi IClassResolver registered"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static Class<?> resolvePrimitiveClass(ObjectStreamClass desc, ClassNotFoundException cnfe) throws ClassNotFoundException {
		Class<?> cl = primClasses.get(desc.getName());
		if (cl != null)
			return cl;
		if (cnfe != null)
			throw cnfe;
		throw new ClassNotFoundException("Could not find class=" + desc); //$NON-NLS-1$
	}

	@Override
	public void close() throws IOException {
		super.close();
		synchronized (trackerLock) {
			if (classResolverST != null) {
				classResolverST.close();
				classResolverST = null;
			}
		}
	}

}
