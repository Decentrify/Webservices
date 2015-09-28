package se.kth.ws.aggregator.ws;

import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.aggregator.ws.model.AvgReplicationLagJSON;
import se.kth.ws.aggregator.ws.model.InternalStateJSON;
import se.kth.ws.aggregator.ws.model.PercentageReplicationLagJSON;
import se.kth.ws.sweep.core.util.Result;
import se.sics.ms.aggregator.design.*;
import se.sics.ms.data.InternalStatePacket;
import se.sics.ws.sweep.util.ResponseStatusWSMapper;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * REST Api of the Visualizer Component in the system.
 * <p/>
 * Created by babbar on 2015-09-10.
 */
public class VisualizerRESTMsgs {

    private static Logger logger = LoggerFactory.getLogger(VisualizerRESTMsgs.class);


//  ==================================
//      AGGREGATED INTERNAL STATE
//  ===================================


    @Path("/getAllState")
    @Produces(MediaType.APPLICATION_JSON)
    public static class AggregatedInternalState {

        private VisualizerSyncI syncI;

        public AggregatedInternalState(VisualizerSyncI syncI) {
            this.syncI = syncI;
        }

        @GET
        public Response getAggregatedInternalState() {

            logger.debug("Initiated a call to fetch the aggregated internal state of the system.");

            if (!syncI.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Service Unavailable").build();
            }

            Result<AggregatedInternalStateContainer> processResult = process(syncI);
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(processResult.getDetails()).build();
            }

            Collection<InternalStateJSON> response = processIntermediateResult(processResult.getValue());
            return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(response).build();
        }

        public Result<AggregatedInternalStateContainer> process(VisualizerSyncI syncI) {

            logger.debug("Process the aggregated internal state request.");
            SettableFuture<Result<AggregatedInternalStateContainer>> result = SettableFuture.create();
            syncI.getAggregatedInternalState(result);

            try {
                return result.get();
            } catch (InterruptedException e) {
                return Result.internalError("Unable to get aggregated information.");
            } catch (ExecutionException e) {
                return Result.internalError("Unable to get aggregated Information.");
            }
        }

        private Collection<InternalStateJSON> processIntermediateResult(AggregatedInternalStateContainer container) {

            Collection<InternalStateJSON> result = new ArrayList<InternalStateJSON>();
            Collection<se.sics.ms.aggregator.design.AggregatedInternalState> aggInternalStates = container.getProcessedWindows();

            if (!aggInternalStates.iterator().hasNext()) {
                return result;
            }

//          Only concerned with the last packet, don't care about the others.

            se.sics.ms.aggregator.design.AggregatedInternalState internalState = aggInternalStates.iterator().next();
            Collection<InternalStatePacket> statePackets = internalState.getInternalStatePackets();

            for (InternalStatePacket packet : statePackets) {

                InternalStateJSON internalStateJSON = new InternalStateJSON(packet.getPartitionId(),
                        packet.getPartitionDepth(), packet.getNumEntries(), packet.getLeaderIdentifier() == null ? null : packet.getLeaderIdentifier());

                result.add(internalStateJSON);
            }

            return result;
        }

    }


    @Path("/getAvgSearchResp")
    @Produces(MediaType.APPLICATION_JSON)
    public static class AvgSearchResponse {

        private VisualizerSyncI syncI;

        public AvgSearchResponse(VisualizerSyncI syncI) {
            this.syncI = syncI;
        }

        @GET
        public Response getAvgSearchResponse() {

            logger.debug("Initiated a call to fetch the average search response.");
            return null;
        }
    }



