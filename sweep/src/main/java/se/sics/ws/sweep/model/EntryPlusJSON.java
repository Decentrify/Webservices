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

import java.util.Date;
import se.sics.ms.types.IndexEntry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EntryPlusJSON {
    private String globalId;
    private Date uploaded;
    private EntryJSON entry;
    
    public EntryPlusJSON() {
    }

    public EntryPlusJSON(IndexEntry entry) {
        this.globalId = entry.getGlobalId();
        this.uploaded = entry.getUploaded();
        this.entry = new EntryJSON(entry);
    }
    
    public String getGlobalId() {
        return globalId;
    }

    public void setGlobalId(String globalId) {
        this.globalId = globalId;
    }

    public Date getUploaded() {
        return uploaded;
    }

    public void setUploaded(Date uploaded) {
        this.uploaded = uploaded;
    }

    public EntryJSON getEntry() {
        return entry;
    }

    public void setEntry(EntryJSON entry) {
        this.entry = entry;
    }
}
