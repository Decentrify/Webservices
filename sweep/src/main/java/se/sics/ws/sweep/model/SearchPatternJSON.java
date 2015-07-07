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
import se.sics.ms.configuration.MsConfig;

/**
 * @author Alex Ormenisan <aaor@kth.se.se>
 */
public class SearchPatternJSON {

    private String fileNamePattern;
    private int minFileSize;
    private int maxFileSize;
    private Date minUploadDate;
    private Date maxUploadDate;
    private String language;
    private MsConfig.Categories category;
    private String descriptionPattern;

    public String getFileNamePattern() {
        return fileNamePattern;
    }

    public void setFileNamePattern(String fileNamePattern) {
        this.fileNamePattern = fileNamePattern;
    }

    public int getMinFileSize() {
        return minFileSize;
    }

    public void setMinFileSize(int minFileSize) {
        this.minFileSize = minFileSize;
    }

    public int getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(int maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public Date getMinUploadDate() {
        return minUploadDate;
    }

    public void setMinUploadDate(Date minUploadDate) {
        this.minUploadDate = minUploadDate;
    }

    public Date getMaxUploadDate() {
        return maxUploadDate;
    }

    public void setMaxUploadDate(Date maxUploadDate) {
        this.maxUploadDate = maxUploadDate;
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

    public String getDescriptionPattern() {
        return descriptionPattern;
    }

    public void setDescriptionPattern(String descriptionPattern) {
        this.descriptionPattern = descriptionPattern;
    }
}