/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - Jeff Briggs, Henry Hughes, Ryan Morse
 *******************************************************************************/

package org.eclipse.linuxtools.systemtap.ui.ide.structures;

import java.util.ArrayList;

import org.eclipse.linuxtools.systemtap.ui.logging.LogManager;
import org.eclipse.linuxtools.systemtap.ui.structures.TreeDefinitionNode;
import org.eclipse.linuxtools.systemtap.ui.structures.TreeNode;
import org.eclipse.linuxtools.systemtap.ui.structures.listeners.IUpdateListener;
import org.eclipse.linuxtools.systemtap.ui.structures.runnable.LoggedCommand;



/**
 * Runs stap -vp1 & stap -up2 in order to get all of the probes/functions
 * that are defined in the tapsets.  Builds probeAlias and function trees
 * with the values obtained from the tapsets.
 * 
 * Ugly code is a result of two issues with getting stap output.  First, 
 * many tapsets do not work under stap -up2.  Second since the output
 * is not a regular language, we can't create a nice lexor/parser combination
 * to do everything nicely.
 * @author Ryan Morse
 */
public class TapsetParser implements Runnable {
	public TapsetParser(String[] tapsets) {
		this.tapsets = tapsets;
		listeners = new ArrayList<IUpdateListener>();
	}
	
	/**
	 * This method sets up everything that is needed before actually creating
	 * a new process. It will run the first pass of stap to build the tapset
	 * tree framework.
	 */
	protected void init() {
		disposed = false;
		functions = new TreeNode("", false);
		probes = new TreeNode("", false);

		String s = readPass1(null);
		parseLevel1(s);
		cleanupTrees();
	}

	/**
	 * This method will initialize everything and then start the process running.
	 * This method should be called by any class wishing to run the parser.
	 */
	public void start() {
		stopped = false;
		init();
		Thread t = new Thread(this, "TapsetParser");
		t.start();
	}
	
	/**
	 * This method chanegs the stop variable which is checked periodically by the
	 * running thread to see if it should stop running.
	 */
	public synchronized void stop() {
		stopped = true;
	}
	
	/**
	 * This method checks to see if the process has been asked to stop
	 * @return Boolean indicating whether or not a stop command has been received
	 */
	public boolean isRunning() {
		return !stopped;
	}
	
	/**
	 * This method checks to see if the process has been disposed or not.
	 * @return Boolean indicating whether or not the parser has been disposed.
	 */
	public boolean isDisposed() {
		return disposed;
	}

	/**
	 * Returns the root node of the tree of functions generated by
	 * parseFiles.  Functions are grouped by source file.
	 * @return A tree of tapset functions grouped by file.
	 */
	public synchronized TreeNode getFunctions() {
		return functions;
	}
	
	/**
	 * Returns the root node of the tree of the probe alias generated by
	 * parseFiles.  Probes are grouped by target location.
	 * @return A tree of tapset probe aliases grouped by probe location.
	 */
	public synchronized TreeNode getProbes() {
		return probes;
	}

	/**
	 * This method checks to see if the parser completed executing on its own.
	 * @return Boolean indicating whether or not the thread finished on its own.
	 */
	public boolean isFinishSuccessful() {
		return successfulFinish;
	}
	
	/**
	 * Runs stap -up2 on both the function and the probe trees.  At this
	 * point the trees are both filled with all data obtained from stap -vp1
	 * After each tree is finished, an update event will be fired so callers
	 * know that they can update.
	 */
	public void run() {
		runPass2Functions();
		fireUpdateEvent();	//Inform listeners that a new batch of functions has variable info
		runPass2Probes();
		fireUpdateEvent();	//Inform listeners that a new batch of probes has variable info
		stop();
		successfulFinish = true;
		fireUpdateEvent();	//Inform listeners that everything is done
	}
	
	/**
	 * This method will register a new listener with the parser
	 * @param listener The listener that will receive updateEvents
	 */
	public void addListener(IUpdateListener listener) {
		if(null != listener)
			listeners.add(listener);
	}
	
	/**
	 * This method will unregister the listener with the parser
	 * @param listener The listener that no longer wants to recieve update events
	 */
	public void removeListener(IUpdateListener listener) {
		if(null != listener)
			listeners.remove(listener);
	}
	
