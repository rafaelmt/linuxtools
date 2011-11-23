/*******************************************************************************
 * Copyright (c) 2009 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Alexander Kurtakov - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.man.parser;

/**
 * Man page bean to ease fetching html-preformatted different parts of a man
 * page.
 */
public class ManPage {

	private StringBuilder rawContent;

	/**
	 * Creates the man page which includes retrieving the raw content and
	 * changing format symbols to html.
	 * 
	 * @param manPage
	 *            The man page.
	 */
	public ManPage(String manPage) {
		parse(new ManParser().getRawManPage(manPage));
	}

	// TODO make bold and underline be ranges instead of separate symbols.
	private void parse(StringBuilder rawManPage) {
		StringBuilder sb = new StringBuilder();
		sb.append(rawManPage);

		while (sb.indexOf("_\b") != -1) { //$NON-NLS-1$
			int index = sb.indexOf("_\b"); //$NON-NLS-1$
			sb.replace(index, index + 3,
					"<u>" + sb.substring(index + 2, index + 3) + "</u>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		while (sb.indexOf("\b") != -1) { //$NON-NLS-1$
			int index = sb.indexOf("\b"); //$NON-NLS-1$
			sb.replace(index - 1, index + 2,
					"<b>" + sb.substring(index - 1, index) + "</b>"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		rawContent = sb;
	}

	/**
	 * Returns html representation of the man page. The whole man page is kept
	 * in one giant &lt;pre&gt; block with bold and underline symbols.
	 * 
	 * @return The whole html man page.
	 */
	public StringBuilder getHtmlPage() {
		StringBuilder sb = new StringBuilder();
		sb.append("<pre>").append(rawContent).append("</pre>"); //$NON-NLS-1$ //$NON-NLS-2$
		return sb;
	}

	/**
	 * Returns stripped representaton of the man page. Stripped parts are:
	 * <ul>
	 * <li>Header - all the parts before <b>NAME</b></li>
	 * <li>Footer - all the parts from <b>AUTHOR</b> till the end</li>
	 * </ul>
	 * 
	 * @return The stripped html content of the man page.
	 */
	public StringBuilder getStrippedHtmlPage() {
		StringBuilder sb = new StringBuilder();
		sb.append(rawContent);
		if (sb.indexOf("<b>N</b>") != -1) { //$NON-NLS-1$
			sb.delete(0, sb.indexOf("<b>N</b>")); //$NON-NLS-1$
		}
		if (sb.indexOf("<b>A</b><b>U</b><b>T</b><b>H</b><b>O</b><b>R</b>") != -1) { //$NON-NLS-1$
			sb.delete(
					sb.indexOf("<b>A</b><b>U</b><b>T</b><b>H</b><b>O</b><b>R</b>"), //$NON-NLS-1$
					sb.length());
		}
		sb.insert(0, "<pre>"); //$NON-NLS-1$
		sb.append("</pre>"); //$NON-NLS-1$
		return sb;
	}
}
