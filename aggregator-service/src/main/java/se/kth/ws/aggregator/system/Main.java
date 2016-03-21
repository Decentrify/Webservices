package se.kth.ws.aggregator.system;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.kompics.Kompics;
import se.sics.ktoolbox.ipsolver.msg.GetIp;

/**
 * Initiating the main component.
 *
 * Created by babbar on 2015-09-07.
 */
public class Main {

    private static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        logger.debug("Booting up the main component.");

        if (Kompics.isOn()) {
            logger.debug("Shutting down the previous kompics instance.");
            Kompics.shutdown();
        }

//      Set the ipType in the host component.
        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        HostComp.setIpType(setIpType);

        Kompics.createAndStart(ServiceLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
    }
}
