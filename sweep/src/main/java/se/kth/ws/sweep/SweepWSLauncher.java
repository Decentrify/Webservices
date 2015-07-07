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
package se.kth.ws.sweep;

import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.util.logging.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.gvod.config.ElectionConfiguration;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ms.common.ApplicationSelf;
import se.sics.ms.configuration.MsConfig;
import se.sics.ms.net.SerializerSetup;
import se.sics.ms.search.SearchPeer;
import se.sics.ms.search.SearchPeerInit;
import se.sics.p2ptoolbox.aggregator.network.AggregatorSerializerSetup;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerConfig;
import se.sics.p2ptoolbox.chunkmanager.ChunkManagerSerializerSetup;
import se.sics.p2ptoolbox.croupier.CroupierConfig;
import se.sics.p2ptoolbox.croupier.CroupierSerializerSetup;
import se.sics.p2ptoolbox.election.core.ElectionConfig;
import se.sics.p2ptoolbox.election.network.ElectionSerializerSetup;
import se.sics.p2ptoolbox.gradient.GradientConfig;
import se.sics.p2ptoolbox.gradient.GradientSerializerSetup;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SweepWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(SweepWSLauncher.class);

    private Component network;
    private Component timer;
    private Component searchPeer;
    private SweepWS sweepWS;
    private Config config;

    public SweepWSLauncher() {
        LOG.info("initiating...");
        int startId = 128;
        registerSerializers(startId);
        connectComponents();

        subscribe(handleStart, control);
        subscribe(handleStop, control);
    }

    // TODO Abhi - this should be part of sweep and also rename the SerializerSetup to SweepSerializerSetup
    private void registerSerializers(int startId) {
        int currentId = startId;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = ElectionSerializerSetup.registerSerializers(currentId);
        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        SerializerSetup.registerSerializers(currentId);
    }

    private void connectComponents() {
        config = ConfigFactory.load();

        SystemConfig systemConfig = new SystemConfig(config);
        GradientConfig gradientConfig = new GradientConfig(config);
        CroupierConfig croupierConfig = new CroupierConfig(config);
        ElectionConfig electionConfig = new ElectionConfig(config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(config);

        timer = create(JavaTimer.class, Init.NONE);
        network = create(NettyNetwork.class, new NettyInit(systemConfig.self));

        //TODO Abhi - why aren't you building this applicationSelf in SearchPeer and instead risk me handing this reference to someone else - shared object problem
        ApplicationSelf applicationSelf = new ApplicationSelf(systemConfig.self);
        //TODO send to searchPeer to populate it so the RestAPI can start
        //TODO - make sure to set the settable future in the constructor of SearchPeer or else you will might deadlock
        SettableFuture<SweepSyncI> sweepSyncI = SettableFuture.create();
        searchPeer = create(SearchPeer.class, new SearchPeerInit(applicationSelf, systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                ElectionConfiguration.build(), chunkManagerConfig, gradientConfig, electionConfig));

        connect(timer.getPositive(Timer.class), searchPeer.getNegative(Timer.class));
        connect(network.getPositive(Network.class), searchPeer.getNegative(Network.class));

        sweepWS = new SweepWS(sweepSyncI);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting...");
            startWebservice();
        }
    };

    private void startWebservice() {
        LOG.info("starting webservice");
        String[] args = null;
        try {
            args = new String[]{"server", config.getString("webservice.server")};
            sweepWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };
    
    public static void main(String[] args) throws IOException {
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        MsConfig.init(args);
        System.setProperty("java.net.preferIPv4Stack", "true");
        Kompics.createAndStart(SweepWSLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
