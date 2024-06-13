package org.opendatakit.aggregate.odktables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.Scope;
import org.opendatakit.aggregate.odktables.rest.entity.TableAcl;
import org.opendatakit.aggregate.odktables.rest.entity.TableAclResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableAclResourceList;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableRole;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.Assert.assertEquals;

@RunWith(org.junit.runners.JUnit4.class)
public class TableAclServiceTestIT extends AbstractServiceTest {

  protected URI resourceUri;

  @Before
  public void setUp() throws Exception, Throwable {
    super.abstractServiceSetUp();
    super.createTable();
    TableResource resource = rt.getForObject(resolveUri(TABLE_API + Test1.tableId), TableResource.class);
    resourceUri = URI.create(resource.getAclUri());
  }

  @Test
  public void testGetAclsOnlyOwner() {
    TableAclResourceList acls = rt.getForObject(resourceUri, TableAclResourceList.class);
    assertEquals(1, acls.getAcls().size());
  }

  @Test
  public void testSetUserAcl() {
    Scope.Type type = Scope.Type.USER;
    String userId = "someone@somewhere.com";
    String uri = Util.buildUri(resourceUri.toASCIIString(), type.name().toLowerCase(), userId);
    TableAcl expected = new TableAcl(TableRole.READER);

    HttpEntity<TableAcl> entity = super.entity(expected);
    ResponseEntity<TableAclResource> resp = rt.exchange(uri, HttpMethod.PUT, entity,
        TableAclResource.class);
    TableAclResource actual = resp.getBody();
    assertEquals(expected.getRole(), actual.getRole());
    assertEquals(new Scope(type, userId), actual.getScope());
  }

  @Test
  public void testSetDefaultAcl() {
    Scope.Type type = Scope.Type.DEFAULT;
    String uri = Util.buildUri(resourceUri.toASCIIString(), type.name().toLowerCase());
    TableAcl expected = new TableAcl(TableRole.READER);

    HttpEntity<TableAcl> entity = super.entity(expected);
    ResponseEntity<TableAclResource> resp = rt.exchange(uri, HttpMethod.PUT, entity,
        TableAclResource.class);
    TableAclResource actual = resp.getBody();
    assertEquals(expected.getRole(), actual.getRole());
    assertEquals(new Scope(Scope.Type.DEFAULT, null), actual.getScope());
  }

  @Test
  public void testDeleteDefaultAcl() {
    Scope.Type type = Scope.Type.DEFAULT;
    String uri = Util.buildUri(resourceUri.toASCIIString(), type.name().toLowerCase());
    rt.delete(uri);
  }

  @Test
  public void testDeleteNonExistentAcl() {
    Scope.Type type = Scope.Type.USER;
    String userId = "someone@somewhere.com";
    String uri = Util.buildUri(resourceUri.toASCIIString(), type.name().toLowerCase(), userId);
    rt.delete(uri);
  }

}
