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
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Server;
import se.kth.ws.sweep.core.SweepSyncI;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;

import java.awt.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.gvod.GVoDRESTMsgs;
import se.kth.ws.sweep.SweepRESTMsgs;
import se.kth.ws.util.BareBonesBrowserLaunch;
import se.kth.ws.util.TrayUI;
import se.sics.gvod.manager.toolbox.GVoDSyncI;

import javax.swing.*;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class DYWS extends Service<Configuration> {

    private static final Logger LOG = LoggerFactory.getLogger(DYWSLauncher.class);

    private SettableFuture<GVoDSyncI> gvodSyncIFuture;
    private SweepSyncI sweepSyncI;
    private TrayUI trayUi;

    public DYWS(SweepSyncI sweepSyncI, SettableFuture<GVoDSyncI> gvodSyncIFuture) {
        this.sweepSyncI = sweepSyncI;
        this.gvodSyncIFuture = gvodSyncIFuture;
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/interface/", "/webapp/"));
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        
        GVoDSyncI gvodSyncI = null;
        try {
            LOG.info("waiting on creation of gvod synchronous interface");
            gvodSyncI = gvodSyncIFuture.get();
            LOG.info("gvod synchronous interface created");
        } catch (InterruptedException ex) {
            LOG.error("gvod synchronous interface was not instantiated - possible wrong creation order");
            throw new RuntimeException(ex);
        } catch (ExecutionException ex) {
            LOG.error("gvod synchronous interface was not instantiated - possible wrong creation order");
            throw new RuntimeException(ex);
        }
        
        environment.addProvider(new SweepRESTMsgs.SearchIndexResource(sweepSyncI));
        environment.addProvider(new SweepRESTMsgs.AddIndexResource(sweepSyncI));
        
        environment.addProvider(new GVoDRESTMsgs.LibraryResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.FilesResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.PendingUploadResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.UploadResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.DownloadResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.RemoveResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.PlayVideoResource(gvodSyncI));
        environment.addProvider(new GVoDRESTMsgs.StopVideoResource(gvodSyncI));

        /*
         * To allow cross origin resource request from angular js client
         */
        environment.addFilter(CrossOriginFilter.class, "/*").
                setInitParam("allowedOrigins", "*").
                setInitParam("allowedHeaders", "Content-Type,Authorization,X-Requested-With,Content-Length,Accept,Origin").
                setInitParam("allowedMethods", "GET,PUT,POST,DELETE,OPTIONS").
                setInitParam("preflightMaxAge", "5184000"). // 2 months
                setInitParam("allowCredentials", "true");

        int webPort = configuration.getHttpConfiguration().getPort();
        LOG.error("WebPort: " + webPort);
        launchTray(webPort);
    }


    /**
     * Launch the tray icon in the system.
     */
    private void launchTray(final int port){

        if (SystemTray.isSupported()) {
            trayUi = new TrayUI(createImage("src/main/resources/icons/dy.png", "tray icon"), port);
        }

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                try {

                    LOG.error("Going to bootup the page after 1500 ms");
                    Thread.sleep(1500);
                    openWebpage(new URL("http://localhost:"+ port+ "/webapp/"));

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    protected static Image createImage(String path, String description) {

        ImageIcon icon = new ImageIcon(path, description);
        return icon.getImage();
    }

    public synchronized static void openWebpage(URI uri) {
        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
            try {
                desktop.browse(uri);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Brower UI could not be launched using Java's Desktop library. "
                    + "Are you running a window manager?");
            System.err.println("If you are using Ubuntu, try: sudo apt-get install libgnome");
            System.err.println("Retrying to launch the browser now using a different method.");
            BareBonesBrowserLaunch.openURL(uri.toASCIIString());
        }
    }

    public static void openWebpage(URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
