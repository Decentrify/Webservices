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
package se.sics.ws.sweep.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class SearchIndexJSON {

    public static class Request {

        private SearchPatternJSON searchPattern;
        private PaginationJSON pagination;

        public SearchPatternJSON getSearchPattern() {
            return searchPattern;
        }

        public void setSearchPattern(SearchPatternJSON searchPattern) {
            this.searchPattern = searchPattern;
        }


        public PaginationJSON getPagination() {
            return pagination;
        }

        public void setPagination(PaginationJSON pagination) {
            this.pagination = pagination;
        }

        public Response success(PaginateEntryPlusJSON searchResult) {
            return new Response(searchResult.getEntries(), "", searchResult.getPaginationJSON());
        }
        
        public Response fail(String errorDescription) {
            return new Response(new ArrayList<EntryPlusJSON>(), errorDescription, pagination);
        }
    }

    public static class Response {

        private String errorDescription;
        private List<EntryPlusJSON> searchResult;
        private PaginationJSON pagination;
        
        public Response() {
        }

        public Response(List<EntryPlusJSON> result, String errorDescription, PaginationJSON pagination) {
            this.errorDescription = errorDescription;
            this.searchResult = result;
            this.pagination = pagination;
        }

        public String getErrorDescription() {
            return errorDescription;
        }

        public void setErrorDescription(String errorDescription) {
            this.errorDescription = errorDescription;
        }

        public List<EntryPlusJSON> getSearchResult() {
            return searchResult;
        }

        public void setSearchResult(ArrayList<EntryPlusJSON> searchResult) {
            this.searchResult = searchResult;
        }

        public PaginationJSON getPagination() {
            return pagination;
        }

        public void setPagination(PaginationJSON pagination) {
            this.pagination = pagination;
        }

        
    }
}
