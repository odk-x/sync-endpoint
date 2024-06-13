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
package org.opendatakit.aggregate.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.opendatakit.aggregate.constants.BeanDefs;
import org.opendatakit.aggregate.server.ServerPreferencesProperties;
import org.opendatakit.aggregate.task.Watchdog;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.DataField;
import org.opendatakit.common.persistence.Datastore;
import org.opendatakit.common.persistence.PersistConsts;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKOverQuotaException;
import org.opendatakit.common.security.User;
import org.opendatakit.common.web.CallingContext;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the mechanisms used in GAE to launch Watchdog during periods of
 * website activity ( triggerWatchdog ) and from within the Watchdog
 * implementation when there are background tasks requiring supervision (
 * scheduleFutureWatchdog ).
 *
 * To do that, it tracks:
 *
 * - the start time of each Watchdog iteration ( updateWatchdogStart ),
 *
 * - the time-at-enqueuing of a Watchdog Task on the AppEngine queue,
 *
 * - the time-to-fire of the earliest future Watchdog activity.
 *
 * @author mitchellsundt@gmail.com
 *
 */
public class BackendActionsTable extends CommonFieldsBase {

  private static final String WATCHDOG_SCHEDULING_ROW_ID = "rid:watchdog_scheduling";
  private static final String WATCHDOG_ENQUEUE_ROW_ID = "rid:watchdog_enqueue";
  private static final String WATCHDOG_START_ROW_ID = "rid:watchdog_start";
  private static final String WATCHDOG_DEATH_TOGGLE_ROW_ID = "rid:watchdog_death_toggle";

  private static final String TABLE_NAME = "_backend_actions";

  private static final DataField LAST_REVISION_DATE = new DataField("LAST_REVISION",
      DataField.DataType.DATETIME, true);

  private static final Log logger = LogFactory.getLog(BackendActionsTable.class);

  /** delay between re-loads of the lastPublisherRevision HashMap. 30 seconds. */
  public static final long HASHMAP_LIFETIME_MILLISECONDS = PersistConsts.MAX_SETTLE_MILLISECONDS * 10L;
  /**
   * delay between re-issuing uploadSubmissions tasks for a given uriFsc. 3.5
   * seconds
   */
  public static final long PUBLISHING_DELAY_MILLISECONDS = 500L + PersistConsts.MAX_SETTLE_MILLISECONDS;
  /**
   * delay between watchdog sweeps (primarily to nudge failed publishers
   * onward). 30 seconds.
   */
  public static final long FAST_PUBLISHING_RETRY_MILLISECONDS = 30L * 1000L;
  /** delay between watchdog sweeps when there are no active tasks. 15 minutes. */
  public static final long IDLING_WATCHDOG_RETRY_INTERVAL_MILLISECONDS = 15L * 60000L;

  // fields used to determine triggering of UploadSubmissions task creation.
  /** last time we reloaded the publisher hashMap from the datastore */
  public static long lastHashmapCleanTimestamp = 0L;
  /** the publisher hashMap<uriFsc, lastUploadSubmissionTaskEnqueueTime> */
  public static Map<String, Long> lastPublisherRevision = new HashMap<String, Long>();

  /** last time we reloaded the lastWatchdog... values from the datastore */
  private static long lastFetchTime = 0L;

  // fields used to determine when to fire a watchdog based upon usage.

  /** last time a Watchdog was started (directly fetched) */
  private static long lastWatchdogStartTime = 0L;
  /** the last time we enqueued a Watchdog to start */
  private static long lastWatchdogEnqueueTime = 0L;
  /** the time when we should next enqueue a Watchdog */
  private static long lastWatchdogSchedulingTime = 0L;

  /**
   * Construct a relation prototype. Only called via
   * {@link #assertRelation(Datastore, User)}
   *
   * @param schemaName
   */
  protected BackendActionsTable(String schemaName) {
    super(schemaName, TABLE_NAME);
    fieldList.add(LAST_REVISION_DATE);
  }

  /**
   * Construct an empty entity. Only called via {@link #getEmptyRow(User)}
   *
   * @param ref
   * @param user
   */
  protected BackendActionsTable(BackendActionsTable ref, User user) {
    super(ref, user);
  }

