package org.eclipse.linuxtools.gcov.test;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

public class JobsRunning implements ICondition {

	private final Object family;
	
	public JobsRunning(Object family) {
		this.family = family;
	}
	
	@Override
	public boolean test() throws Exception {
		Job[] allJobs = Job.getJobManager().find(family);
		return allJobs.length == 0;
	}
	
	@Override
	public void init(SWTBot bot) {
		
	}
	
	@Override
	public String getFailureMessage() {
		return "Jobs still running...";
	}
}