	/**
	 * This method will fire an updateEvent to all listeners.
	 */
	private void fireUpdateEvent() {
		for(int i=0; i<listeners.size(); i++)
			((IUpdateListener)listeners.get(i)).handleUpdateEvent();
	}
	
	/**
	 * Runs the stap with the given options and returns the output generated 
	 * @param options String[] of any optional parameters to pass to stap
	 * @param probe String containing the script to run stap on
	 * @param level integer representing what point to stop stap at (1,2,3,4,5)
	 */
	protected String runStap(String[] options, String probe, int level) {
		String[] script = null;
		
		int size = 4;	//start at 4 for stap, -pX, -e, script
		if(null != tapsets && tapsets.length > 0 && tapsets[0].trim().length() > 0)
			size += tapsets.length<<1;
		if(null != options && options.length > 0 && options[0].trim().length() > 0)
			size += options.length;
		
		script = new String[size];
		script[0] = "stap";
		script[1] = "-p" + level;
		script[size-2] = "-e";
		script[size-1] = probe;
		
		//Add extra tapset directories
		if(null != tapsets && tapsets.length > 0 && tapsets[0].trim().length() > 0) {
			for(int i=0; i<tapsets.length; i++) {
				script[2+(i<<1)] = "-I";
				script[3+(i<<1)] = tapsets[i];
			}
		}
		if(null != options && options.length > 0 && options[0].trim().length() > 0) {
			for(int i=0; i<options.length; i++)
				script[script.length-options.length-2+i] = options[i];
		}
		
		LoggedCommand cmd = new LoggedCommand(script, null, null, 0);
		cmd.start();
		
		//Block to prevent errors.
		while(cmd.isRunning()) {
			try {
				Thread.sleep(100);
			} catch(InterruptedException e) {
				LogManager.logCritical("InterruptedException runStap: " + e.getMessage(), this);
			}
		}
		
		cmd.stop();	//While stop was already called we do this to ensure things are shutdown before proceding
		String s = cmd.getOutput();
		cmd.dispose();
		
		return s;
	}
	
	/**
	 * Returns a String containing all of the content from the files
	 * contained in the tapset libraries.  This file always returns
	 * what ever it could get, even if an exception was generated.
	 * 
	 * stap -vp1 -e 'probe begin{}' 
	 * Will list everything defined in the tapsets
	 * @return the tapset library consolodated into a single string
	 */
	private String readPass1(String script) {
		String[] options;
		if(null == script) {
			script = "probe begin{}";
			options = new String[] {"-v"};
		} else {
			options = null;
		}

		return runStap(options, script, 1);
	}

	/**
	 * Parses the output generated from running stap -vp1. Pulls out all functions
	 * and probe aliases from the provided string. Populates the probe and function
	 * trees.
	 * 
	 * ProbeTree organized as:
	 * 	Root->Files->ProbePoints->Variables
	 * 
	 * FunctionTree organized as:
	 * 	Root->Files->Functions
	 * @param s The entire output from running stap -vp1.
	 */
	private void parseLevel1(String s) {
		String prev = null;
		String prev2 = null;
		StringBuilder token = new StringBuilder("");
		TreeNode parent;
		TreeNode item;
		char currChar;
		boolean isProbe = false;
		
		boolean contains;
		TreeNode child;
		int z;
		
		for(int i=0; i<s.length(); i++) {
			currChar = s.charAt(i);
			
			if(!Character.isWhitespace(currChar) && '}' != currChar && '{' != currChar) {
				token.append(currChar);
			} else if(token.length() > 0){
				prev2 = prev;
				prev = token.toString();
				token.delete(0, token.length());
			}

			//Only check for new values when starting a fresh token.
			if(1 == token.length()) {
				if("probe".equals(prev2) && "=".equals(token.toString())) {
					//Probe alias found
					do {
						currChar = s.charAt(++i);
						token.append(currChar);
					} while('{' != currChar && i < s.length());
					
					parent = probes.getChildAt(probes.getChildCount()-1);
					parent.add(new TreeDefinitionNode("probe " + token.toString().substring(2, token.length()-1), prev, parent.getData().toString(), true));
					isProbe = true;
				} else if("function".equals(prev2)) {
					//Function found
					do {
						currChar = s.charAt(++i);
						token.append(currChar);
					} while(')' != currChar && i < s.length());

					parent = functions.getChildAt(functions.getChildCount()-1);
					parent.add(new TreeDefinitionNode(prev + token.toString(), prev + token.toString(), parent.getData().toString(), true));
					isProbe = false;
				} else if("file".equals(prev2)) {
					//New file started
					if(prev.lastIndexOf('/') > 0)
						prev2 = prev.substring(prev.lastIndexOf('/')+1);
					functions.add(new TreeNode(prev, prev2, false));
					probes.add(new TreeNode(prev, prev2, false));
					isProbe = false;
				}
			} else if(prev2 != null && prev2.length() > 2 && token.length() > 2 && isProbe &&
					  '(' == prev2.charAt(0) && ')' == prev2.charAt(prev2.length()-1) &&
					  '(' == token.charAt(0) && ')' == token.charAt(token.length()-1) &&
					  "=".equals(prev)) {
				//Put all variables in the probe tree
				item = probes.getChildAt(probes.getChildCount()-1);
				prev2 = prev2.substring(1,prev2.length()-1);

				child = item.getChildAt(item.getChildCount()-1);
				contains = false;
				for(z=0; z<child.getChildCount(); z++) {
					if(child.getChildAt(z).toString().equals(prev2)) {
						contains = true;
						break;
					}
				}
			
				if(!contains)
					child.add(new TreeNode(prev2 + ":unknown", prev2, false));
				
				prev2 = null;
			}
		}
	}
	
