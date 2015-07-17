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

import se.kth.ws.sweep.core.SweepSyncI;
import com.google.common.util.concurrent.SettableFuture;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.assets.AssetsBundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Configuration;
import com.yammer.dropwizard.config.Environment;
import java.util.concurrent.ExecutionException;
import org.eclipse.jetty.servlets.CrossOriginFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SweepWS extends Service<Configuration> {

    private static final Logger LOG = LoggerFactory.getLogger(SweepWSLauncher.class);

    private SweepSyncI sweepSyncI;

    public SweepWS(SweepSyncI sweepSyncI) {
        this.sweepSyncI = sweepSyncI;
    }

    @Override
    public void initialize(Bootstrap<Configuration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/interface/", "/webapp/"));
    }

    @Override
    public void run(Configuration configuration, Environment environment) throws Exception {
        environment.addProvider(new SweepRESTMsgs.SearchIndexResource(sweepSyncI));
        environment.addProvider(new SweepRESTMsgs.AddIndexResource(sweepSyncI));

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
