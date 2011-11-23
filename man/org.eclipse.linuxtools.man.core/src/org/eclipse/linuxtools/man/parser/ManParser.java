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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.eclipse.linuxtools.man.Activator;
import org.eclipse.linuxtools.man.preferences.PreferenceConstants;

/**
 * Parser for the man executable output.
 * 
 */
public class ManParser {

	/**
	 * Returns the raw representation of the man executable for a given man page
	 * i.e. `man ls`.
	 * 
	 * @param manPage
	 *            The man page to fetch.
	 * @return Raw output of the man command.
	 */
	public StringBuilder getRawManPage(String manPage) {
		String manExecutable = Activator.getDefault().getPreferenceStore()
				.getString(PreferenceConstants.P_PATH);
		ProcessBuilder builder = new ProcessBuilder(manExecutable, manPage);
		builder.redirectErrorStream(true);
		Process process;
		StringBuilder sb = new StringBuilder();
		try {
			process = builder.start();
			if (!(System.getProperty("os.name").toLowerCase() //$NON-NLS-1$
					.indexOf("windows") == 0)) { //$NON-NLS-1$
				process.waitFor();
			}

			InputStream manContent = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					manContent));

			String line = null;
			try {
				while ((line = reader.readLine()) != null) {
					sb.append(line + "\n"); //$NON-NLS-1$
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					manContent.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return sb;
	}
}
