/*
 * Copyright 2012-2014 Continuuity, Inc.
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

package com.continuuity.explore.client;

import com.continuuity.common.conf.Constants;
import com.continuuity.common.http.HttpMethod;
import com.continuuity.common.http.HttpRequest;
import com.continuuity.common.http.HttpRequests;
import com.continuuity.common.http.HttpResponse;
import com.continuuity.explore.service.ColumnDesc;
import com.continuuity.explore.service.Explore;
import com.continuuity.explore.service.ExploreException;
import com.continuuity.explore.service.Handle;
import com.continuuity.explore.service.HandleNotFoundException;
import com.continuuity.explore.service.Result;
import com.continuuity.explore.service.Status;

import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A base for an Explore Client that talks to a server implementing {@link Explore} over HTTP.
 */
public abstract class AbstractExploreClient implements Explore, ExploreClient {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractExploreClient.class);
  private static final Gson GSON = new Gson();

  private static final Type MAP_TYPE_TOKEN = new TypeToken<Map<String, String>>() { }.getType();
  private static final Type COL_DESC_LIST_TYPE = new TypeToken<List<ColumnDesc>>() { }.getType();
  private static final Type ROW_LIST_TYPE = new TypeToken<List<Result>>() { }.getType();

  protected abstract InetSocketAddress getExploreServiceAddress();

  protected abstract String getAuthorizationToken();

  @Override
  public boolean isAvailable() {
    try {
      HttpResponse response = doGet(String.format("explore/status"));
      return HttpResponseStatus.OK.getCode() == response.getResponseCode();
    } catch (Exception e) {
      LOG.info("Caught exception when checking Explore availability", e);
      return false;
    }
  }

  @Override
  public Handle enableExplore(String datasetInstance) throws ExploreException {
    HttpResponse response = doPost(String.format("explore/instances/%s/enable", datasetInstance), null, null);
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return Handle.fromId(parseResponseAsMap(response, "handle"));
    }
    throw new ExploreException("Cannot enable explore on dataset " + datasetInstance + ". Reason: " +
                               getDetails(response));
  }

  @Override
  public Handle disableExplore(String datasetInstance) throws ExploreException {
    HttpResponse response = doPost(String.format("explore/instances/%s/disable", datasetInstance), null, null);
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return Handle.fromId(parseResponseAsMap(response, "handle"));
    }
    throw new ExploreException("Cannot disable explore on dataset " + datasetInstance + ". Reason: " +
                               getDetails(response));
  }

  @Override
  public Handle execute(String statement) throws ExploreException {
    HttpResponse response = doPost("data/queries", GSON.toJson(ImmutableMap.of("query", statement)), null);
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return Handle.fromId(parseResponseAsMap(response, "handle"));
    }
    throw new ExploreException("Cannot execute query. Reason: " + getDetails(response));
  }

  @Override
  public Status getStatus(Handle handle) throws ExploreException, HandleNotFoundException {
    HttpResponse response = doGet(String.format("data/queries/%s/%s", handle.getHandle(), "status"));
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return parseJson(response, Status.class);
    }
    throw new ExploreException("Cannot get status. Reason: " + getDetails(response));
  }

  @Override
  public List<ColumnDesc> getResultSchema(Handle handle) throws ExploreException, HandleNotFoundException {
    HttpResponse response = doGet(String.format("data/queries/%s/%s", handle.getHandle(), "schema"));
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return parseJson(response, COL_DESC_LIST_TYPE);
    }
    throw new ExploreException("Cannot get result schema. Reason: " + getDetails(response));
  }

  @Override
  public List<Result> nextResults(Handle handle, int size) throws ExploreException, HandleNotFoundException {
    HttpResponse response = doPost(String.format("data/queries/%s/%s", handle.getHandle(), "next"),
                                   GSON.toJson(ImmutableMap.of("size", size)), null);
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return parseJson(response, ROW_LIST_TYPE);
    }
    throw new ExploreException("Cannot get next results. Reason: " + getDetails(response));
  }

  @Override
  public void cancel(Handle handle) throws ExploreException, HandleNotFoundException {
    HttpResponse response = doPost(String.format("data/queries/%s/%s", handle.getHandle(), "cancel"), null, null);
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return;
    }
    throw new ExploreException("Cannot cancel operation. Reason: " + getDetails(response));
  }

  @Override
  public void close(Handle handle) throws ExploreException, HandleNotFoundException {
    HttpResponse response = doDelete(String.format("data/queries/%s", handle.getHandle()));
    if (HttpResponseStatus.OK.getCode() == response.getResponseCode()) {
      return;
    }
    throw new ExploreException("Cannot close operation. Reason: " + getDetails(response));
  }

  private String parseResponseAsMap(HttpResponse response, String key) throws ExploreException {
    Map<String, String> responseMap = parseJson(response, MAP_TYPE_TOKEN);
    if (responseMap.containsKey(key)) {
      return responseMap.get(key);
    }

    String message = String.format("Cannot find key %s in server response: %s", key,
                                   new String(response.getResponseBody(), Charsets.UTF_8));
    LOG.error(message);
    throw new ExploreException(message);
  }

  private <T> T parseJson(HttpResponse response, Type type) throws ExploreException {
    String responseString = new String(response.getResponseBody(), Charsets.UTF_8);
    try {
      return GSON.fromJson(responseString, type);
    } catch (JsonSyntaxException e) {
      String message = String.format("Cannot parse server response: %s", responseString);
      LOG.error(message, e);
      throw new ExploreException(message, e);
    } catch (JsonParseException e) {
      String message = String.format("Cannot parse server response as map: %s", responseString);
      LOG.error(message, e);
      throw new ExploreException(message, e);
    }
  }

  private HttpResponse doGet(String resource) throws ExploreException {
    return doRequest(resource, "GET", null, null);
  }

  private HttpResponse doPost(String resource, String body, Map<String, String> headers) throws ExploreException {
    return doRequest(resource, "POST", headers, body);
  }

  private HttpResponse doDelete(String resource) throws ExploreException {
    return doRequest(resource, "DELETE", null, null);
  }

  private HttpResponse doRequest(String resource, String requestMethod,
                                 @Nullable Map<String, String> headers,
                                 @Nullable String body) throws ExploreException {
    Map<String, String> newHeaders = headers;
    if (getAuthorizationToken() != null && !getAuthorizationToken().isEmpty()) {
      newHeaders = (headers != null) ? Maps.newHashMap(headers) : Maps.<String, String>newHashMap();
      newHeaders.put("Authorization", "Bearer " + getAuthorizationToken());
    }
    String resolvedUrl = resolve(resource);
    try {
      URL url = new URL(resolvedUrl);
      if (body != null) {
        return HttpRequests.execute(HttpRequest.builder(HttpMethod.valueOf(requestMethod), url)
                                      .addHeaders(newHeaders).withBody(body).build());
      } else {
        return HttpRequests.execute(HttpRequest.builder(HttpMethod.valueOf(requestMethod), url)
                                      .addHeaders(newHeaders).build());
      }
    } catch (IOException e) {
      throw new ExploreException(
        String.format("Error connecting to Explore Service at %s while doing %s with headers %s and body %s",
                      resolvedUrl, requestMethod,
                      newHeaders == null ? "null" : Joiner.on(",").withKeyValueSeparator("=").join(newHeaders),
                      body == null ? "null" : body), e);
    }
  }

  private String getDetails(HttpResponse response) {
    return String.format("Response code: %s, message:'%s', body: '%s'",
                         response.getResponseCode(), response.getResponseMessage(),
                         response.getResponseBody() == null ?
                           "null" : new String(response.getResponseBody(), Charsets.UTF_8));

  }

  private String resolve(String resource) {
    InetSocketAddress addr = getExploreServiceAddress();
    String url = String.format("http://%s:%s%s/%s", addr.getHostName(), addr.getPort(),
                               Constants.Gateway.GATEWAY_VERSION, resource);
    LOG.trace("Explore URL = {}", url);
    return url;
  }
}
