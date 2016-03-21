package se.kth.ws.aggregator.ws;

import com.google.common.util.concurrent.SettableFuture;
import se.kth.ws.sweep.core.util.Result;
import se.sics.ms.aggregator.design.AggregatedInternalStateContainer;

/**
 * Visualizer Synchronous Interface.
 *
 * Created by babbar on 2015-09-10.
 */
public interface VisualizerSyncI {


    /**
     * Inform if the component that will implement this
     * interface is ready to handle the requests.
     *
     * @return true if ready.
     */
    public boolean isReady();


    /**
     * The request indicates the visualizer to start with the processing of the internal state
     * collected from all the nodes in the system.
     *
     * @param settableFuture settable future.
     */
    public void getAggregatedInternalState(SettableFuture<Result<AggregatedInternalStateContainer>> settableFuture);

}
