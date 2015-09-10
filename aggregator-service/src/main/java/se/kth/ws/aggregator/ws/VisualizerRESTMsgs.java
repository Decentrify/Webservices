package se.kth.ws.aggregator.ws;

import com.sun.jersey.core.header.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * REST Api of the Visualizer Component in the system.
 *
 * Created by babbar on 2015-09-10.
 */
public class VisualizerRESTMsgs {

    private static Logger logger = LoggerFactory.getLogger(VisualizerRESTMsgs.class);


    @Path("/getAllState")
    @Produces(MediaType.APPLICATION_JSON)
    public static class AggregatedInternalState {

        @GET
        public Response getAggregatedInternalState(){

            logger.debug("Initiated a call to fetch the aggregated internal state of the system.");
            return null;
        }
    }



    @Path("/getAvgSearchResp")
    @Produces(MediaType.APPLICATION_JSON)
    public static class AvgSearchResponse {

        @GET
        public Response getAvgSearchResponse(){

            logger.debug("Initiated a call to fetch the average search response.");
            return null;
        }
    }

}
