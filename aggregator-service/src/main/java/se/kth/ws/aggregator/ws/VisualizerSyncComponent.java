package se.kth.ws.aggregator.ws;

import com.google.common.util.concurrent.SettableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.aggregator.util.DesignerEnum;

import java.util.UUID;
import se.kth.ws.sweep.core.util.Result;
import se.sics.kompics.ClassMatchedHandler;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.ktoolbox.aggregator.server.VisualizerPort;
import se.sics.ktoolbox.aggregator.server.event.WindowProcessing;
import se.sics.ms.aggregator.design.AggregatedInternalStateContainer;
import se.sics.ms.aggregator.design.PercentileLagDesignInfoContainer;
import se.sics.ms.aggregator.design.ReplicationLagDesignInfoContainer;

/**
 * The main component that act as a async / sync conversion between the client and the
 * Created by babbar on 2015-09-10.
 */
public class VisualizerSyncComponent extends ComponentDefinition implements VisualizerSyncI {

    private Logger logger = LoggerFactory.getLogger(VisualizerSyncComponent.class);
    private SettableFuture pendingJob;
    private int DEFAULT_START_LOC = 0;
    private int DEFAULT_END_LOC = 1;

    private Positive<VisualizerPort> visualizerPort = requires(VisualizerPort.class);

    public VisualizerSyncComponent(){

        logger.debug("Initializing the component.");
        subscribe(startHandler, control);
        subscribe(aggregatedStateResponse, visualizerPort);
        subscribe(replicationLagHandler, visualizerPort);
        subscribe(percentileReplicationLag, visualizerPort);
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            logger.debug("Component has started.");
        }
    };

    @Override
    public boolean isReady() {
        return this.getComponentCore().state().equals(Component.State.ACTIVE);
    }

    @Override
    public void getAggregatedInternalState(SettableFuture<Result<AggregatedInternalStateContainer>> settableFuture) {

        if(pendingJob != null){
            logger.warn("Job already being executed by the system, returning busy status.");
            settableFuture.set(Result.busy("Job Already being executed."));
            return;
        }

        pendingJob = settableFuture;
        WindowProcessing.Request request = new WindowProcessing.Request(UUID.randomUUID(), DesignerEnum.AggInternalState.getName(), DEFAULT_START_LOC, DEFAULT_END_LOC);
        trigger(request, visualizerPort);
    }

    @Override
    public void getAverageReplicationLag(SettableFuture<Result<ReplicationLagDesignInfoContainer>> settableFuture) {

        int startLoc = DEFAULT_START_LOC;
        int endLoc = DEFAULT_START_LOC + 20;

        if(pendingJob != null){
            logger.warn("Job already being executed by the system, returning busy status.");
            settableFuture.set(Result.busy("Job Already being executed."));
            return;
        }

        pendingJob = settableFuture;
        WindowProcessing.Request request = new WindowProcessing.Request(UUID.randomUUID(), DesignerEnum.ReplicationLagDesigner.getName(), startLoc, endLoc);
        trigger(request, visualizerPort);


    }

    @Override
    public void getPercentileReplicationLag(SettableFuture<Result<PercentileLagDesignInfoContainer>> settableFuture) {


        int startLoc = DEFAULT_START_LOC;
        int endLoc = DEFAULT_START_LOC + 20;

        if(pendingJob != null){
            logger.warn("Job already being executed by the system, returning busy status.");
            settableFuture.set(Result.busy("Job Already being executed."));
            return;
        }

        pendingJob = settableFuture;
        WindowProcessing.Request request = new WindowProcessing.Request( UUID.randomUUID(), DesignerEnum.PercentileLagDesigner.getName(), startLoc, endLoc );
        trigger(request, visualizerPort);


    }


    /**
     * Special type of handler matching on the aggregated internal state of
     * the system.
     *
     */
    ClassMatchedHandler<AggregatedInternalStateContainer, WindowProcessing.Response<AggregatedInternalStateContainer>> aggregatedStateResponse = new ClassMatchedHandler<AggregatedInternalStateContainer, WindowProcessing.Response<AggregatedInternalStateContainer>>() {
        @Override
        public void handle(AggregatedInternalStateContainer content, WindowProcessing.Response<AggregatedInternalStateContainer> context) {

            logger.debug("Received response from the application regarding the aggregated state of the system.");

            if(pendingJob == null){
                logger.warn("Pending job should not be null as only one job is allowed for now.");
                return;
            }

            pendingJob.set(Result.ok(content));  // Inform the waiting application about the new result being available.
            pendingJob = null;      // Reset the pending job for the next operation.
        }
    };


    /**
     * Handler matching for the calculation of the replication lag in the system.
     */
    ClassMatchedHandler<ReplicationLagDesignInfoContainer, WindowProcessing.Response<ReplicationLagDesignInfoContainer>> replicationLagHandler = new ClassMatchedHandler<ReplicationLagDesignInfoContainer, WindowProcessing.Response<ReplicationLagDesignInfoContainer>>() {
        @Override
        public void handle(ReplicationLagDesignInfoContainer content, WindowProcessing.Response<ReplicationLagDesignInfoContainer> context) {

            logger.debug("Received response from the application regarding the aggregated state of the system.");

            if(pendingJob == null){
                logger.warn("Pending job should not be null as only one job is allowed for now.");
                return;
            }

            pendingJob.set(Result.ok(content));  // Inform the waiting application about the new result being available.
            pendingJob = null;      // Reset the pending job for the next operation.
        }
    };



    /**
     * Handler for the calculation of the percentile replication lag in the various
     * regions of the system.
     */
    ClassMatchedHandler<PercentileLagDesignInfoContainer, WindowProcessing.Response<PercentileLagDesignInfoContainer>> percentileReplicationLag = new ClassMatchedHandler<PercentileLagDesignInfoContainer, WindowProcessing.Response<PercentileLagDesignInfoContainer>>() {
        @Override
        public void handle(PercentileLagDesignInfoContainer content, WindowProcessing.Response<PercentileLagDesignInfoContainer> context) {

            logger.debug("Received response from the application regarding the aggregated state of the system.");

            if(pendingJob == null){
                logger.warn("Pending job should not be null as only one job is allowed for now.");
                return;
            }

            pendingJob.set(Result.ok(content));  // Inform the waiting application about the new result being available.
            pendingJob = null;      // Reset the pending job for the next operation.
        }
    };



}
