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
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import javax.ws.rs.Consumes;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sics.ms.types.IndexEntry;
import se.sics.ms.types.SearchPattern;
import se.sics.ws.sweep.model.AddEntryJSON;
import se.sics.ws.sweep.model.EntryPlusJSON;
import se.sics.ws.sweep.model.SearchIndexJSON;
import se.sics.ws.sweep.toolbox.Result;
import se.sics.ws.sweep.util.ResponseStatusWSMapper;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 */
public class SweepRESTMsgs {

    private static final Logger LOG = LoggerFactory.getLogger(SweepWS.class);

    @Path("/add")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class AddIndexResource {

        private SweepSyncI sweep;

        public AddIndexResource(SweepSyncI sweepSyncI) {
            this.sweep = sweepSyncI;
        }

        @PUT
        public Response add(AddEntryJSON.Request request) {
            LOG.info("received AddEntry request for:{}", request.getEntry().getFileName());
            Result validateRes = ProcessAddEntry.validate(request);
            if (!validateRes.ok()) {
                return Response.status(ResponseStatusWSMapper.map(validateRes.status)).entity(request.getResponse(validateRes.getDetails())).build();
            }
            Result<IndexEntry> parseResult = ProcessAddEntry.parseFromJSON(request);
            if (!parseResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(request.getResponse(parseResult.getDetails())).build();
            }
            Result processResult = ProcessAddEntry.process(parseResult.value.get(), sweep);
            return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(request.getResponse(processResult.getDetails())).build();
        }
    }

    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public static class SearchIndexResource {

        private SweepSyncI sweep;

        public SearchIndexResource(SweepSyncI sweepSyncI) {
            this.sweep = sweepSyncI;
        }

        @PUT
        public Response search(SearchIndexJSON.Request request) {
            LOG.info("received SearchIndex request for:{}", request.getSearchPattern().getFileNamePattern());

            Result<SearchPattern> parseResult = ProcessSearchIndex.parseFromJSON(request);
            if (!parseResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(request.getResponse(null, parseResult.getDetails())).build();
            }

            Result<ArrayList<IndexEntry>> processResult = ProcessSearchIndex.process(parseResult.value.get(), sweep);
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(request.getResponse(null, processResult.getDetails())).build();
            }

            Result<ArrayList<EntryPlusJSON>> parseToJSONResult = ProcessSearchIndex.parseToJSON(processResult.value.get());
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseToJSONResult.status)).entity(request.getResponse(null, parseToJSONResult.getDetails())).build();
            }
            SearchIndexJSON.Response response = request.getResponse(parseToJSONResult.value.get(), parseToJSONResult.getDetails());
            return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(response).build();
        }
    }

    static class ProcessAddEntry {

        public static Result validate(AddEntryJSON.Request request) {
            if (request.getEntry().getUrl() == null) {
                return Result.badRequest("URL is missing");
            } else if ((request.getEntry().getFileName() == null) || (request.getEntry().getFileName().trim().length() == 0)) {
                return Result.badRequest("File name is missing or invalid");
            } else if (request.getEntry().getFileSize() == 0) {
                return Result.badRequest("File size is invalid");
            } else if (request.getEntry().getLanguage() == null) {
                return Result.badRequest("Language is missing");
            } else {
                return Result.ok(request);
            }
        }

        public static Result<IndexEntry> parseFromJSON(AddEntryJSON.Request request) {
            IndexEntry entry = new IndexEntry(
                    UUID.randomUUID().toString(),
                    request.getEntry().getUrl(),
                    request.getEntry().getFileName(),
                    request.getEntry().getFileSize(),
                    new Date(),
                    request.getEntry().getLanguage(),
                    request.getEntry().getCategory(),
                    request.getEntry().getDescription());
            return Result.ok(entry);
        }

        public static Result process(IndexEntry entry, SweepSyncI sweep) {
            SettableFuture<se.sics.ws.sweep.toolbox.Result> opFuture = SettableFuture.create();
            sweep.add(entry, opFuture);
            try {
                return opFuture.get();
            } catch (InterruptedException ex) {
                return Result.internalError("Request interupted");
            } catch (ExecutionException ex) {
                return Result.internalError("Request interupted");
            }
        }
    }

    static class ProcessSearchIndex {

        public static Result<SearchPattern> parseFromJSON(SearchIndexJSON.Request request) {
            SearchPattern searchPattern = new SearchPattern(
                    request.getSearchPattern().getFileNamePattern(),
                    request.getSearchPattern().getMinFileSize(),
                    request.getSearchPattern().getMaxFileSize(),
                    request.getSearchPattern().getMinUploadDate(),
                    request.getSearchPattern().getMaxUploadDate(),
                    request.getSearchPattern().getLanguage(),
                    request.getSearchPattern().getCategory(),
                    request.getSearchPattern().getDescriptionPattern());

            return Result.ok(searchPattern);
        }

        public static Result<ArrayList<IndexEntry>> process(SearchPattern searchPattern, SweepSyncI sweep) {
            SettableFuture<Result<ArrayList<IndexEntry>>> opFuture = SettableFuture.create();
            sweep.search(searchPattern, opFuture);

            try {
                return opFuture.get();
            } catch (InterruptedException ex) {
                return Result.internalError("Request interupted");
            } catch (ExecutionException ex) {
                return Result.internalError("Request interupted");
            }
        }

        public static Result<ArrayList<EntryPlusJSON>> parseToJSON(ArrayList<IndexEntry> resultList) {
            ArrayList<EntryPlusJSON> returnList = new ArrayList<EntryPlusJSON>();

            for (IndexEntry entry : resultList) {
                returnList.add(new EntryPlusJSON(entry));
            }

            return Result.ok(returnList);
        }
    }
}
