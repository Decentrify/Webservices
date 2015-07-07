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

import se.sics.ms.configuration.MsConfig;
import se.sics.ms.types.IndexEntry;

/**
 * @author Alex Ormenisan <aaor@kth.se>
 */
public class EntryJSON {

    private String url;
    private String fileName;
    private long fileSize;
    private String language;
    private MsConfig.Categories category;
    private String description;

    public EntryJSON() {
    }
    
    public EntryJSON(IndexEntry entry) {
        this.url = entry.getUrl();
        this.fileName = entry.getFileName();
        this.fileSize = entry.getFileSize();
        this.language = entry.getLanguage();
        this.category = entry.getCategory();
        this.description = entry.getDescription();
    }
            
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public MsConfig.Categories getCategory() {
        return category;
    }

    public void setCategory(MsConfig.Categories category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
