/*******************************************************************************
 * Copyright (c) 2010 Ericsson
 * 
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Matthew Khouzam - Initial API and implementation
 *   Francois Chouinard - Refactoring, slider support, bug fixing 
 *   Patrick Tasse - Improvements and bug fixing
 ******************************************************************************/

package org.eclipse.linuxtools.tmf.ui.widgets;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseWheelListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.PlatformUI;

/**
 * <b><u>TmfVirtualTable</u></b>
 * <p>
 * TmfVirtualTable allows for the tabular display of arbitrarily large data sets
 * (well, up to Integer.MAX_VALUE or ~2G rows).
 * 
 * It is essentially a Composite of Table and Slider, where the number of rows
 * in the table is set to fill the table display area. The slider is rank-based.
 * 
 * It differs from Table with the VIRTUAL style flag where an empty entry is
 * created for each virtual row. This does not scale well for very large data sets.
 * 
 * Styles:
 * H_SCROLL, V_SCROLL, SINGLE, CHECK, FULL_SELECTION, HIDE_SELECTION, NO_SCROLL
 */
public class TmfVirtualTable extends Composite {

	// The table
	private Table   fTable;
	private int     fTableRows         = 0;      // Number of table rows
	private int     fFullyVisibleRows  = 0;      // Number of fully visible table rows
	private int     fFrozenRowCount    = 0;      // Number of frozen table rows at top of table

	private int     fTableTopEventRank = 0;      // Global rank of the first entry displayed
	private int     fSelectedEventRank = 0;      // Global rank of the selected event
	private boolean fPendingSelection  = false;  // Pending selection update

	private int       fTableItemCount  = 0;

	// The slider
	private Slider fSlider;

	private int fLinuxItemHeight = 0;            // Calculated item height for Linux workaround
	private TooltipProvider tooltipProvider = null;
	private IDoubleClickListener doubleClickListener = null;
	
	// ------------------------------------------------------------------------
	// Constructor
	// ------------------------------------------------------------------------

