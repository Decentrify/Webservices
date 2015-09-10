package se.kth.ws.aggregator.ws;

import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * REST API exposed by the visualizer to create the
 * visualizations for the system.
 *
 * Created by babbar on 2015-09-10.
 */
public class VisualizerWS  extends Service<Configuration>{

    private Logger logger = LoggerFactory.getLogger(VisualizerWS.class);
    private VisualizerSyncI visualizerSyncI;

    public VisualizerWS(VisualizerSyncI syncI){
        this.visualizerSyncI = syncI;
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        logger.debug("time to run any initialization code.");
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {

        logger.debug("environment configuration setup and registering the rest calls.");

        logger.debug("Initiating with the registering of the REST Calls.");

        environment.addProvider(new VisualizerRESTMsgs.AggregatedInternalState(this.visualizerSyncI));
        environment.addProvider(new VisualizerRESTMsgs.AvgSearchResponse(this.visualizerSyncI));

        logger.debug("Enabling the Cross Origin Request.");
        /*
         * To allow cross origin resource request from angular js client
         */
        environment.addFilter(CrossOriginFilter.class, "/*").
                setInitParam("allowedOrigins", "*").
                setInitParam("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin").
                setInitParam("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS").
                setInitParam("preflightMaxAge", "5184000"). // 2 months
                setInitParam("allowCredentials", "true");
    }
}
