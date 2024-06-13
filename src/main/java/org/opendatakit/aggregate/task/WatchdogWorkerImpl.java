/*
 * Copyright (C) 2010 University of Washington
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
package org.opendatakit.aggregate.task;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.exception.ODKExternalServiceException;
import org.opendatakit.aggregate.exception.ODKFormNotFoundException;
import org.opendatakit.aggregate.exception.ODKIncompleteSubmissionData;
import org.opendatakit.aggregate.util.BackendActionsTable;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.web.CallingContext;

import java.util.Date;

/**
 * Common worker implementation for restarting stalled tasks.
 *
 * @author wbrunette@gmail.com
 * @author mitchellsundt@gmail.com
 *
 */
public class WatchdogWorkerImpl {

  private Log logger = LogFactory.getLog(WatchdogWorkerImpl.class);

  private static class SubmissionMetadata {
    public final String uri;
    public final Date markedAsCompleteDate;

    SubmissionMetadata(String uri, Date markedAsCompleteDate) {
      this.uri = uri;
      this.markedAsCompleteDate = markedAsCompleteDate;
    }
  }


  public void checkTasks(CallingContext cc) throws ODKExternalServiceException,
      ODKFormNotFoundException, ODKDatastoreException, ODKIncompleteSubmissionData {
    logger.info("---------------------BEGIN Watchdog");
    boolean cullThisWatchdog = false;
    boolean activeTasks = true;
    Watchdog wd = null;
    try {
      wd = (Watchdog) cc.getBean(BeanDefs.WATCHDOG);
      cullThisWatchdog = BackendActionsTable.updateWatchdogStart(wd, cc);
    } finally {
      // NOTE: if the above threw an exception, we re-start the watchdog.
      // otherwise, we restart it only if there is work to be done.
      BackendActionsTable.rescheduleWatchdog(activeTasks, cullThisWatchdog, cc);
      logger.info("---------------------END Watchdog");
    }
  }







}