  // Only called from within the persistence layer.
  @Override
  public CommonFieldsBase getEmptyRow(User user) {
    BackendActionsTable t = new BackendActionsTable(this, user);
    return t;
  }

  private Date getLastRevisionDate() {
    return getDateField(LAST_REVISION_DATE);
  }

  private void setLastRevisionDate(Date value) {
    setDateField(LAST_REVISION_DATE, value);
  }

  private static BackendActionsTable relation = null;

  /**
   * This is private because this table implements a singleton pattern.
   *
   * @param datastore
   * @param user
   * @return
   * @throws ODKDatastoreException
   */
  private static synchronized final BackendActionsTable assertRelation(Datastore datastore,
      User user) throws ODKDatastoreException {
    if (relation == null) {
      BackendActionsTable relationPrototype;
      relationPrototype = new BackendActionsTable(datastore.getDefaultSchemaName());
      datastore.assertRelation(relationPrototype, user);
      relation = relationPrototype;
    }
    return relation;
  }

  /**
   * This retrieves the singleton record.
   *
   * @param uri
   * @param datastore
   * @param user
   * @return
   * @throws ODKDatastoreException
   */
  private static final BackendActionsTable getSingletonRecord(String uri, Datastore datastore,
      User user) throws ODKDatastoreException {
    BackendActionsTable prototype = assertRelation(datastore, user);
    BackendActionsTable record = null;
    try {
      record = datastore.getEntity(prototype, uri, user);
    } catch (ODKEntityNotFoundException e) {
      record = datastore.createEntityUsingRelation(prototype, user);
      record.setStringField(prototype.primaryKey, uri);
      record.setLastRevisionDate(new Date(0)); // NOTE: Defaults differently
                                               // than SecurityRevisionsTable
      datastore.putEntity(record, user);
    }
    return record;
  }

  public static final synchronized boolean mayHaveRecentPublisherRevision(String uriFsc, CallingContext cc)
        throws ODKDatastoreException {
    boolean wasDaemon = cc.getAsDeamon();
    cc.setAsDaemon(true);
    try {
      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();
      long now = System.currentTimeMillis();
      if (lastHashmapCleanTimestamp + HASHMAP_LIFETIME_MILLISECONDS < now) {
        lastPublisherRevision.clear();
        lastHashmapCleanTimestamp = now;
      }

      BackendActionsTable t = null;
      Long oldTime = lastPublisherRevision.get(uriFsc);
      if (oldTime == null) {
        // see if we have anything in the table (created if missing).
        t = BackendActionsTable.getSingletonRecord(uriFsc, ds, user);
        oldTime = t.getLastRevisionDate().getTime();
        lastPublisherRevision.put(uriFsc, oldTime);
      }
      return ( oldTime + HASHMAP_LIFETIME_MILLISECONDS + PUBLISHING_DELAY_MILLISECONDS > now );
    } finally {
      cc.setAsDaemon(wasDaemon);
    }
  }


  /**
   * Updates the time the watchdog last ran. Called only from within the
   * WatchdogWorkerImpl class.
   *
   * @param cc
   * @return true if the watchdog should be culled (not rescheduled)
   */
  public static final synchronized boolean updateWatchdogStart(Watchdog wd, CallingContext cc) {
    boolean wasDaemon = cc.getAsDeamon();

    try {
      cc.setAsDaemon(true);
      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();

      // fetch the scheduling row
      BackendActionsTable t = getSingletonRecord(WATCHDOG_START_ROW_ID, ds, user);
      long oldStartTime = t.getLastRevisionDate().getTime();
      lastWatchdogStartTime = System.currentTimeMillis();
      t.setLastRevisionDate(new Date(lastWatchdogStartTime));
      ds.putEntity(t, user);

      long expectedNextStart = oldStartTime + FAST_PUBLISHING_RETRY_MILLISECONDS;

      if (expectedNextStart > lastWatchdogStartTime) {
        logger.warn("watchdog started early: " + Long.toString(lastWatchdogStartTime) + " vs "
            + Long.toString(expectedNextStart));
        if (expectedNextStart > lastWatchdogStartTime + PersistConsts.MAX_SETTLE_MILLISECONDS) {
          // we likely have 2 or more Watchdog timers running.
          // use the DEATH_TOGGLE datetime as a boolean flag
          // that we alternately toggle. Whenever it is `true`
          // we should not requeue this Watchdog. Over time,
          // this will cause the extra Watchdog threads to
          // die out. Similar to the way detecting cycles
          // works in a linked list.
          t = getSingletonRecord(WATCHDOG_DEATH_TOGGLE_ROW_ID, ds, user);
          long sense = t.getLastRevisionDate().getTime();
          t.setLastRevisionDate(new Date(sense + 1));
          ds.putEntity(t, user);

          boolean culled = (sense & 1L) == 1L;
          if (culled) {
            // culling only occurs on GAE
            logger.warn("---------------WATCHDOG SHOULD BE CULLED!");
          }
          return culled;
        }
      }
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
    } finally {
      cc.setAsDaemon(wasDaemon);
    }
    return false;
  }

