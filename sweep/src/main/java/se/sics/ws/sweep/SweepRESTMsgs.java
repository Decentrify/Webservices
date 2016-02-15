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
package se.sics.ws.sweep;

import se.sics.ws.sweep.core.SweepSyncI;
import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;
import java.util.Collection;
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
import se.sics.ms.events.paginateAware.UiSearchResponse;
import se.sics.ms.types.IndexEntry;
import se.sics.ms.types.SearchPattern;
import se.sics.ms.util.PaginateInfo;
import se.sics.ws.sweep.model.*;
import se.sics.ws.sweep.core.util.Result;
import se.sics.ms.types.ApplicationEntry;
import se.sics.ws.sweep.util.ResponseStatusWSMapper;

/**
 * @author Alex Ormenisan <aaor@kth.se>
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
            if(!sweep.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(request.fail("sweep is not ready")).build();
            }
            Result validateRes = validateRequest(request);
            if (!validateRes.ok()) {
                return Response.status(ResponseStatusWSMapper.map(validateRes.status)).entity(request.fail(validateRes.getDetails())).build();
            }
            Result<IndexEntry> parseResult = parseFromJSON(request);
            if (!parseResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(request.fail(parseResult.getDetails())).build();
            }
            
            Result processResult = process(parseResult.getValue(), sweep);
            if(!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(request.fail(processResult.getDetails())).build();
            }
            return Response.status(Response.Status.OK).entity(request.success()).build();
        }

        private Result validateRequest(AddEntryJSON.Request request) {
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
        
        private Result<IndexEntry> parseFromJSON(AddEntryJSON.Request request) {
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
            SettableFuture<Result> opFuture = SettableFuture.create();
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
            if(!sweep.isReady()) {
                return Response.status(Response.Status.SERVICE_UNAVAILABLE).entity(request.fail("sweep is not ready")).build();
            }

            Result<SearchPattern> parseResult = parseFromJSON(request);
            if (!parseResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(request.fail(parseResult.getDetails())).build();
            }

            Result<UiSearchResponse> processResult = process(parseResult.getValue(), request.getPagination(), sweep);
            if (!processResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(processResult.status)).entity(request.fail(processResult.getDetails())).build();
            }

            Result<PaginateEntryPlusJSON> parseToJSONResult = parseToJSONPaginate(processResult.getValue());
            if (!parseToJSONResult.ok()) {
                return Response.status(ResponseStatusWSMapper.map(parseToJSONResult.status)).entity(request.fail(parseToJSONResult.getDetails())).build();
            }
            SearchIndexJSON.Response response = request.success(parseToJSONResult.getValue());
            return Response.status(ResponseStatusWSMapper.map(parseResult.status)).entity(response).build();
        }
        
        private Result<SearchPattern> parseFromJSON(SearchIndexJSON.Request request) {
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

        /**
         * Start processing the search request.
         *
         * @param searchPattern pattern of the request
         * @param paginationJSON json containing pagination information.
         * @param sweep sync interface
         *
         * @return Response
         */
        private Result<UiSearchResponse> process( SearchPattern searchPattern, PaginationJSON paginationJSON, SweepSyncI sweep) {
            SettableFuture<Result<UiSearchResponse>> opFuture = SettableFuture.create();
            sweep.search(searchPattern, paginationJSON, opFuture);

            try {
                return opFuture.get();
            } catch (InterruptedException ex) {
                return Result.internalError("Request interupted");
            } catch (ExecutionException ex) {
                return Result.internalError("Request interupted");
            }
        }
        
        private Result<ArrayList<EntryPlusJSON>> parseToJSON(ArrayList<ApplicationEntry> resultList) {
            ArrayList<EntryPlusJSON> returnList = new ArrayList<EntryPlusJSON>();

            for (ApplicationEntry ae : resultList) {
                returnList.add(new EntryPlusJSON(ae.getEntry()));
            }

            return Result.ok(returnList);
        }


        private Result<PaginateEntryPlusJSON> parseToJSONPaginate(UiSearchResponse searchResponse) {

            ArrayList<EntryPlusJSON> entryList = new ArrayList<EntryPlusJSON>();
            Collection<ApplicationEntry> resultList = searchResponse.getEntries();

            PaginateInfo paginateInfo = searchResponse.getPaginateInfo();
            int numHits = searchResponse.getNumHits();

            PaginationJSON paginationJSON = new PaginationJSON(paginateInfo.getFrom(), paginateInfo.getSize(), numHits);

            for (ApplicationEntry ae : resultList) {
                entryList.add(new EntryPlusJSON(ae.getEntry()));
            }

            PaginateEntryPlusJSON result = new PaginateEntryPlusJSON(paginationJSON, entryList);
            return Result.ok(result);
        }
    }
}