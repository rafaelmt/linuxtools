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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.linuxtools.gcov.Activator;
import org.eclipse.linuxtools.gcov.utils.BEDataInputStream;
import org.eclipse.linuxtools.gcov.utils.LEDataInputStream;
import org.eclipse.linuxtools.gcov.utils.MasksGenerator;



public class GcdaRecordsParser {


	private static final int GCOV_DATA_MAGIC = 0x67636461; // en ASCII: 67=g 63=c 64=d 61=a
	private static final int GCOV_TAG_FUNCTION = 0x01000000; 
	private static final  int GCOV_COUNTER_ARCS = 0x01a10000; 
	private static final int GCOV_TAG_OBJECT_SYMMARY = 0xa1000000; 
	private static final int GCOV_TAG_PROGRAM_SUMMARY = 0xa3000000;

	private final ArrayList<GcnoFunction> fnctns;
	private long objSmryNbrPgmRuns = 0;
	private long pgmSmryChksm = 0;
	private long objSmryChksm = 0;
	private long objSmryArcCnts = 0;
	private long objSmrytotalCnts = 0;
	private long objSmryRunMax = 0;
	private long objSmrySumMax = 0;


	public GcdaRecordsParser(ArrayList<GcnoFunction> fnctns) {
		this.fnctns = fnctns;
	}

	public void parseGcdaRecord(DataInput stream) throws IOException, CoreException {
		// header data
		int magic = 0;

		// data & flags to process tests
		GcnoFunction currentFnctn = null;

		//read magic
		magic = stream.readInt();
		//read version
		//version = stream.readInt();
		stream.readInt();
		//read stamp
		//stamp = stream.readInt();
		stream.readInt();

		if (magic == GCOV_DATA_MAGIC){
			stream = new BEDataInputStream((DataInputStream) stream);
		}else{
			magic = (magic >> 16) |	(magic << 16);
			magic = ((magic	& 0xff00ff)	<< 8) |	((magic	>> 8) &	0xff00ff);
			if (magic == GCOV_DATA_MAGIC){
				stream = new LEDataInputStream((DataInputStream) stream);
			}else{
				String message = magic + " :desn't correspond to a correct data file header\n";
				Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
				throw new CoreException(status);
			}
		}

		while (true) {
			try {
				// parse header
				int tag = stream.readInt();
				long length = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
				// parse gcda data
				switch ((int) tag) {
				case GCOV_TAG_FUNCTION: {
					long fnctnId = stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK;
					if (!fnctns.isEmpty()) {
						boolean fnctnFound = false;
						for (GcnoFunction f: fnctns) {
							if (f.getIdent() == fnctnId) {
								fnctnFound = true;
								currentFnctn = f;
								long fnctnChksm = stream.readInt()&  MasksGenerator.UNSIGNED_INT_MASK;
								if (f.getCheksum() != fnctnChksm){
									String message = "Checksums don't correspond for " +
									currentFnctn.getName() + " (Id: " + fnctnId + ")\n";
									Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
									throw new CoreException(status);
								}
								break;
							}
						}

						if (fnctnFound == false){
							currentFnctn = null;
							String message = "Function with Id: " + fnctnId +
							" not found in function list\n";
							Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
							throw new CoreException(status);
						}

					}
					break;
				}

				case GCOV_COUNTER_ARCS: {
					if (currentFnctn == null){
						String message = "Missing function or duplicate counter tag\n";
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
						throw new CoreException(status);
					}

					if (length != 2 * (currentFnctn.getNumCounts())){
						String message = "GCDA content is inconsistent\n";
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
						throw new CoreException(status);
					}

					ArrayList<Block> fnctnBlcks = currentFnctn.getFunctionBlocks();
					if (fnctnBlcks.isEmpty()){
						String message = "Function block list is empty\n";
						Status status = new Status(Status.ERROR, Activator.PLUGIN_ID, message);
						throw new CoreException(status);
					}

					for (int i = 0; i < fnctnBlcks.size(); i++) {
						Block b = fnctnBlcks.get(i);
						int nonFakeExit = 0;
						
						ArrayList<Arc> arcsExit = b.getExitArcs();
						for (Arc extArc: arcsExit) {
							if (extArc.isFake() == false)
								nonFakeExit++;
							if (extArc.isOnTree() == false) {
								long arcsCnts = stream.readLong();
								extArc.setCount(arcsCnts);
								extArc.setCountValid(true);
								b.decNumSuccs();
								extArc.getDstnatnBlock().decNumPreds();
							}
						}

						// If there is only one non-fake exit, it is an
						// unconditional branch.
						if (nonFakeExit == 1) {
							for (Arc extArc: arcsExit) {
								if (extArc.isFake() == false) {
									extArc.setUnconditionnal(true);

									// If this block is instrumenting a call, it might be
									// an artificial block. It is not artificial if it has
									// a non-fallthrough exit, or the destination of this
									// arc has more than one entry. Mark the destination
									// block as a return site, if none of those conditions hold.

									if (b.isCallSite() == true
											&& extArc.isFallthrough() == true
											&& extArc.getDstnatnBlock()
											.getEntryArcs().get(0)
											.equals(extArc)
											&& extArc.getDstnatnBlock()
											.getEntryArcs().size() == 1)
										extArc.getDstnatnBlock()
										.setCallReturn(true);
								}
							}
						}
					}

					// counters arcs process data reset
					currentFnctn = null;
					break;
				}

				case GCOV_TAG_OBJECT_SYMMARY: {
					objSmryChksm = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
					objSmryArcCnts = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
					objSmryNbrPgmRuns = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
					objSmrytotalCnts = stream.readLong();
					objSmryRunMax = stream.readLong();
					objSmrySumMax = stream.readLong();
					break;
				}

				// program summary tag
				case GCOV_TAG_PROGRAM_SUMMARY: {
					//long[] pgmSmryskips = new long[(int) length];
					pgmSmryChksm = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
					for (int i = 0; i < length - 1; i++) {
						//pgmSmryskips[i] = (stream.readInt() &  MasksGenerator.UNSIGNED_INT_MASK);
						stream.readInt();
					}
					break;
				}

				default: {
					//System.out.println("wrong gcda tag");
					break;
				}
				}
			}
			catch (EOFException _) {
				break;
			}
		}
	}

	/**
	 * @return the objSmryNbrPgmRuns
	 */
	public long getObjSmryNbrPgmRuns() {
		return objSmryNbrPgmRuns;
	}

	/**
	 * @return the objSmryChksm
	 */
	public long getObjSmryChksm() {
		return objSmryChksm;
	}

	/**
	 * @return the objSmryArcCnts
	 */
	public long getObjSmryArcCnts() {
		return objSmryArcCnts;
	}

	/**
	 * @return the objSmrytotalCnts
	 */
	public long getObjSmrytotalCnts() {
		return objSmrytotalCnts;
	}

	/**
	 * @return the objSmryRunMax
	 */
	public long getObjSmryRunMax() {
		return objSmryRunMax;
	}

	/**
	 * @return the objSmrySumMax
	 */
	public long getObjSmrySumMax() {
		return objSmrySumMax;
	}

	/**
	 * @return the pgmSmryChksm
	 */
	public long getPgmSmryChksm() {
		return pgmSmryChksm;
	}



}