  private static final void logValues(String tag, long now, long futureMilliseconds,
      long requestedTime) {
    String msg;
    if (requestedTime == -1L) {
      msg = String.format(
          "%7$s last Fetch: %1$18s [S: %2$18s Eq: %3$18s Fs: %4$18s] futureMillis: %5$18s",
          Long.toString(lastFetchTime - now), Long.toString(lastWatchdogStartTime - now),
          Long.toString(lastWatchdogEnqueueTime - now),
          Long.toString(lastWatchdogSchedulingTime - now), Long.toString(futureMilliseconds),
          Long.toString(requestedTime - now), tag);
    } else {
      msg = String
          .format(
              "%7$s last Fetch: %1$18s [S: %2$18s Eq: %3$18s Fs: %4$18s] futureMillis: %5$18s requested: %6$18s",
              Long.toString(lastFetchTime - now), Long.toString(lastWatchdogStartTime - now),
              Long.toString(lastWatchdogEnqueueTime - now),
              Long.toString(lastWatchdogSchedulingTime - now), Long.toString(futureMilliseconds),
              Long.toString(requestedTime - now), tag);
    }
    logger.info(msg);
  }

  private static final String INCOMING = "incoming-";
  private static final String FETCHED = "-fetched-";
  private static final String SCHEDULED = "Fs-update";
  private static final String CLEARED = "Fs-clear-";
  private static final String ENQUEUED = "Eq-update";

  private static class WatchdogRecords {
    private BackendActionsTable startTime = null;
    private BackendActionsTable enqueueTime = null;
    private BackendActionsTable schedulingTime = null;

    WatchdogRecords(long now, CallingContext cc) throws ODKDatastoreException {

      // refetch all the data if it is more than the settle period old...
      if (lastFetchTime + PersistConsts.MAX_SETTLE_MILLISECONDS < now) {
        fetchAll(now, cc);
      }
    }

    void fetchAll(long now, CallingContext cc) throws ODKDatastoreException {
      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();

      if (lastFetchTime == now)
        return;

      // this is gratuitous, but it puts the queue activities in context
      // it would only be updated on the background process if we didn't
      // read it here.
      startTime = getSingletonRecord(WATCHDOG_START_ROW_ID, ds, user);
      lastWatchdogStartTime = startTime.getLastRevisionDate().getTime();

      enqueueTime = getSingletonRecord(WATCHDOG_ENQUEUE_ROW_ID, ds, user);
      lastWatchdogEnqueueTime = enqueueTime.getLastRevisionDate().getTime();

      schedulingTime = getSingletonRecord(WATCHDOG_SCHEDULING_ROW_ID, ds, user);
      lastWatchdogSchedulingTime = schedulingTime.getLastRevisionDate().getTime();

      lastFetchTime = now;

      logValues(FETCHED, now, -1L, -1L);
    }

  }

