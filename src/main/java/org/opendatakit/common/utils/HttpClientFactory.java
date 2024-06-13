/*
 * Copyright (C) 2011 University of Washington.
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
package org.opendatakit.common.utils;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.io.SocketConfig;
/**
 * Interface for the bean that will construct an HttpClient that is appropriate
 * for the GAE or Tomcat container in which the application runs. The GAE
 * variant maps the Apache request into underlying GAE URLFetchService. The
 * Tomcat variant just uses the default Apache client.
 * 
 * @author mitchellsundt@gmail.com
 * @author wbrunette@gmail.com
 * 
 */
public interface HttpClientFactory {

	/**
	 * All arguments can be null. Updated to Apache 4.5.2 active usage.
	 * 
	 * @param socketConfig
	 * @param connectionConfig
	 * @param requestConfig
	 * @return
	 */
  public CloseableHttpClient createHttpClient(SocketConfig socketConfig, ConnectionConfig connectionConfig, RequestConfig requestConfig);
}
