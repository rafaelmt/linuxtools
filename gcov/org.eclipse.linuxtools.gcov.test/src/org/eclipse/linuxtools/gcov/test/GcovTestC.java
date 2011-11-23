package org.eclipse.linuxtools.gcov.test;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotMenu;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
	org.eclipse.linuxtools.gcov.test.GcovTestC.CreateProject.class,
	org.eclipse.linuxtools.gcov.test.GcovTestC.PopulateProject.class,
	org.eclipse.linuxtools.gcov.test.GcovTestC.CompileProject.class,
	org.eclipse.linuxtools.gcov.test.GcovTestC.OpenGcovFileDetails.class,
	org.eclipse.linuxtools.gcov.test.GcovTestC.OpenGcovSummary.class
})
public class GcovTestC {


		private static SWTWorkbenchBot	bot;

		private static final String PROJECT_NAME = "Gcov_C_test";
		private static final String PROJECT_TYPE = "C Project";
		

		@BeforeClass
		public static void beforeClass() throws Exception {
			bot = new SWTWorkbenchBot();
			bot.perspectiveByLabel("C/C++").activate();
			bot.activeShell().activate();
			SWTBotMenu menu = bot.menu("Build Automatically");
			menu.click();
		}

		@RunWith(SWTBotJunit4ClassRunner.class)
		public static class CreateProject {
			@Test
			public void test() {
				System.out.println("Test: " + getClass().getName());
				GcovTest.createProject(bot, PROJECT_NAME, PROJECT_TYPE);
			}
		}

		@RunWith(SWTBotJunit4ClassRunner.class)
		public static class PopulateProject {
			@Test
			public void test() throws Exception {
				GcovTest.populateProject(bot, PROJECT_NAME);
			}
		}

		@RunWith(SWTBotJunit4ClassRunner.class)
		public static class CompileProject {
			@Test
			public void test() throws Exception {
				GcovTest.compileProject(bot, PROJECT_NAME);
			}
		}
		
		@RunWith(SWTBotJunit4ClassRunner.class)
		public static class OpenGcovFileDetails {
			@Test
			public void test() throws Exception {
				GcovTest.openGcovFileDetails(bot, PROJECT_NAME);
			}
		}
		
		@RunWith(SWTBotJunit4ClassRunner.class)
		public static class OpenGcovSummary {
			@Test
			public void test() throws Exception {
				GcovTest.openGcovSummary(bot, PROJECT_NAME);
			}
		}
		
}
