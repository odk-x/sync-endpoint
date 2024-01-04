package org.opendatakit.aggregate.odktables;


import org.junit.After;
import org.opendatakit.aggregate.odktables.rest.entity.TableDefinition;
import org.opendatakit.aggregate.odktables.rest.entity.TableResource;
import org.opendatakit.aggregate.odktables.rest.entity.TableResourceList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractServiceTest {

  private static final Logger log = LoggerFactory.getLogger(AbstractServiceTest.class);

  @Component
  public class LoggingInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

      logRequest(request, body);
      ClientHttpResponse response = execution.execute(request, body);
      logResponse(response);

      return response;
    }

    private void logRequest(HttpRequest request, byte[] body) throws IOException {
        log.warn("===log request start===");
        log.warn("URI: " + request.getURI());
        log.warn("Method: " + request.getMethod());
        log.warn("Headers: " +  request.getHeaders());
        log.warn("Request body: " +  new String(body, "UTF-8"));
        log.warn("===log request end===");

    }

    private void logResponse(ClientHttpResponse response) throws IOException {
        log.warn("===log response start===");
        log.warn("Status code:  " +  response.getStatusCode());
        log.warn("Status text: " +  response.getStatusText());
        log.warn("Headers: " +  response.getHeaders());
        log.warn("Response body:  " +  StreamUtils.copyToString(response.getBody(), Charset.defaultCharset()));
        log.warn("===log response end===");

    }
  }

  public static final String TABLE_API = "tables/";

  private String baseUrl;
  private String appId = "default";
  private URI baseUri;
  protected RestTemplate rt;
  protected HttpHeaders reqHeaders;
  private String tableDefinitionUri = null;

  
  // call this from any Before action in derived class
  public void abstractServiceSetUp() throws Exception, Throwable {
    String hostname = System.getProperty("test.server.hostname");
    baseUrl = System.getProperty("test.server.baseUrl", "/");
    String port = System.getProperty("test.server.port");

    String username = System.getProperty("test.adminUsername");
    String password = System.getProperty("test.adminPassword");

    this.baseUri = URI.create("http://" + hostname + ":" + port + baseUrl + "odktables/" + appId + "/");

    log.warn("baseUri: " + baseUri);

    // RestTemplate
    try {
      this.rt = new RestTemplate();

      SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
      rt.setRequestFactory(new BufferingClientHttpRequestFactory(factory));
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }

    this.rt.getInterceptors().add(new LoggingInterceptor());
    this.rt.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));

    
    this.rt.setErrorHandler(new ErrorHandler());
    List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();

    converters.add(new MappingJackson2HttpMessageConverter());
    // converters.add(new AllEncompassingFormHttpMessageConverter());
    this.rt.setMessageConverters(converters);

    // HttpHeaders
    List<MediaType> acceptableMediaTypes = new ArrayList<MediaType>();
    acceptableMediaTypes.add(MediaType.APPLICATION_JSON);

    this.reqHeaders = new HttpHeaders();
    reqHeaders.setAccept(acceptableMediaTypes);
    reqHeaders.setContentType(MediaType.APPLICATION_JSON);
  }
  
  protected URI resolveUri(String str) {
    return baseUri.resolve(str);
  }

  @After
  public void abstractServiceTearDown() throws Exception {
    try {
      if ( tableDefinitionUri != null ) {
        URI uri = resolveUri(tableDefinitionUri);
        this.rt.delete(uri);
      }
    } catch (Exception e) {
      // ignore
      System.out.println(e);
    }
  }

  protected TableResource createTable() throws Throwable  {
    URI uri = resolveUri(TABLE_API + Test1.tableId);

    TableDefinition definition = new TableDefinition(Test1.tableId, null, Test1.columns);
    HttpEntity<TableDefinition> entity = entity(definition);

    ResponseEntity<TableResource> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.PUT, entity, TableResource.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResource rsc = resp.getBody();
    tableDefinitionUri = rsc.getDefinitionUri();
    return rsc;
  }

  protected TableResource createAltTable() throws Throwable  {
    URI uri = resolveUri(TABLE_API + Test1.tableId);

    TableDefinition definition = new TableDefinition(Test1.tableId, null, Test1.altcolumns);
    HttpEntity<TableDefinition> entity = entity(definition);

    ResponseEntity<TableResource> resp;
    try {
      resp = rt.exchange(uri, HttpMethod.PUT, entity, TableResource.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResource rsc = resp.getBody();
    tableDefinitionUri = rsc.getDefinitionUri();
    return rsc;
  }

  protected TableResourceList getTables(String cursor, String fetchLimit) throws Throwable {
    URI uri = resolveUri(TABLE_API);

    UriComponentsBuilder builder = UriComponentsBuilder.fromUri(uri);
    builder.queryParam("cursor", cursor);
    builder.queryParam("fetchLimit", fetchLimit);

    uri = builder.build().toUri();

    ResponseEntity<TableResourceList> resp;
    try {
      resp = rt.exchange(uri,
              HttpMethod.GET,
              null,
              TableResourceList.class);
    } catch ( Throwable t ) {
      t.printStackTrace();
      throw t;
    }
    TableResourceList trl = resp.getBody();
    return trl;
  }

  protected <V> HttpEntity<V> entity(V entity) {
    return new HttpEntity<V>(entity, reqHeaders);
  }

  private class ErrorHandler implements ResponseErrorHandler {
    @Override
    public void handleError(ClientHttpResponse resp) throws IOException {
      HttpStatus status = resp.getStatusCode();
      String body = readInput(resp.getBody());
      if (status.value() / 100 == 4)
        throw new HttpClientErrorException(status, body);
      else if (status.value() / 100 == 5)
        throw new HttpServerErrorException(status, body);
    }

    @Override
    public boolean hasError(ClientHttpResponse resp) throws IOException {
      return resp.getStatusCode().value() / 100 != 2;
    }

    private String readInput(InputStream is) throws IOException {
      BufferedReader br = new BufferedReader(new InputStreamReader(is));
      StringBuilder sb = new StringBuilder();
      String line;
      while ((line = br.readLine()) != null) {
        sb.append(line);
      }
      return sb.toString();
    }
  }
}
