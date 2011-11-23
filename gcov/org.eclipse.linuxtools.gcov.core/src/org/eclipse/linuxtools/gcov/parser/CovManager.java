/*******************************************************************************
 * Copyright (c) 2009 STMicroelectronics.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Xavier Raynaud <xavier.raynaud@st.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.linuxtools.gcov.parser;

import java.io.BufferedInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.IBinaryParser.IBinaryObject;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.binutils.utils.STSymbolManager;
import org.eclipse.linuxtools.gcov.Activator;
import org.eclipse.linuxtools.gcov.model.CovFileTreeElement;
import org.eclipse.linuxtools.gcov.model.CovFolderTreeElement;
import org.eclipse.linuxtools.gcov.model.CovFunctionTreeElement;
import org.eclipse.linuxtools.gcov.model.CovRootTreeElement;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

/**
 * @author Xavier Raynaud <xavier.raynaud@st.com>
 *
 */
public class CovManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5582066617970911413L;
	// input
	private final String binaryPath;
	// results
	private final ArrayList<Folder> allFolders = new ArrayList<Folder>();
	private final ArrayList<SourceFile> allSrcs = new ArrayList<SourceFile>();
	private final ArrayList<GcnoFunction> allFnctns = new ArrayList<GcnoFunction>();
	private final HashMap<String, SourceFile> sourceMap = new HashMap<String, SourceFile>();
	private long nbrPgmRuns = 0;
	// for view
	private CovRootTreeElement rootNode;
	private IProject project;

	/**
	 * Constructor
	 * @param binaryPath
	 * @param project the project that will be used to get the path to run commands
	 */
	public CovManager(String binaryPath, IProject project) {
		this.binaryPath = binaryPath;
		this.project = project;
	}

	/**
	 * Constructor
	 * @param binaryPath
	 */
	public CovManager(String binaryPath) {
		this(binaryPath, null);
	}

	/**
	 * parse coverage files, execute resolve graph algorithm, process counts for functions, 
	 * lines and folders.    
	 * @param List of coverage files paths
	 * @throws CoreException, IOException, InterruptedException
	 */

	public void processCovFiles(List<String> covFilesPaths, String initialGcda) throws CoreException, IOException, InterruptedException {
		GcdaRecordsParser daRcrd = null;
		DataInput traceFile;
		
		Map<File, File> sourcePath = new HashMap<File, File>();
		
		if (initialGcda != null) {
			File initialGcdaFile = new File(initialGcda).getAbsoluteFile();
			for (String s : covFilesPaths) {
				File gcda = new File(s).getAbsoluteFile();
				if (gcda.getName().equals(initialGcdaFile.getName()) && !gcda.equals(initialGcdaFile)) {
					if (!sourcePath.isEmpty()) {
						// hum... another file has the same name...
						// sorry, we have to clean sourcePath
						sourcePath.clear();
						break;
					} else {
						addSourceLookup(sourcePath, initialGcdaFile, gcda);
					}
				}
			}
		}
		
		
		
		for (String gcdaPath: covFilesPaths) {
			String gcnoPath = gcdaPath.replace(".gcda", ".gcno");
			// parse GCNO file
			traceFile = OpenTraceFileStream(gcnoPath, ".gcno", sourcePath);
			if (traceFile == null) return;
			GcnoRecordsParser noRcrd = new GcnoRecordsParser(sourceMap, allSrcs);
			noRcrd.parseData(traceFile);

			// add new functions parsed to AllSrcs array
			for (GcnoFunction f: noRcrd.getFnctns()) {
				allFnctns.add(f);
			}

			//close the input stream
			if(traceFile.getClass() == DataInputStream.class)
				((DataInputStream)traceFile).close();

			// parse GCDA file
			traceFile = OpenTraceFileStream(gcdaPath, ".gcda", sourcePath);
			if (traceFile == null) return;
			if (noRcrd.getFnctns().isEmpty()){
				String message = gcnoPath + " doesn't contain any function:\n";
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
				throw new CoreException(status);
			}
			daRcrd = new GcdaRecordsParser(noRcrd.getFnctns());
			daRcrd.parseGcdaRecord(traceFile);

			//close the input stream
			if(traceFile.getClass() == DataInputStream.class)
				((DataInputStream)traceFile).close();
		}

		// to fill the view title
		if (daRcrd != null)
			nbrPgmRuns = daRcrd.getObjSmryNbrPgmRuns();
		/* process counts from data parsed */

		// solve graph for each function
		for (GcnoFunction gf : allFnctns) {
			gf.solveGraphFnctn();
		}

		// allocate lines
		for (SourceFile sourceFile: allSrcs) {
			sourceFile.createLines();
		}

		// add line counts
		for (GcnoFunction gf : allFnctns) {
			gf.addLineCounts(allSrcs);
		}

		// accumulate lines
		for (SourceFile sf: allSrcs) {
			sf.accumulateLineCounts();
		}

		/* compute counts by folder*/

		// make the folders list
		for (SourceFile sf : allSrcs) {
			File srcFile = new File (sf.getName());
			String folderName = srcFile.getParent();
			if (folderName == null) folderName = "?";
			Folder folder = null;
			for (Folder f: allFolders) {
				if (f.getPath().equals(folderName))
					folder = f;
			}
			if (folder == null){
				folder = new Folder(folderName);
				allFolders.add(folder);
			}
			folder.addSrcFiles(sf);
		}

		// assign sourcesList for each folder
		for (Folder f : allFolders) {
			f.accumulateSourcesCounts();
		}	
	}


	/**
	 * fill the model by count results
	 * @throws CoreException, IOException, InterruptedException
	 */

	public void fillGcovView() {
		// process counts for summary level
		int summaryTotal = 0, summaryInstrumented = 0, summaryExecuted = 0;
		for (Folder f : allFolders) {
			summaryTotal+=f.getNumLines();
			summaryInstrumented+=f.getLinesInstrumented();
			summaryExecuted+=f.getLinesExecuted();
		}		

		// fill rootNode model: the entry of the contentProvider
		rootNode = new CovRootTreeElement("Summary", summaryTotal,summaryExecuted,
				summaryInstrumented);
		IBinaryObject binaryObject = STSymbolManager.sharedInstance.getBinaryObject(new Path(binaryPath));
		
		for (Folder fldr : allFolders) {
			String folderLocation = fldr.getPath();
			CovFolderTreeElement fldrTreeElem = new CovFolderTreeElement(
					rootNode, folderLocation, fldr.getNumLines(), fldr.getLinesExecuted(), 
					fldr.getLinesInstrumented());
			rootNode.addChild(fldrTreeElem);

			for (SourceFile src : fldr.getSrcFiles()) {
				CovFileTreeElement srcTreeElem = new CovFileTreeElement(
						fldrTreeElem, src.getName(), src.getNumLines(), src
						.getLinesExecuted(), src.getLinesInstrumented());
				fldrTreeElem.addChild(srcTreeElem);

				for (GcnoFunction fnctn : src.getFnctns()) {
					String name = fnctn.getName();
					name = STSymbolManager.sharedInstance.demangle(binaryObject, name, project);;
					srcTreeElem.addChild(new CovFunctionTreeElement(
							srcTreeElem, name, fnctn.getSrcFile(), fnctn
							.getFirstLineNmbr(), fnctn.getCvrge()
							.getLinesExecuted(), fnctn.getCvrge()
							.getLinesInstrumented()));
				}
			}
		}
	}

	// transform String path to stream
	private DataInput OpenTraceFileStream(String filePath, String extension, Map<File, File> sourcePath) throws FileNotFoundException{
		File f = new File(filePath).getAbsoluteFile();
		if (f.isFile() && f.canRead()) {
			FileInputStream fis = new FileInputStream(f);
			InputStream inputStream = new BufferedInputStream(fis);
			return new DataInputStream(inputStream);
		} else {
			String postfix = "";
			File dir = null;
			do {
				if ("".equals(postfix)) postfix = f.getName();
				else postfix = f.getName() + File.separator + postfix;
				f = f.getParentFile();
				if (f != null) {
					dir = sourcePath.get(f);
				} else break;
			} while (dir == null);
			
			if (dir != null) {
				f = new File(dir, postfix);
				if (f.isFile() && f.canRead()) {
					return OpenTraceFileStream(f.getAbsolutePath(), extension, sourcePath);
				}
			}
			

			Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			FileDialog fg = new FileDialog(shell, SWT.OPEN);
			fg.setFilterExtensions(new String[] {"*" + extension, "*.*", "*"});
			fg.setFileName(f.getName());
			String s = fg.open();
			if (s == null) return null;
			else {
				f = new File(s).getAbsoluteFile();
				addSourceLookup(sourcePath, f, new File(filePath).getAbsoluteFile());
				if (f.isFile() && f.canRead()) {
					FileInputStream fis = new FileInputStream(f);
					InputStream inputStream = new BufferedInputStream(fis);
					return new DataInputStream(inputStream);
				}
			}
		}
		return null;
	}


	public ArrayList<SourceFile> getAllSrcs() {
		return allSrcs;
	}

	public ArrayList<GcnoFunction> getAllFnctns() {
		return allFnctns;
	}

	public CovRootTreeElement getRootNode() {
		return rootNode;
	}

	public String getBinaryPath() {
		return binaryPath;
	}

	public SourceFile getSourceFile(String sourcePath){
		return sourceMap.get(sourcePath);
	}

	public long getNbrPgmRuns() {
		return nbrPgmRuns;
	}

	/**
	 * Retrieve a list containing gcda paths from a binary file  
	 * @return
	 * @throws CoreException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public List<String> getGCDALocations() throws CoreException, IOException, InterruptedException
	{	
		IBinaryObject binaryObject = STSymbolManager.sharedInstance.getBinaryObject(new Path(binaryPath));
		String binaryPath = binaryObject.getPath().toOSString();
		List<String> l = new LinkedList<String>();
		String cpu = binaryObject.getCPU();
		Process p;
		if ("sh".equals(cpu)) {
			String stringsTool = "sh4strings"; 
			p = getStringsProcess(stringsTool, binaryPath);
			if ( p == null) p = getStringsProcess("sh4-linux-strings", binaryPath);
		} else if ("stxp70".equals(cpu)) {
			String stringsTool = "stxp70v3-strings"; 
			p = getStringsProcess(stringsTool, binaryPath);
		} else if ("st200".equals(cpu)){
			String stringsTool = cpu + "strings";
			p = getStringsProcess(stringsTool, binaryPath);
		} else  {
			String stringsTool = "strings";
			p = getStringsProcess(stringsTool, binaryPath);
		}
		if (p == null) {
			Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, IStatus.ERROR,
					"An error occured during analysis: unable to retrieve gcov data", new IOException());
			Activator.getDefault().getLog().log(status);
			return l;
		}
		ThreadConsumer t = new ThreadConsumer(p, l);
		t.start();
		p.waitFor();
		t.join();
		return l;
	}
	
	
	private Process getStringsProcess(String stringsTool, String binaryPath) {
		try {
			Process p = Runtime.getRuntime().exec(new String[] {stringsTool, binaryPath });
			return p;
		} catch (Exception _) {
			return null;
		}
	}
	
	

	private final class ThreadConsumer extends Thread
	{
		private final Process p;
		private final List<String> list;
		ThreadConsumer(Process p, List<String> files)
		{
			super();
			this.p = p;
			this.list = files;
		}

		public void run()
		{
			try {
				populateGCDAFiles(p.getInputStream());
			} catch (Exception _) {
			}
		}

		private void populateGCDAFiles(InputStream s) throws IOException
		{
			InputStreamReader isr = new InputStreamReader(s);
			LineNumberReader lnr = new LineNumberReader(isr);
			String line = null;
			while ((line =lnr.readLine()) != null) {
				if (line.endsWith(".gcda"))
				{
					// absolute .gcda filepaths retrieved using the "strings" tool may
					// be prefixed by random printable characters so strip leading
					// characters until the filepath starts with "X:/", "X:\", "/"  or "\"
					// FIXME: need a more robust mechanism to locate .gcda files [Bugzilla 329710]
					while ((line.length() > 6) && !line.matches("^([A-Za-z]:)?[/\\\\].*")) {
						line = line.substring(1);
					}
					IPath p = new Path(line);
					String filename = p.toString();
					
					
					if (!list.contains(filename)) list.add(filename);
				}
			}
		}
	}

	public void dumpProcessCovFilesResult(PrintStream ps) throws FileNotFoundException {
		ps.println("Parse gcda and gcno files done, resolve graph algorithm executed, now display results");
		ps.println("- PRINT FUNCTIONS ARRAY : ");
		for (int i = 0; i < allFnctns.size(); i++) {
			ps.println("-- FUNCTION " +i);
			ps.println("     name = " + allFnctns.get(i).getName());
			ps.println("     instrumentd lines = " + allFnctns.get(i).getCvrge().getLinesInstrumented());
			ps.println("     executed lines = "+ allFnctns.get(i).getCvrge().getLinesExecuted());
		}		
		ps.println("- PRINT SRCS ARRAY : ");
		for (int i = 0; i < allSrcs.size(); i++) {
			ps.println("-- FILE " + i);
			ps.println("     name = " + allSrcs.get(i).getName());
			ps.println("     total lines = " + allSrcs.get(i).getNumLines());
			ps.println("     instrumentd lines = "+ allSrcs.get(i).getLinesInstrumented());
			ps.println("     executed lines = "+ allSrcs.get(i).getLinesExecuted());
		}
	}


	/**
	 * @return the sourceMap
	 */
	public HashMap<String, SourceFile> getSourceMap() {
		return sourceMap;
	}
	

	private void addSourceLookup(Map<File, File> map, File hostPath, File compilerPath) {
		while (hostPath.getName().equals(compilerPath.getName())) {
			hostPath = hostPath.getParentFile();
			compilerPath = compilerPath.getParentFile();
		}
		map.put(compilerPath, hostPath);
	}
	
}
