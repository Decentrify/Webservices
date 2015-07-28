/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * GVoD is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package se.kth.ws.gvod;

import se.sics.gvod.manager.toolbox.GVoDSyncI;
import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.system.GVoDLauncher;
import se.sics.kompics.Kompics;
import se.sics.ktoolbox.ipsolver.msg.GetIp;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class GVoDWSLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(GVoDWSLauncher.class);
    
    private static void startWebservice(SettableFuture<GVoDSyncI> sweepSyncI) {
        LOG.info("starting webservice");
        try {
            Config config = ConfigFactory.load();
            String[] args = new String[]{"server", config.getString("webservice.server")};
            GVoDWS gvodWS = new GVoDWS(sweepSyncI);
            gvodWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        GetIp.NetworkInterfacesMask ipType = GetIp.NetworkInterfacesMask.PUBLIC;
        if(args.length == 1 && args[0].equals("-tenDot")) {
            ipType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        System.setProperty("java.net.preferIPv4Stack", "true");
        
        //TODO Alex set future into the launcher gvod
        SettableFuture<GVoDSyncI> gvodSyncI = SettableFuture.create();
        GVoDLauncher.setSyncIFuture(gvodSyncI);
        GVoDLauncher.setIpType(ipType);
        Kompics.createAndStart(GVoDLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        
        startWebservice(gvodSyncI);
        
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
    
}