  /**
   * This is effectively GAE-specific: Tomcat installations use a scheduled
   * executor to periodically fire the watchdog (and do not use this mechanism).
   * i.e., on Tomcat, the calls to: Watchdog.onUsage(delay, cc); are no-ops.
   *
   * Schedule a watchdog to run the specified number of milliseconds into the
   * future (zero is OK).
   *
   * @param watchdog
   * @param futureMilliseconds
   * @param cc
   */
  private static final synchronized void scheduleFutureWatchdog(Watchdog wd,
      long futureMilliseconds, CallingContext cc) {
    boolean wasDaemon = cc.getAsDeamon();

    long now = System.currentTimeMillis();
    logValues(INCOMING, now, futureMilliseconds, -1L);

    try {
      cc.setAsDaemon(true);
      Datastore ds = cc.getDatastore();
      User user = cc.getCurrentUser();

      WatchdogRecords records = new WatchdogRecords(now, cc);

      // don't schedule any timer before the blackoutTime
      long blackoutTime = Math.max(lastWatchdogEnqueueTime + FAST_PUBLISHING_RETRY_MILLISECONDS,
          now);

      // Revise the request to start at the end of the blackout period.
      // Two cases: (1) immediate request (2) future request
      long requestedWatchdogSchedulingTime = Math.max(blackoutTime, now + futureMilliseconds);

      // Update the BackendActionsTable records and/or fire a Watchdog if:
      // (1) there is an active timer that is in the past
      // or
      // (2) the adjusted request time is now
      // or
      // (3) there is no active scheduling time (and this request is in the
      // future)
      // or
      // (4) the active scheduling time should be lowered due to this request.
      if ((lastWatchdogSchedulingTime < now && lastWatchdogSchedulingTime > lastWatchdogEnqueueTime)
          || (requestedWatchdogSchedulingTime == now)
          || (lastWatchdogSchedulingTime <= lastWatchdogEnqueueTime)
          || (requestedWatchdogSchedulingTime < lastWatchdogSchedulingTime)) {

        // refetch and update our values...
        records.fetchAll(now, cc);

        // and recompute things...
        blackoutTime = Math.max(lastWatchdogEnqueueTime + FAST_PUBLISHING_RETRY_MILLISECONDS, now);

        requestedWatchdogSchedulingTime = Math.max(blackoutTime, now + futureMilliseconds);

        // OK -- see if the conditions for update still apply...
        boolean activeSchedulingTimeInThePast = (lastWatchdogSchedulingTime < now && lastWatchdogSchedulingTime > lastWatchdogEnqueueTime);

        if (activeSchedulingTimeInThePast || (requestedWatchdogSchedulingTime == now)
            || (lastWatchdogSchedulingTime <= lastWatchdogEnqueueTime)
            || (requestedWatchdogSchedulingTime < lastWatchdogSchedulingTime)) {
          // YES... update everything

          // Case (1) and/or (2)
          // enqueue any request first...
          if (activeSchedulingTimeInThePast || requestedWatchdogSchedulingTime == now) {

            // fire the Watchdog ONLY if:
            // we are doing fast publishing and the watchdog has not been run in a while
            // or
            // we are not doing fast publishing.
            // During fast publishing, the watchdog should be enqueued during rescheduleWatchdog()...
            if ((wd.getFasterWatchdogCycleEnabled() &&
                  (lastWatchdogStartTime < (now - (FAST_PUBLISHING_RETRY_MILLISECONDS + PUBLISHING_DELAY_MILLISECONDS))))
                  || !wd.getFasterWatchdogCycleEnabled()) {
              wd.onUsage(0L, cc); // no wait, as we are well past due...
            }

            // update enqueue value...
            records.enqueueTime.setLastRevisionDate(new Date(now));
            lastWatchdogEnqueueTime = now;
            ds.putEntity(records.enqueueTime, user);
            logValues(ENQUEUED, now, futureMilliseconds, requestedWatchdogSchedulingTime);
          }

          // If we fired a stale request (case (1)) then don't schedule
          // anything.
          // The Watchdog itself will handle queuing its next runtime.
          if (!activeSchedulingTimeInThePast) {
            // EXCLUDING Case (1)...

            // if we fired an immediate request, we should
            // reset the scheduling time to the past (retroactively)
            // to cancel any future request.
            if (requestedWatchdogSchedulingTime == now) {
              // Case (2)...
              if (lastWatchdogSchedulingTime > lastWatchdogEnqueueTime) {
                // there's a future timer that should be cleared
                records.schedulingTime.setLastRevisionDate(new Date(now));
                lastWatchdogSchedulingTime = now;
                ds.putEntity(records.schedulingTime, user);
                logValues(CLEARED, now, futureMilliseconds, requestedWatchdogSchedulingTime);
              }
            } else {
              // Case (3) or (4) - schedule a future event
              records.schedulingTime.setLastRevisionDate(new Date(requestedWatchdogSchedulingTime));
              lastWatchdogSchedulingTime = requestedWatchdogSchedulingTime;
              ds.putEntity(records.schedulingTime, user);
              logValues(SCHEDULED, now, futureMilliseconds, requestedWatchdogSchedulingTime);
            }
          }
        }
      }
    } catch (ODKDatastoreException e) {
      e.printStackTrace();
    } finally {
      cc.setAsDaemon(wasDaemon);
    }
  }