	/**
	 * This method is used to build up the list of functions that were found
	 * durring the first pass of stap.  These functions will then all be passed
	 * on to have stap -up2 run on them in order to find the variable types
	 * associated with them.
	 */
	private void runPass2Functions() {
		int i, j, k, l=0;
		TreeNode child;
		String function;
		String[] parameters = new String[0];
		StringBuilder probe = new StringBuilder("");

		ArrayList<String> functionNames = new ArrayList<String>();
		
		//Add Functions
		for(i=0; i<functions.getChildCount(); i++) {
			child = functions.getChildAt(i);
			for(j=0; j<child.getChildCount(); j++) {
				probe.delete(0, probe.length());
				function = child.getChildAt(j).toString();
				probe.append(function.substring(0, function.indexOf("(")+1));
				function = function.substring(function.indexOf("(")+1, function.indexOf(")"));
				parameters = function.split(",");

				//Make sure each parameter has a distinct name so there isn't a type problem
				if(parameters[0].length() > 0) {
					for(k=0; k<parameters.length; k++) {
						if(k>0)
							probe.append(",");
						probe.append(parameters[k] + l++);
					}
				}
				probe.append(")\n");
				functionNames.add(probe.toString());
			}
		}
		parameters = (String[])functionNames.toArray(parameters);
		runPass2FunctionSet(parameters, 0, parameters.length-1);
	}
	
	/**
	 * This method runs stap -up2 on the specified group of functions.
	 * If errors result, it will break the batch in half and run again
	 * on each subset
	 * @param funcs The list of all functions available in the tapsets
	 * @param low The lower bound of functions to use in this set
	 * @param high The upper bound of functions to use in this set
	 */
	private void runPass2FunctionSet(String[] funcs, int low, int high) {
		if(low == high)
			return;
		if(stopped)
			return;
		
		StringBuilder functionStr = new StringBuilder("probe begin{\n");
		for(int i=low; i<high; i++)
			functionStr.append(funcs[i]);
		functionStr.append("}\n");

		String result = runStap(new String[] {"-u"}, functionStr.toString(), 2);
		
		if(0 < result.trim().length()) {
			parsePass2Functions(result);
		} else if(low+1 != high) {
			runPass2FunctionSet(funcs, low, low+((high-low)>>1));
			runPass2FunctionSet(funcs, low+((high-low)>>1), high);
		}
	}
	
	/**
	 * Runs stap -up2 on the probe tree.  The tree is broken up into
	 * smaller components to allow components to be completed at a time.
	 */
	private void runPass2Probes() {
		//Add Probes
		TreeNode temp;
		for(int i=0; i<probes.getChildCount(); i++) {
			if(stopped)
				return;
			temp = probes.getChildAt(i);
			runPass2ProbeSet(temp, 0, temp.getChildCount());
		}
	}
	
