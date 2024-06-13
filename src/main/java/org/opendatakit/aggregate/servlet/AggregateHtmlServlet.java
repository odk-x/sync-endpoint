/*
 * Copyright (C) 2011 University of Washington
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.ContextFactory;
import org.opendatakit.aggregate.constants.common.UIConsts;
import org.opendatakit.aggregate.server.ServerPreferencesProperties;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.constants.BasicConsts;
import org.opendatakit.common.web.constants.HtmlConsts;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

/**
 * Stupid class to wrap the Aggregate.html page that GWT uses for all its UI
 * presentation. Needed so that access to the page can be managed by Spring
 * Security.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class AggregateHtmlServlet extends ServletUtilBase {

  private static final Log logger = LogFactory.getLog(AggregateHtmlServlet.class);
  /**
	 *
	 */
  private static final long serialVersionUID = 5811797423869654357L;

  public static final String ADDR = UIConsts.HOST_PAGE_BASE_ADDR;

  public static final String PAGE_CONTENTS_FIRST = "<!doctype html>"
      + "<!-- The DOCTYPE declaration above will set the    -->"
      + "<!-- browser's rendering engine into               -->"
      + "<!-- \"Standards Mode\". Replacing this declaration  -->"
      + "<!-- with a \"Quirks Mode\" doctype may lead to some -->"
      + "<!-- differences in layout.                        -->"
      + ""
      + "<html>"
      + "  <head>"
      + "	<meta http-equiv=\"content-type\" content=\"text/html; charset=UTF-8\">"
      + "  <link rel=\"shortcut icon\" href=\"favicon.ico\"/>"
      + "	<title>ODK Aggregate</title>"
      + "	<script type=\"text/javascript\" language=\"javascript\" src=\"javascript/jquery-1.11.1.min.js\"></script>"
      + "	<script type=\"text/javascript\" language=\"javascript\" src=\"javascript/resize.js\"></script>"
      + "	<script type=\"text/javascript\" language=\"javascript\" src=\"javascript/main.js\"></script>"
      + "    <script type=\"text/javascript\" language=\"javascript\" src=\"aggregateui/aggregateui.nocache.js\"></script>"
      + "    <script type=\"text/javascript\" language=\"javascript\" src=\"https://maps.googleapis.com/maps/api/js?";
      public static final String PAGE_CONTENTS_SECOND = "sensor=false\"></script>"
      + "    <link type=\"text/css\" rel=\"stylesheet\" href=\"AggregateUI.css\">"
      + "    <link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/button.css\">"
      + "    <link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/table.css\">"
      + "    <link type=\"text/css\" rel=\"stylesheet\" href=\"stylesheets/navigation.css\">"
      + "  </head>"
      + "  <body>"
      + "    <iframe src=\"javascript:''\" id=\"__gwt_historyFrame\" tabIndex='-1' style=\"position:absolute;width:0;height:0;border:0\"></iframe>"
      + "    <noscript>"
      + "      <div style=\"width: 22em; position: absolute; left: 50%; margin-left: -11em; color: red; background-color: white; border: 1px solid red; padding: 4px; font-family: sans-serif\">"
      + "        Your web browser must have JavaScript enabled"
      + "        in order for this application to display correctly."
      + "      </div>"
      + "    </noscript>" + "	<div id=\"not_secure_content\"></div><br><div id=\"error_content\"></div><div id=\"dynamic_content\"></div>"
 	  + "  </body>"
      + "</html>";

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException,
      IOException {
    CallingContext cc = ContextFactory.getCallingContext(this, req);

    // Check to make sure we are using the canonical server name.
    // If not, redirect to that name.  This ensures that authentication
    // cookies will have the proper realm(s) established for them.
    String newUrl = cc.getServerURL() + BasicConsts.FORWARDSLASH + ADDR;
    String query = req.getQueryString();
    if (query != null && query.length() != 0) {
      newUrl += "?" + query;
    }
    URL url = new URL(newUrl);
    if (!url.getHost().equalsIgnoreCase(req.getServerName())) {
      // we should redirect over to the proper fully-formed URL.
      logger.info("Incoming servername: " + req.getServerName() + " expected: " + url.getHost() + " -- redirecting.");
      resp.sendRedirect(newUrl);
      return;
    }

    // OK. We are using the canonical server name.
    resp.setContentType(HtmlConsts.RESP_TYPE_HTML);
    resp.setCharacterEncoding(HtmlConsts.UTF8_ENCODE);
    resp.addHeader(HtmlConsts.X_FRAME_OPTIONS, HtmlConsts.X_FRAME_SAMEORIGIN);
    PrintWriter out = resp.getWriter();
    out.print(PAGE_CONTENTS_FIRST);
    String simpleApiKey;
    try {
      simpleApiKey = ServerPreferencesProperties.getGoogleSimpleApiKey(cc);
      if ( simpleApiKey != null && simpleApiKey.length() != 0 ) {
        out.print("key=" + encodeParameter(simpleApiKey) + "&amp;");
      }
    } catch (ODKEntityNotFoundException e) {
      e.printStackTrace();
      logger.info("Unable to access Map APIKey");
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
      logger.info("Unable to access Map APIKey");
    }
    out.print(PAGE_CONTENTS_SECOND);
  }

}
