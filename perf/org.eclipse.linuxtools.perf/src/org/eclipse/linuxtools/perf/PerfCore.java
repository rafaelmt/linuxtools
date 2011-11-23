/*******************************************************************************
 * (C) Copyright 2010 IBM Corp. 2010
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thavidu Ranatunga (IBM) - Initial implementation.
 *******************************************************************************/
package org.eclipse.linuxtools.perf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.linuxtools.perf.model.TreeParent;
import org.eclipse.linuxtools.perf.model.PMCommand;
import org.eclipse.linuxtools.perf.model.PMDso;
import org.eclipse.linuxtools.perf.model.PMEvent;
import org.eclipse.linuxtools.perf.model.PMFile;
import org.eclipse.linuxtools.perf.model.PMSymbol;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

public class PerfCore {
	public static String spitStream(BufferedReader br, String blockTitle, PrintStream print) {

		StringBuffer strBuf = new StringBuffer();
		String line = null;
        try {
			while (( line = br.readLine()) != null){
				strBuf.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		String str = strBuf.toString();
		if (!str.trim().equals("")) {
			if (print != null) {
				print.println(blockTitle + ": \n" +str + "\n END OF " + blockTitle);
			} else {
				System.out.println(blockTitle + ": \n" +str + "\n END OF " + blockTitle);
			}
		}
		return str;
	}
	private static HashMap<String,ArrayList<String>> eventList = null;
	public static HashMap<String,ArrayList<String>> getEventList() {
		//cache'ing
		if (eventList == null) {
			//if (PerfPlugin.DEBUG_ON) System.out.println("Event list cache empty, loading new event list.");
			eventList = loadEventList();
		}
		return eventList;
	}
	public static HashMap<String,ArrayList<String>> loadEventList() {
		HashMap<String,ArrayList<String>> events = new HashMap<String,ArrayList<String>>();
		Process p = null;
		BufferedReader input = null;
		BufferedReader error = null;
		try {
			// Alternatively can try with -i flag
			p = Runtime.getRuntime().exec("perf list"); //(char 1 as -t is a custom field seperator						
			p.waitFor();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			spitStream(error,"Perf Report (eventsList) STDERR", null);
		} catch( IOException e ) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}		
		String line;
		try {
			while (( line = input.readLine()) != null){
				if (line.contains("[")) {
					String event;
					String cat;
					if (line.contains(PerfPlugin.STRINGS_HWBREAKPOINTS)) {
						cat = PerfPlugin.STRINGS_HWBREAKPOINTS;
						event = line.substring(1,line.indexOf("[", PerfPlugin.MagicPerfListNumber)).trim();
					} else if (line.contains(PerfPlugin.STRINGS_RAWHWEvents)) {
						cat = PerfPlugin.STRINGS_RAWHWEvents;
						event = line.substring(1,line.indexOf("[", PerfPlugin.MagicPerfListNumber)).trim();
					} else {
						event = line.substring(1,PerfPlugin.MagicPerfListNumber).trim(); //magic number but corresponds with parse-events.c in Perf.
						if (event.contains("OR")) {
							event = event.split("OR")[0]; //filter out the abbreviations.
						}
						cat = line.substring(PerfPlugin.MagicPerfListNumber + 1).split("\\]")[0].trim();
					}
					ArrayList<String> catevs = events.get(cat);
					if (catevs == null) {
						catevs = new ArrayList<String>();
						events.put(cat, catevs);
					}
					catevs.add(event.trim());
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return events;
	}
	//Gets the current version of perf
	public static String getPerfVersion(String[] environ, File workingDir) {
		Process p = null;
		try {
			if (workingDir == null) {	
					p = Runtime.getRuntime().exec("perf --version");				
			} else {
				p = Runtime.getRuntime().exec("perf --version", environ, workingDir); //runs with a specific working dir and environment.
			}			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		//p.waitFor();
		BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
		return spitStream(input, "Perf --version STDOUT", null);
	}
	
	//Generates a perf record command string with the options set in the given config. (If null uses default).
	public static String getRecordString(ILaunchConfiguration config) {
		String base = "perf record -f";
		if (config == null) {
			return base;
		} else {
			String s = base;
			try {
				if (config.getAttribute(PerfPlugin.ATTR_Record_Realtime, PerfPlugin.ATTR_Record_Realtime_default))
					s = s.concat(" -r");
				if (config.getAttribute(PerfPlugin.ATTR_Record_Verbose, PerfPlugin.ATTR_Record_Verbose_default))
					s = s.concat(" -v");
				if (config.getAttribute(PerfPlugin.ATTR_Multiplex, PerfPlugin.ATTR_Multiplex_default))
					s = s.concat(" -M");
				ArrayList<String> selE = (ArrayList<String>) config.getAttribute(PerfPlugin.ATTR_SelectedEvents, PerfPlugin.ATTR_SelectedEvents_default);
				if (!config.getAttribute(PerfPlugin.ATTR_DefaultEvent, PerfPlugin.ATTR_DefaultEvent_default) 
														&& selE != null) {					
					for(String e : selE) {
						s = s.concat(" -e " + e);
					}
				}
			} catch (CoreException e) { }			
			return s;
		}
	}

	public static String[] getReportString(ILaunchConfiguration config, String perfDataLoc) {
		ArrayList<String> base = new ArrayList<String>(); 
		base.addAll( Arrays.asList( ("perf report --sort comm,dso,sym -n -t " + (char)1).split(" ") ) );//(char 1 as -t is a custom field seperator)
		if (config != null) {
			try {
				String kernelLoc = config.getAttribute(PerfPlugin.ATTR_Kernel_Location, PerfPlugin.ATTR_Kernel_Location_default);
				if (kernelLoc != PerfPlugin.ATTR_Kernel_Location_default) {
					base.add("--vmlinux");
					base.add(kernelLoc);
				}
				if (config.getAttribute(PerfPlugin.ATTR_ModuleSymbols, PerfPlugin.ATTR_ModuleSymbols_default))
					base.add("-m");
				if (config.getAttribute(PerfPlugin.ATTR_HideUnresolvedSymbols, PerfPlugin.ATTR_HideUnresolvedSymbols_default))
					base.add("-U");
				if (perfDataLoc != null) {
					base.add("-i");
					base.add(perfDataLoc);
				}
			} catch (CoreException e) { }			
		}
		return (String[])base.toArray( new String[base.size()] );
	}

	public static String[] getAnnotateString(ILaunchConfiguration config, String dso, String symbol, String perfDataLoc, boolean OldPerfVersion) {
		ArrayList<String> base = new ArrayList<String>();
		if (OldPerfVersion) {
			base.addAll( Arrays.asList( new String[]{"perf", "annotate", "-s", symbol, "-l", "-P"} ) );
		} else {
			base.addAll( Arrays.asList( new String[]{"perf", "annotate", "-d", dso, "-s", symbol, "-l", "-P"} ) );
		}
		if (config != null) {
			try {
				String kernelLoc = config.getAttribute(PerfPlugin.ATTR_Kernel_Location, PerfPlugin.ATTR_Kernel_Location_default);
				if (kernelLoc != PerfPlugin.ATTR_Kernel_Location_default) {
					base.add("--vmlinux");
					base.add(kernelLoc);
				}
				if (config.getAttribute(PerfPlugin.ATTR_ModuleSymbols, PerfPlugin.ATTR_ModuleSymbols_default))
					base.add("-m");
				if (perfDataLoc != null) {
					base.add("-i");
					base.add(perfDataLoc);
				}
			} catch (CoreException e) { }			
		}
		
		//(Annotate string per symbol)
		//if (PerfPlugin.DEBUG_ON) System.out.println(Arrays.toString( (String[])base.toArray( new String[base.size()] ) ));
		return (String[])base.toArray( new String[base.size()] );
	}
	//Runs Perf Record on the given binary and records into perf.data before calling Report() to feed in the results. 
	public static void Record(String binaryPath) {
		BufferedReader error = null;
		try {
			Process perfRecord = Runtime.getRuntime().exec(getRecordString(null) + " " + binaryPath);
			error = new BufferedReader(new InputStreamReader(perfRecord.getErrorStream()));
			perfRecord.waitFor();			
			spitStream(error,"Perf Record STDERR", null);
		} catch( IOException e ) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		Report();
	}
	public static void Report() {
		Report(null,null,null,null,null,null);
	}
	// Runs assuming perf.data has already been recorded, environ and workingDir can be set to null to use default
	//perfDataLoc is optional - it is used to provide a pre-existing data file instead of something recorded from
	//whatever project is being profiled. It is only used for junit tests atm.
    public static void Report(ILaunchConfiguration config, String[] environ, File workingDir, IProgressMonitor monitor, String perfDataLoc, PrintStream print) {
		TreeParent invisibleRoot = PerfPlugin.getDefault().getModelRoot(); 
		if (invisibleRoot == null) {
			invisibleRoot = new TreeParent("");
			PerfPlugin.getDefault().setModelRoot(invisibleRoot);
		} else {
			invisibleRoot.clear();
		}
		
		boolean OldPerfVersion = false;
		if (getPerfVersion(environ, workingDir).contains("perf version 0.0.2.PERF")) {
			OldPerfVersion = true;
			if (print != null) { print.println("WARNING: You are running an older version of Perf, please update if you can. The plugin may produce unpredicatable results."); }
		}
		
		
		BufferedReader input = null;
		BufferedReader error = null;
		String line = null;
		Process p = null;
		String items[];
		float percent;
		
		if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
		
		try {
			// Alternatively can try with -i flag
			if (workingDir == null) {
				p = Runtime.getRuntime().exec(getReportString(config, perfDataLoc));				
			} else {
				p = Runtime.getRuntime().exec(getReportString(config, perfDataLoc), environ, workingDir); //runs with a specific working dir and environment.
			}			
			//p.waitFor();
			input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
			//spitting error stream moved to end of while loop, due to commenting of p.waitFor()
		} catch( IOException e ) {
			e.printStackTrace();
		/*} catch (InterruptedException e) {
			e.printStackTrace();*/
		}
		
		if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
		
		line = null;
		Double samples;
		String comm,dso,symbol;
		boolean kernelFlag;
		PMEvent currentEvent = null;
		PMCommand currentCommand = null;
		PMDso currentDso = null;
		PMFile currentFile = null;
		PMSymbol currentSym = null;
        try {
        	//Set up the event parent depending on whats selected.
        	if (config.getAttribute(PerfPlugin.ATTR_DefaultEvent, PerfPlugin.ATTR_DefaultEvent_default)) {
        		currentEvent = new PMEvent("Default Event");
        		invisibleRoot.addChild(currentEvent);
        	} else if (!config.getAttribute(PerfPlugin.ATTR_MultipleEvents, PerfPlugin.ATTR_MultipleEvents_default)) {
        		ArrayList<String> selE = (ArrayList<String>) config.getAttribute(PerfPlugin.ATTR_SelectedEvents, PerfPlugin.ATTR_SelectedEvents_default);
        		if (selE != null) {
        			currentEvent = new PMEvent(selE.get(0));
        		} else {
        			// this should never happen.
        			currentEvent = new PMEvent("Error please fix profiling events chosen in launch config");
        		}
        	}
        	
        	
			while (( line = input.readLine()) != null){
				if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
				//System.out.println("Reading line: " + line);
				if ((line.startsWith("#"))) {
					//if (PerfPlugin.DEBUG_ON) System.out.println("Reading line: " + line);
					// # is comment line, but if we're in multi-event mode then we need to scan for event name.
					if (line.contains("Events:") 
							&& config.getAttribute(PerfPlugin.ATTR_MultipleEvents, PerfPlugin.ATTR_MultipleEvents_default)
							&& !config.getAttribute(PerfPlugin.ATTR_DefaultEvent, PerfPlugin.ATTR_DefaultEvent_default)) {
						String[] tmp = line.trim().split(" ");
						currentEvent = new PMEvent(tmp[tmp.length - 1]);
						//if (PerfPlugin.DEBUG_ON) System.out.println("Event is " + tmp[tmp.length - 1]);
						invisibleRoot.addChild(currentEvent);
						currentCommand = null;
					} else if (line.contains("Samples:")) { //"samples" was used instead of events in an older version, some incompatibilities may arise.
						if (print != null) { print.println("WARNING: You are running an older version of Perf, please update if you can. The plugin may produce unpredicatable results."); }
						invisibleRoot.addChild(new PMEvent("WARNING: You are running an older version of Perf, the plugin may produce unpredicatable results."));
					}
				} else {
					items = line.trim().split(""+(char)1); // using custom field separator. for default whitespace use " +"
					if (items.length != 5) { if (!line.trim().equals("")) { System.err.println("Err INVALID: " + line + "//length:" + items.length); }; continue; }
					percent = Float.parseFloat(items[0].substring(0, items[0].length() - 1)); //percent column
					samples = Double.parseDouble(items[1]); //samples column
					comm = items[2]; //command column
					dso = items[3]; //dso column
					symbol = items[4]; //symbol column 
					kernelFlag = (""+symbol.charAt(1)).equals("k");			
					
					//if (PerfPlugin.DEBUG_ON) System.out.println(percent + "//" + samples + "//" + comm + "//" + dso + "//" + kernelFlag + "//" + symbol);
					if ((currentCommand == null) || (!currentCommand.getName().equals(comm))) {
						currentCommand = currentEvent.getCommand(comm);
						if(currentCommand == null) {
							currentCommand = new PMCommand(comm);
							currentEvent.addChild(currentCommand);
						}
					}
					if ((currentDso == null) || (!currentDso.getName().equals(dso))) {
						currentDso = currentCommand.getDso(dso);
						if (currentDso == null) {
							currentDso = new PMDso(dso,kernelFlag);
							currentCommand.addChild(currentDso);
						}
					}
					currentFile = currentDso.getFile(PerfPlugin.STRINGS_UnfiledSymbols); // Store in unfiled for now, will re-organize during perf-annotate if the source code is available. hehe pun intended -unfiled literally ;)
					currentSym = new PMSymbol(symbol, samples, percent);
					currentFile.addChild(currentSym);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (CoreException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		spitStream(error,"Perf Report STDERR", print);

		boolean SourceLineNumbers = PerfPlugin.ATTR_SourceLineNumbers_default;
		boolean Kernel_SourceLineNumbers = PerfPlugin.ATTR_Kernel_SourceLineNumbers_default;
		try {
			//Check if the user has selected the option to skip the following block, or part of it.
			SourceLineNumbers = config.getAttribute(PerfPlugin.ATTR_SourceLineNumbers, PerfPlugin.ATTR_SourceLineNumbers_default);
			Kernel_SourceLineNumbers = config.getAttribute(PerfPlugin.ATTR_Kernel_SourceLineNumbers, PerfPlugin.ATTR_Kernel_SourceLineNumbers_default);
			//if (!SourceLineNumbers && PerfPlugin.DEBUG_ON) System.out.println("Skipping source lines");
			//if (!Kernel_SourceLineNumbers && PerfPlugin.DEBUG_ON) System.out.println("Skipping kernel source lines");
		} catch (CoreException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
		
		if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
		
		if (SourceLineNumbers) {
			for (TreeParent ev : invisibleRoot.getChildren()) {
				if (!(ev instanceof PMEvent)) continue;
				currentEvent = (PMEvent)ev;
				for (TreeParent c : currentEvent.getChildren()) {
					if (!(c instanceof PMCommand)) continue;
					currentCommand = (PMCommand)c;
					for (TreeParent d : currentCommand.getChildren()) {
						if (!(d instanceof PMDso)) continue;					
						currentDso = (PMDso)d;
						if ((!Kernel_SourceLineNumbers) && currentDso.isKernelDso()) continue;
						for (TreeParent s : currentDso.getFile(PerfPlugin.STRINGS_UnfiledSymbols).getChildren()) {
							if (!(s instanceof PMSymbol)) continue;
							
							if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
							
							currentSym = (PMSymbol)s;
							try {
								final String[] annotateCmd = getAnnotateString(config, currentDso.getName(), currentSym.getName().substring(4), perfDataLoc, OldPerfVersion);
								if (workingDir == null) {
									p = Runtime.getRuntime().exec(annotateCmd);
								} else {
									p = Runtime.getRuntime().exec(annotateCmd, environ, workingDir);
								}
								//p.waitFor(); // actually, readLine() in the while later automatically 'waits' when theres nothing left to read but not terminated yet but if we wait in rare occurances perf never exits as the buffer fills up apparently
								input = new BufferedReader(new InputStreamReader(p.getInputStream()));
								error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
								//spitStream(input,"Perf Annotate INPUT");
								//spitStream(error,"Perf Annotate STDERR"); Leaving this on, with waitFor commented out hangs because the input buffer is never read so its still waiting for it to free up. Moved to end.
								
								
							} catch( IOException e ) {
								e.printStackTrace();
							/*} catch (InterruptedException e) {
								e.printStackTrace();*/
							}
							
							if (monitor != null && monitor.isCanceled()) { RefreshView(); return; }
							
							boolean grabBlock = false;
							boolean blockStarted = false;
							String dsoName,lineRef;
					        try {
								while (( line = input.readLine()) != null){
									if (line.startsWith("Sorted summary for file")) {
										grabBlock = true;
										dsoName = line.replace("Sorted summary for file ","");
										blockStarted = false;
										//if (PerfPlugin.DEBUG_ON) System.out.println("Grabbing " + dsoName);
										if ((workingDir != null) && (dsoName.startsWith("./"))) {
											if (workingDir.getAbsolutePath().endsWith("/")) {
												dsoName = workingDir.getAbsolutePath() + dsoName.substring(2); // path already ends with '/', so trim './' 
											} else {
												dsoName = workingDir.getAbsolutePath() + dsoName.substring(1); // path doesn't have '/', so trim just the '.'
											}
										}
										currentDso.setPath(dsoName);
									} else if (line.startsWith("---")) {
										if (blockStarted) {
											blockStarted = false;
											grabBlock = false;
										} else {
											blockStarted = true;
										}
									} else if (grabBlock && blockStarted) {
										//process the line.
										items = line.trim().split(" +");
										if (items.length != 2) { if (!line.trim().equals("")) { System.err.println("Err INVALID: " + line); }; continue; }
										percent = Float.parseFloat(items[0].substring(0, items[0].length() - 1));
										lineRef = items[1];
										items = lineRef.split(":");
										if (currentDso == null) { 
											//if (PerfPlugin.DEBUG_ON) System.err.println("Parsed line ref without being in valid block, shouldn't happen.");
											break; 
										} else {
											currentSym.addPercent(Integer.parseInt(items[1]), percent);
											if (currentSym.getParent().getName().equals(PerfPlugin.STRINGS_UnfiledSymbols)) {
												//Symbol currently in unfiled symbols (from Perf Report), move it into it proper area.
												currentSym.getParent().removeChild(currentSym);
												currentDso.getFile(items[0]).addChild(currentSym);
											} else if (!currentSym.getFile().getPath().equals(items[0])) {
												//if (PerfPlugin.DEBUG_ON) System.err.println("Multiple paths found for this symbol.");
												currentSym.markConflict();
												currentSym.getParent().removeChild(currentSym);
												currentDso.getFile(PerfPlugin.STRINGS_MultipleFilesForSymbol).addChild(currentSym);
											}
										}
										//if (PerfPlugin.DEBUG_ON) System.out.println("pc: " + percent + " lr:" + lineRef);									
									}
								}
							} catch (IOException e) {
								e.printStackTrace();
							}				
						}
						if (currentDso.getFile(PerfPlugin.STRINGS_UnfiledSymbols).getChildren().length == 0) {
							currentDso.removeChild(currentDso.getFile(PerfPlugin.STRINGS_UnfiledSymbols));
						}
						spitStream(error,"Perf Annotate STDERR", print);
					}
				}
			}
		}
		
		RefreshView();
    }
    
    public static void RefreshView() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				//Try to switch the active view to Perf.
				try {
					PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView(PerfPlugin.VIEW_ID);
					PerfPlugin.getDefault().getProfileView().refreshModel();
				} catch (NullPointerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();					
				} catch (PartInitException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
    }
}

