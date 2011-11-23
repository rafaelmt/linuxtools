package org.eclipse.linuxtools.lttng.ui.views.project.dialogs;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.eclipse.linuxtools.lttng.ui.views.project.dialogs.messages"; //$NON-NLS-1$
	public static String AddTraceWizard_invalidTraceLocation;
	public static String AddTraceWizard_windowTitle;
	public static String AddTraceWizardPage_columnHeader;
	public static String AddTraceWizardPage_description;
	public static String AddTraceWizardPage_windowTitle;
	public static String ImportTraceWizardPage_BadTraceVersion;
	public static String ImportTraceWizardPage_BadTraceVersionMsg1;
	public static String ImportTraceWizardPage_BadTraceVersionMsg2;
	public static String NewProjectWizard_Description;
	public static String NewProjectWizard_Title;
	public static String NewExperimentDialog_DialogTitle;
	public static String NewExperimentDialog_ExperimentLabel; 
	public static String TraceErrorDialog_DalogTitle;
	public static String TraceErrorDialog_DialogMsgLabel;
	public static String TraceLibraryPath_label;
	public static String TraceLibraryPath_browseBtn;
	public static String TraceLibraryPathWizardPage_SpecifiedTraceLibraryLocation_notExists;
	public static String TraceLibraryPathWizardPage_TraceLoaderLibrary_notExists;
	public static String TraceLibraryPathWizardPage_Title;
	public static String TraceLibraryPathWizardPage_Description;
	public static String SelectTrace_ErrorTitle;
	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

    private Messages() {
    }
}
