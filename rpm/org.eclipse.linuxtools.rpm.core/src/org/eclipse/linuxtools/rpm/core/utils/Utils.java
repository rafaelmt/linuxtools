/*******************************************************************************
 * Copyright (c) 2009-2010 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.rpm.core.utils;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Utilities for calling system executables.
 * 
 */
public class Utils {

	/**
	 * Runs the given command and parameters.
	 * 
	 * @param command
	 *            The command with all parameters.
	 * @return Stream containing the combined content of stderr and stdout.
	 * @throws IOException
	 *             If IOException occurs.
	 */
	public static BufferedProcessInputStream runCommandToInputStream(String... command)
			throws IOException {
		BufferedProcessInputStream in = null;
		ProcessBuilder pBuilder = new ProcessBuilder(command);
		pBuilder = pBuilder.redirectErrorStream(true);
		Process child = pBuilder.start();
		in = new BufferedProcessInputStream(child);
		return in;
	}

	/**
	 * Runs the given command and parameters.
	 * 
	 * @param outStream
	 *            The stream to write the output to.
	 * 
	 * @param command
	 *            The command with all parameters.
	 * @return int The return value of the command.
	 * @throws IOException If an IOException occurs.
	 */
	public static int runCommand(final OutputStream outStream,
			String... command) throws IOException {
		ProcessBuilder pBuilder = new ProcessBuilder(command);
		pBuilder = pBuilder.redirectErrorStream(true);
		Process child = pBuilder.start();
		final BufferedInputStream in = new BufferedInputStream(child
				.getInputStream());
		Job readinJob = new Job("") { //$NON-NLS-1$

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					int i;
					while ((i = in.read()) != -1) {
						outStream.write(i);
					}
					outStream.flush();
					outStream.close();
					in.close();
				} catch (IOException e) {
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}

		};
		readinJob.schedule();
		
		try {
			child.waitFor();
			readinJob.join();
		} catch (InterruptedException e) {
			child.destroy();
			readinJob.cancel();
		}
		return child.exitValue();
	}

	/**
	 * Run a command and return its output.
	 * @param command The command to execute.
	 * @return The output of the executed command.
	 * @throws IOException If an I/O exception occurred.
	 */
	public static String runCommandToString(String... command)
			throws IOException {
		BufferedInputStream in = runCommandToInputStream(command);
		return inputStreamToString(in);
	}

	/**
	 * Reads the content of the given InputStream and returns its textual
	 * representation.
	 * 
	 * @param stream
	 *            The InputStream to read.
	 * @return Textual content of the stream.
	 * @throws IOException If an IOException occurs.
	 */
	public static String inputStreamToString(InputStream stream)
			throws IOException {
		StringBuilder retStr = new StringBuilder(); 
		int c;
		while ((c = stream.read()) != -1) {
			retStr.append((char) c);
		}
		stream.close();
		return retStr.toString();
	}
	
	/**
	 * Checks whether a file exists.
	 * 
	 * @param cmdPath The file path to be checked.
	 * @return <code>true</code> if the file exists, <code>false</code> otherwise.
	 */
	public static boolean fileExist(String cmdPath) {
		return new File(cmdPath).exists();
	}
	
	/**
	 * Copy file from one destination to another.
	 * @param in The source file.
	 * @param out The destination.
	 * @throws IOException If an I/O exception occurs.
	 */
	public static void copyFile(File in, File out) throws IOException {
		FileChannel inChannel = new FileInputStream(in).getChannel();
		FileChannel outChannel = new FileOutputStream(out).getChannel();
		try {
			inChannel.transferTo(0, inChannel.size(), outChannel);
		} catch (IOException e) {
			throw e;
		} finally {
			if (inChannel != null)
				inChannel.close();
			if (outChannel != null)
				outChannel.close();
		}
	}
}
