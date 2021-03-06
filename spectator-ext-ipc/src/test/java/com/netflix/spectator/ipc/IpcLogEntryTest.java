/*
 * Copyright 2014-2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spectator.ipc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.spectator.api.BasicTag;
import com.netflix.spectator.api.DefaultRegistry;
import com.netflix.spectator.api.DistributionSummary;
import com.netflix.spectator.api.ManualClock;
import com.netflix.spectator.api.Registry;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RunWith(JUnit4.class)
public class IpcLogEntryTest {

  private final ManualClock clock = new ManualClock();
  private final ObjectMapper mapper = new ObjectMapper();
  private final IpcLogEntry entry = new IpcLogEntry(clock);

  @Before
  public void before() {
    clock.setWallTime(0L);
    clock.setMonotonicTime(0L);
    entry.reset();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> toMap(IpcLogEntry entry) {
    try {
      return (Map<String, Object>) mapper.readValue(entry.toString(), Map.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void startTime() {
    long expected = 1234567890L;
    clock.setWallTime(expected);
    long actual = (int) entry
        .markStart()
        .convert(this::toMap)
        .get("start");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void startTimeOnlyComputedLatency() {
    long expected = 157;
    long t = 1234567890L;
    clock.setMonotonicTime(TimeUnit.MILLISECONDS.toNanos(t));
    entry.markStart();
    clock.setMonotonicTime(TimeUnit.MILLISECONDS.toNanos(t + expected));
    double actual = (double) entry
        .convert(this::toMap)
        .get("latency");
    Assert.assertEquals(expected / 1000.0, actual, 1e-12);
  }

  @Test
  public void latency() {
    long expected = 157L;
    double actual = (double) entry
        .withLatency(expected, TimeUnit.MILLISECONDS)
        .convert(this::toMap)
        .get("latency");
    Assert.assertEquals(expected / 1000.0, actual, 1e-12);
  }

  @Test
  public void latencyAndStart() {
    long expected = 157L;
    long t = 1234567890L;
    clock.setWallTime(t);
    entry.markStart();
    clock.setWallTime(t + expected + 42);
    double actual = (double) entry
        .withLatency(expected, TimeUnit.MILLISECONDS)
        .convert(this::toMap)
        .get("latency");
    Assert.assertEquals(expected / 1000.0, actual, 1e-12);
  }

  @Test
  public void owner() {
    String expected = "runtime";
    String actual = (String) entry
        .withOwner(expected)
        .convert(this::toMap)
        .get("owner");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void standardProtocol() {
    String expected = IpcProtocol.http_2.value();
    String actual = (String) entry
        .withProtocol(IpcProtocol.http_2)
        .convert(this::toMap)
        .get("protocol");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void customProtocol() {
    String expected = "nflx-proto";
    String actual = (String) entry
        .withProtocol(expected)
        .convert(this::toMap)
        .get("protocol");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void errorGroup() {
    String expected = IpcErrorGroup.cancelled.value();
    String actual = (String) entry
        .withErrorGroup(IpcErrorGroup.cancelled)
        .convert(this::toMap)
        .get("errorGroup");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void errorReason() {
    String expected = "connection_failed";
    String actual = (String) entry
        .withErrorReason(expected)
        .convert(this::toMap)
        .get("errorReason");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void exceptionClass() {
    IOException io = new IOException("host not found exception");
    String expected = "java.io.IOException";
    String actual = (String) entry
        .withException(io)
        .convert(this::toMap)
        .get("exceptionClass");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void exceptionMessage() {
    String expected = "host not found exception";
    IOException io = new IOException(expected);
    String actual = (String) entry
        .withException(io)
        .convert(this::toMap)
        .get("exceptionMessage");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void attempt() {
    String expected = IpcAttempt.third_up.value();
    String actual = (String) entry
        .withAttempt(7)
        .convert(this::toMap)
        .get("attempt");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void attemptFinal() {
    String expected = IpcAttemptFinal.is_true.value();
    String actual = (String) entry
        .withAttemptFinal(true)
        .convert(this::toMap)
        .get("attemptFinal");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void vip() {
    String expected = "www:7001";
    String actual = (String) entry
        .withVip(expected)
        .convert(this::toMap)
        .get("vip");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void endpoint() {
    String expected = "/api/v1/test";
    String actual = (String) entry
        .withEndpoint(expected)
        .convert(this::toMap)
        .get("endpoint");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void clientNode() {
    String expected = "i-12345";
    String actual = (String) entry
        .withClientNode(expected)
        .convert(this::toMap)
        .get("clientNode");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void serverNode() {
    String expected = "i-12345";
    String actual = (String) entry
        .withServerNode(expected)
        .convert(this::toMap)
        .get("serverNode");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void addRequestZoneHeader() {
    Map<String, Object> map = entry
        .addRequestHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-east-1", map.get("clientRegion"));
    Assert.assertEquals("us-east-1e", map.get("clientZone"));
  }

  @Test
  public void addRequestZoneHeaderExplicitRegion() {
    Map<String, Object> map = entry
        .withClientRegion("us-west-1")
        .addRequestHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-west-1", map.get("clientRegion"));
    Assert.assertEquals("us-east-1e", map.get("clientZone"));
  }

  @Test
  public void addRequestZoneHeaderExplicitZone() {
    Map<String, Object> map = entry
        .withClientZone("us-west-1b")
        .addRequestHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-west-1", map.get("clientRegion"));
    Assert.assertEquals("us-west-1b", map.get("clientZone"));
  }

  @Test
  public void addResponseZoneHeader() {
    Map<String, Object> map = entry
        .addResponseHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-east-1", map.get("serverRegion"));
    Assert.assertEquals("us-east-1e", map.get("serverZone"));
  }

  @Test
  public void addResponseZoneHeaderExplicitRegion() {
    Map<String, Object> map = entry
        .withServerRegion("us-west-1")
        .addResponseHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-west-1", map.get("serverRegion"));
    Assert.assertEquals("us-east-1e", map.get("serverZone"));
  }

  @Test
  public void addResponseZoneHeaderExplicitZone() {
    Map<String, Object> map = entry
        .withServerZone("us-west-1b")
        .addResponseHeader(NetflixHeader.Zone.headerName(), "us-east-1e")
        .convert(this::toMap);
    Assert.assertEquals("us-west-1", map.get("serverRegion"));
    Assert.assertEquals("us-west-1b", map.get("serverZone"));
  }

  @Test
  public void addRequestAsgHeader() {
    Map<String, Object> map = entry
        .addRequestHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("clientApp"));
    Assert.assertEquals("www-test", map.get("clientCluster"));
    Assert.assertEquals("www-test-v011", map.get("clientAsg"));
  }

  @Test
  public void addRequestAsgHeaderCaseSensitivty() {
    Map<String, Object> map = entry
        .addRequestHeader("netflix-asg", "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("clientApp"));
    Assert.assertEquals("www-test", map.get("clientCluster"));
    Assert.assertEquals("www-test-v011", map.get("clientAsg"));
  }

  @Test
  public void addRequestAsgHeaderExplicitApp() {
    Map<String, Object> map = entry
        .withClientApp("foo")
        .addRequestHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("foo", map.get("clientApp"));
    Assert.assertEquals("www-test", map.get("clientCluster"));
    Assert.assertEquals("www-test-v011", map.get("clientAsg"));
  }

  @Test
  public void addRequestAsgHeaderExplicitCluster() {
    Map<String, Object> map = entry
        .withClientCluster("foo")
        .addRequestHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("clientApp"));
    Assert.assertEquals("foo", map.get("clientCluster"));
    Assert.assertEquals("www-test-v011", map.get("clientAsg"));
  }

  @Test
  public void addRequestAsgHeaderExplicitAsg() {
    Map<String, Object> map = entry
        .withClientAsg("foo")
        .addRequestHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("foo", map.get("clientApp"));
    Assert.assertEquals("foo", map.get("clientCluster"));
    Assert.assertEquals("foo", map.get("clientAsg"));
  }

  @Test
  public void addResponseAsgHeader() {
    Map<String, Object> map = entry
        .addResponseHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("serverApp"));
    Assert.assertEquals("www-test", map.get("serverCluster"));
    Assert.assertEquals("www-test-v011", map.get("serverAsg"));
  }

  @Test
  public void addResponseAsgHeaderCaseSensitivty() {
    Map<String, Object> map = entry
        .addResponseHeader("netflix-asg", "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("serverApp"));
    Assert.assertEquals("www-test", map.get("serverCluster"));
    Assert.assertEquals("www-test-v011", map.get("serverAsg"));
  }

  @Test
  public void addResponseAsgHeaderExplicitApp() {
    Map<String, Object> map = entry
        .withServerApp("foo")
        .addResponseHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("foo", map.get("serverApp"));
    Assert.assertEquals("www-test", map.get("serverCluster"));
    Assert.assertEquals("www-test-v011", map.get("serverAsg"));
  }

  @Test
  public void addResponseAsgHeaderExplicitCluster() {
    Map<String, Object> map = entry
        .withServerCluster("foo")
        .addResponseHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("www", map.get("serverApp"));
    Assert.assertEquals("foo", map.get("serverCluster"));
    Assert.assertEquals("www-test-v011", map.get("serverAsg"));
  }

  @Test
  public void addResponseAsgHeaderExplicitAsg() {
    Map<String, Object> map = entry
        .withServerAsg("foo")
        .addResponseHeader(NetflixHeader.ASG.headerName(), "www-test-v011")
        .convert(this::toMap);
    Assert.assertEquals("foo", map.get("serverApp"));
    Assert.assertEquals("foo", map.get("serverCluster"));
    Assert.assertEquals("foo", map.get("serverAsg"));
  }

  @Test
  public void addRequestNodeHeader() {
    Map<String, Object> map = entry
        .addRequestHeader(NetflixHeader.Node.headerName(), "i-12345")
        .convert(this::toMap);
    Assert.assertEquals("i-12345", map.get("clientNode"));
  }

  @Test
  public void addResponseNodeHeader() {
    Map<String, Object> map = entry
        .addResponseHeader(NetflixHeader.Node.headerName(), "i-12345")
        .convert(this::toMap);
    Assert.assertEquals("i-12345", map.get("serverNode"));
  }

  @Test
  public void addRequestVipHeader() {
    Map<String, Object> map = entry
        .addRequestHeader(NetflixHeader.Vip.headerName(), "www:7001")
        .convert(this::toMap);
    Assert.assertEquals("www:7001", map.get("vip"));
  }

  @Test
  public void addResponseEndpointHeader() {
    Map<String, Object> map = entry
        .addResponseHeader(NetflixHeader.Endpoint.headerName(), "/api/v1/test")
        .convert(this::toMap);
    Assert.assertEquals("/api/v1/test", map.get("endpoint"));
  }

  @Test
  public void httpStatusOk() {
    String actual = (String) entry
        .withHttpStatus(200)
        .convert(this::toMap)
        .get("result");
    Assert.assertEquals(IpcResult.success.value(), actual);
  }

  @Test
  public void httpStatus400() {
    String actual = (String) entry
        .withHttpStatus(400)
        .convert(this::toMap)
        .get("result");
    Assert.assertEquals(IpcResult.failure.value(), actual);
  }

  @Test
  public void httpStatusWithExplicitResult() {
    String actual = (String) entry
        .withResult(IpcResult.failure)
        .withHttpStatus(200)
        .convert(this::toMap)
        .get("result");
    Assert.assertEquals(IpcResult.failure.value(), actual);
  }

  @Test
  public void httpStatus429() {
    String actual = (String) entry
        .withHttpStatus(429)
        .convert(this::toMap)
        .get("errorGroup");
    Assert.assertEquals(IpcErrorGroup.client_throttled.value(), actual);
  }

  @Test
  public void httpStatus503() {
    String actual = (String) entry
        .withHttpStatus(503)
        .convert(this::toMap)
        .get("errorGroup");
    Assert.assertEquals(IpcErrorGroup.server_throttled.value(), actual);
  }

  @Test
  public void httpMethod() {
    String expected = "GET";
    String actual = (String) entry
        .withHttpMethod(expected)
        .convert(this::toMap)
        .get("httpMethod");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void uri() {
    String expected = "http://foo.com/api/v1/test";
    String actual = (String) entry
        .withUri(URI.create(expected))
        .convert(this::toMap)
        .get("uri");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void uriPath() {
    String expected = "http://foo.com/api/v1/test";
    String actual = (String) entry
        .withUri(URI.create(expected))
        .convert(this::toMap)
        .get("path");
    Assert.assertEquals("/api/v1/test", actual);
  }

  @Test
  public void remoteAddress() {
    String expected = "123.45.67.89";
    String actual = (String) entry
        .withRemoteAddress(expected)
        .convert(this::toMap)
        .get("remoteAddress");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void remotePort() {
    int expected = 42;
    int actual = (int) entry
        .withRemotePort(expected)
        .convert(this::toMap)
        .get("remotePort");
    Assert.assertEquals(expected, actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void addTags() {
    Map<String, String> actual = (Map<String, String>) entry
        .addTag(new BasicTag("k1", "v1"))
        .addTag("k2", "v2")
        .convert(this::toMap)
        .get("additionalTags");
    Assert.assertEquals(2, actual.size());
    Assert.assertEquals("v1", actual.get("k1"));
    Assert.assertEquals("v2", actual.get("k2"));
  }

  @Test
  public void regionFromZoneAWS() {
    String expected = "us-east-1";
    String actual = (String) entry
        .withClientZone(expected + "e")
        .convert(this::toMap)
        .get("clientRegion");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void regionFromZoneGCE() {
    String expected = "us-east1";
    String actual = (String) entry
        .withClientZone(expected + "-e")
        .convert(this::toMap)
        .get("clientRegion");
    Assert.assertEquals(expected, actual);
  }

  @Test
  public void regionFromZoneUnknownShort() {
    String expected = "foo";
    String actual = (String) entry
        .withClientZone(expected)
        .convert(this::toMap)
        .get("clientRegion");
    Assert.assertNull(actual);
  }

  @Test
  public void regionFromZoneUnknownLong() {
    String expected = "foobarbaz";
    String actual = (String) entry
        .withClientZone(expected)
        .convert(this::toMap)
        .get("clientRegion");
    Assert.assertNull(actual);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void customHeaders() {
    List<Map<String, Object>> actual = (List<Map<String, Object>>) entry
        .addRequestHeader("foo", "bar")
        .addRequestHeader("foo", "bar2")
        .addRequestHeader("abc", "def")
        .convert(this::toMap)
        .get("requestHeaders");
    Assert.assertEquals(3, actual.size());
  }

  @Test
  public void stringEscape() {
    for (char c = 0; c < 65535; ++c) {
      String actual = (String) entry
          .withErrorReason("" + c)
          .convert(this::toMap)
          .get("errorReason");
      if (actual.length() == 0) {
        Assert.assertTrue(Character.isISOControl(c));
        Assert.assertEquals("", actual);
      } else {
        Assert.assertEquals("" + c, actual);
      }
      entry.reset();
    }
  }

  @Test
  public void inflightRequests() {
    Registry registry = new DefaultRegistry();
    DistributionSummary summary = registry.distributionSummary("ipc.client.inflight");
    IpcLogger logger = new IpcLogger(registry, clock, LoggerFactory.getLogger(getClass()));
    IpcLogEntry logEntry = logger.createClientEntry();

    Assert.assertEquals(0L, summary.totalAmount());
    logEntry.markStart();
    Assert.assertEquals(1L, summary.totalAmount());
    logEntry.markEnd();
    Assert.assertEquals(1L, summary.totalAmount());
  }

  @Test
  public void inflightRequestsMany() {
    Registry registry = new DefaultRegistry();
    DistributionSummary summary = registry.distributionSummary("ipc.client.inflight");
    IpcLogger logger = new IpcLogger(registry, clock, LoggerFactory.getLogger(getClass()));

    for (int i = 0; i < 10; ++i) {
      logger.createClientEntry().markStart().markEnd();
    }
    Assert.assertEquals((10 * 11) / 2, summary.totalAmount());
    Assert.assertEquals(10L, summary.count());
  }
}
