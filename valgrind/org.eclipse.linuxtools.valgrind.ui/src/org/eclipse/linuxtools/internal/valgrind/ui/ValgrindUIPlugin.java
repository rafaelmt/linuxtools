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
package org.eclipse.linuxtools.internal.valgrind.ui;

import java.util.HashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.linuxtools.internal.valgrind.core.PluginConstants;
import org.eclipse.linuxtools.valgrind.ui.IValgrindToolView;
import org.eclipse.linuxtools.valgrind.ui.ValgrindUIConstants;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

public class ValgrindUIPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = PluginConstants.UI_PLUGIN_ID;
	public static final String TOOLBAR_LOC_GROUP_ID = "toolbarLocal"; //$NON-NLS-1$
	public static final String TOOLBAR_EXT_GROUP_ID = "toolbarExtensions"; //$NON-NLS-1$
	
	// Extension point constants
	protected static final String EXT_ELEMENT = "view"; //$NON-NLS-1$
	protected static final String EXT_ATTR_ID = "definitionId"; //$NON-NLS-1$
	protected static final String EXT_ATTR_CLASS = "class"; //$NON-NLS-1$

	protected HashMap<String, IConfigurationElement> toolMap;

	// The shared instance
	private static ValgrindUIPlugin plugin;

	protected ValgrindViewPart view;
	// The page containing the created Valgrind view
	protected IWorkbenchPage activePage;
	
	/**
	 * The constructor
	 */
	public ValgrindUIPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static ValgrindUIPlugin getDefault() {
		return plugin;
	}

	public void createView(final String contentDescription, final String toolID) {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
					activePage.showView(ValgrindUIConstants.VIEW_ID, null, IWorkbenchPage.VIEW_CREATE);

					// create the view's tool specific controls and populate content description
					view.createDynamicContent(contentDescription, toolID);

					view.refreshView();
				} catch (PartInitException e) {
					e.printStackTrace();
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}					
		});
	}

	/**
	 * Shows the Valgrind view in the active page and gives it focus.
	 */
	public void showView() {
		Display.getDefault().syncExec(new Runnable() {
			public void run() {
				try {
					activePage.showView(ValgrindUIConstants.VIEW_ID);
				} catch (PartInitException e) {
					e.printStackTrace();
				}
			}			
		});
	}
	
	/**
	 * Refreshes the Valgrind view
	 */
	public void refreshView() {
		if (view != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					view.refreshView();
				}				
			});
		}
	}
	
	/**
	 * Empties the contents of the view and restores its original state.
	 */
	public void resetView() {
		if (view != null) {
			Display.getDefault().syncExec(new Runnable() {
				public void run() {
					try {
						view.createDynamicContent(Messages.getString("ValgrindViewPart.No_Valgrind_output"), null); //$NON-NLS-1$
					} catch (CoreException e) {
						e.printStackTrace();
					}
				}				
			});
		}
	}

	protected void setView(ValgrindViewPart view) {
		this.view = view;
	}

	/**
	 * @return the Valgrind view
	 */
	public ValgrindViewPart getView() {
		return view;
	}
	
	public static Shell getActiveWorkbenchShell() {
		IWorkbenchWindow window = getDefault().getWorkbench().getActiveWorkbenchWindow();
		if (window != null) {
			return window.getShell();
		}
		return null;
	}

	protected void initializeToolMap() {
		toolMap = new HashMap<String, IConfigurationElement>();
		IExtensionPoint extPoint = Platform.getExtensionRegistry().getExtensionPoint(PLUGIN_ID, PluginConstants.VIEW_EXT_ID);
		IConfigurationElement[] configs = extPoint.getConfigurationElements();
		for (IConfigurationElement config : configs) {
			if (config.getName().equals(EXT_ELEMENT)) {
				String id = config.getAttribute(EXT_ATTR_ID);
				if (id != null && config.getAttribute(EXT_ATTR_CLASS) != null) {
					toolMap.put(id, config);
				}
			}
		}
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path.
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	protected HashMap<String, IConfigurationElement> getToolMap() {
		if (toolMap == null) {
			initializeToolMap();
		}
		return toolMap;
	}

	public IValgrindToolView getToolView(String id) throws CoreException {
		IValgrindToolView view = null;
		IConfigurationElement config = getToolMap().get(id);
		if (config != null) {
			Object obj = config.createExecutableExtension(EXT_ATTR_CLASS);
			if (obj instanceof IValgrindToolView) {
				view = (IValgrindToolView) obj;
			}
		}
		if (view == null) {
			throw new CoreException(new Status(IStatus.ERROR, PLUGIN_ID, Messages.getString("ValgrindUIPlugin.Cannot_retrieve_view"))); //$NON-NLS-1$
		}
		return view;
	}
}
