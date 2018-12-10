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

package org.opendatakit.common.web;

import javax.servlet.ServletContext;

import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.Ignore;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.security.Realm;
import org.opendatakit.common.security.User;
import org.opendatakit.common.security.UserService;
import org.opendatakit.common.security.spring.UserServiceImpl;
import org.opendatakit.common.web.constants.BasicConsts;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

@Ignore("not a test")
public class TestContextFactory {

	/**
	 * Singleton of the application context
	 */

	@Ignore("not a test")
	public static final class CallingContextImpl implements CallingContext {
		final String serverUrl;
		final String secureServerUrl;
		final String webApplicationBase;
		final Datastore datastore;
		final UserService userService;
		boolean asDaemon = true; // otherwise there isn't a current user...

		CallingContextImpl() {
			Properties appProps = new Properties();

			try {
				appProps.load(new FileInputStream("external-resources/integration.properties"));
			} catch (IOException e) {
				System.err.println("PROBLEM FINDING PROPERTY FILE!!!!!!!!!!!!");
				try {
					String current = new java.io.File(".").getCanonicalPath();
					System.err.println("Current dir:" + current);
				} catch (Exception e2) {
					e2.printStackTrace();
				}
				e.printStackTrace();
			}

			String baseUrl = appProps.getProperty("test.server.baseUrl","");
			if ( baseUrl.length() > 0 && !baseUrl.startsWith(BasicConsts.FORWARDSLASH)) {
				baseUrl = BasicConsts.FORWARDSLASH + baseUrl;
			}
			webApplicationBase = baseUrl;

			String hostname = appProps.getProperty("test.server.hostname", "localhost");
			String port = appProps.getProperty("test.server.port","8888");
			String secureport = appProps.getProperty("test.server.secure.port","8443");

			String jdbcDriver = appProps.getProperty("jdbc.driverClassName");
			String jdbcUrl = appProps.getProperty("jdbc.url");

			String dbUsername =  appProps.getProperty("jdbc.username");
			String dbPassword =  appProps.getProperty("jdbc.password");
			String dbSchema =  appProps.getProperty("jdbc.schema");

			serverUrl = "http://" + hostname + ":" + port + webApplicationBase;
			secureServerUrl = "https://" + hostname + ":" + secureport + webApplicationBase;

			try {
				BasicDataSource dataSource = new BasicDataSource();
				dataSource.setDriverClassName(jdbcDriver);
				dataSource.setUrl(jdbcUrl);
				dataSource.setUsername(dbUsername);
				dataSource.setPassword(dbPassword);
				dataSource.setMaxIdle(10);
				dataSource.setMinIdle(5);
				dataSource.setMaxTotal(100);
				dataSource.setMaxConnLifetimeMillis(590000);
				dataSource.setMaxWaitMillis(30000);
				dataSource.setValidationQuery("select schema_name from information_schema.schemata limit 1");
				dataSource.setValidationQueryTimeout(1);
				dataSource.setTestOnBorrow(true);

				if(jdbcDriver.equals("org.postgresql.Driver")) {
					org.opendatakit.common.persistence.engine.pgres.DatastoreImpl db = new org.opendatakit.common.persistence.engine.pgres.DatastoreImpl();
					db.setDataSource(dataSource);
					db.setSchemaName(dbSchema);
					datastore = db;
				} else if (jdbcDriver.equals("com.mysql.jdbc.Driver")) {
					org.opendatakit.common.persistence.engine.mysql.DatastoreImpl db = new org.opendatakit.common.persistence.engine.mysql.DatastoreImpl();
					db.setDataSource(dataSource);
					db.setSchemaName(dbSchema);
					datastore = db;
				} else {
					throw new RuntimeException("UNABLE TO DETERMINE DATABASE TYPE for TextContextFactory");
				}

				Realm realm = new Realm();
				realm.setIsGaeEnvironment(false);
				realm.setRealmString("opendatakit.org ODK Aggregate");
				realm.setHostname(hostname);
				realm.setPort(Integer.parseInt(port));
				realm.setSecurePort(Integer.parseInt(secureport));
				realm.setChannelType("REQUIRES_INSECURE_CHANNEL");
				realm.setSecureChannelType("REQUIRES_INSECURE_CHANNEL");

				UserServiceImpl us = new UserServiceImpl();
				us.setRealm(realm);
				userService = us;

			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public Object getBean(String beanName) {
			return null;
		}

		@Override
		public Datastore getDatastore() {
			return datastore;
		}

		@Override
		public UserService getUserService() {
			return userService;
		}

		@Override
		public ServletContext getServletContext() {
			return null;
		}

		@Override
		public String getWebApplicationURL() {
			return webApplicationBase + BasicConsts.FORWARDSLASH;
		}

		@Override
		public String getWebApplicationURL(String servletAddr) {
			return webApplicationBase + BasicConsts.FORWARDSLASH + servletAddr;
		}

		@Override
		public String getServerURL() {
			return serverUrl;
		}

		@Override
		public String getSecureServerURL() {
			return secureServerUrl;
		}

		@Override
		public void setAsDaemon(boolean asDaemon ) {
			this.asDaemon = asDaemon;
		}

		@Override
		public boolean getAsDeamon() {
			return asDaemon;
		}

		@Override
		public User getCurrentUser() {
			return asDaemon ? userService.getDaemonAccountUser() : userService.getCurrentUser();
		}
	}

	/**
	 * Private constructor
	 */
	private TestContextFactory() {}

	public static CallingContext getCallingContext() {
		return new CallingContextImpl();
	}

}
