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

package se.kth.ws.sweep.core;

import com.google.common.util.concurrent.SettableFuture;
import java.util.ArrayList;

import se.sics.ms.events.paginateAware.UiSearchResponse;
import se.sics.ms.types.IndexEntry;
import se.sics.ms.types.SearchPattern;
import se.kth.ws.sweep.core.util.Result;
import se.sics.ms.types.ApplicationEntry;
import se.sics.ws.sweep.model.PaginationJSON;

/**
 * @author Alex Ormenisan <aaor@sics.se>
 * 
 * Optional stage 1 - proto Optional - java 1.6 compatible. (Stage 2 is monadic
 * Optional - probably java 1.8 Optional)
 *
 * uses the Optional pattern. Anything that can be null should be Optional. If
 * not optional the attribute must not be null and no null defensive programming
 * will be used by user of this object
 */
public interface SweepSyncI {
    public boolean isReady();
    public void add(IndexEntry entry, SettableFuture<Result> opFuture);
    public void search(SearchPattern searchPattern, PaginationJSON paginationJSON, SettableFuture<Result<UiSearchResponse>> opFuture);
}