  /**
   * This is effectively GAE-specific: Tomcat installations use a scheduled
   * executor to periodically fire the watchdog (and do not use this mechanism).
   *
   * Check whether a watchdog should be spun up. Spin one up every
   * IDLING_WATCHDOG_RETRY_INTERVAL_MILLISECONDS. Note that if the Watchdog
   * determines that there is work pending, it will schedule a watchdog every
   * FAST_PUBLISHING_RETRY_MILLISECONDS.
   *
   * @param cc
   */
  public static final synchronized void triggerWatchdog(CallingContext cc) {

    Watchdog wd = (Watchdog) cc.getBean(BeanDefs.WATCHDOG);

    // don't schedule any timer before the next idling retry time
    long nextIdlingRetryTime;

    if (wd.getFasterWatchdogCycleEnabled()) {
      nextIdlingRetryTime = lastWatchdogEnqueueTime + FAST_PUBLISHING_RETRY_MILLISECONDS;
    } else {
      nextIdlingRetryTime = lastWatchdogEnqueueTime + IDLING_WATCHDOG_RETRY_INTERVAL_MILLISECONDS;
    }

    long futureMilliseconds = Math.max(0L, nextIdlingRetryTime - System.currentTimeMillis());

    scheduleFutureWatchdog(wd, futureMilliseconds, cc);
  }

  /**
   * This is effectively GAE-specific: Tomcat installations use a scheduled
   * executor to periodically fire the watchdog (and do not use this mechanism).
   *
   * At the completion of the current watchdog, if we have active tasks or have
   * fast publishing enabled, schedule a new watchdon
   * FAST_PUBLISHING_RETRY_MILLISECONDS into the future. Note that if we do not
   * have fast publishing enabled, the publishing does not take immediate
   * effect, but is a suggested next start time, and only if the website is
   * active will it be honored.
   *
   * @param hasActiveTasks
   *          -- whether active tasks were detected
   * @param cullThisWatchdog
   *          -- whether this watchdog should be rescheduled
   * @param cc
   */
  public static final synchronized void rescheduleWatchdog(boolean hasActiveTasks,
      boolean cullThisWatchdog, CallingContext cc) {

    Watchdog wd = (Watchdog) cc.getBean(BeanDefs.WATCHDOG);

    try {
      boolean disabled = ServerPreferencesProperties.getFasterBackgroundActionsDisabled(cc);
      boolean wasFastPublishing = ServerPreferencesProperties.getFasterWatchdogCycleEnabled(cc);

      if (!hasActiveTasks && wasFastPublishing) {
        // we should switch to the slower watchdog cycle
        ServerPreferencesProperties.setFasterWatchdogCycleEnabled(cc, false);
        wd.setFasterWatchdogCycleEnabled(false);
      }

      if (hasActiveTasks) {
        if (!disabled) {
          if (!wasFastPublishing) {
            // switch to the faster watchdog cycle
            ServerPreferencesProperties.setFasterWatchdogCycleEnabled(cc, true);
            wd.setFasterWatchdogCycleEnabled(true);
          } else {
            // schedule the next watchdog in the future.
            // if we are culling the watchdog, then do not enqueue it.
            if (!cullThisWatchdog) {
              wd.onUsage(FAST_PUBLISHING_RETRY_MILLISECONDS, cc);
            }
          }
        }
        // and regardless, update the next-eligible-requeue time
        // this is only used if fast publishing is disabled.
        scheduleFutureWatchdog(wd, FAST_PUBLISHING_RETRY_MILLISECONDS, cc);
      }
    } catch (ODKEntityNotFoundException e) {
      e.printStackTrace();
    } catch (ODKOverQuotaException e) {
      e.printStackTrace();
    }
  }
}
