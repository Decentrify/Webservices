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
package se.sics.ws.sweep.toolbox;

import com.google.common.base.Optional;

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
public class Result<V extends Object> {

    public final Status status;
    private final Optional<String> details;
    public final Optional<V> value;

    public Result(Status status, Optional<String> details, Optional<V> value) {
        this.status = status;
        this.details = details;
        this.value = value;
    }

    public Result(Status status, Optional<String> details) {
        this.status = status;
        this.details = details;
        this.value = Optional.absent();
    }

    public Result(V val) {
        this.status = Status.OK;
        this.details = Optional.absent();
        this.value = Optional.of(val);
    }

    public boolean ok() {
        return status.equals(Status.OK);
    }
    
    public String getDetails() {
        if(details.isPresent()) {
            return details.get();
        }
        return "";
    }
    
    public static enum Status {
        OK, TIMEOUT, INTERNAL_ERROR, BAD_REQUEST, OTHER
    }
    
    public static Result badRequest(String reason) {
        return new Result(Status.BAD_REQUEST, Optional.of(reason), Optional.absent());
    }
    
    public static Result ok(Object value) {
        return new Result(Status.OK, Optional.absent(), Optional.fromNullable(value));
    }
    
    public static Result failed(Status status, String reason) {
        return new Result(status, Optional.fromNullable(reason), Optional.absent());
    }
    
    public static Result internalError(String reason) {
        return new Result(Status.INTERNAL_ERROR, Optional.fromNullable(reason));
    }
}
