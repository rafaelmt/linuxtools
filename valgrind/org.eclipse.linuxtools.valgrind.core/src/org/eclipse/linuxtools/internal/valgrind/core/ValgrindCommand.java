/*******************************************************************************
 * Copyright (c) 2008 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Elliott Baron <ebaron@redhat.com> - initial API and implementation
 *******************************************************************************/ 
package org.eclipse.linuxtools.internal.valgrind.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.eclipse.cdt.utils.pty.PTY;
import org.eclipse.core.resources.IProject;
import org.eclipse.linuxtools.tools.launch.core.factory.CdtSpawnerProcessFactory;
import org.eclipse.linuxtools.tools.launch.core.factory.RuntimeProcessFactory;

public class ValgrindCommand {
	protected static final String WHICH_CMD = "which"; //$NON-NLS-1$
	protected static final String VALGRIND_CMD = "valgrind"; //$NON-NLS-1$

	protected Process process;
	protected String[] args;

	public String getValgrindCommand() {
		return VALGRIND_CMD;
	}

	public String whichVersion(IProject project) throws IOException {
		StringBuffer out = new StringBuffer();
		Process p = RuntimeProcessFactory.getFactory().exec(new String[] { VALGRIND_CMD, CommandLineConstants.OPT_VERSION }, project);
		readIntoBuffer(out, p);
		return out.toString().trim();
	}
	
	public void execute(String[] commandArray, Object env, File wd, String exeFile, boolean usePty, IProject project) throws IOException {
		args = commandArray;
		try {
			process = startProcess(commandArray, env, wd, exeFile, usePty, project);
		}
		catch (IOException e) {
			if (process != null) {
				process.destroy();
			}
			throw e;		
		}
	}

	public Process getProcess() {
		return process;
	}

	public String getCommandLine() {
		StringBuffer ret = new StringBuffer();
		for (String arg : args) {
			ret.append(arg + " "); //$NON-NLS-1$
		}
		return ret.toString().trim();
	}
	
	protected Process startProcess(String[] commandArray, Object env, File workDir, String binPath, boolean usePty, IProject project) throws IOException {
		if (workDir == null) {
			return CdtSpawnerProcessFactory.getFactory().exec(commandArray, (String[]) env, project);
		}
		if (PTY.isSupported() && usePty) {
			return CdtSpawnerProcessFactory.getFactory().exec(commandArray, (String[]) env, workDir, new PTY(), project);
		}
		else {
			return CdtSpawnerProcessFactory.getFactory().exec(commandArray, (String[]) env, workDir, project);
		}
	}

	protected void readIntoBuffer(StringBuffer out, Process p) throws IOException {
		boolean success;
		InputStream in;
		try {
			if (success = (p.waitFor() == 0)) {
				in = p.getInputStream();
			}
			else {
				in = p.getErrorStream();
			}
			int ch;
			while ((ch = in.read()) != -1) {
				out.append((char) ch);
			}
			if (!success) {
				throw new IOException(out.toString());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
