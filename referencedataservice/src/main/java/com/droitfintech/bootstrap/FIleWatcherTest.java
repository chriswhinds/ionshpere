package com.droitfintech.bootstrap;

import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchService;
import java.util.List;
import java.util.concurrent.TimeUnit;


/**
 * Created by christopherwhinds on 4/11/17.
 */
public class FIleWatcherTest {


        public static void main(String[] args) {

            //define a folder root
            Path myDir = Paths.get("/Users/christopherwhinds/github/referencedataservice/partyEvents");

            try {
                WatchService watcher = myDir.getFileSystem().newWatchService();

                int counter = 0;

                 while(true){

                     myDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE,StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);

                     //WatchKey watckKey = watcher.poll();
                     //WatchKey watckKey = watcher.take();
                     counter = counter+=1;
                     System.out.println("Waiting for work loop counter " + counter );
                     WatchKey watckKey = watcher.poll(3, TimeUnit.SECONDS);

                     if(watckKey == null) {
                         System.out.println("Looping Back for more work...");
                         continue;
                     }

                     System.out.println("Processing work " + counter);


                     List<WatchEvent<?>> events = watckKey.pollEvents();

                     for (WatchEvent event : events) {
                         if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                             System.out.println("Created: " + event.context().toString());
                         }
                         if (event.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
                             System.out.println("Delete: " + event.context().toString());
                         }
                         if (event.kind() == StandardWatchEventKinds.ENTRY_MODIFY) {
                             System.out.println("Modify: " + event.context().toString());
                         }
                     }


                 }

            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
            }
        }



}