	/**
	 * @param parent
	 * @param style
	 */
	public TmfVirtualTable(Composite parent, int style) {
		super(parent, style & (~SWT.H_SCROLL) & (~SWT.V_SCROLL) & (~SWT.SINGLE) & (~SWT.FULL_SELECTION) & (~SWT.HIDE_SELECTION) & (~SWT.CHECK));

		// Create the controls
		createTable(style & (SWT.H_SCROLL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.HIDE_SELECTION | SWT.CHECK));
		createSlider(style & SWT.V_SCROLL);
		
		// Prevent the slider from being traversed
		setTabList(new Control[] { fTable });

		// Set the layout
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 2;
		gridLayout.horizontalSpacing = 0;
		gridLayout.verticalSpacing   = 0;
		gridLayout.marginWidth  = 0;
		gridLayout.marginHeight = 0;
		setLayout(gridLayout);
		
		GridData tableGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
		fTable.setLayoutData(tableGridData);

		GridData sliderGridData = new GridData(SWT.FILL, SWT.FILL, false, true);
		fSlider.setLayoutData(sliderGridData);

		// Add the listeners
		fTable.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseScrolled(MouseEvent event) {
				if (fTableItemCount <= fFullyVisibleRows) {
					return;
				}
				fTableTopEventRank -= event.count;
				if (fTableTopEventRank < 0) {
					fTableTopEventRank = 0;
				}
				int latestFirstRowOffset = fTableItemCount - fFullyVisibleRows;
				if (fTableTopEventRank > latestFirstRowOffset) {
					fTableTopEventRank = latestFirstRowOffset;
				}

				fSlider.setSelection(fTableTopEventRank);
				refreshTable();
			}
		});

		fTable.addListener(SWT.MouseWheel, new Listener() {
			// disable mouse scroll of horizontal scroll bar
			@Override
            public void handleEvent(Event event) {
				event.doit = false;
			}
		});

		fTable.addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				int tableHeight = Math.max(0, fTable.getClientArea().height - fTable.getHeaderHeight());
				fFullyVisibleRows = tableHeight / getItemHeight();
				if (fTableItemCount > 0) {
					fSlider.setThumb(Math.max(1, Math.min(fTableRows, fFullyVisibleRows)));
				}
			}
		});
		// Implement a "fake" tooltip
		final String TOOLTIP_DATA_KEY = "_TABLEITEM";
		final Listener labelListener = new Listener () {
			public void handleEvent (Event event) {
				Label label = (Label)event.widget;
				Shell shell = label.getShell ();
				switch (event.type) {
				case SWT.MouseDown:
					Event e = new Event ();
					e.item = (TableItem) label.getData (TOOLTIP_DATA_KEY);
					// Assuming table is single select, set the selection as if
					// the mouse down event went through to the table
					fTable.setSelection (new TableItem [] {(TableItem) e.item});
					fTable.notifyListeners (SWT.Selection, e);
					shell.dispose ();
					fTable.setFocus();
					break;
				case SWT.MouseExit:
				case SWT.MouseWheel:
					shell.dispose ();
					break;
				}
			}
		};

		Listener tableListener = new Listener () {
			Shell tip = null;
			Label label = null;
			public void handleEvent (Event event) {
				switch (event.type) {
				case SWT.Dispose:
				case SWT.KeyDown:
				case SWT.MouseMove: {
					if (tip == null) break;
					tip.dispose ();
					tip = null;
					label = null;
					break;
				}
				case SWT.MouseHover: {
					TableItem item = fTable.getItem (new Point(event.x, event.y));
					if (item != null) {
						for(int i=0;i<fTable.getColumnCount();i++){
							Rectangle bounds = item.getBounds(i);
							if (bounds.contains(event.x,event.y)){
								if (tip != null  && !tip.isDisposed()) tip.dispose();
								if (tooltipProvider == null) {
									return;
								} else {
									String tooltipText = tooltipProvider.getTooltip(i, item.getData());
									if (tooltipText == null) return;
									tip = new Shell(fTable.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
									tip.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
									FillLayout layout = new FillLayout();
									layout.marginWidth = 2;
									tip.setLayout(layout);
									label = new Label(tip, SWT.WRAP);
									label.setForeground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
									label.setBackground(PlatformUI.getWorkbench().getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
									label.setData(TOOLTIP_DATA_KEY, item);
									label.setText(tooltipText);

									label.addListener(SWT.MouseExit, labelListener);
									label.addListener(SWT.MouseDown, labelListener);
									label.addListener(SWT.MouseWheel, labelListener);
									Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
									Point pt = fTable.toDisplay(bounds.x, bounds.y);
									tip.setBounds(pt.x, pt.y, size.x, size.y);
									tip.setVisible(true);
								}
								break;
							}
						}
					}
				}
				}
			}
		};
		fTable.addListener(SWT.Dispose, tableListener);
		fTable.addListener(SWT.KeyDown, tableListener);
		fTable.addListener(SWT.MouseMove, tableListener);
		fTable.addListener(SWT.MouseHover, tableListener);
		addControlListener(new ControlAdapter() {
			@Override
			public void controlResized(ControlEvent event) {
				resize();
			}
		});

		// And display
		refresh();
	}

	// ------------------------------------------------------------------------
	// Table handling
	// ------------------------------------------------------------------------

	/**
	 * Create the table and add listeners
	 */
	private void createTable(int style) {
		fTable = new Table(this, style | SWT.NO_SCROLL);

		fTable.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				if (fTable.getSelectionIndices().length > 0) {
					handleTableSelection();
				}
			}
		});

        fTable.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
				handleTableKeyEvent(event);
			}
			@Override
			public void keyReleased(KeyEvent event) {
			}
		});

        fTable.addListener(
        		SWT.MouseDoubleClick, new Listener() {
        			@Override
        			public void handleEvent(Event event) {
        				if (doubleClickListener != null) {
        					TableItem item = fTable.getItem (new Point (event.x, event.y));
        					if (item != null) {
        						for(int i=0;i<fTable.getColumnCount();i++){
        							Rectangle bounds = item.getBounds(i);
        							if (bounds.contains(event.x,event.y)){
        								doubleClickListener.handleDoubleClick(TmfVirtualTable.this, item, i);
        								break;
        							}
        						}
        					}
        				}
        			}
        		}
        );
	}

	/**
	 * Update the rows and selected item
	 */
	private void handleTableSelection() {
		int selectedRow = fTable.getSelectionIndices()[0];
		if (selectedRow < fFrozenRowCount) {
			fSelectedEventRank = selectedRow;
		} else {
			fSelectedEventRank = fTableTopEventRank + selectedRow;
		}

		/*
		 * Feature in Windows. When a partially visible table item is selected,
		 * after ~500 ms the top index is changed to ensure the selected item is
		 * fully visible. This leaves a blank space at the bottom of the virtual
		 * table. The workaround is to update the top event rank, refresh the
		 * table and reset the top index to 0 after a sufficient delay.
		 */
		if (selectedRow >= fFullyVisibleRows) {
			final Display display = fTable.getDisplay();
			Thread thread = new Thread("Top index check") { //$NON-NLS-1$
				@Override
				public void run() {
					try {
						Thread.sleep(600);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					display.asyncExec(new Runnable() {
						@Override
                        public void run() {
							if (fTable.isDisposed()) return;
							int topIndex = fTable.getTopIndex();
							if (topIndex != 0) {
								fTableTopEventRank += topIndex;
								refreshTable();
								fSlider.setSelection(fTableTopEventRank);
								fTable.setTopIndex(0);
							}
						}
					});
				}
			};
			thread.start();
		}
	}

	/**
	 * Handle key-based navigation in table.
	 * 
	 * @param event
	 */
	private void handleTableKeyEvent(KeyEvent event) {

		int lastEventRank        = fTableItemCount - 1;
		int lastPageTopEntryRank = Math.max(0, fTableItemCount - fFullyVisibleRows);

		int previousSelectedEventRank = fSelectedEventRank;
		int selectedRow = fSelectedEventRank - fTableTopEventRank;
		boolean needsRefresh = false;

		// In all case, perform the following steps:
		// - Update the selected entry rank (within valid range)
		// - Update the selected row
		// - Update the page's top entry if necessary (which also adjusts the selected row)
		// - If the top displayed entry was changed, table refresh is needed
		switch (event.keyCode) {

			case SWT.ARROW_DOWN: {
				event.doit = false;
				if (fSelectedEventRank < lastEventRank) {
					fSelectedEventRank++;
					selectedRow = fSelectedEventRank - fTableTopEventRank;
					if (selectedRow >= fFullyVisibleRows) {
						fTableTopEventRank++;
						needsRefresh = true;
					}
				}
				break;
			}

			case SWT.ARROW_UP: {
				event.doit = false;
				if (fSelectedEventRank > 0) {
					fSelectedEventRank--;
					selectedRow = fSelectedEventRank - fTableTopEventRank;
					if (selectedRow < fFrozenRowCount && fTableTopEventRank > 0) {
						fTableTopEventRank--;
						needsRefresh = true;
					}
				}
				break;
			}

			case SWT.END: {
				event.doit = false;
				fTableTopEventRank = lastPageTopEntryRank;
				fSelectedEventRank = lastEventRank;
				needsRefresh = true;
				break;
			}

			case SWT.HOME: {
				event.doit = false;
				fSelectedEventRank = fFrozenRowCount;
				fTableTopEventRank = 0;
				needsRefresh = true;
				break;
			}

			case SWT.PAGE_DOWN: {
				event.doit = false;
				if (fSelectedEventRank < lastEventRank) {
					fSelectedEventRank += fFullyVisibleRows;
					if (fSelectedEventRank > lastEventRank) {
						fSelectedEventRank = lastEventRank;
					}
					selectedRow = fSelectedEventRank - fTableTopEventRank;
					if (selectedRow > fFullyVisibleRows - 1) {
						fTableTopEventRank += fFullyVisibleRows;
						if (fTableTopEventRank > lastPageTopEntryRank) {
							fTableTopEventRank = lastPageTopEntryRank;
						}
						needsRefresh = true;
					}
				}
				break;
			}

			case SWT.PAGE_UP: {
				event.doit = false;
				if (fSelectedEventRank > 0) {
					fSelectedEventRank -= fFullyVisibleRows;
					if (fSelectedEventRank < fFrozenRowCount) {
						fSelectedEventRank = fFrozenRowCount;
					}
					selectedRow = fSelectedEventRank - fTableTopEventRank;
					if (selectedRow < 0) {
						fTableTopEventRank -= fFullyVisibleRows;
						if (fTableTopEventRank < 0) {
							fTableTopEventRank = 0;
						}
						needsRefresh = true;
					}
				}
				break;
			}
			default: {
				return;
			}
		}

		boolean done = true;
		if (needsRefresh) {
			done = refreshTable(); // false if table items not updated yet in this thread
		} else {
			fTable.select(selectedRow);
		}

		if (fFullyVisibleRows < fTableItemCount) {
			fSlider.setSelection(fTableTopEventRank);
		}

		if (fSelectedEventRank != previousSelectedEventRank && fTable.getSelection().length > 0) {
			if (done) {
    	        Event e = new Event();
    	        e.item = fTable.getSelection()[0];
    			fTable.notifyListeners(SWT.Selection, e);
			} else {
				fPendingSelection = true;
			}
		}
	}

	private boolean setDataItem(int index, TableItem item) {
		if (index != -1) {
			Event event = new Event();
			event.item  = item;
			if (index < fFrozenRowCount) {
				event.index = index;
			} else {
				event.index = index + fTableTopEventRank;
			}
			event.doit  = true;
			notifyListeners(SWT.SetData, event);
			return event.doit; // false if table item not updated yet in this thread
		}
		return true;
	}

	// ------------------------------------------------------------------------
	// Slider handling
	// ------------------------------------------------------------------------

	private void createSlider(int style) {
		fSlider = new Slider(this, SWT.VERTICAL | SWT.NO_FOCUS);
		fSlider.setMinimum(0);
		fSlider.setMaximum(0);
		if ((style & SWT.V_SCROLL) == 0) {
			fSlider.setVisible(false);
		}

		fSlider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.detail) {
					case SWT.ARROW_DOWN:
					case SWT.ARROW_UP:
					case SWT.NONE:
					case SWT.END:
					case SWT.HOME:
					case SWT.PAGE_DOWN:
					case SWT.PAGE_UP: {
			        	fTableTopEventRank = fSlider.getSelection();
						refreshTable();
						break;
					}
		        }
			}
		});
	}

	// ------------------------------------------------------------------------
	// Simulated Table API
	// ------------------------------------------------------------------------

	public void setHeaderVisible(boolean b) {
		fTable.setHeaderVisible(b);
	}

	public void setLinesVisible(boolean b) {
		fTable.setLinesVisible(b);
	}

	public TableItem[] getSelection() {
		return fTable.getSelection();
	}

	@Override
	public void addKeyListener(KeyListener listener) {
		fTable.addKeyListener(listener);
	}

	@Override
	public void addMouseListener(MouseListener listener) {
		fTable.addMouseListener(listener);
	}

	public void addSelectionListener(SelectionListener listener) {
		fTable.addSelectionListener(listener);
	}

	@Override
	public void setMenu(Menu menu) {
		fTable.setMenu(menu);
	}

	public void clearAll() {
		setItemCount(0);
	}
	
	public void setItemCount(int nbItems) {
		nbItems = Math.max(0, nbItems);

		if (nbItems != fTableItemCount) {
			fTableItemCount = nbItems;
			fTable.remove(fTableItemCount, fTable.getItemCount() - 1);
			fSlider.setMaximum(nbItems);
			resize();
			int tableHeight = Math.max(0, fTable.getClientArea().height - fTable.getHeaderHeight());
			fFullyVisibleRows = tableHeight / getItemHeight();
			if (fTableItemCount > 0) {
				fSlider.setThumb(Math.max(1, Math.min(fTableRows, fFullyVisibleRows)));
			}
		}
	}

	public int getItemCount() {
		return fTableItemCount;
	}
	
	public int getItemHeight() {
		/*
		 * Bug in Linux.  The method getItemHeight doesn't always return the correct value.
		 */
		if (fLinuxItemHeight >= 0 && System.getProperty("os.name").contains("Linux")) { //$NON-NLS-1$ //$NON-NLS-2$
			if (fLinuxItemHeight != 0) {
				return fLinuxItemHeight;
			}
			if (fTable.getItemCount() > 1) {
				int itemHeight = fTable.getItem(1).getBounds().y - fTable.getItem(0).getBounds().y;
				if (itemHeight > 0) {
					fLinuxItemHeight = itemHeight;
					return fLinuxItemHeight;
				}
			}
		} else {
			fLinuxItemHeight = -1; // Not Linux, don't perform os.name check anymore
		}
		return fTable.getItemHeight();
	}

	public int getTopIndex() {
		return fTableTopEventRank + fFrozenRowCount;
	}

	public void setTopIndex(int i) {
		if (fTableItemCount > 0) {
			i = Math.min(i, fTableItemCount - 1);
			i = Math.max(i, fFrozenRowCount);

			fTableTopEventRank = i - fFrozenRowCount;
			if (fFullyVisibleRows < fTableItemCount) {
				fSlider.setSelection(fTableTopEventRank);
			}

			refreshTable();
		}
	}

	public int indexOf(TableItem ti) {
		int index = fTable.indexOf(ti);
		if (index < fFrozenRowCount) {
			return index;
		} else {
			return (index - fFrozenRowCount) + getTopIndex();
		}
	}

	public TableColumn[] getColumns() {
		return fTable.getColumns();
	}

	public TableItem getItem(Point point) {
		return fTable.getItem(point);
	}
	
	private void resize() {
		// Compute the numbers of rows that fit the new area
		int tableHeight = Math.max(0, getSize().y - fTable.getHeaderHeight());
		int itemHeight = getItemHeight();
		fTableRows = Math.min((tableHeight + itemHeight - 1) / itemHeight, fTableItemCount);

		if (fTableTopEventRank + fFullyVisibleRows > fTableItemCount) {
			// If we are at the end, get elements before to populate
			fTableTopEventRank = Math.max(0, fTableItemCount - fFullyVisibleRows);
			refreshTable();
		} else if (fTableRows > fTable.getItemCount() || fTableItemCount < fTable.getItemCount()) {
			// Only refresh if new table items are needed or if table items need to be deleted
			refreshTable();
		}

	}

	// ------------------------------------------------------------------------
	// Controls interactions
	// ------------------------------------------------------------------------

	@Override
	public boolean setFocus() {
		boolean isVisible = isVisible();
		if (isVisible) {
			fTable.setFocus();
		}
		return isVisible;
	}
	
	public void refresh() {
		boolean done = refreshTable();
		if (fPendingSelection && done) {
			fPendingSelection = false;
			if (fTable.getSelection().length > 0) {
    	        Event e = new Event();
    	        e.item = fTable.getSelection()[0];
    			fTable.notifyListeners(SWT.Selection, e);
			}
		}
	}

	public void setColumnHeaders(ColumnData columnData[]) {
		for (int i = 0; i < columnData.length; i++) {
			TableColumn column = new TableColumn(fTable, columnData[i].alignment, i);
			column.setText(columnData[i].header);
			if (columnData[i].width > 0) {
				column.setWidth(columnData[i].width);
			} else {
				column.pack();
			}
		}
	}

	public int removeAll() {
		setItemCount(0);
		fSlider.setMaximum(0);
		fTable.removeAll();
		fSelectedEventRank = fFrozenRowCount;
		return 0;
	}
	
	private boolean refreshTable() {
		boolean done = true;
		for (int i = 0; i < fTableRows; i++) {
			if (i + fTableTopEventRank < fTableItemCount) {
				TableItem tableItem;
				if (i < fTable.getItemCount()) {
					tableItem = fTable.getItem(i);
				} else {
					tableItem = new TableItem(fTable, SWT.NONE);
				}
				done &= setDataItem(i, tableItem); // false if table item not updated yet in this thread
			} else {
				if (fTable.getItemCount() > fTableItemCount - fTableTopEventRank) {
					fTable.remove(fTableItemCount - fTableTopEventRank);
				}
			}
		}

		int lastRowOffset = fTableTopEventRank + fTableRows - 1;
		if ((fSelectedEventRank >= fTableTopEventRank + fFrozenRowCount) && (fSelectedEventRank <= lastRowOffset)) {
			int selectedRow = fSelectedEventRank - fTableTopEventRank;
			fTable.select(selectedRow);
		} else if (fSelectedEventRank < fFrozenRowCount) {
			fTable.select(fSelectedEventRank);
		} else {
			fTable.deselectAll();
		}
		return done;
	}

	public void setSelection(int i) {
		if (fTableItemCount > 0) {
			i = Math.min(i, fTableItemCount - 1);
			i = Math.max(i, 0);

			fSelectedEventRank = i;
			if ((i < fTableTopEventRank + fFrozenRowCount && i >= fFrozenRowCount) ||
				(i >= fTableTopEventRank + fFullyVisibleRows)) {
				fTableTopEventRank = Math.max(0, i - fFrozenRowCount - fFullyVisibleRows / 2);
			}
			if (fFullyVisibleRows < fTableItemCount) {
				fSlider.setSelection(fTableTopEventRank);
			}

			refreshTable();

		}
	}

	public int getSelectionIndex() {
		int index = fTable.getSelectionIndex();
		if (index == -1) {
			return fSelectedEventRank;
		}
		if (index < fFrozenRowCount) {
			return index;
		} else {
			return (index - fFrozenRowCount) + getTopIndex();
		}
	}
	
	public void setFrozenRowCount(int count) {
		fFrozenRowCount = count;
		refreshTable();
	}

	public TableEditor createTableEditor() {
		return new TableEditor(fTable);
	}

	public Control createTableEditorControl(Class<? extends Control> control) {
		try {
			return (Control) control.getConstructor(Composite.class, int.class).newInstance(new Object[] {fTable, SWT.NONE});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * @return the tooltipProvider
	 */
	public TooltipProvider getTooltipProvider() {
		return tooltipProvider;
	}

	/**
	 * @param tooltipProvider the tooltipProvider to set
	 */
	public void setTooltipProvider(TooltipProvider tooltipProvider) {
		this.tooltipProvider = tooltipProvider;
	}

	/**
	 * @return the doubleClickListener
	 */
	public IDoubleClickListener getDoubleClickListener() {
		return doubleClickListener;
	}

	/**
	 * @param doubleClickListener the doubleClickListener to set
	 */
	public void setDoubleClickListener(IDoubleClickListener doubleClickListener) {
		this.doubleClickListener = doubleClickListener;
	}
	
}
