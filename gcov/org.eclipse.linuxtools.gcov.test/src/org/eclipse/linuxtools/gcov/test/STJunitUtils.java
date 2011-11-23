/*******************************************************************************
 * Copyright (c) 2009 STMicroelectronics.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Xavier Raynaud <xavier.raynaud@st.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.gcov.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.net.URL;

import junit.framework.Assert;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;


/**
 * This class only contains some tools to facilitate tests
 * (compare)
 * @author Xavier Raynaud <xavier.raynaud@st.com>
 */
public class STJunitUtils {
	
	/**
	 * Utility method to compare files
	 * @param dumpFile
	 * @param refFile
	 */
	public static boolean compare(String dumpFile, String refFile, boolean deleteDumpFileIfOk) {
		String message = "Comparing ref file ("+refFile+ ")and dump file (" + 
		  dumpFile+")";
		boolean equals = false;
		System.out.println(message);
		try {
			InputStream is1 = new FileInputStream(dumpFile);
			InputStream is2 = new FileInputStream(refFile);
			equals = compare(is1, is2);
			if (!equals) {
				System.out.println(message +  "... FAILED");
				junit.framework.Assert.assertEquals(message + ": not correspond ", true, false);
			}
			else {
				System.out.println(message +  "... successful");
			}
			// delete dump only for successful tests
			if (equals && deleteDumpFileIfOk)  
				new File(dumpFile).delete();
		}catch (FileNotFoundException _) {
			message += "... FAILED: One of these files may not exist";
			System.out.println(message);
			junit.framework.Assert.assertNull(message, _);
		}
		catch (Exception _) {
			message += ": exception raised ... FAILED";
			System.out.println(message);
			junit.framework.Assert.assertNull(message, _);
		}
		return equals;
	}
	
	/**
	 * Utility method to compare files
	 * @param dumpFile
	 * @param refFile
	 * @return
	 */
	public static boolean compareIgnoreEOL(String dumpFile, String refFile, boolean deleteDumpFileIfOk) {
		String message = "Comparing ref file ("+refFile+ ")and dump file (" + 
		  dumpFile+")";
		boolean equals = false;		
		try {
		LineNumberReader is1 = new LineNumberReader(new FileReader(dumpFile));
		LineNumberReader is2 = new LineNumberReader(new FileReader(refFile));
			do {
				String line1 = is1.readLine();
				String line2 = is2.readLine();				
				if (line1 == null) {
					if (line2 == null) {
						equals = true;
					}
					break;
				} else if (line2 == null || !line1.equals(line2)) {
					break;
				}				
			} while (true);
			
			if (!equals) {
 				junit.framework.Assert.assertEquals(message + ": not correspond ", true, false);
			}
			
			is1.close();
			is2.close();
			// delete dump only for successful tests
			if (equals && deleteDumpFileIfOk) {
				new File(dumpFile).delete();
			}
		}catch (FileNotFoundException _) {
			message += "... FAILED: One of these files may not exist";
			junit.framework.Assert.assertNull(message, _);
		}
		catch (Exception _) {
			message += ": exception raised ... FAILED";
			junit.framework.Assert.assertNull(message, _);
		}
		return equals;
	}
	
	/**
	 * Utility method to compare exported CSV files
	 * @param dumpFile
	 * @param refFile
	 * @return
	 */
	public static boolean compareCSVIgnoreEOL(String dumpFile, String refFile, boolean deleteDumpFileIfOk) {
		String message = "Comparing ref file ("+refFile+ ")and dump file (" + 
		  dumpFile+")";
		boolean equals = false;
		String str = "[in-charge]"; // this string can be dumped according to binutils version installed on local machine
		
		try {
		LineNumberReader is1 = new LineNumberReader(new FileReader(dumpFile));
		LineNumberReader is2 = new LineNumberReader(new FileReader(refFile));
			do {
				String line1 = is1.readLine();
				String line2 = is2.readLine();
				int length = str.length();				
				if (line1 == null) {
					if (line2 == null) {
						equals = true;
					}
					break;
				} else if (line1.contains(str)){
					int idx = line1.indexOf("[in-charge]");
					char c = line1.charAt(idx -1);
					if (c == ' ' ){
						idx--;
						length++;
					}
					line1 = line1.substring(0, idx) + line1.substring(idx+length, line1.length());
					if (!line1.equals(line2))
						break;
				} else if (line2 == null || !line1.equals(line2)) {
					break;
				} 				
			} while (true);
			
			if (!equals) {
 				junit.framework.Assert.assertEquals(message + ": not correspond ", true, false);
			}

			is1.close();
			is2.close();
			// delete dump only for successful tests
			if (equals && deleteDumpFileIfOk) {
				new File(dumpFile).delete();
			}
		}catch (FileNotFoundException _) {
			message += "... FAILED: One of these files may not exist";
			junit.framework.Assert.assertNull(message, _);
		}
		catch (Exception _) {
			message += ": exception raised ... FAILED";
			junit.framework.Assert.assertNull(message, _);
		}
		return equals;
	}

	/**
	 * Utility method to compare Input streams
	 * @param ISdump
	 * @param ISref
	 * @return
	 * @throws IOException
	 */
	public static boolean compare(InputStream ISdump, InputStream ISref) throws IOException {
		try {
			boolean equals = false;
			do {
				int char1 = ISdump.read();
				int char2 = ISref.read();
				if (char1 != char2)
					break;
				if (char1 == -1) {
					equals = true;
					break;
				}
			} while (true);
			return equals;
		} finally {
			ISdump.close();
			ISref.close();
		}
	}

	/**
	 * Gets the absolute path of a resource in the given plugin
	 * @param pluginId
	 * @param relativeName
	 * @return an absolute path to a file
	 */
	public static String getAbsolutePath(String pluginId, String relativeName) {
		Bundle b = Platform.getBundle(pluginId);
		URL url = FileLocator.find(b, new Path(relativeName), null);
		try {
			url = FileLocator.toFileURL(url);
		} catch (IOException e) {
			Assert.assertNotNull("Problem locating " + relativeName + " in" + pluginId,e);
		}
		String filename = url.getFile();
		return filename;
	}
}
