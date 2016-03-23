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
import se.sics.gvod.manager.toolbox.GVoDSyncI;
import se.sics.gvod.network.GVoDSerializerSetup;
import se.sics.gvod.system.HostManagerComp;
import se.sics.kompics.*;
import se.sics.kompics.network.Network;
import se.sics.kompics.timer.Timer;
import se.sics.kompics.timer.java.JavaTimer;
import se.sics.ktoolbox.aggregator.AggregatorSerializerSetup;
import se.sics.ktoolbox.cc.heartbeat.CCHeartbeatPort;
import se.sics.ms.net.SweepSerializerSetup;
import se.sics.ms.ports.UiPort;

import java.io.IOException;
import java.net.Socket;
import se.sics.gvod.core.aggregation.VodCoreAggregation;
import se.sics.gvod.system.HostManagerKCWrapper;
import se.sics.ktoolbox.cc.bootstrap.CCOperationPort;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapDisconnected;
import se.sics.ktoolbox.cc.bootstrap.event.status.CCBootstrapReady;
import se.sics.ktoolbox.cc.heartbeat.event.status.CCHeartbeatReady;
import se.sics.ktoolbox.cc.mngr.CCMngrComp;
import se.sics.ktoolbox.cc.mngr.event.CCMngrStatus;
import se.sics.ktoolbox.croupier.CroupierPort;
import se.sics.ktoolbox.croupier.CroupierSerializerSetup;
import se.sics.ktoolbox.croupier.aggregation.CroupierAggregation;
import se.sics.ktoolbox.election.ElectionSerializerSetup;
import se.sics.ktoolbox.election.aggregation.ElectionAggregation;
import se.sics.ktoolbox.gradient.GradientPort;
import se.sics.ktoolbox.gradient.GradientSerializerSetup;
import se.sics.ktoolbox.gradient.aggregation.GradientAggregation;
import se.sics.ktoolbox.netmngr.NetMngrReady;
import se.sics.ktoolbox.netmngr.NetworkMngrComp;
import se.sics.ktoolbox.netmngr.NetworkMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.OMngrSerializerSetup;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp;
import se.sics.ktoolbox.overlaymngr.OverlayMngrComp.Init;
import se.sics.ktoolbox.overlaymngr.OverlayMngrPort;
import se.sics.ktoolbox.util.aggregation.BasicAggregation;
import se.sics.ktoolbox.util.config.impl.SystemKCWrapper;
import se.sics.ktoolbox.util.network.nat.NatAwareAddress;
import se.sics.ktoolbox.util.overlays.id.OverlayIdRegistry;
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
    private String logPrefix = "";
    //******************************CONNECTIONS*********************************
    //DO NOT CONNECT TO
    //internal
    private Positive<StatusPort> otherStatusPort = requires(StatusPort.class);
    //********************************CONFIG************************************
    private SystemKCWrapper systemConfig;
    //******************************AUX_STATE***********************************
    private NatAwareAddress selfAdr;
    private byte[] schemaId = null;
    //********************************CLEANUP***********************************
    private Component timerComp;
    private Component netMngrComp;
    private Component ccMngrComp;
    private Component oMngrComp;
    private Component sweepHostComp;
    private Component sweepSyncComp;
    private Component vodHostComp;
    private DYWS dyWS;
    private Socket socket;

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;
    //**************************************************************************

    public DYWSLauncher() {
        LOG.info("initiating...");
        gvodSyncIFuture = SettableFuture.create();
        registerSerializers();
        registerPortTracking();

        systemConfig = new SystemKCWrapper(config());

        subscribe(handleStart, control);
        subscribe(handleNetReady, otherStatusPort);
        subscribe(handleCCReady, otherStatusPort);
        subscribe(handleCaracalDisconnect, otherStatusPort);
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

    Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting: solvingIp");

            timerComp = create(JavaTimer.class, Init.NONE);
            setNetMngr();
            trigger(Start.event, timerComp.control());
            trigger(Start.event, netMngrComp.control());
        }
    };

    private void setNetMngr() {
        LOG.info("{}setting up network mngr", logPrefix);
        NetworkMngrComp.ExtPort netExtPorts = new NetworkMngrComp.ExtPort(timerComp.getPositive(Timer.class));
        netMngrComp = create(NetworkMngrComp.class, new NetworkMngrComp.Init(netExtPorts));
        connect(netMngrComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
    }

    ClassMatchedHandler handleNetReady
            = new ClassMatchedHandler<NetMngrReady, Status.Internal<NetMngrReady>>() {
                @Override
                public void handle(NetMngrReady content, Status.Internal<NetMngrReady> container) {
                    LOG.info("{}network mngr ready", logPrefix);
                    selfAdr = content.systemAdr;
                    setCCMngr();
                    trigger(Start.event, ccMngrComp.control());
                }
            };

    private void setCCMngr() {
        LOG.info("{}setting up caracal client", logPrefix);
        CCMngrComp.ExtPort ccMngrExtPorts = new CCMngrComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class));
        ccMngrComp = create(CCMngrComp.class, new CCMngrComp.Init(selfAdr, ccMngrExtPorts));
        connect(ccMngrComp.getPositive(StatusPort.class), otherStatusPort.getPair(), Channel.TWO_WAY);
    }

    ClassMatchedHandler handleCCReady
            = new ClassMatchedHandler<CCMngrStatus.Ready, Status.Internal<CCMngrStatus.Ready>>() {
                @Override
                public void handle(CCMngrStatus.Ready content, Status.Internal<CCMngrStatus.Ready> container) {
                    LOG.info("{}caracal client ready", logPrefix);
                    schemaId = content.schemas.getId("gvod.metadata");
                    if (schemaId == null) {
                        LOG.error("exception:vod schema undefined shutting down");
                        System.exit(1);
                    }

                    if (dyWS != null) {
                        dyWS.setIsServerDown(false);
                    }

                    setOMngr();
                    setSweep();
                    setSweepSync();
                    setVoDHost();
                    startWebservice();
                    LOG.info("overlay owners:\n{}", OverlayIdRegistry.print());

                    trigger(Start.event, oMngrComp.control());
                    trigger(Start.event, oMngrComp.control());
                    trigger(Start.event, vodHostComp.control());
                    trigger(Start.event, sweepHostComp.control());
                    trigger(Start.event, sweepSyncComp.control());
                    
                }
            };

    /**
     * Caracal client gets disconnected.
     */
    ClassMatchedHandler handleCaracalDisconnect
            = new ClassMatchedHandler<CCMngrStatus.Disconnected, Status.Internal<CCMngrStatus.Disconnected>>() {

                @Override
                public void handle(CCMngrStatus.Disconnected content, Status.Internal<CCMngrStatus.Disconnected> container) {

                    LOG.debug("Caracal client disconnected, need to initiate counter measures.");

                    //Inform the web service if it has already been booted.
                    if (dyWS != null) {
                        dyWS.setIsServerDown(true);
                    }
                }
            };

    private void setOMngr() {
        LOG.info("{}setting up overlay mngr", logPrefix);
        OverlayMngrComp.ExtPort oMngrExtPorts = new OverlayMngrComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class), ccMngrComp.getPositive(CCHeartbeatPort.class));
        oMngrComp = create(OverlayMngrComp.class, new OverlayMngrComp.Init(selfAdr, oMngrExtPorts));
    }
    
    private void setVoDHost() {
        HostManagerComp.ExtPort vodHostExtPorts = new HostManagerComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class), ccMngrComp.getPositive(CCOperationPort.class),
                oMngrComp.getPositive(OverlayMngrPort.class), oMngrComp.getPositive(CroupierPort.class),
                oMngrComp.getNegative(OverlayViewUpdatePort.class));
        vodHostComp = create(HostManagerComp.class, new HostManagerComp.HostManagerInit(selfAdr, vodHostExtPorts,
                new HostManagerKCWrapper(config()), gvodSyncIFuture, schemaId));

    }

    private void setSweep() {
        SearchPeerComp.ExtPort extPort = new SearchPeerComp.ExtPort(timerComp.getPositive(Timer.class),
                netMngrComp.getPositive(Network.class),
                oMngrComp.getPositive(CroupierPort.class), oMngrComp.getPositive(GradientPort.class),
                oMngrComp.getNegative(OverlayViewUpdatePort.class));
        sweepHostComp = create(SearchPeerComp.class, new SearchPeerComp.Init(selfAdr, extPort,
                GradientConfiguration.build(), SearchConfiguration.build()));
        connect(sweepHostComp.getNegative(OverlayMngrPort.class), oMngrComp.getPositive(OverlayMngrPort.class), Channel.TWO_WAY);
    }

    private void setSweepSync() {
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

    @Override
    public Fault.ResolveAction handleFault(Fault fault) {
        LOG.error("exception:{} shutting down", fault.getCause());
        System.exit(1);
        return Fault.ResolveAction.RESOLVED;
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
