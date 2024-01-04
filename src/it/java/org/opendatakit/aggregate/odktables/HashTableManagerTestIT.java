/*
 * Copyright (C) 2013 University of Washington
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

package org.opendatakit.aggregate.odktables;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.TableManager.WebsafeTables;
import org.opendatakit.aggregate.odktables.api.OdkTables;
import org.opendatakit.aggregate.odktables.api.RealizedTableService;
import org.opendatakit.aggregate.odktables.api.TableService;
import org.opendatakit.aggregate.odktables.exception.PermissionDeniedException;
import org.opendatakit.aggregate.odktables.exception.TableAlreadyExistsException;
import org.opendatakit.aggregate.odktables.relation.DbTableEntry;
import org.opendatakit.aggregate.odktables.relation.DbTableFileInfo;
import org.opendatakit.aggregate.odktables.relation.EntityConverter;
import org.opendatakit.aggregate.odktables.rest.entity.Column;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifest;
import org.opendatakit.aggregate.odktables.rest.entity.OdkTablesFileManifestEntry;
import org.opendatakit.aggregate.odktables.rest.entity.Scope;
import org.opendatakit.aggregate.odktables.rest.entity.TableEntry;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResourceList;
import org.opendatakit.aggregate.odktables.rest.entity.TableRole.TablePermission;
import org.opendatakit.aggregate.odktables.security.TablesUserPermissions;
import org.opendatakit.common.ermodel.Entity;
import org.opendatakit.common.ermodel.Query;
import org.opendatakit.common.persistence.CommonFieldsBase;
import org.opendatakit.common.persistence.QueryResumePoint;
import org.opendatakit.common.persistence.exception.ODKDatastoreException;
import org.opendatakit.common.persistence.exception.ODKEntityNotFoundException;
import org.opendatakit.common.persistence.exception.ODKTaskLockException;
import org.opendatakit.common.utils.WebUtils;
import org.opendatakit.common.web.CallingContext;
import org.opendatakit.common.web.TestContextFactory;
import org.opendatakit.common.web.constants.BasicConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(org.junit.runners.JUnit4.class)
public class HashTableManagerTestIT extends AbstractServiceTest {

    private static final String ODK_CLIENT_VERSION = "2";
    private static final String TEXT_PLAIN = "text/plain";
    private static final String EMPTY_MD5_HASH = "d41d8cd98f00b204e9800998ecf8427e";
    private static final String TEST_FILE_1 = "\n" +
            "\n" +
            "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aenean non justo efficitur, hendrerit felis nec, laoreet urna. Sed consectetur, est eu facilisis aliquam, mauris turpis consequat dolor, ac euismod quam mauris in justo. Nam tempor porta lorem vel sollicitudin. Nam tincidunt risus id congue scelerisque. Integer luctus, orci a consectetur pellentesque, nulla nisl semper augue, eu suscipit arcu arcu non neque. Etiam a sodales nibh, non bibendum neque. Integer sit amet ligula volutpat, volutpat velit vel, lacinia arcu. Vivamus a malesuada urna. Nullam vitae lacinia ante.\n" +
            "\n" +
            "Sed massa libero, finibus ut orci eu, lacinia semper nulla. Cras quam massa, interdum at nisi non, ultricies lobortis turpis. Vivamus sit amet eleifend leo. Aenean imperdiet fermentum convallis. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Maecenas condimentum felis vitae nibh viverra, vel mattis neque mattis. Vivamus sit amet dictum elit, vel aliquam nunc. Cras bibendum viverra dolor, at iaculis nunc.\n" +
            "\n" +
            "Ut tincidunt pellentesque efficitur. In porttitor sodales feugiat. Quisque quis enim tortor. Donec sagittis odio at tortor iaculis porta. Nam turpis mauris, ultricies ut sapien in, viverra laoreet risus. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Curabitur feugiat diam et ligula bibendum scelerisque.\n" +
            "\n" +
            "Suspendisse laoreet augue ut porttitor congue. Proin consectetur sollicitudin auctor. Donec pretium iaculis ipsum vel luctus. Fusce justo justo, fringilla vestibulum diam ac, lobortis lacinia libero. Morbi at metus tellus. Sed venenatis, lacus in luctus convallis, tortor diam sagittis elit, tristique semper justo tortor ut odio. Quisque eu nulla elit. Aenean eu libero sit amet orci ullamcorper hendrerit nec a justo. Nullam urna nunc, placerat quis purus vel, fermentum molestie erat. Nam dui neque, hendrerit sit amet congue vel, convallis vel nibh. Donec sodales lectus turpis, nec luctus dolor congue id.\n" +
            "\n" +
            "Aenean porttitor nulla dictum mollis suscipit. Vestibulum eu sollicitudin dolor, ut luctus tellus. In in ultrices turpis. Vestibulum finibus dapibus nisi, commodo volutpat eros egestas eget. Duis auctor tellus vitae mauris malesuada, et dignissim elit aliquet. Ut nibh arcu, dapibus et orci nec, faucibus malesuada tellus. Duis et nulla nulla. Cras ut nisi nec felis ullamcorper interdum. Morbi est nisi, lacinia et convallis quis, bibendum non sapien. Pellentesque nec tellus lectus. Maecenas accumsan vehicula egestas. Suspendisse varius diam eu est dignissim, et pretium felis rhoncus. ";
    private static final String TEST_FILE_1_PATH = "test1/tmp.txt";
    private static final String TEST_FILE_2 = "alkjfdkladjfklajdlkfjsal;dkfjasdlk fj alk";
    private static final String TEST_FILE_2_PATH = "test2/tmp2.txt";

    private CallingContext cc;
    private TablesUserPermissions userPermissions;
    private TableManager tm;
    private String tableId;
    private String tableId2;


    @SuppressWarnings("unused")
    private List<Column> columns;

    private class MockCurrentUserPermissions implements TablesUserPermissions {

        @Override
        public String getOdkTablesUserId() {
            return "myid";
        }

        @Override
        public void checkPermission(String appId, String tableId, TablePermission permission)
                throws ODKDatastoreException, PermissionDeniedException {
            return;
        }

        @Override
        public boolean hasPermission(String appId, String tableId, TablePermission permission)
                throws ODKDatastoreException {
            return true;
        }

        @Override
        public boolean hasFilterScope(String appId, String tableId, TablePermission permission, String rowId, Scope filterScope) {
            return true;
        }

    }

    @Before
    public void setUp() throws Throwable {
        super.abstractServiceSetUp();
        this.cc = TestContextFactory.getCallingContext();
        userPermissions = new MockCurrentUserPermissions();

        this.tm = new TableManager(T.appId, userPermissions, cc);
        this.tableId = T.tableId;
        this.tableId2 = T.tableId + "2";
        this.columns = T.columns;
    }

    @After
    public void tearDown() throws Exception {
        // clear table 1
        try {
            tm.deleteTable(tableId);
        } catch (ODKEntityNotFoundException e) {
            // ignore
        }

        // clear table 2
        try {
            tm.deleteTable(tableId2);
        } catch (ODKEntityNotFoundException e) {
            // ignore
        }

        // clear app files
        cleanAppLevelFiles();
    }

    @Test
    public void testGetTablesEmpty() throws ODKDatastoreException {
        WebsafeTables result = tm.getTables(null, 2000);
        assertTrue(result.tables.isEmpty());
    }

    @Test
    public void testGetTablesWithNoFiles() throws Throwable {
        createBothTable1N2();

        verifyFileManifestsAreUsingEmptyMD5();

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }
    }

    @Test
    public void testAddSingleTableLevelFiles() throws Throwable {
        // first establish blank table
        createTable1Only();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length)
                , null, fileContent);

        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(table1ManifestETag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

    }

    @Test
    public void testAddNRemoveSingleTableLevelFiles() throws Throwable {
        // first establish blank table
        createTable1Only();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(table1ManifestETag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

        // remove file
        fm.deleteFile(ODK_CLIENT_VERSION, tableId, TEST_FILE_1_PATH);

        table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        assertEquals(EMPTY_MD5_HASH, table1ManifestETag);

        trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }
    }

    @Test
    public void testAddSameSingleTableLevelFileToBothTables() throws Throwable {
        // first establish blank tables
        createBothTable1N2();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        // put file for table 1
        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // put file for table 2
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId2, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        String table2ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId2, T.appId, cc);
        assertEquals(table1ManifestETag, table2ManifestETag);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(table1ManifestETag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

    }

    @Test
    public void testAddTwoTableLevelFiles() throws Throwable {
        // first establish blank table
        createTable1Only();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add file 1
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String firstFileManifestEtag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(firstFileManifestEtag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

        // add file 2
        fileContent = TEST_FILE_2.getBytes(StandardCharsets.UTF_8);
        fi = new FileContentInfo(TEST_FILE_2_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String bothFilesManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        assertNotEquals(firstFileManifestEtag, bothFilesManifestETag);

        trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        for (TableResource tr : trl.getTables()) {
            assertEquals(bothFilesManifestETag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

    }

    @Test
    public void testAddTwoTableLevelFilesRemoveOne() throws Throwable {
        // first establish blank table
        createTable1Only();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add file 1
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String firstFileManifestEtag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(firstFileManifestEtag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

        // add file 2
        fileContent = TEST_FILE_2.getBytes(StandardCharsets.UTF_8);
        fi = new FileContentInfo(TEST_FILE_2_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String bothFilesManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        assertNotEquals(firstFileManifestEtag, bothFilesManifestETag);

        trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        for (TableResource tr : trl.getTables()) {
            assertEquals(bothFilesManifestETag, tr.getTableLevelManifestETag());
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }

        // remove file
        fm.deleteFile(ODK_CLIENT_VERSION, tableId, TEST_FILE_2_PATH);

        String oneRemovedManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        assertNotEquals(EMPTY_MD5_HASH, oneRemovedManifestETag);
        assertEquals(firstFileManifestEtag, oneRemovedManifestETag);

        trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(oneRemovedManifestETag, tr.getTableLevelManifestETag());
        }
    }

    @Test
    public void testAddDifferentSingleTableLevelFileToBothTables() throws Throwable {
        // first establish blank tables
        createBothTable1N2();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        // put file for table 1
        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // put file for table 2
        fileContent = TEST_FILE_2.getBytes(StandardCharsets.UTF_8);
        fi = new FileContentInfo(TEST_FILE_2_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId2, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);


        // verify etags match
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        String table2ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId2, T.appId, cc);
        assertNotEquals(table1ManifestETag, table2ManifestETag);

        TableResourceList trl = getTables("", "2000");
        assertEquals(EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertTrue((tr.getTableLevelManifestETag().equals(table1ManifestETag)) ||
                    (tr.getTableLevelManifestETag().equals(table2ManifestETag)));
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }
    }

    @Test
    public void testAddSingleAppLevelFiles() throws Throwable {
        TableResourceList trl = getTables("", "2000");
        assertEquals("TableResourceList should equal empty APP files", EMPTY_MD5_HASH, trl.getAppLevelManifestETag());
        List<TableResource> tables = trl.getTables();
        assertEquals("Should have no tables", 0, tables.size());

        // first establish blank table
        createTable1Only();

        // check number of tables in table manager
        TableManager tm = new TableManager(T.appId, userPermissions, cc);
        TableManager.WebsafeTables wsTables = tm.getTables(null, 2000);
        List<TableEntry> teList = wsTables.tables;
        assertEquals("TM Should have one table", 1, teList.size());

        WebsafeTables websafeResult = tm.getTables(
                QueryResumePoint.fromWebsafeCursor(WebUtils.safeDecode("")), 2000);
        teList = websafeResult.tables;
        assertEquals("TM Should have one table", 1, teList.size());
        assertEquals("Resources Should have one table", 1, teList.size());

        websafeResult = tm.getTables(
                QueryResumePoint.fromWebsafeCursor(WebUtils.safeDecode(null)), 2000);
        teList = websafeResult.tables;
        assertEquals("TM Should have one table", 1, teList.size());

        List<TableEntry> filteredList = new ArrayList<TableEntry>();
        Query query = DbTableEntry.getRelation(cc).query("HasTableManagerTestIT.testAddSingleAppLevelFiles", cc);
        query.addSort(DbTableEntry.getRelation(cc).getDataField(CommonFieldsBase.CREATION_DATE_COLUMN_NAME),
                org.opendatakit.common.persistence.Query.Direction.ASCENDING);
        // we need the filter to activate the sort...
        query.addFilter(DbTableEntry.getRelation(cc).getDataField(CommonFieldsBase.CREATION_DATE_COLUMN_NAME),
                org.opendatakit.common.persistence.Query.FilterOperation.GREATER_THAN, BasicConsts.EPOCH);
        Query.WebsafeQueryResult result = query.execute(null, 2000);
        List<DbTableEntry.DbTableEntryEntity> results = new ArrayList<DbTableEntry.DbTableEntryEntity>();
        for (Entity e : result.entities) {
            results.add(new DbTableEntry.DbTableEntryEntity(e));
        }
        EntityConverter converter = new EntityConverter();
        List<TableEntry> tablesEntriesQuery = converter.toTableEntries(results);
        for (TableEntry e : tablesEntriesQuery) {
            if (userPermissions.hasPermission(T.appId, e.getTableId(), TablePermission.READ_TABLE_ENTRY)) {
                filteredList.add(e);
            }
        }
        assertEquals("TableEntries before permission check", 1, tablesEntriesQuery.size());
        assertEquals("PermissionFiltered Should have one table after query", 1, filteredList.size());

        trl = getTables("", "2000");
        assertEquals("TableResourceList should equal empty APP files", EMPTY_MD5_HASH, trl.getAppLevelManifestETag());
        tables = trl.getTables();
        assertEquals("Should have one table", 1, tables.size());
        for (TableResource tr : tables) {
           String eTagValue = FileManifestUtils.getTableLevelManifestETag(tr.getTableId(), T.appId, ODK_CLIENT_VERSION, cc);

            FileManifestManager manifestManager = new FileManifestManager(T.appId, ODK_CLIENT_VERSION, cc);
            OdkTablesFileManifest manifest = manifestManager.getManifestForTable(tr.getTableId());

            String calculatedETagFromManifest = FileManifestUtils.calculateUpdatedETagFromManifest(manifest);

            Thread.sleep(100);
            assertEquals("ETag calculated from Manifest should equal from util", eTagValue, calculatedETagFromManifest);
            assertEquals("eTagValue from util should equals getTables Etag", eTagValue, tr.getTableLevelManifestETag());
            assertEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }


        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();
        // uses  String appManifestETag = FileManifestUtils.getAppLevelManifestETag(T.appId, cc);

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);

        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, DbTableFileInfo.NO_TABLE_ID, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        String appManifestETag = FileManifestUtils.getAppLevelManifestETag(T.appId, cc);
        assertNotEquals("appManifest should not equals to empty", EMPTY_MD5_HASH, appManifestETag);

        trl = getTables(null, "2000");
        assertNotEquals("TableResourceList should not equals to empty", EMPTY_MD5_HASH, trl.getAppLevelManifestETag());

        // verify etags match
        assertEquals(appManifestETag, trl.getAppLevelManifestETag());

        tables = trl.getTables();
        for (TableResource tr : tables) {
            assertEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }
    }

    @Test
    public void testAddDifferentSingleTableLevelFileToBothTablesNAddAppLevel() throws Throwable {

        // first establish blank tables
        createBothTable1N2();

        // verify no files yet
        verifyFileManifestsAreUsingEmptyMD5();

        // add a file
        byte[] fileContent = TEST_FILE_1.getBytes(StandardCharsets.UTF_8);

        FileManager fm = new FileManager(T.appId, cc);
        FileContentInfo fi = new FileContentInfo(TEST_FILE_1_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length)
                , null, fileContent);

        // put the app level fiel
        ConfigFileChangeDetail outcome = fm.putFile(ODK_CLIENT_VERSION, DbTableFileInfo.NO_TABLE_ID, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // put file for table 1
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // put file for table 2
        fileContent = TEST_FILE_2.getBytes(StandardCharsets.UTF_8);
        fi = new FileContentInfo(TEST_FILE_2_PATH, TEXT_PLAIN, Long.valueOf(fileContent.length), null, fileContent);
        outcome = fm.putFile(ODK_CLIENT_VERSION, tableId2, fi, userPermissions);
        assertEquals(ConfigFileChangeDetail.FILE_NEWLY_CREATED, outcome);

        // verify etags match
        String appManifestETag = FileManifestUtils.getAppLevelManifestETag(T.appId, cc);
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        String table2ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId2, T.appId, cc);
        assertNotEquals(table1ManifestETag, table2ManifestETag);
        assertEquals(appManifestETag, table1ManifestETag);

        TableResourceList trl = getTables("", "2000");
        assertEquals(appManifestETag, trl.getAppLevelManifestETag());

        List<TableResource> tables = trl.getTables();
        for (TableResource tr : tables) {
            assertTrue((tr.getTableLevelManifestETag().equals(table1ManifestETag)) ||
                    (tr.getTableLevelManifestETag().equals(table2ManifestETag)));
            assertNotEquals(EMPTY_MD5_HASH, tr.getTableLevelManifestETag());
        }
    }

    ////////////////////////////////////////////////////////////
    /////////////// HELPER FUNCTIONS ///////////////////////////
    ////////////////////////////////////////////////////////////
    private void createTable1Only() throws ODKDatastoreException, TableAlreadyExistsException, PermissionDeniedException, ODKTaskLockException {
        List<TableEntry> expected = new ArrayList<TableEntry>();
        TableEntry entry = tm.createTable(tableId, T.columns);
        TableEntry one = tm.getTable(tableId);
        expected.add(one);

        WebsafeTables result = tm.getTables(null, 2000);
        List<TableEntry> actual = result.tables;
        assertEquals(1, actual.size());

        Util.assertCollectionSameElements(expected, actual);
    }

    private void createBothTable1N2() throws ODKDatastoreException, TableAlreadyExistsException, PermissionDeniedException, ODKTaskLockException {
        List<TableEntry> expected = new ArrayList<TableEntry>();

        TableEntry entry = tm.createTable(tableId, T.columns);
        TableEntry one = tm.getTable(tableId);

        TableEntry entry2 = tm.createTable(tableId2, T.columns);
        tm.createTable(tableId2, T.columns);
        TableEntry two = tm.getTable(tableId2);

        expected.add(one);
        expected.add(two);

        WebsafeTables result = tm.getTables(null, 2000);
        List<TableEntry> actual = result.tables;
        assertEquals(2, actual.size());

        Util.assertCollectionSameElements(expected, actual);
    }

    private void verifyFileManifestsAreUsingEmptyMD5() throws ODKDatastoreException, ODKTaskLockException {
        String appManifestETag = FileManifestUtils.getAppLevelManifestETag(T.appId, cc);
        assertEquals(EMPTY_MD5_HASH, appManifestETag);
        String table1ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId, T.appId, cc);
        assertEquals(EMPTY_MD5_HASH, table1ManifestETag);
        String table2ManifestETag = FileManifestUtils.getTableLevelManifestETag(tableId2, T.appId, cc);
        assertEquals(EMPTY_MD5_HASH, table2ManifestETag);
    }

    private void cleanAppLevelFiles() throws ODKDatastoreException, ODKTaskLockException {
        FileManager fm = new FileManager(T.appId, cc);
        FileManifestManager manifestManager = new FileManifestManager(T.appId, ODK_CLIENT_VERSION, cc);
        OdkTablesFileManifest manifest = manifestManager.getManifestForAppLevelFiles();
        for (OdkTablesFileManifestEntry entry : manifest.getFiles()) {
            fm.deleteFile(ODK_CLIENT_VERSION, DbTableFileInfo.NO_TABLE_ID, entry.filename);
        }
    }

}
