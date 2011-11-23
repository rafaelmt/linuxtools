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

import java.io.DataInput;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.gcov.Activator;
import org.eclipse.linuxtools.gcov.utils.BEDataInputStream;
import org.eclipse.linuxtools.gcov.utils.GcovStringReader;
import org.eclipse.linuxtools.gcov.utils.LEDataInputStream;
import org.eclipse.linuxtools.gcov.utils.MasksGenerator;


public class GcnoRecordsParser {
	
	private static final int GCOV_NOTE_MAGIC = 0x67636e6f; // en ASCII: 67=g 63=c 6e=n 6f=o
	private static final int GCOV_TAG_FUNCTION = 0x01000000; 
	private static final int GCOV_TAG_BLOCKS = 0x01410000;
	private static final int GCOV_TAG_ARCS = 0x01430000;
	private static final int GCOV_TAG_LINES = 0x01450000;
	
	private GcnoFunction fnctn = null;	
	private final ArrayList<GcnoFunction> fnctns  = new ArrayList<GcnoFunction>();
	private final ArrayList<SourceFile> currentAllSrcs;
	private final HashMap<String, SourceFile> sourceMap;
	
	public GcnoRecordsParser(HashMap<String, SourceFile> sourceMap, ArrayList<SourceFile> AllSrcs){
		this.sourceMap = sourceMap;
		this.currentAllSrcs = AllSrcs;
	}
	
