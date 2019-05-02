package com.droitfintech.staticservice;

import com.droitfintech.bootstrap.AbstractReferenceDataService;
import com.droitfintech.bootstrap.DroitReferenceDataService;
import com.droitfintech.partyservice.PartyReferenceDataService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by christopherwhinds on 4/10/17.
 */
public class StaticDataReferenceDataService  extends AbstractReferenceDataService implements DroitReferenceDataService {

    public static String SERVICE_HOST = "staticDataServicehostname" ;
    public static String SERVICE_PORT = "staticDataServicePort";
    public static String PARTICIPANT_LOCATION   = "participantLocation";
    public static String FXRATES_LOCATION       = "fxratesLocation";
    public static String USERCREDENTIALS_LOCATION = "userCredentialsLocation";

    public static HashMap<String,Map> particpantCache = new HashMap<>();

    private ObjectMapper mapper = new ObjectMapper();

    public static String PARTICIPANT_CACHE        = "participantCache";

    private static Logger logger = LoggerFactory.getLogger(StaticDataReferenceDataService.class);

    protected  StaticDataServiceIOCTRL ioCtrl;

    @Override
    public void runService() {
        loadBootstrapConfiguration();
        createControlService();

    }

    @Override
    public void createControlService() {

        ioCtrl = new StaticDataServiceIOCTRL(serviceConfig);
        loadParticipantCache();
        ioCtrl.startServer();
    }

    /****
     * Load the Particpants in to the cache in the system propeties
     */
    private void loadParticipantCache(){

        try {

            String serviceHomeLocation =   System.getProperty(SERVICE_HOME);

            String participantsFileLocation =  serviceHomeLocation +  (String) StaticDataServiceIOCTRL.serviceProperties.get(StaticDataReferenceDataService.PARTICIPANT_LOCATION);
            File participantsFile = new File(participantsFileLocation);
            if (!participantsFile.exists()) {

                logger.error("STOP  Service shutting down Participants file not found at location:" + participantsFileLocation);
                System.exit(99);

            }
            byte[] docBuffer = new byte[(int)participantsFile.length()];
            try {
                // create a byte array of the file in correct format
                //read the file into a byte array and set it in the content to send on the response

                FileInputStream targetFileStream = new FileInputStream(participantsFile);
                IOUtils.readFully(targetFileStream,docBuffer);

                ArrayList<Map<String,Object>> participants = mapper.readValue(docBuffer,ArrayList.class);


                Iterator<Map<String,Object>> mapIterator = participants.iterator();
                while(mapIterator.hasNext()){
                    Map participant = mapIterator.next();
                    StaticDataReferenceDataService.particpantCache.put((String)participant.get("id"),participant);
                }




            } catch (IOException e) {
                logger.error("Exception Occured:",e);

            }

        } catch (Exception e) {
            logger.error("Exception Occured:",e);

        }


    }

}
