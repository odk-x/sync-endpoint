/*
 * Copyright (C) 2016 University of Washington.
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

package org.opendatakit.aggregate.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;
import org.opendatakit.common.security.server.SecurityServiceUtil;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.HtmlConsts;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Servlet to return the set of roles that a user has been granted.
 * 
 * This *always* returns a JSON array of role names. 
 * 
 * I.e., Accept header is ignored.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class GetGrantedRolesAndDefaultServlet extends HttpServlet {

  /**
   * Serial number for serialization
   */
  private static final long serialVersionUID = -9115712148453254161L;

  /**
   * URI from base
   */
  public static final String ADDR = "roles/grantedDefault";

  private static final ObjectMapper mapper = new ObjectMapper();

  /**
   * Handler for HTTP Get request that returns the list of roles assigned to this user.
   * 
   * Assumed to return a entity body that is a JSON serialization of a list.
   *
   * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest,
   *      javax.servlet.http.HttpServletResponse)
   */
  @Override
  public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);
    
    Map<String,Object> roleGrantStruct = SecurityServiceUtil.getRolesAndDefaultGroup(cc);
        
    resp.addHeader(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION);
    resp.addHeader("Access-Control-Allow-Origin", "*");
    resp.addHeader("Access-Control-Allow-Credentials", "true");
    resp.addHeader(HttpHeaders.HOST, cc.getServerURL());
    resp.setContentType(HtmlConsts.RESP_TYPE_JSON);
    resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
    
    PrintWriter out = resp.getWriter();
    out.write(mapper.writeValueAsString(roleGrantStruct));
    out.flush();

    resp.setStatus(HttpStatus.SC_OK);
  }
}
