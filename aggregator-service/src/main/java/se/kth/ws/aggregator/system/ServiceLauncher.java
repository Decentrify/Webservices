package se.kth.ws.aggregator.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Start;

/**
 * Launcher for the main service.
 *
 * Created by babbar on 2015-09-07.
 */
public class ServiceLauncher extends ComponentDefinition{

    private Logger logger  = LoggerFactory.getLogger(ServiceLauncher.class);

    public ServiceLauncher(){
        subscribe(startHandler, control);
    }


    Handler<Start> startHandler = new Handler<Start>() {
        @Override
        public void handle(Start event) {

            logger.debug("Component started.");
            Component host = create(HostComp.class, new HostCompInit());
            trigger(Start.event, host.control());
        }
    };

}
