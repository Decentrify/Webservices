/*
 * Copyright (C) 2009 Swedish Institute of Computer Science (SICS) Copyright (C)
 * 2009 Royal Institute of Technology (KTH)
 *
 * Sweep is free software; you can redistribute it and/or
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

package se.kth.ws.sweep.core;

import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.kth.ws.sweep.core.util.Result;
import se.sics.kompics.Component;
import se.sics.kompics.Component.State;
import se.sics.kompics.ComponentDefinition;
import se.sics.kompics.Handler;
import se.sics.kompics.Positive;
import se.sics.kompics.Start;
import se.sics.kompics.Stop;
import se.sics.ms.events.UiAddIndexEntryRequest;
import se.sics.ms.events.UiAddIndexEntryResponse;
import se.sics.ms.events.UiSearchRequest;
import se.sics.ms.events.UiSearchResponse;
import se.sics.ms.ports.UiPort;
import se.sics.ms.types.ApplicationEntry;
import se.sics.ms.types.IndexEntry;
import se.sics.ms.types.SearchPattern;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SweepSyncComponent extends ComponentDefinition implements SweepSyncI {
    
    private static final Logger LOG = LoggerFactory.getLogger(SweepSyncComponent.class);
    
    private final Positive<UiPort> sweep = requires(UiPort.class);
    private SettableFuture pendingJob;
    
    public SweepSyncComponent() {
        LOG.info("initiating...");
        pendingJob = null;
        
        subscribe(handleStart, control);
        subscribe(handleStop, control);
        subscribe(handleAddResponse, sweep);
        subscribe(handleSearchResponse, sweep);
    }
    
    private Handler handleStart = new Handler<Start>() {
        @Override
        public void handle(Start event) {
            LOG.info("starting...");
        }
    };
    
    private Handler handleStop = new Handler<Stop>() {
        @Override
        public void handle(Stop event) {
            LOG.info("stopping...");
        }
    };

    @Override
    public boolean isReady() {
        return this.getComponentCore().state().equals(State.ACTIVE);
    }
    
    @Override
    public void add(IndexEntry entry, SettableFuture<Result> opFuture) {
        LOG.debug("received add request");
        if(pendingJob != null) {
            LOG.debug("reject - busy");
            opFuture.set(Result.busy("handling another operation"));
            return;
        }
        pendingJob = opFuture;
        trigger(new UiAddIndexEntryRequest(entry), sweep);
    }
    
    private Handler handleAddResponse = new Handler<UiAddIndexEntryResponse>() {
        @Override
        public void handle(UiAddIndexEntryResponse response) {
            LOG.debug("received add response");
            if(response.isSuccessful()) {
                pendingJob.set(Result.ok(true));
            } else {
                pendingJob.set(Result.internalError("add failed"));
            }
            pendingJob = null;
        }
    };

    @Override
    public void search(SearchPattern searchPattern, SettableFuture<Result<ArrayList<ApplicationEntry>>> opFuture) {
        LOG.info("received search request");
        if(pendingJob != null) {
            LOG.debug("reject - busy");
            opFuture.set(Result.busy("handling another operation"));
            return;
        }
        pendingJob = opFuture;
        trigger(new UiSearchRequest(searchPattern), sweep);
    }
    
    private Handler handleSearchResponse = new Handler<UiSearchResponse>() {
        @Override
        public void handle(UiSearchResponse response) {
            LOG.debug("received search response");
            pendingJob.set(Result.ok(response.getResults()));
            pendingJob = null;
        }
    };
}    