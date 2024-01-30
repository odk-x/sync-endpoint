package org.opendatakit.aggregate.odktables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendatakit.aggregate.odktables.rest.entity.UserInfoList;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.net.URI;

import static org.junit.Assert.assertEquals;

@RunWith(org.junit.runners.JUnit4.class)
public class UserTestIT extends AbstractServiceTest {

    @Before
    public void setUp() throws Throwable {
        super.abstractServiceSetUp();
    }

    @Test
    public void testAdminCanGetListOfUsers() throws Throwable {
        /* this test assumes the integrated testing system has
        prepopulated LDAP with 4 users + anonymous resulting in 5 users*/

        URI uri = resolveUri("usersInfo");

        assertEquals("URI: " + uri.toURL(), "http://localhost:8888/odktables/default/usersInfo", uri.toURL().toString());

        ResponseEntity<UserInfoList> resp;
        try {
            resp = rt.exchange(uri,
                    HttpMethod.GET,
                    null,
                    UserInfoList.class);
        } catch ( Throwable t ) {
            t.printStackTrace();
            throw t;
        }
        UserInfoList users = resp.getBody();
        assertEquals(5, users.size());
    }

}
