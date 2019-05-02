package com.droitfintech.partyservice;


import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * @author ashraf
 *
 */
public class PartyEventFileListener implements FileAlterationListener {

	private static Logger logger = LoggerFactory.getLogger(PartyEventFileListener.class);

	private PartyReader reader ;
	private PartyJsonPersister persister;


	// Create a new FileAlterationObserver on the given directory
	//private FileAlterationObserver fileAlterationObserver = new FileAlterationObserver(directory);

	private FileAlterationObserver fileAlterationObserver;
	private FileAlterationMonitor monitor ;

	private int pollingIntervalInMilliseconds = 3000;





	/**
	 * Constructor
	 * @param reader
	 * @param persister
	 */
	public PartyEventFileListener(PartyReader reader,
								  PartyJsonPersister persister){
		this.reader = reader;
		this.persister = persister;

	}

	/***
	 * Start the ball rolling , initialize
     */
	public void startMonitoringForPartyEvents(){
		fileAlterationObserver = new FileAlterationObserver(reader.getUpdateDirectory().toFile().getPath());
		fileAlterationObserver.addListener(this);
		// Create a new FileAlterationMonitor with the given pollingInterval period
		monitor = new FileAlterationMonitor(pollingIntervalInMilliseconds);
		// Add the previously created FileAlterationObserver to FileAlterationMonitor
		monitor.addObserver(fileAlterationObserver);
		try {
			logger.info("PartyEventFileListener Monitoring starting...");
			monitor.start();
			logger.info("PartyEventFileListener Monitoring started...");
		} catch (Exception e) {
			logger.error("Exception occured ",e);
		}

	}

	/***
	 * Monitor start
	 * @param observer
     */
	@Override
	public void onStart(final FileAlterationObserver observer) {
		//logger.debug("The PartyEventFileListener has Reset on {} ", observer.getDirectory().getAbsolutePath());
	}

	/**
	 * NOOP
	 * @param directory
     */
	@Override
	public void onDirectoryCreate(final File directory) {
		//NOOP
	}

	/**
	 * NOOP
	 * @param directory
	 */
	@Override
	public void onDirectoryChange(final File directory) {
		//NOOP
	}

	/**
	 * NOOP
	 * @param directory
	 */
	@Override
	public void onDirectoryDelete(final File directory) {
		//NOOP
	}

	/***
	 * Event Fired when New Party Event file is dropped in the party Events folder
	 * @param file
     */
	@Override
	public void onFileCreate(final File file) {
		this.processEventFile(file);
	}

	/****
	 * Procees the event file
	 * @param eventFile
     */
	private void processEventFile(File eventFile){

		String eventFileName =  eventFile.getName();
		if( eventFileName.endsWith(".json")) {

			if(eventFileName.contains("delete")) {

				logger.info("Got party delete {}",eventFileName);
				PartyJsonUpdatePersistor up = new PartyJsonUpdatePersistor(persister);
				reader.parseAndDeleteParties(eventFile.getAbsolutePath(), up);
				Collection<String> updates = up.getDeletes();
				logger.info("Processe deleted {} parties", (updates != null ? updates.size() : 0) );

			}else {

				logger.info("Got party update event file {} " ,eventFileName);
				PartyJsonUpdatePersistor up = new PartyJsonUpdatePersistor(persister);
				reader.parseAndPersistParties(eventFile.getAbsolutePath(), up);
				Map<String, Object> updates = up.getUpdates();
				logger.info("Process updated {} parties", (updates != null ? updates.size() : 0) );


			}
		}


	}


	/***
	 * Event Fired when New Party Event file(s) touched for Service Restart
	 * @param file
	 */
	@Override
	public void onFileChange(final File file) {
		this.processEventFile(file);
	}

	/**
	 * NOOP , Don't care about deletes
	 * @param file
	 */
	@Override
	public void onFileDelete(final File file) {


	}

	/***
	 * Monitor reset
	 * @param observer
     */
	@Override
	public void onStop(final FileAlterationObserver observer) {
		//logger.debug("The PartyEventFileListener has stopped on {} ", observer.getDirectory().getAbsolutePath());
	}

}
