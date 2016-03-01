/*******************************************************************************
 * Copyright (c) 2010, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.eclipse.jdt.core.Signature;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.api.tools.internal.IApiXmlConstants;
import org.eclipse.pde.api.tools.internal.provisional.comparator.IDelta;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This task can be used to convert the report generated by the
 * apitooling.apideprecation task into an html report.
 */
public class APIDeprecationReportConversionTask extends Task {
	static final class ConverterDefaultHandler extends DefaultHandler {
		private String[] arguments;
		private List<String> argumentsList;
		private String componentID;
		private boolean debug;
		private int flags;
		private String key;
		private String kind;
		private Map<String, List<Entry>> map;
		private String typename;
		private int elementType;

		public ConverterDefaultHandler(boolean debug) {
			this.map = new HashMap<String, List<Entry>>();
			this.debug = debug;
		}

		@Override
		public void endElement(String uri, String localName, String name) throws SAXException {
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				Entry entry = new Entry(this.flags, this.elementType, this.key, this.typename, this.arguments, this.kind);
				List<Entry> list = this.map.get(this.componentID);
				if (list != null) {
					list.add(entry);
				} else {
					ArrayList<Entry> value = new ArrayList<Entry>();
					value.add(entry);
					this.map.put(componentID, value);
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList != null && this.argumentsList.size() != 0) {
					this.arguments = new String[this.argumentsList.size()];
					this.argumentsList.toArray(this.arguments);
				}
			}
		}

		public Map<String, List<Entry>> getEntries() {
			return this.map;
		}

		/*
		 * Only used in debug mode
		 */
		private void printAttribute(Attributes attributes, String name) {
			System.out.println("\t" + name + " = " + String.valueOf(attributes.getValue(name))); //$NON-NLS-1$ //$NON-NLS-2$
		}