//  ======================================
//      AVERAGE REPLICATION LAG
//  ======================================

    @Path("/getAvgReplicationLag")
    @Produces(MediaType.APPLICATION_JSON)
    public static class AvgReplicationLag {

        private VisualizerSyncI syncI;

        public AvgReplicationLag(VisualizerSyncI syncI) {
            this.syncI = syncI;
        }

        @GET
        public Response getAvgReplicationLag() {

            logger.debug("Received a call to fetch the average replication lag in the system.");
            if (!syncI.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Service Unavailable").build();
            }

            Result<ReplicationLagDesignInfoContainer> processResult = process(syncI);
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(processResult.getDetails()).build();
            }

            List<AvgReplicationLagJSON> response = processIntermediateResult(processResult.getValue());
            return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(response).build();
        }


        /**
         * Based on the sync interface, create future and wait for the result to be
         * calculated by the application.
         *
         * @param syncI
         * @return
         */
        private Result<ReplicationLagDesignInfoContainer> process(VisualizerSyncI syncI) {

            logger.debug("Process the aggregated internal state request.");
            SettableFuture<Result<ReplicationLagDesignInfoContainer>> result = SettableFuture.create();
            syncI.getAverageReplicationLag(result);

            try {
                return result.get();
            } catch (InterruptedException e) {
                return Result.internalError("Unable to get average replication lag information.");
            } catch (ExecutionException e) {
                return Result.internalError("Unable to get average replication lag information.");
            }
        }


        /**
         * Based on the container concerning the design information, process the
         * information and construct the appropriate JSON object to be returned to the user.
         *
         * @param container container.
         * @return replication lag list.
         */
        private List<AvgReplicationLagJSON> processIntermediateResult(ReplicationLagDesignInfoContainer container) {

            List<AvgReplicationLagJSON> result = new ArrayList<>();
            Collection<ReplicationLagDesignInfo> collection = container.getProcessedWindows();

            for (ReplicationLagDesignInfo designInfo : collection) {

                AvgReplicationLagJSON json = new AvgReplicationLagJSON(designInfo.time, designInfo.maxLag, designInfo.minLag, designInfo.averageLag);
                result.add(json);
            }

            return result;
        }
    }




//  ======================================
//      PERCENTILE REPLICATION LAG
//  ======================================

    @Path("/getPerReplicationLag")
    @Produces(MediaType.APPLICATION_JSON)
    public static class PercentageReplicationLag {

        private VisualizerSyncI syncI;

        public PercentageReplicationLag(VisualizerSyncI syncI) {
            this.syncI = syncI;
        }

        @GET
        public Response getPercentageReplicationLag() {

            logger.debug("Received a call to fetch the average replication lag in the system.");
            if (!syncI.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity("Service Unavailable").build();
            }

            Result<PercentileLagDesignInfoContainer> processResult = process(syncI);
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(processResult.getDetails()).build();
            }

            List<PercentageReplicationLagJSON> response = processIntermediateResult(processResult.getValue());
            return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(response).build();
        }


        /**
         * Based on the sync interface, create future and wait for the result to be
         * calculated by the application.
         *
         * @param syncI
         * @return
         */
        private Result<PercentileLagDesignInfoContainer> process(VisualizerSyncI syncI) {

            logger.debug("Process the aggregated internal state request.");
            SettableFuture<Result<PercentileLagDesignInfoContainer>> result = SettableFuture.create();
            syncI.getPercentileReplicationLag(result);

            try {
                return result.get();
            } catch (InterruptedException e) {
                return Result.internalError("Unable to get average replication lag information.");
            } catch (ExecutionException e) {
                return Result.internalError("Unable to get average replication lag information.");
            }
        }


        /**
         * Based on the container concerning the design information, process the
         * information and construct the appropriate JSON object to be returned to the user.
         *
         * @param container container.
         * @return replication lag list.
         */
        private List<PercentageReplicationLagJSON> processIntermediateResult(PercentileLagDesignInfoContainer container) {

            List<PercentageReplicationLagJSON> result = new ArrayList<>();
            Collection<PercentileLagDesignInfo> collection = container.getProcessedWindows();

            for (PercentileLagDesignInfo designInfo : collection) {

                PercentageReplicationLagJSON json = new PercentageReplicationLagJSON(designInfo.time, designInfo.fiftyPercentileRingLag, designInfo.seventyFivePercentileRingLag, designInfo.ninetyPercentileRingLag);
                result.add(json);
            }

            return result;
        }
    }

}
