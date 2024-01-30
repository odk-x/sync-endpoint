package org.opendatakit.aggregate.odktables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.SavepointTypeManipulator;
import org.opendatakit.aggregate.odktables.rest.entity.Row;
import org.opendatakit.aggregate.odktables.rest.entity.RowFilterScope;
import org.opendatakit.aggregate.odktables.rest.entity.RowList;
import org.opendatakit.aggregate.odktables.rest.entity.RowOutcomeList;
import org.opendatakit.aggregate.odktables.rest.entity.RowResourceList;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(org.junit.runners.JUnit4.class)
public class DataServiceTestIT extends AbstractServiceTest {

  protected URI resourceUri;
  
  @Before
  public void setUp() throws Exception, Throwable {
    super.abstractServiceSetUp();
    super.createTable();
    TableResource resource = rt.getForObject(resolveUri(TABLE_API + Test1.tableId), TableResource.class);
    resourceUri = URI.create(resource.getDataUri());
  }

  @Test
  public void testGetRowsNoRows() {
    RowResourceList rows = rt.getForObject(resourceUri, RowResourceList.class);
    assertTrue(rows.getRows().isEmpty());
  }

  @Test
  public void testInsertRow() throws Throwable {
    String rowId = Test1.Data.DYLAN.getId();
    String uri = Util.buildUri(resourceUri.toASCIIString());

    Row expected = Row.forInsert(rowId, Test1.form_id_1, Test1.locale_1, SavepointTypeManipulator.complete(),
        Test1.savepoint_timestamp_1, Test1.savepoint_creator_1, RowFilterScope.EMPTY_ROW_FILTER, Test1.Data.DYLAN.getValues());

    RowList list = new RowList();
    ArrayList<Row> rows = new ArrayList<Row>();
    rows.add(expected);
    list.setRows(rows);
    
    HttpEntity<RowList> entity = super.entity(list);
    ResponseEntity<RowOutcomeList> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.PUT, entity, RowOutcomeList.class);
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
    RowOutcomeList outcomes = resp.getBody();
    Row actual = outcomes.getRows().get(0);
    assertEquals(expected.getRowId(), actual.getRowId());
    assertNotNull(actual.getRowETag());
  }

}
