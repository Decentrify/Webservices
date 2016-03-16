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
package se.sics.ws.dy;

import com.google.common.util.concurrent.SettableFuture;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ws.sweep.core.SweepSyncComponent;
import se.sics.ws.sweep.core.SweepSyncI;
import se.sics.caracaldb.MessageRegistrator;
import se.sics.gvod.common.util.VoDHeartbeatServiceEnum;
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.network.netty.NettyInit;
import se.sics.kompics.network.netty.NettyNetwork;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.AggregatorSerializerSetup;
import se.sics.ktoolbox.cc.bootstrap.CCBootstrapComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatComp;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ktoolbox.ipsolver.IpSolverComp;
import se.sics.ktoolbox.ipsolver.IpSolverPort;
import se.sics.ktoolbox.ipsolver.msg.GetIp;
import se.sics.ms.net.SweepSerializerSetup;
import se.sics.ms.ports.UiPort;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.EnumSet;
import se.sics.gvod.core.aggregation.VodCoreAggregation;
import se.sics.gvod.system.HostManagerKCWrapper;
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.chunkmngr.ChunkManagerSerializerSetup;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.croupier.aggregation.CroupierAggregation;
import se.sics.ktoolbox.election.ElectionSerializerSetup;
import se.sics.ktoolbox.election.aggregation.ElectionAggregation;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.gradient.aggregation.GradientAggregation;
import se.sics.ktoolbox.netmngr.NetworkMngrComp;
import se.sics.ktoolbox.netmngr.NetworkMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.OMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp.Init;
import se.sics.ktoolbox.overlaymngr.OverlayMngrPort;
import se.sics.ktoolbox.util.address.AddressUpdate;
import se.sics.ktoolbox.util.address.AddressUpdatePort;
import se.sics.ktoolbox.util.aggregation.BasicAggregation;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.KAddress;
import se.sics.ktoolbox.util.network.basic.BasicAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.network.nat.NatAwareAddressImpl;
import se.sics.ktoolbox.util.overlays.view.OverlayViewUpdatePort;
import se.sics.ktoolbox.util.setup.BasicSerializerSetup;
import se.sics.ktoolbox.util.status.Status;
import se.sics.ktoolbox.util.status.StatusPort;
import se.sics.ms.gvod.config.GradientConfiguration;
import se.sics.ms.gvod.config.SearchConfiguration;
import se.sics.ms.search.SearchPeerComp;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWSLauncher extends ComponentDefinition {

    private Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);
    //******************************CONNECTIONS*********************************
    //DO NOT CONNECT TO
    //internal
    private Positive<StatusPort> otherStatusPort = requires(StatusPort.class);
    private Positive<AddressUpdatePort> addressUpdatePort = requires(AddressUpdatePort.class);
    //********************************CONFIG************************************
    private SystemKCWrapper systemConfig;
    //******************************AUX STATE***********************************
    private KAddress selfAdr;
    //********************************CLEANUP***********************************
    private Component timerComp;
    private Component netMngrComp;
    private Component caracalClientComp;
    private Component heartbeatComp;
    private Component overlayMngrComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    private Socket socket;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;

    byte[] vodSchemaId = null;
    //**************************************************************************

    public DYWSLauncher() {
        LOG.info("initiating...");
        gvodSyncIFuture = SettableFuture.create();
        registerSerializers();
        registerPortTracking();

        systemConfig = new SystemKCWrapper(config());

        phase1();

        subscribe(handleStart, control);
        subscribe(handleAddressUpdate, addressUpdatePort);
    }

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting: solvingIp");
        }
    };

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("exception:{} shutting down", fault.getCause());
        System.exit(1);
        return Fault.ResolveAction.RESOLVED;
    }

    private void registerSerializers() {
        MessageRegistrator.register();
        int currentId = 128;
        currentId = BasicSerializerSetup.registerBasicSerializers(currentId);
        currentId = CroupierSerializerSetup.registerSerializers(currentId);
        currentId = GradientSerializerSetup.registerSerializers(currentId);
        currentId = OMngrSerializerSetup.registerSerializers(currentId);
        currentId = NetworkMngrSerializerSetup.registerSerializers(currentId);
        currentId = ElectionSerializerSetup.registerSerializers(currentId);
        currentId = AggregatorSerializerSetup.registerSerializers(currentId);
        currentId = ChunkManagerSerializerSetup.registerSerializers(currentId);
        currentId = SweepSerializerSetup.registerSerializers(currentId);
        currentId = GVoDSerializerSetup.registerSerializers(currentId);
    }

    private void registerPortTracking() {
        BasicAggregation.registerPorts();
        CroupierAggregation.registerPorts();
        GradientAggregation.registerPorts();
        ElectionAggregation.registerPorts();
        VodCoreAggregation.registerPorts();
    }

    private void phase1() {
        //timer
        timerComp = create(JavaTimer.class, Init.NONE);
        //network mngr
        NetworkMngrComp.ExtPort netMngrExtPorts = new NetworkMngrComp.ExtPort(timerComp.getPositive(Timer.class));
        netMngrComp = create(NetworkMngrComp.class, new NetworkMngrComp.Init(netMngrExtPorts));
        //mini-hack - get rid of this, make dbMngr ask for the self through ports
        connect(netMngrComp.getPositive(AddressUpdatePort.class), addressUpdatePort.getPair(), Channel.TWO_WAY);
    }

    private Handler handleAddressUpdate = new Handler<AddressUpdate.Indication>() {
        @Override
        public void handle(AddressUpdate.Indication update) {
            LOG.info("update address");
            if (selfAdr == null) {
                selfAdr = update.localAddress;
                //phase2
                connectCaracalClient();
                connectHeartbeat();
                subscribe(handleCaracalDisconnect, otherStatusPort);
                subscribe(handleCaracalReady, otherStatusPort);
                subscribe(handleHeartbeatReady, otherStatusPort);

                trigger(Start.event, caracalClientComp.control());
                trigger(Start.event, heartbeatComp.control());
            }
        }
    };

    private void connectCaracalClient() {
        caracalClientComp = create(CCBootstrapComp.class, new CCBootstrapComp.CCBootstrapInit(selfAdr));
        connect(caracalClientComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(caracalClientComp.getNegative(Network.class), netMngrComp.getPositive(Network.class), Channel.TWO_WAY);
        connect(caracalClientComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
    }

    private void connectHeartbeat() {
        heartbeatComp = create(CCHeartbeatComp.class, new CCHeartbeatComp.CCHeartbeatInit(selfAdr));
        connect(heartbeatComp.getNegative(Timer.class), timerComp.getPositive(Timer.class), Channel.TWO_WAY);
        connect(heartbeatComp.getNegative(CCOperationPort.class), caracalClientComp.getPositive(CCOperationPort.class), Channel.TWO_WAY);
        connect(heartbeatComp.getNegative(StatusPort.class), caracalClientComp.getPositive(StatusPort.class), Channel.TWO_WAY);
        connect(heartbeatComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
    }

    ClassMatchedHandler handleCaracalReady
            = new ClassMatchedHandler<CCBootstrapReady, Status.Internal<CCBootstrapReady>>() {

                @Override
                public void handle(CCBootstrapReady content, Status.Internal<CCBootstrapReady> container) {

                    LOG.info("starting: received schemas");
                    vodSchemaId = content.caracalSchemaData.getId("gvod.metadata");
                    if (vodSchemaId == null) {
                        LOG.error("exception:vod schema undefined shutting down");
                        System.exit(1);
                    }

                    if (dyWS != null) {
                        dyWS.setIsServerDown(false);
                    }
                }
            };

    /**
     * Caracal client gets disconnected.
     */
    ClassMatchedHandler handleCaracalDisconnect
            = new ClassMatchedHandler<CCBootstrapDisconnected, Status.Internal<CCBootstrapDisconnected>>() {

                @Override
                public void handle(CCBootstrapDisconnected content, Status.Internal<CCBootstrapDisconnected> container) {

                    LOG.debug("Caracal client disconnected, need to initiate counter measures.");

                    //Inform the web service if it has already been booted.
                    if (dyWS != null) {
                        dyWS.setIsServerDown(true);
                    }
                }
            };

    private Handler handleHeartbeatReady = new Handler<Status.Internal<CCHeartbeatReady>>() {
        @Override
        public void handle(Status.Internal<CCHeartbeatReady> e) {
            LOG.info("starting: system");
            if (overlayMngrComp == null) {
                phase3();
            }
        }
    };

    private void phase3() {
        connectOverlayMngr();
        connectSweep();
        connectSweepSync();
        connectVoDHost();
        startWebservice();

        trigger(Start.event, overlayMngrComp.control());
        trigger(Start.event, vodHostComp.control());
        trigger(Start.event, sweepHostComp.control());
        trigger(Start.event, sweepSyncComp.control());
    }

    private void connectOverlayMngr() {
        OverlayMngrComp.ExtPort omngrExtPorts = new OverlayMngrComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class), netMngrComp.getPositive(AddressUpdatePort.class),
                heartbeatComp.getPositive(CCHeartbeatPort.class));
        overlayMngrComp = create(OverlayMngrComp.class, new OverlayMngrComp.Init(omngrExtPorts));
    }

    private void connectVoDHost() {
        HostManagerComp.ExtPort vodHostExtPorts = new HostManagerComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class), netMngrComp.getPositive(AddressUpdatePort.class),
                caracalClientComp.getPositive(CCOperationPort.class),
                overlayMngrComp.getPositive(OverlayMngrPort.class), overlayMngrComp.getPositive(CroupierPort.class),
                overlayMngrComp.getNegative(OverlayViewUpdatePort.class));
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(vodHostExtPorts,
                new HostManagerKCWrapper(config(), selfAdr), gvodSyncIFuture, vodSchemaId));

    }

    private void connectSweep() {
        SearchPeerComp.ExtPort extPort = new SearchPeerComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class), netMngrComp.getPositive(AddressUpdatePort.class),
                overlayMngrComp.getPositive(CroupierPort.class), overlayMngrComp.getPositive(GradientPort.class),
                overlayMngrComp.getNegative(OverlayViewUpdatePort.class));
        sweepHostComp = create(SearchPeerComp.class, new SearchPeerComp.Init(selfAdr, extPort,
                GradientConfiguration.build(), SearchConfiguration.build()));
    }

    private void connectSweepSync() {
        sweepSyncComp = create(SweepSyncComponent.class, Init.NONE);
        sweepSyncI = (SweepSyncI) sweepSyncComp.getComponent();
        connect(sweepSyncComp.getNegative(UiPort.class), sweepHostComp.getPositive(UiPort.class), Channel.TWO_WAY);

    }

    private void startWebservice() {
        LOG.info("starting webservice");

        try {
            dyWS = new DYWS(sweepSyncI, gvodSyncIFuture);
            Config config = ConfigFactory.load();
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
        if (Kompics.isOn()) {
            Kompics.shutdown();
        }
        Kompics.createAndStart(DYWSLauncher.class, Runtime.getRuntime().availableProcessors(), 20); // Yes 20 is totally arbitrary
        try {
            Kompics.waitForTermination();
        } catch (InterruptedException ex) {
            System.exit(1);
        }
    }
}
