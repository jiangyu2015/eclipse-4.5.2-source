/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.core.sourcelookup.containers;


import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * Storage implementation for zip entries.
 * <p>
 * This class may be instantiated.
 * </p>
 * @see IStorage
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ZipEntryStorage extends PlatformObject implements IStorage {

	/**
	 * Zip file associated with zip entry
	 */
	private ZipFile fArchive;

	/**
	 * Zip entry
	 */
	private ZipEntry fZipEntry;

	/**
	 * Constructs a new storage implementation for the
	 * given zip entry in the specified zip file
	 *
	 * @param archive zip file
	 * @param entry zip entry
	 */
	public ZipEntryStorage(ZipFile archive, ZipEntry entry) {
		setArchive(archive);
		setZipEntry(entry);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getContents()
	 */
	@Override
	public InputStream getContents() throws CoreException {
		try {
			return getArchive().getInputStream(getZipEntry());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, SourceLookupMessages.ZipEntryStorage_0, e));
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getFullPath()
	 */
	@Override
	public IPath getFullPath() {
		return new Path(getArchive().getName()).append(getZipEntry().getName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#getName()
	 */
	@Override
	public String getName() {
		int index = getZipEntry().getName().lastIndexOf('\\');
		if (index == -1) {
			index = getZipEntry().getName().lastIndexOf('/');
		}
		if (index == -1) {
			return getZipEntry().getName();
		}
		return getZipEntry().getName().substring(index + 1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IStorage#isReadOnly()
	 */
	@Override
	public boolean isReadOnly() {
		return true;
	}

	/**
	 * Sets the archive containing the zip entry.
	 *
	 * @param archive a zip file
	 */
	private void setArchive(ZipFile archive) {
		fArchive = archive;
	}

	/**
	 * Returns the archive containing the zip entry.
	 *
	 * @return zip file
	 */
	public ZipFile getArchive() {
		return fArchive;
	}

	/**
	 * Sets the entry that contains the source.
	 *
	 * @param entry the entry that contains the source
	 */
	private void setZipEntry(ZipEntry entry) {
		fZipEntry = entry;
	}

	/**
	 * Returns the entry that contains the source
	 *
	 * @return zip entry
	 */
	public ZipEntry getZipEntry() {
		return fZipEntry;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object object) {
		return object instanceof ZipEntryStorage &&
			 getArchive().equals(((ZipEntryStorage)object).getArchive()) &&
			 getZipEntry().getName().equals(((ZipEntryStorage)object).getZipEntry().getName());
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return getZipEntry().getName().hashCode();
	}
}