	private SourceFile findOrAdd (String fileName, ArrayList<SourceFile> srcs)
	{		
		SourceFile newsrc = sourceMap.get(fileName);
		if (newsrc == null) {
			 newsrc = new SourceFile(fileName, srcs.size()+1);
			 srcs.add(newsrc);
			 sourceMap.put(fileName, newsrc);
		}
		return newsrc; //return the new added element	
	}
	
	
	public void parseData(DataInput stream) throws IOException, CoreException{
		// header data
		int magic = 0;
		// blocks data
		ArrayList<Block> blocks = null;
		// source file data
		SourceFile source = null;
		// flag
		boolean parseFirstFnctn = false;
		
		magic = stream.readInt();
		//version = stream.readInt();
		stream.readInt();
		//stamp = stream.readInt();
		stream.readInt();
		
		if (magic == GCOV_NOTE_MAGIC){
			stream = new BEDataInputStream((DataInputStream) stream);
		}else{
			magic = (magic >> 16) |	(magic << 16);
			magic = ((magic	& 0xff00ff)	<< 8) |	((magic	>> 8) &	0xff00ff);
			if (magic == GCOV_NOTE_MAGIC){
				stream = new LEDataInputStream((DataInputStream) stream);
			}else{
				String message = magic + " :desn't correspond to a correct note file header\n";
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
				throw new CoreException(status);
			}
		}
		
		/*------------------------------------------------------------------------------
		System.out.println("Gcno LE, Magic "+magic+" version "+version+" stamp "+stamp);
		*/
		
		while (true){
			try{		
				int tag;
				// parse header
				while (true){
					tag=stream.readInt();
					if (tag == GCOV_TAG_FUNCTION || tag == GCOV_TAG_BLOCKS || 
							tag == GCOV_TAG_ARCS || tag == GCOV_TAG_LINES)  
						break;
				}
				int length=stream.readInt();

				// parse gcno data
				if ((int)tag ==  GCOV_TAG_FUNCTION){
					// before parse new function, add current function to functions list
					if (parseFirstFnctn == true) fnctns.add(fnctn);
					
					long fnctnIdent = (stream.readInt()&MasksGenerator.UNSIGNED_INT_MASK);
					long fnctnChksm = (stream.readInt()&MasksGenerator.UNSIGNED_INT_MASK);
					String fnctnName = GcovStringReader.readString(stream);
					String fnctnSrcFle = GcovStringReader.readString(stream);
					long fnctnFrstLnNmbr= (stream.readInt()&MasksGenerator.UNSIGNED_INT_MASK);
					
					fnctn = new GcnoFunction(fnctnIdent, fnctnChksm, fnctnName, fnctnSrcFle, fnctnFrstLnNmbr);
					SourceFile srcFle2 = findOrAdd (fnctn.getSrcFile(),currentAllSrcs);
					if (fnctn.getFirstLineNmbr() >= srcFle2.getNumLines()){
						srcFle2.setNumLines((int)fnctn.getFirstLineNmbr()+1);
					}
					srcFle2.addFnctn(fnctn);
			        parseFirstFnctn = true;
					continue;
				}
				
				else if ((int)tag ==  GCOV_TAG_BLOCKS){
					blocks = new ArrayList<Block>();
					for (int i = 0; i < length; i++) {
						long BlckFlag = stream.readInt()& MasksGenerator.UNSIGNED_INT_MASK;
						Block blck = new Block(BlckFlag);
						blocks.add(blck);
					}
					fnctn.setNumBlocks(length);
					continue;
				}	
				else if (tag ==  GCOV_TAG_ARCS){
					int srcBlockIndice = stream.readInt();
					int nmbrArcs = (length-1)/2;
					 ArrayList<Arc> arcs = new ArrayList<Arc>(nmbrArcs);

					for (int i = 0; i < nmbrArcs; i++) {
						int dstnatnBlockIndice = stream.readInt();
						long flag = (stream.readInt()&MasksGenerator.UNSIGNED_INT_MASK);
						Arc arc = new Arc(srcBlockIndice, dstnatnBlockIndice, flag, blocks);
						arcs.add(arc);					
					}
					
					// each arc, register it as exit of the src block
					Block srcBlk = blocks.get(srcBlockIndice);													
					for (Arc a : arcs) {
						srcBlk.addExitArcs(a);
						srcBlk.incNumSuccs();
					}
					
					// each arc, register it as entry of its dstntn block
					for (Arc a : arcs) {
						Block dstntnBlk = a.getDstnatnBlock();
						dstntnBlk.addEntryArcs(a);
						dstntnBlk.incNumPreds();
					}
					
					for (Arc a : arcs) {
						if (a.isFake() == true) {
							if (a.getSrcBlock() != null ) {
								 // Exceptional exit from this function, the 
								 // 	source block must be a call.
								srcBlk = blocks.get((int)srcBlockIndice);		
								srcBlk.setCallSite(true);
								a.setCallNonReturn(true);
							} else {
								a.setNonLoclaReturn(true);
								Block dstntnBlk = a.getDstnatnBlock();
								dstntnBlk.setNonLocalReturn(true);
							}	
						}
						
						if (a.isOnTree() == false) 
							fnctn.incNumCounts();
						 	//nbrCounts++;							
					}
					
					fnctn.setFunctionBlocks(blocks);
					continue;
				}
				
				
				else if (tag ==  GCOV_TAG_LINES) {
					int numBlock = stream.readInt();
					long[] lineNos = new long[length-1];			
					int ix = 0;
					do {
						long lineNumber = stream.readInt()&MasksGenerator.UNSIGNED_INT_MASK;
						if (lineNumber != 0){
							if (ix == 0){
								lineNos[ix++] = 0;
								lineNos[ix++] = source.getIndex();
							}
							lineNos[ix++]=lineNumber;
							if (lineNumber >= source.getNumLines()){
								source.setNumLines((int)lineNumber+1);
							}
						} else {
							String fileName = GcovStringReader.readString(stream);
							if (fileName == "NULL string") 
								break;

								source = findOrAdd (fileName, currentAllSrcs);
								lineNos[ix++]=0;
								lineNos[ix++]=source.getIndex();
						}
					} while (true);
					
					fnctn.getFunctionBlocks().get(((int)numBlock))
					.setEncoding(lineNos);
				
					fnctn.getFunctionBlocks().get(((int)numBlock))
					.setNumLine(ix);
					
					continue;
				}
			}
			catch (EOFException e) {				
				
				fnctn.setFunctionBlocks(blocks);
				fnctns.add(fnctn);	
							
				break;			

			}
		}//while		
	}
	
	/* Getters */
	public ArrayList<GcnoFunction> getFnctns() {
		return fnctns;
	}
	
	public ArrayList<SourceFile> getcurrentAllSrcs() {
		return currentAllSrcs;
	}
}
