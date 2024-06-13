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
package org.opendatakit.common.utils.tomcat;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.BasicHttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.opendatakit.common.utils.HttpClientFactory;

/**
 * Implementation that just uses Apache's default http client.
 * 
 * @author mitchellsundt@gmail.com
 * 
 */
public class TomcatHttpClientFactoryImpl implements HttpClientFactory {

  public TomcatHttpClientFactoryImpl() {
  }

  @Override
  public CloseableHttpClient createHttpClient(SocketConfig socketConfig,
      ConnectionConfig connectionConfig, RequestConfig requestConfig) {
    HttpClientBuilder builder = HttpClientBuilder.create();
    BasicHttpClientConnectionManager cm = new BasicHttpClientConnectionManager();
    boolean connectionManagerSet = false;

    if (socketConfig != null) {
      cm.setSocketConfig(socketConfig);
      connectionManagerSet = true;
    }
    if (connectionConfig != null) {
      cm.setConnectionConfig(connectionConfig);
      connectionManagerSet = true;
    }
    if(connectionManagerSet) {
      builder.setConnectionManager(cm);
    }

    if (requestConfig != null) {
      builder.setDefaultRequestConfig(requestConfig);
    }
    return builder.build();
  }

}