		@Override
		public void startElement(String uri, String localName, String name, Attributes attributes) throws SAXException {
			if (IApiXmlConstants.DELTA_ELEMENT_NAME.equals(name)) {
				if (this.debug) {
					System.out.println("name : " + name); //$NON-NLS-1$
					/*
					 * <delta compatible="true"
					 * componentId="org.eclipse.equinox.p2.ui_0.1.0"
					 * element_type="CLASS_ELEMENT_TYPE" flags="25" key=
					 * "schedule(Lorg/eclipse/equinox/internal/provisional/p2/ui/operations/ProvisioningOperation;Lorg/eclipse/swt/widgets/Shell;I)Lorg/eclipse/core/runtime/jobs/Job;"
					 * kind="ADDED" oldModifiers="9" newModifiers="9"
					 * restrictions="0" type_name=
					 * "org.eclipse.equinox.internal.provisional.p2.ui.ProvisioningOperationRunner"
					 * />
					 */
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPATIBLE);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE);
					printAttribute(attributes, IApiXmlConstants.ATTR_FLAGS);
					printAttribute(attributes, IApiXmlConstants.ATTR_KEY);
					printAttribute(attributes, IApiXmlConstants.ATTR_KIND);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_NEW_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_OLD_MODIFIERS);
					printAttribute(attributes, IApiXmlConstants.ATTR_RESTRICTIONS);
					printAttribute(attributes, IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				}
				this.componentID = attributes.getValue(IApiXmlConstants.ATTR_NAME_COMPONENT_ID);
				this.flags = Integer.parseInt(attributes.getValue(IApiXmlConstants.ATTR_FLAGS));
				this.elementType = Util.getDeltaElementTypeValue(attributes.getValue(IApiXmlConstants.ATTR_NAME_ELEMENT_TYPE));
				this.typename = attributes.getValue(IApiXmlConstants.ATTR_NAME_TYPE_NAME);
				this.key = attributes.getValue(IApiXmlConstants.ATTR_KEY);
				this.kind = attributes.getValue(IApiXmlConstants.ATTR_KIND);
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENTS.equals(name)) {
				if (this.argumentsList == null) {
					this.argumentsList = new ArrayList<String>();
				} else {
					this.argumentsList.clear();
				}
			} else if (IApiXmlConstants.ELEMENT_DELTA_MESSAGE_ARGUMENT.equals(name)) {
				this.argumentsList.add(attributes.getValue(IApiXmlConstants.ATTR_VALUE));
			}
		}
	}

	static class Entry {
		String[] arguments;
		int flags;
		int elementType;
		String key;
		String typeName;
		String kind;

		private static final String ADDED = "ADDED"; //$NON-NLS-1$
		private static final String REMOVED = "REMOVED"; //$NON-NLS-1$

		public Entry(int flags, int elementType, String key, String typeName, String[] arguments, String kind) {
			this.flags = flags;
			this.key = key.replace('/', '.');
			if (typeName != null) {
				this.typeName = typeName.replace('/', '.');
			}
			this.arguments = arguments;
			this.kind = kind;
			this.elementType = elementType;
		}

		public String getDisplayString() {
			StringBuffer buffer = new StringBuffer();
			if (this.typeName != null && this.typeName.length() != 0) {
				buffer.append(this.typeName);
				if (this.flags == IDelta.DEPRECATION) {
					switch (this.elementType) {
						case IDelta.ANNOTATION_ELEMENT_TYPE:
						case IDelta.INTERFACE_ELEMENT_TYPE:
						case IDelta.ENUM_ELEMENT_TYPE:
						case IDelta.CLASS_ELEMENT_TYPE:
							// If the root type is deprecated, don't repeat the
							// typename
							if (!this.typeName.equals(this.key)) {
								buffer.append('.');
								buffer.append(this.key);
							}
							break;
						case IDelta.CONSTRUCTOR_ELEMENT_TYPE:
							int indexOf = key.indexOf('(');
							if (indexOf == -1) {
								return null;
							}
							int index = indexOf;
							String selector = key.substring(0, index);
							String descriptor = key.substring(index, key.length());
							buffer.append('#');
							buffer.append(Signature.toString(descriptor, selector, null, false, false));
							break;
						case IDelta.METHOD_ELEMENT_TYPE:
							indexOf = key.indexOf('(');
							if (indexOf == -1) {
								return null;
							}
							index = indexOf;
							selector = key.substring(0, index);
							descriptor = key.substring(index, key.length());
							buffer.append('#');
							buffer.append(Signature.toString(descriptor, selector, null, false, true));
							break;
						case IDelta.FIELD_ELEMENT_TYPE:
							buffer.append('#');
							buffer.append(this.key);
							break;
						default:
							break;
					}
				}
			}

			return CommonUtilsTask.convertToHtml(String.valueOf(buffer));
		}

		public String getDisplayKind() {
			if (ADDED.equals(this.kind)) {
				return Messages.APIDeprecationReportConversionTask_KindDeprecated;
			} else if (REMOVED.equals(this.kind)) {
				return Messages.APIDeprecationReportConversionTask_KindUndeprecated;
			}
			return Messages.ChangedElement;
		}
	}

	boolean debug;

	private String htmlFileLocation;
	private String xmlFileLocation;

	private void dumpEndEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deprecationReportTask_endComponentEntry, componentID));
	}

	private void dumpEntries(Map<String, List<Entry>> entries, StringBuffer buffer) {
		dumpHeader(buffer);
		List<Map.Entry<String, List<Entry>>> allEntries = new ArrayList<Map.Entry<String, List<Entry>>>();
		for (Map.Entry<String, List<Entry>> entry : entries.entrySet()) {
			allEntries.add(entry);
		}
		Collections.sort(allEntries, new Comparator<Object>() {
			@SuppressWarnings("unchecked")
			@Override
			public int compare(Object o1, Object o2) {
				Map.Entry<String, List<Entry>> entry1 = (Map.Entry<String, List<Entry>>) o1;
				Map.Entry<String, List<Entry>> entry2 = (Map.Entry<String, List<Entry>>) o2;
				return entry1.getKey().compareTo(entry2.getKey());
			}
		});
		for (Map.Entry<String, List<Entry>> mapEntry : allEntries) {
			String key = mapEntry.getKey();
			List<Entry> values = mapEntry.getValue();
			dumpEntryForComponent(buffer, key);
			Collections.sort(values, new Comparator<Object>() {
				@Override
				public int compare(Object o1, Object o2) {
					Entry entry1 = (Entry) o1;
					Entry entry2 = (Entry) o2;
					String typeName1 = entry1.typeName;
					String typeName2 = entry2.typeName;
					if (typeName1 == null) {
						if (typeName2 == null) {
							return entry1.key.compareTo(entry2.key);
						}
						return -1;
					} else if (typeName2 == null) {
						return 1;
					}
					if (!typeName1.equals(typeName2)) {
						return typeName1.compareTo(typeName2);
					}
					return entry1.key.compareTo(entry2.key);
				}
			});
			if (debug) {
				System.out.println("Entries for " + key); //$NON-NLS-1$
			}
			for (Entry entry : values) {
				if (debug) {
					if (entry.typeName != null) {
						System.out.print(entry.typeName);
						System.out.print('#');
					}
					System.out.println(entry.key);
				}
				dumpEntry(buffer, entry);
			}
			dumpEndEntryForComponent(buffer, key);
		}
		dumpFooter(buffer);
	}

	private void dumpEntry(StringBuffer buffer, Entry entry) {
		buffer.append(NLS.bind(Messages.deprecationReportTask_entry, entry.getDisplayKind(), entry.getDisplayString()));
	}

	private void dumpEntryForComponent(StringBuffer buffer, String componentID) {
		buffer.append(NLS.bind(Messages.deprecationReportTask_componentEntry, componentID));
	}

	private void dumpFooter(StringBuffer buffer) {
		buffer.append(Messages.deprecationReportTask_footer);
	}

	private void dumpHeader(StringBuffer buffer) {
		buffer.append(Messages.deprecationReportTask_header);
	}

	/**
	 * Run the ant task
	 */
	@Override
	public void execute() throws BuildException {
		if (this.xmlFileLocation == null) {
			throw new BuildException(Messages.deprecationReportTask_missingXmlFileLocation);
		}
		if (this.debug) {
			System.out.println("xmlFileLocation : " + this.xmlFileLocation); //$NON-NLS-1$
			System.out.println("htmlFileLocation : " + this.htmlFileLocation); //$NON-NLS-1$
		}
		SAXParserFactory factory = SAXParserFactory.newInstance();
		SAXParser parser = null;
		try {
			parser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
		if (parser == null) {
			throw new BuildException(Messages.deprecationReportTask_couldNotCreateSAXParser);
		}

		File file = new File(this.xmlFileLocation);
		if (this.htmlFileLocation == null) {
			this.htmlFileLocation = extractNameFromXMLName();
			if (this.debug) {
				System.out.println("output name :" + this.htmlFileLocation); //$NON-NLS-1$
			}
		}
		try {
			ConverterDefaultHandler defaultHandler = new ConverterDefaultHandler(this.debug);
			parser.parse(file, defaultHandler);
			StringBuffer buffer = new StringBuffer();
			dumpEntries(defaultHandler.getEntries(), buffer);
			writeOutput(buffer);
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
	}

	private String extractNameFromXMLName() {
		int index = this.xmlFileLocation.lastIndexOf('.');
		StringBuffer buffer = new StringBuffer();
		buffer.append(this.xmlFileLocation.substring(0, index)).append(".html"); //$NON-NLS-1$
		return String.valueOf(buffer);
	}

	/**
	 * Set the debug value.
	 * <p>
	 * The possible values are: <code>true</code>, <code>false</code>
	 * </p>
	 * <p>
	 * Default is <code>false</code>.
	 * </p>
	 * 
	 * @param debugValue the given debug value
	 */
	public void setDebug(String debugValue) {
		this.debug = Boolean.toString(true).equals(debugValue);
	}

	/**
	 * Set the path of the html file to generate.
	 * 
	 * <p>
	 * The location is set using an absolute path.
	 * </p>
	 * 
	 * <p>
	 * This is optional. If not set, the html file name is retrieved from the
	 * xml file name by replacing ".xml" in ".html".
	 * </p>
	 * 
	 * @param htmlFilePath the path of the html file to generate
	 */
	public void setHtmlFile(String htmlFilePath) {
		this.htmlFileLocation = htmlFilePath;
	}

	/**
	 * Set the path of the xml file to convert to html.
	 * 
	 * <p>
	 * The path is set using an absolute path.
	 * </p>
	 * 
	 * @param xmlFilePath the path of the xml file to convert to html
	 */
	public void setXmlFile(String xmlFilePath) {
		this.xmlFileLocation = xmlFilePath;
	}

	private void writeOutput(StringBuffer buffer) throws IOException {
		FileWriter writer = null;
		BufferedWriter bufferedWriter = null;
		try {
			writer = new FileWriter(this.htmlFileLocation);
			bufferedWriter = new BufferedWriter(writer);
			bufferedWriter.write(String.valueOf(buffer));
		} finally {
			if (bufferedWriter != null) {
				bufferedWriter.close();
			}
		}
	}
}
