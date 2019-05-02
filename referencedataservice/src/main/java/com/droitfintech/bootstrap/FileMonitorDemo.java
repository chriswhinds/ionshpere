package com.droitfintech.bootstrap;

import java.io.File;

import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

/**
 * @author ashraf
 *
 */
public class FileMonitorDemo {

	// Get the user home directory to be monitored
	private static final String FOLDER = "/Users/christopherwhinds/github/referencedataservice/partyEvents";

	// The monitor will perform polling on the folder every 30 seconds
	private static final long pollingInterval = 3000;

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {

		// Change this to match the environment you want to watch.
		final File directory = new File(FOLDER);

		// Create a new FileAlterationObserver on the given directory
		FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(directory);

		// Create a new FileAlterationListenerImpl and pass it the previously created FileAlterationObserver
		fileAlterationObserver.addListener(new FileAlterationListenerImpl());

		// Create a new FileAlterationMonitor with the given pollingInterval period
		final FileAlterationMonitor monitor = new FileAlterationMonitor(pollingInterval);

		// Add the previously created FileAlterationObserver to FileAlterationMonitor
		monitor.addObserver(fileAlterationObserver);

		// Start the FileAlterationMonitor
		monitor.start();


	}

}
