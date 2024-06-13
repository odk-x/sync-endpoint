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

package org.opendatakit.aggregate.odktables.impl.api;

import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.aggregate.odktables.rest.entity.Error;
import org.opendatakit.aggregate.odktables.rest.entity.Error.ErrorType;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;

public class RuntimeExceptionMapper implements ExceptionMapper<RuntimeException> {

  private final MediaType type;

  public RuntimeExceptionMapper(MediaType type) {
    this.type = type;
  }

  @Override
  public Response toResponse(RuntimeException e) {
    e.printStackTrace();

    String msg = e.getMessage();
    if (msg == null) {
      msg = e.toString();
    }

    if (e instanceof IllegalArgumentException) {
      return Response.status(Status.BAD_REQUEST)
          .entity(new Error(ErrorType.BAD_REQUEST, "Bad arguments: " + msg)).type(type)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else if (e instanceof IllegalStateException) {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(new Error(ErrorType.INTERNAL_ERROR, "Illegal state: " + msg)).type(type)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    } else {
      return Response.status(Status.INTERNAL_SERVER_ERROR)
          .entity(new Error(ErrorType.INTERNAL_ERROR, msg)).type(type)
          .header(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION)
          .header("Access-Control-Allow-Origin", "*")
          .header("Access-Control-Allow-Credentials", "true").build();
    }
  }

}
