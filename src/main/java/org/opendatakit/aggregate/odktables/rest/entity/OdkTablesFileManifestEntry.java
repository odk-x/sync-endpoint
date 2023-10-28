/*
 * Copyright (C) 2012-2013 University of Washington
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.opendatakit.aggregate.odktables.rest.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This represents information about a file so that a phone running ODKTables
 * will be able to check to see if it has the most recent version of the file,
 * and if not will be able to download the file. It is meant to be mostly a
 * struct that is parsed into and recovered from JSON.
 *
 * @author sudar.sam@gmail.com
 *
 */
public class OdkTablesFileManifestEntry implements Comparable<OdkTablesFileManifestEntry> {

  /**
   * This is the name of the file relative to
   * the either the 'config' directory (for
   * app-level and table-level files) or the
   * row's attachments directory (for row-level
   * attachments).
   *
   * I.e., for the new directory structure,
   * if the manifest holds configpath files, it is under:
   *   /sdcard/opendatakit/{appId}/config
   * if the manifest holds rowpath files, it is under:
   *   /sdcard/opendatakit/{appId}/data/attachments/{tableId}/{rowId}
   */
  public String filename;

  @JsonProperty(required = false)
  public Long contentLength;

  @JsonProperty(required = false)
  public String contentType;

  /**
   * This is the md5hash of the file, which will be used
   * for checking whether or not the version of the file
   * on the phone is current.
   */
  @JsonProperty(required = false)
  public String md5hash;

  /**
   * If the file is an image, this is the md5 of the
   * reduced version of the image. Otherwise, the hash is
   * an md5 on the original.
   * Used for understanding when to download
   * full size or reduced size images
   */
  @JsonProperty(required = false)
  public String reducedImageMd5Hash;

  /**
   * If the image should be reduced (objects of this class are passed from services to sync-endpoint)
   * Either "true" or "false"
   */
  @JsonProperty(required = false)
  public String reduceImage;

  /**
   * This is the url from which the current version of the file can be
   * downloaded.
   */
  @JsonProperty(required = false)
  public String downloadUrl;

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((filename == null) ? 0 : filename.hashCode());
    result = prime * result + ((contentLength == null) ? 0 : contentLength.hashCode());
    result = prime * result + ((contentType == null) ? 0 : contentType.hashCode());
    result = prime * result + ((md5hash == null) ? 0 : md5hash.hashCode());
    result = prime * result + ((reducedImageMd5Hash == null) ? 0 : reducedImageMd5Hash.hashCode());
    result = prime * result + ((reduceImage == null) ? 0 : reduceImage.hashCode());
    result = prime * result + ((downloadUrl == null) ? 0 : downloadUrl.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OdkTablesFileManifestEntry)) {
      return false;
    }
    OdkTablesFileManifestEntry other = (OdkTablesFileManifestEntry) obj;
    // The 'reducedImageMd5Hash' and 'reduce' parameters not included in equals
    // because could have other downstream effects.
    return (filename == null ? other.filename == null : filename.equals(other.filename))
        && (contentLength == null ? other.contentLength == null : contentLength
            .equals(other.contentLength))
        && (contentType == null ? other.contentType == null : contentType.equals(other.contentType))
        && (md5hash == null ? other.md5hash == null : md5hash.equals(other.md5hash))
        && (downloadUrl == null ? other.downloadUrl == null : downloadUrl.equals(other.downloadUrl));
  }

  @Override
  public int compareTo(OdkTablesFileManifestEntry other) {
    // The 'reducedImageMd5Hash' and 'reduce' parameters are not compared
    // because doing so could have other downstream effects.
    if ( filename == null ) {
      if ( other.filename != null ) {
        return -1;
      }
    } else if ( other.filename == null ) {
      return 1;
    }
    
    int cmp = filename.compareTo(other.filename);
    if ( cmp != 0 ) {
      return cmp;
    }

    if ( md5hash == null ) {
      if ( other.md5hash != null ) {
        return -1;
      }
    } else if ( other.md5hash == null ) {
      return 1;
    }
    
    cmp = md5hash.compareTo(other.md5hash);
    return cmp;
  }

}
