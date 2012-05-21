package org.eclipse.linuxtools.internal.oprofile.remote.launch.configuration;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.linuxtools.internal.oprofile.launch.configuration.OprofileCounter;
import org.eclipse.linuxtools.internal.oprofile.launch.configuration.OprofileEventConfigTab;
import org.eclipse.linuxtools.internal.oprofile.launch.launching.OprofileLaunchConfigurationDelegate;
import org.eclipse.ptp.core.IPTPLaunchConfigurationConstants;
import org.eclipse.swt.widgets.Button;


/**
 * Thic class represents the event configuration tab of the launcher dialog.
 */
public class OprofileRemoteEventConfigTab extends OprofileEventConfigTab  {
	protected Button defaultEventCheck;
	protected OprofileCounter[] counters = null;
	protected CounterSubTab[] counterSubTabs;
	private Boolean hasPermissions = null;
	
	
	public OprofileRemoteEventConfigTab(){
	}
	
	protected Boolean hasPermissions(IProject project){
		if (this.hasPermissions == null){
			this.hasPermissions = OprofileLaunchConfigurationDelegate.hasPermissions(project);
		}
		return this.hasPermissions;
		
	}
	
	
	protected IProject getProject(ILaunchConfiguration configuration) {
		String name = null;
		try {
			name = configuration.getAttribute(IPTPLaunchConfigurationConstants.ATTR_PROJECT_NAME, (String) null);
		} catch (CoreException e) {
			return null;
		}
		if (name == null) {
			return null;
		}

		return ResourcesPlugin.getWorkspace().getRoot().getProject(name);
	}
	
	public void initializeFrom(ILaunchConfiguration config) {
		// Force re-check of permissions every time the view is initialized
		this.hasPermissions = null;
		super.initializeFrom(config);
	}
}

