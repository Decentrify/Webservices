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
package se.kth.ws.dy;

import com.google.common.util.concurrent.SettableFuture;
import se.kth.ws.sweep.core.SweepSyncI;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.net.InetAddress;
import java.util.EnumSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.core.SweepSyncComponent;
import se.sics.gvod.bootstrap.cclient.CaracalPSManagerComp;
import se.sics.gvod.bootstrap.server.peermanager.PeerManagerPort;
import se.sics.gvod.bootstrap.server.peermanager.msg.CaracalReady;
import se.sics.gvod.config.GradientConfiguration;
import se.sics.gvod.config.SearchConfiguration;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.gvod.system.HostManagerConfig;
import se.sics.kompics.Component;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Init;
import se.sics.kompics.Kompics;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.common.ApplicationSelf;
import se.sics.ms.configuration.MsConfig;
import se.sics.ms.net.SerializerSetup;
import se.sics.ms.ports.UiPort;
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
import se.sics.p2ptoolbox.tgradient.TreeGradientConfig;
import se.sics.p2ptoolbox.util.config.SystemConfig;
import se.sics.p2ptoolbox.util.serializer.BasicSerializerSetup;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);
    private static GetIp.NetworkInterfacesMask ipType;

    public static void setIpType(GetIp.NetworkInterfacesMask setIpType) {
        ipType = setIpType;
    }

    private Component ipSolver;
    private Component network;
    private Component timer;
    private Component sweep;
    private Component sweepSync;
    private Component gvod;
    private Component caracalProxy;
    private DYWS dyWS;
    private Config config;
    private SystemConfig systemConfig;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;

    private HostManagerConfig gvodConfig;
    //**************************************************************************

    public DYWSLauncher() {
        LOG.info("initiating...");
        if (ipType == null) {
            LOG.error("launcher logic error - ipType not set");
            throw new RuntimeException("launcher logic error - ipType not set");
        }

        registerSerializers();

        subscribe(handleStart, control);
        subscribe(handleStop, control);

        ipSolver = create(IpSolverComp.class, new IpSolverComp.IpSolverInit());
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting: solvingIp");
            Positive<IpSolverPort> ipSolverPort = ipSolver.getPositive(IpSolverPort.class);
            subscribe(handleGetIp, ipSolverPort);
            trigger(new GetIp.Req(EnumSet.of(ipType)), ipSolverPort);
        }
    };

    Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };

    public Handler handleGetIp = new Handler<GetIp.Resp>() {
        @Override
        public void handle(GetIp.Resp resp) {
            LOG.info("starting: setting up caracal connection");

            InetAddress ip = null;
            if (!resp.addrs.isEmpty()) {
                ip = resp.addrs.get(0).getAddr();
                if (resp.addrs.size() > 1) {
                    LOG.warn("multiple ips detected, proceeding with:{}", ip);
                }
            }

            config = ConfigFactory.load();
            systemConfig = new SystemConfig(config, ip);
            gvodConfig = new HostManagerConfig(config, ip);

            timer = create(JavaTimer.class, Init.NONE);
            trigger(Start.event, timer.control());
            network = create(NettyNetwork.class, new NettyInit(systemConfig.self));
            trigger(Start.event, network.control());
            
            caracalConnectPhase();
        }
    };

    private void registerSerializers() {
        int currentId = 128;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = ElectionSerializerSetup.registerSerializers(currentId);
        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        currentId = SerializerSetup.registerSerializers(currentId);
        currentId = GVoDSerializerSetup.registerSerializers(currentId);
    }

    private void caracalConnectPhase() {
        //TODO Alex should create and start only on open nodes
        caracalProxy = create(CaracalPSManagerComp.class, new CaracalPSManagerComp.CaracalPSManagerInit(gvodConfig.getCaracalPSManagerConfig()));
        connect(caracalProxy.getNegative(Timer.class), timer.getPositive(Timer.class));
        trigger(Start.event, caracalProxy.control());

        subscribe(handleCaracalReady, caracalProxy.getPositive(PeerManagerPort.class));
    }

    private Handler handleCaracalReady = new Handler<CaracalReady>() {

        @Override
        public void handle(CaracalReady event) {
            LOG.info("starting: system");
            connectComponents();
        }
    };

    private void connectComponents() {
        connectSweep();
        connectSweepSync();
        connectGVoD();
        startWebservice();
    }

    private void connectSweep() {
        GradientConfig gradientConfig = new GradientConfig(config);
        CroupierConfig croupierConfig = new CroupierConfig(config);
        ElectionConfig electionConfig = new ElectionConfig(config);
        ChunkManagerConfig chunkManagerConfig = new ChunkManagerConfig(config);
        TreeGradientConfig treeGradientConfig = new TreeGradientConfig(config);

        //TODO Abhi - why aren't you building this applicationSelf in SearchPeer and instead risk me handing this reference to someone else - shared object problem
        ApplicationSelf applicationSelf = new ApplicationSelf(systemConfig.self);
        sweep = create(SearchPeer.class, new SearchPeerInit(applicationSelf, systemConfig, croupierConfig,
                SearchConfiguration.build(), GradientConfiguration.build(),
                chunkManagerConfig, gradientConfig, electionConfig, treeGradientConfig));
        connect(timer.getPositive(Timer.class), sweep.getNegative(Timer.class));
        connect(network.getPositive(Network.class), sweep.getNegative(Network.class));

        trigger(Start.event, sweep.control());
    }

    private void connectSweepSync() {
        sweepSync = create(SweepSyncComponent.class, Init.NONE);
        sweepSyncI = (SweepSyncI) sweepSync.getComponent();
        connect(sweep.getPositive(UiPort.class), sweepSync.getNegative(UiPort.class));
        trigger(Start.event, sweepSync.control());
    }

    private void connectGVoD() {
        gvodSyncIFuture = SettableFuture.create();
        gvod = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(gvodConfig, caracalProxy, gvodSyncIFuture));
        connect(gvod.getNegative(Network.class), network.getPositive(Network.class));
        connect(gvod.getNegative(Timer.class), timer.getPositive(Timer.class));
        trigger(Start.event, gvod.control());
    }

    private void startWebservice() {
        LOG.info("starting webservice");

        try {
            dyWS = new DYWS(sweepSyncI, gvodSyncIFuture);
            String[] args = new String[]{"server", config.getString("webservice.server")};
            dyWS.run(args);
        } catch (ConfigException.Missing ex) {
            LOG.error("bad configuration, could not find webservice.server");
            throw new RuntimeException(ex);
        } catch (Exception ex) {
            LOG.error("webservice error");
            throw new RuntimeException(ex);
        }
    }

    public static void main(String[] args) throws IOException {
        GetIp.NetworkInterfacesMask setIpType = GetIp.NetworkInterfacesMask.PUBLIC;
        if (args.length == 1 && args[0].equals("-tenDot")) {
            setIpType = GetIp.NetworkInterfacesMask.TEN_DOT_PRIVATE;
        }
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        MsConfig.init(args);
        System.setProperty("java.net.preferIPv4Stack", "true");
        DYWSLauncher.setIpType(setIpType);
        Kompics.createAndStart(DYWSLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
