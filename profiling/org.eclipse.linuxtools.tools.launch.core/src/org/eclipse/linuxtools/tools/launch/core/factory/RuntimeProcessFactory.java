/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Otavio Busatto Pontes <obusatto@br.ibm.com> - initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.tools.launch.core.factory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IProject;

/*
 * Create process using Runtime.getRuntime().exec and prepends the
 * 'Linux tools path' project property to the environment PATH.
 * Use this factory instead of Runtime.getRuntime().exec if the command you
 * are running may be in the linux tools path selected in the project property
 * page.
 */
public class RuntimeProcessFactory extends LinuxtoolsProcessFactory {
	private static RuntimeProcessFactory instance = null;
	private static final String WHICH_CMD = "which"; //$NON-NLS-1$

	private String[] tokenizeCommand(String command) {
		StringTokenizer tokenizer = new StringTokenizer(command);
		String[] cmdarray = new String[tokenizer.countTokens()];
		for (int i = 0; tokenizer.hasMoreElements(); i++)
			cmdarray[i] = tokenizer.nextToken();

		return cmdarray;
	}

	private String[] fillPathCommand(String[] cmdarray, String[] envp) throws IOException {
		cmdarray[0] = whichCommand(cmdarray[0], envp);
		return cmdarray;
	}

	public String whichCommand(String command, IProject project) throws IOException {
		System.out.println(whichCommand(command, updateEnvironment(null, project)));
		return whichCommand(command, updateEnvironment(null, project));
	}

	public String whichCommand(String command, String[] envp) throws IOException {
		Process p = Runtime.getRuntime().exec(new String[] {WHICH_CMD, command}, envp);
		try {
			if (p.waitFor() == 0) {
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
				command = reader.readLine();
			} else {
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				throw new IOException(reader.readLine());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return command;
	}

	public static RuntimeProcessFactory getFactory() {
		if (instance == null)
			instance = new RuntimeProcessFactory();
		return instance;
	}

	public Process exec(String cmd, IProject project) throws IOException {
		return exec(cmd, null, null, project);
	}

	public Process exec(String[] cmdarray, IProject project) throws IOException {
		return exec(cmdarray, null, project);
	}

	public Process exec(String[] cmdarray, String[] envp, IProject project) throws IOException {
		return exec(cmdarray, envp, null, project);
	}

	public Process exec(String cmd, String[] envp, IProject project) throws IOException {
		return exec(cmd, envp, null, project);
	}

	public Process exec(String cmd, String[] envp, File dir, IProject project)
		throws IOException {
		return exec(tokenizeCommand(cmd), envp, dir, project);
	}

	public Process exec(String cmdarray[], String[] envp, File dir, IProject project)
		throws IOException {
		envp = updateEnvironment(envp, project);
		cmdarray = fillPathCommand(cmdarray, envp);

		return Runtime.getRuntime().exec(cmdarray, envp, dir);
	}
}