	/**
	 * Runs stap -up2 on the selected probe group, using high and low
	 * to determin which subelements to select.
	 * @param probe The top level probe group to probe.
	 * @param low The lower bound of child elements of probe to include
	 * @param high The upper bound of child elements of probe to inclue
	 */
	private void runPass2ProbeSet(TreeNode probe, int low, int high) {
		if(low == high)
			return;
		
		TreeNode temp;
		StringBuilder probeStr = new StringBuilder("");
		String result;
		
		for(int i=low; i<high; i++) {
			temp = probe.getChildAt(i);
			if(temp.getData().toString().startsWith("probe"))
				probeStr.append("\nprobe " + temp.toString() + "{}");
			else
				runPass2ProbeSet(temp, 0, temp.getChildCount());
		}
		result = runStap(new String[] {"-u"}, probeStr.toString(), 2);
		
		if(0 < result.trim().length()) {
			boolean success = parsePass2Probes(result, probe);
			if(!success) {
				runPass2ProbeSet(probe, low, low+((high-low)>>1));
				runPass2ProbeSet(probe, low+((high-low)>>1), high);
			}
		} else if(low+1 != high) {
			runPass2ProbeSet(probe, low, low+((high-low)>>1));
			runPass2ProbeSet(probe, low+((high-low)>>1), high);
		}
	}

	/**
	 * Removes all directories that do not contain any fuctions or
	 * probe aliases from both trees.
	 */
	protected void cleanupTrees() {
		for(int i=functions.getChildCount()-1; i>=0; i--) {
			if(0 == functions.getChildAt(i).getChildCount())
				functions.remove(i);
			if(0 == probes.getChildAt(i).getChildCount())
				probes.remove(i);
		}

		functions.sortTree();
		probes.sortTree();
		
		formatProbes();
	}
	
	/**
	 * Reorders the probes tree so that probes are grouped by type
	 * instead of by file they were defined in.
	 * 
	 * ProbeTree organized by class grouping.  ie:
	 * 	syscall
	 * 		syscall.open
	 * 			filename
	 * 			flags
	 * 			mode
	 * 			name
	 * 		syscall.open.return
	 * 			name
	 * 			retstr
	 * 		syscall.read
	 * 		...
	 * 	tcp
	 * 		tcp.disconnect
	 * 		tcp.disconnect.return
	 */
	private void formatProbes() {
		TreeNode probes2 = new TreeNode("", false);
		TreeNode probe, fileNode, probeGroup, probeFolder;
		String directory;
		String[] folders;
		boolean added;
		
		for(int j,i=0; i<probes.getChildCount(); i++) {	//Probe main group
			fileNode = probes.getChildAt(i);
			for(j=0; j<fileNode.getChildCount(); j++) {	//Actual probes
				probe = fileNode.getChildAt(j);

				directory = probe.toString();
				if(directory.endsWith(".return") || directory.endsWith(".entry"))
					directory = directory.substring(0, directory.lastIndexOf('.'));
				folders = directory.split("\\.");

				probeGroup = probes2;
				for(int k=0; k<folders.length-1; k++) {	//Complete path directory
					added = false;
					for(int l=0; l<probeGroup.getChildCount(); l++) {	//Destination folder
						probeFolder = probeGroup.getChildAt(l);
						if(probeFolder.toString().equals(folders[k])) {
							probeGroup = probeFolder;
							added = true;
							break;
						}
					}
					if(!added) {	//Create brand new folder since it doesn't exist yet
						probeFolder = new TreeNode(folders[k], false);
						probeGroup.add(probeFolder);
						probeGroup = probeFolder;
					}
				}
				probeGroup.add(probe);	//Add the probe to its appropriate directory
			}
		}
		probes = probes2;
		probes.sortTree();
	}
	
	/**
	 * Parses the output generated from running stap -up2 on the list of functions. 
	 * Will update the function tree with return values for each function.
	 * @param s The entire output from running stap -up2 on the functions.
	 */
	private void parsePass2Functions(String s) {
		int i, j, k;
		TreeNode child, child2;
		String childString;
		String[] functionLines = new String[0];
		if(s.contains("# functions") && s.contains("# probes"))
			functionLines = s.substring(s.indexOf("# functions"), s.indexOf("# probes")).split("\n");

		//Rename the functions with types
		for(i=0; i<functionLines.length; i++) {
			for(j=0; j<functions.getChildCount(); j++) {
				child = functions.getChildAt(j);
				for(k=0; k<child.getChildCount(); k++) {
					child2 = child.getChildAt(k);
					childString = child2.toString();
					if (childString.indexOf("(") != -1) {
					if(functionLines[i].startsWith(childString.substring(0, childString.indexOf("(")).trim() + ":")) {
						child2.setData(functionLines[i]);
						break;
					}
					}
				}
			}
		}
	}
	
