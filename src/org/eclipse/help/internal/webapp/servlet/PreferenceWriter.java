/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.help.internal.webapp.servlet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.help.internal.webapp.WebappResources;
import org.eclipse.help.internal.webapp.data.UrlUtil;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Used by the about.html page to display help system preferences
 */

public class PreferenceWriter {
	private StringBuffer buf;
	private Locale locale;
	private boolean isXML;

	public PreferenceWriter(StringBuffer buf, Locale locale) {
		this(buf, locale, false);
	}

	public PreferenceWriter(StringBuffer buf, Locale locale, boolean isXML) {
		this.buf = buf;
		this.locale = locale;
		this.isXML = isXML;
	}

	public void writePreferences() {
		writePreference("org.eclipse.help"); //$NON-NLS-1$
		writePreference("org.eclipse.help.base"); //$NON-NLS-1$
	}

	private void writePreference(String plugin) {
		try {
		    IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(plugin);
			Set<String> keySet = new HashSet<String>();
		    prefs = DefaultScope.INSTANCE.getNode(plugin);
			keySet.addAll(Arrays.asList(prefs.keys()));
			String[] allKeys = keySet.toArray(new String[keySet.size()]);
			if (allKeys.length > 0) {
			    Arrays.sort(allKeys);

			    if (!isXML) {
			    	buf.append("\n<h3>"); //$NON-NLS-1$
			    	buf.append(plugin);
			    	buf.append("</h3>\n"); //$NON-NLS-1$
			    	buf.append("<table>");  //$NON-NLS-1$
			    } else {
					buf.append("\n    <plugin\n          title=\""); //$NON-NLS-1$
					buf.append(XMLGenerator.xmlEscape(plugin));
					buf.append("\">"); //$NON-NLS-1$
			    }
			    for (String key : allKeys) {
			    	String value = Platform.getPreferencesService().getString
			    			(plugin, key, "", null); //$NON-NLS-1$
			    	if (!isXML) {
				    	buf.append("\n    <tr>\n"); //$NON-NLS-1$
				    	buf.append("        <td>"); //$NON-NLS-1$
				    	buf.append(UrlUtil.htmlEncode(key));
				    	buf.append("</td>\n        <td>"); //$NON-NLS-1$
						buf.append(UrlUtil.htmlEncode(value));
						buf.append("</td>\n    </tr>"); //$NON-NLS-1$
			    	} else {
				    	buf.append("\n        <"); //$NON-NLS-1$
			    		buf.append(key);
			    		buf.append(">"); //$NON-NLS-1$
						buf.append(value);
				    	buf.append("</"); //$NON-NLS-1$
			    		buf.append(key);
			    		buf.append(">"); //$NON-NLS-1$
			    	}
			    }
			    if (!isXML) {
			    	buf.append("\n</table>"); //$NON-NLS-1$
			    } else {
			    	buf.append("\n    </plugin>"); //$NON-NLS-1$
			    }
			}
		} catch (BackingStoreException e) {
			buf.append(WebappResources.getString("badPreferences", locale)); //$NON-NLS-1$
		}
	}

}
