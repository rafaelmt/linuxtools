/*******************************************************************************
 * Copyright (c) 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Patrick Tasse - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.views.colors;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.linuxtools.tmf.filter.model.ITmfFilterTreeNode;
import org.eclipse.linuxtools.tmf.filter.xml.TmfFilterContentHandler;
import org.eclipse.linuxtools.tmf.filter.xml.TmfFilterXMLWriter;
import org.eclipse.swt.graphics.RGB;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

public class ColorSettingsXML {

	private static final String COLOR_SETTINGS_TAG = "COLOR_SETTINGS"; //$NON-NLS-1$
	private static final String COLOR_SETTING_TAG = "COLOR_SETTING"; //$NON-NLS-1$
	private static final String FG_TAG = "FG"; //$NON-NLS-1$
	private static final String BG_TAG = "BG"; //$NON-NLS-1$
	private static final String R_ATTR = "R"; //$NON-NLS-1$
	private static final String G_ATTR = "G"; //$NON-NLS-1$
	private static final String B_ATTR = "B"; //$NON-NLS-1$
	private static final String TICK_COLOR_TAG = "TICK_COLOR"; //$NON-NLS-1$
	private static final String INDEX_ATTR = "INDEX"; //$NON-NLS-1$
	private static final String FILTER_TAG = "FILTER"; //$NON-NLS-1$

	public static void save(String pathName, ColorSetting[] colorSettings) {
		try {
			DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
			Document document = documentBuilder.newDocument();
			
			Element rootElement = document.createElement(COLOR_SETTINGS_TAG);
			document.appendChild(rootElement);
	
			for (ColorSetting colorSetting : colorSettings) {
				Element colorSettingElement = document.createElement(COLOR_SETTING_TAG);
				rootElement.appendChild(colorSettingElement);
				
				Element fgElement = document.createElement(FG_TAG);
				colorSettingElement.appendChild(fgElement);
				RGB foreground = colorSetting.getForegroundRGB();
				fgElement.setAttribute(R_ATTR, Integer.toString(foreground.red));
				fgElement.setAttribute(G_ATTR, Integer.toString(foreground.green));
				fgElement.setAttribute(B_ATTR, Integer.toString(foreground.blue));
				
				Element bgElement = document.createElement(BG_TAG);
				colorSettingElement.appendChild(bgElement);
				RGB background = colorSetting.getBackgroundRGB();
				bgElement.setAttribute(R_ATTR, Integer.toString(background.red));
				bgElement.setAttribute(G_ATTR, Integer.toString(background.green));
				bgElement.setAttribute(B_ATTR, Integer.toString(background.blue));
				
				Element tickColorElement = document.createElement(TICK_COLOR_TAG);
				colorSettingElement.appendChild(tickColorElement);
				int index = colorSetting.getTickColorIndex();
				tickColorElement.setAttribute(INDEX_ATTR, Integer.toString(index));
				
				if (colorSetting.getFilter() != null) {
					Element filterElement = document.createElement(FILTER_TAG);
					colorSettingElement.appendChild(filterElement);
					TmfFilterXMLWriter.buildXMLTree(document, colorSetting.getFilter(), filterElement);
				}
			}
	
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
		
			Transformer transformer = transformerFactory.newTransformer();
	        DOMSource source = new DOMSource(document);
	        StreamResult result =  new StreamResult(new File(pathName));
			transformer.transform(source, result);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerConfigurationException e) {
			e.printStackTrace();
		} catch (TransformerException e) {
			e.printStackTrace();
		}
	}
	
	public static ColorSetting[] load(String pathName) {
		if (! new File(pathName).canRead()) {
			return new ColorSetting[0];
		}
		SAXParserFactory parserFactory = SAXParserFactory.newInstance(); 
        parserFactory.setNamespaceAware(true); 

        ColorSettingsContentHandler handler = new ColorSettingsContentHandler();
		try {
			XMLReader saxReader = parserFactory.newSAXParser().getXMLReader();
	        saxReader.setContentHandler(handler);
	        saxReader.parse(pathName);
	        return handler.colorSettings.toArray(new ColorSetting[0]);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// In case of error, dispose the partial list of color settings
		for (ColorSetting colorSetting : handler.colorSettings) {
			colorSetting.dispose();
		}
		return new ColorSetting[0];
	}
	
	private static class ColorSettingsContentHandler extends DefaultHandler {

		private ArrayList<ColorSetting> colorSettings = new ArrayList<ColorSetting>(0);
		private RGB fg;
		private RGB bg;
		private int tickColorIndex;
		private ITmfFilterTreeNode filter;
		private TmfFilterContentHandler filterContentHandler;
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			if (localName.equals(COLOR_SETTINGS_TAG)) {
				colorSettings = new ArrayList<ColorSetting>();
			} else if (localName.equals(COLOR_SETTING_TAG)) {
				fg = null;
				bg = null;
				filter = null;
			} else if (localName.equals(FG_TAG)) {
				int r = Integer.valueOf(attributes.getValue(R_ATTR));
				int g = Integer.valueOf(attributes.getValue(G_ATTR));
				int b = Integer.valueOf(attributes.getValue(B_ATTR));
				fg = new RGB(r, g, b);
			} else if (localName.equals(BG_TAG)) {
				int r = Integer.valueOf(attributes.getValue(R_ATTR));
				int g = Integer.valueOf(attributes.getValue(G_ATTR));
				int b = Integer.valueOf(attributes.getValue(B_ATTR));
				bg = new RGB(r, g, b);
			} else if (localName.equals(TICK_COLOR_TAG)) {
				int index = Integer.valueOf(attributes.getValue(INDEX_ATTR));
				tickColorIndex = index;
			} else if (localName.equals(FILTER_TAG)) {
				filterContentHandler = new TmfFilterContentHandler();
			} else if (filterContentHandler != null) {
				filterContentHandler.startElement(uri, localName, qName, attributes);
			}
		}
		
		/* (non-Javadoc)
		 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (localName.equals(COLOR_SETTINGS_TAG)) {
			} else if (localName.equals(COLOR_SETTING_TAG)) {
				ColorSetting colorSetting = new ColorSetting(fg, bg, tickColorIndex, filter);
				colorSettings.add(colorSetting);
			} else if (localName.equals(FILTER_TAG)) {
				filter = filterContentHandler.getTree();
				filterContentHandler = null;
			} else if (filterContentHandler != null) {
				filterContentHandler.endElement(uri, localName, qName);
			}
		}

	}
}
