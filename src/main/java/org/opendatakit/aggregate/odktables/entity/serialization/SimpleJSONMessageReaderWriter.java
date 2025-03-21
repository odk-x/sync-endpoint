/*
 * Copyright (C) 2012-2013 University of Washington
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

package org.opendatakit.aggregate.odktables.entity.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.opendatakit.aggregate.odktables.rest.ApiConstants;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
@Provider
public class SimpleJSONMessageReaderWriter<T> implements MessageBodyReader<T>,
    MessageBodyWriter<T> {

  private static final ObjectMapper mapper = new ObjectMapper();
  private static final String DEFAULT_ENCODING = "utf-8";

  public static class JSONWrapper {
    byte[] buffer;
    public JSONWrapper(byte[] buffer) {
      this.buffer = buffer;
    }
  };

  @Context
  private HttpHeaders headers;

  @Context
  ServletContext context;

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation annotations[],
      MediaType mediaType) {
    return mediaType.getType().equals(MediaType.APPLICATION_JSON_TYPE.getType())
        && mediaType.getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype());
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation annotations[],
      MediaType mediaType) {
    return mediaType.getType().equals(MediaType.APPLICATION_JSON_TYPE.getType())
        && mediaType.getSubtype().equals(MediaType.APPLICATION_JSON_TYPE.getSubtype());
  }

   @Override
    public T readFrom(Class<T> aClass, Type genericType, Annotation[] annotations,
        MediaType mediaType, MultivaluedMap<String, String> map, InputStream stream)
        throws IOException, WebApplicationException {
        String encoding = getCharsetAsString(mediaType);
        String requestBody = "";

        try {
            if (!encoding.equalsIgnoreCase(DEFAULT_ENCODING)) {
                throw new IllegalArgumentException("Charset for the request is not utf-8, received: " + encoding);
            }

            // Check if request body is compressed (gzip)
            String contentEncoding = (headers != null && headers.getRequestHeaders().containsKey("Content-Encoding"))
                    ? headers.getRequestHeaders().get("Content-Encoding").get(0) : null;
            if ("gzip".equalsIgnoreCase(contentEncoding)) {
                // System.out.println("Detected Gzipped request, decompressing...");
                stream = new GZIPInputStream(stream);
            }

            // Read the InputStream into a String before parsing
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, Charset.forName(ApiConstants.UTF8_ENCODE)));
            StringBuilder rawInput = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                rawInput.append(line).append("\n");
            }
            requestBody = rawInput.toString();

            // System.out.println("Decompressed Request Body: " + requestBody);
            return mapper.readValue(requestBody, aClass);
        } catch (Exception e) {
            System.err.println("Unexpected error while reading JSON: " + e.getMessage()); 
            System.out.println("Received Encoding: " + encoding);            
            if (headers != null) {
                for (java.util.Map.Entry<String, List<String>> header : headers.getRequestHeaders().entrySet()) {
                    System.out.println("Header: " + header.getKey() + " = " + String.join(", ", header.getValue()));
                }
            }
            

            // Log raw request body in case of an error
            System.err.println("Raw Request Body (on error): " + requestBody);

            // Log stack trace for debugging
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());

            throw new IOException("Error parsing JSON request: " + e.getMessage(), e);
        }
    }

  @Override
  public void writeTo(T o, Class<?> aClass, Type type, Annotation[] annotations,
      MediaType mediaType, MultivaluedMap<String, Object> map, OutputStream rawStream)
      throws IOException, WebApplicationException {
    String encoding = getCharsetAsString(mediaType);
    try {
      if (!encoding.equalsIgnoreCase(DEFAULT_ENCODING)) {
        throw new IllegalArgumentException("charset for the response is not utf-8");
      }

      /**
       * This is an optimization because of the weird way Wink handles request/response
       * processing. I'd like to do post-processing on the constructed response, but
       * am forced to do pre-processing. We only do this for JSON response path.
       */
      byte[] bytes = null;
      {
        Object obj = context.getAttribute(NotModifiedHandler.jsonBufferKey);
        if ( obj != null && obj instanceof JSONWrapper ) {
          JSONWrapper wrapper = (JSONWrapper) obj;
          bytes = wrapper.buffer;
        }
      }
      if ( bytes == null ) {
        // write object to a byte array
        ByteArrayOutputStream bas = new ByteArrayOutputStream(8192);
        OutputStreamWriter w = new OutputStreamWriter(bas,
            Charset.forName(ApiConstants.UTF8_ENCODE));
        mapper.writeValue(w, o);
        // get the array
        bytes = bas.toByteArray();
      }
      /**
       * OK. At this point, bytes[] holds the serialized response entity.
       */
      map.putSingle(ApiConstants.OPEN_DATA_KIT_VERSION_HEADER, ApiConstants.OPEN_DATA_KIT_VERSION);
      map.putSingle("Access-Control-Allow-Origin", "*");
      map.putSingle("Access-Control-Allow-Credentials", "true");

      rawStream.write(bytes);
      rawStream.flush();
      rawStream.close();

    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public long getSize(T arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
    return -1;
  }

  protected static String getCharsetAsString(MediaType m) {
    if (m == null) {
      return DEFAULT_ENCODING;
    }
    String result = m.getParameters().get("charset");
    return (result == null) ? DEFAULT_ENCODING : result;
  }
}