	/**
	 * Parses the output generated from running stap -up2 on the list of probes. 
	 * Will update the probe alias tree with additional variables, as well as
	 * placing the type associated with the variable.
	 * @param s The entire output from running stap -up2 on the provided probeSet.
	 * @param probeSet The group of probes that the String s corresponds to
	 */
	private boolean parsePass2Probes(String s, TreeNode probeSet) {
		LogManager.logDebug("Start parseLevel2Probes: probeSet-" + probeSet, this);
		TreeNode tree = new TreeNode("", false);
		TreeNode probe = null;
		String[] probeLines = null;
		boolean variables = false;
		String line;
		
		if(s.contains("# probes"))
			probeLines = s.substring(s.indexOf("# probes")).split("\n");

		if(null == probeLines)
			return false;
		
		//Build Pass 2 tree
		for(int i=0; i<probeLines.length; i++) {
			line = probeLines[i].trim();
			
			if(line.startsWith("kernel.")) {
				probe = new TreeNode(line, false);
				tree.add(probe);
				//probe = lookupProbe(line, probeSet);
				variables = false;
			} else if(line.equals("# locals") && null != probe) {
				variables = true;
			} else if(null != probe && variables) {
				if(line.contains(":"))
					probe.add(new TreeNode(line, line.substring(0, line.lastIndexOf(":")).trim(), false));
			} else {
				probe = null;
			}
		}
		
		//Consolidate pass1 and pass2 trees
		int i, j, k, l;
		boolean matched;
		TreeNode one, two, oneC, twoC;
		for(i=0; i<probeSet.getChildCount(); i++) {
			for(j=0; j<tree.getChildCount(); j++) {
				one = probeSet.getChildAt(i);
				two = tree.getChildAt(j);

				if(probesMatch(one, two)) {
					for(l=0; l<two.getChildCount(); l++) {
						matched = false;
						twoC = two.getChildAt(l);
						for(k=0; k<one.getChildCount(); k++) {
							oneC = one.getChildAt(k);
							if(oneC.getData().toString().substring(0, oneC.getData().toString().indexOf(":")).
								equals(twoC.getData().toString().substring(0, twoC.getData().toString().indexOf(":")))) {
								oneC.setData(twoC.getData());
								matched = true;
							}
						}
						if(!matched)
							one.add(new TreeNode(twoC.getData(), twoC.toString(), false));
					}
				}
			}
		}
		
		tree.dispose();
		tree = null;
		return true;
	}
	
	/**
	 * Compares the probes contained in the treeNodes to make sure they
	 * are actually probing the same kernel location.
	 * @param one A treeNode generated from stap -p1.
	 * @param two A treeNode generated from stap -up2.
	 * @return A boolean signifing if the treeNodes represent the same probe point.
	 */
	private boolean probesMatch(TreeNode one, TreeNode two) {
		try {
			String valOneA = one.getData().toString();
			String valTwoA = two.getData().toString();
			String valOneB = "";
			String valTwoB = valTwoA.substring(valTwoA.indexOf("\"")+1, valTwoA.indexOf("@"));
	
			if(valOneA.contains("\""))
				valOneB = valOneA.substring(valOneA.indexOf("\"")+1);
			if(valOneB.contains("\""))
				valOneB = valOneB.substring(0, valOneB.indexOf("\""));
	
			if(valOneB.equals(valTwoB)) {
				if(valOneA.contains(".return") == valTwoA.contains(".return"))
					return true;
			}
		} catch(Exception e) {
			LogManager.logCritical("Exception probesMatch: " + e.getMessage() + "\n" + one + "\n" + two, this);
		}
		return false;
	}
	
	/**
	 * This method will clean up everything from the run.
	 */
	public void dispose() {
		if(!disposed) {
			disposed = true;
			functions.dispose();
			functions = null;
			probes.dispose();
			probes = null;
			tapsets = null;
			listeners.clear();
			listeners = null;
		}
	}
	
	private boolean stopped = true;
	private boolean disposed = true;
	private boolean successfulFinish = false;
	private ArrayList<IUpdateListener> listeners;
	private TreeNode functions;
	private TreeNode probes;
	private String[] tapsets;
}