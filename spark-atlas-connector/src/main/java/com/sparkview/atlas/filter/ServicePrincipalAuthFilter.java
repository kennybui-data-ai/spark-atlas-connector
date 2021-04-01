/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sparkview.atlas.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.Long;

import org.apache.commons.io.Charsets;

import javax.ws.rs.core.HttpHeaders;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientRequest;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.filter.ClientFilter;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

// import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// import java.text.SimpleDateFormat;
// import java.util.Date;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

// import static org.junit.Assert.assertThat;

/**
 * Client filter adding HTTP Basic Authentication header to the HTTP request, if
 * no such header is already present
 *
 * @author Jakub.Podlesak@Sun.COM, Craig.McClanahan@Sun.COM
 * 
 *         modified HTTP basic auth filter to Service Principal Auth for Azure
 *         Purview
 */
public final class ServicePrincipalAuthFilter extends ClientFilter {

    private static final Logger LOG = LoggerFactory.getLogger(ServicePrincipalAuthFilter.class);

    private String authentication;
    static private final Charset CHARACTER_SET = Charset.forName("iso-8859-1");

    private String accessToken;
    private final String oauthURL;
    // private final String oauthData;
    private final List<NameValuePair> oauthData = new ArrayList<NameValuePair>();

    // private SimpleDateFormat formatter_now= new SimpleDateFormat("yyyy-MM-dd 'at'
    // HH:mm:ss z");
    // private Instant instant = Instant.now();
    // private OffsetDateTime expiration = instant.atOffset(ZoneOffset.UTC);
    private static DateTimeFormatter utcFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME.withZone(ZoneId.from(ZoneOffset.UTC));
    private Instant expiration = Instant.now();

    /**
     * Creates a new Service Principal Authentication filter using provided
     * tenantID, clientID, and clientSecret. This constructor allows you to avoid
     * storing plain password value in a String variable.
     *
     * @param tenantID
     * @param clientID
     * @param clientSecret
     */
    public ServicePrincipalAuthFilter(final String tenantID, final String clientID, final String clientSecret) {
        try {
            // final byte[] prefix = (username + ":").getBytes(CHARACTER_SET);
            // final byte[] usernamePassword = new byte[prefix.length + password.length];

            // System.arraycopy(prefix, 0, usernamePassword, 0, prefix.length);
            // System.arraycopy(password, 0, usernamePassword, prefix.length,
            // password.length);

            oauthURL = "https://login.microsoftonline.com/" + tenantID + "/oauth2/token";

            // JSONObject json = new JSONObject();
            // json.put("resource", "73c2949e-da2d-457a-9607-fcc665198967");
            // json.put("client_id", clientID);
            // json.put("grant_type", "client_credentials");
            // json.put("client_secret", clientSecret);

            // oauthData = json.toString();

            oauthData.add(new BasicNameValuePair("resource", "73c2949e-da2d-457a-9607-fcc665198967"));
            oauthData.add(new BasicNameValuePair("client_id", clientID));
            oauthData.add(new BasicNameValuePair("grant_type", "client_credentials"));
            oauthData.add(new BasicNameValuePair("client_secret", clientSecret));

            // LOG.info(oauthData);

            setAuthentication();
            // authentication = "Basic " + new String(Base64.encode(usernamePassword),
            // "ASCII");

            LOG.info("ServicePrincipalAuthFilter constructer COMPLETE");
        } catch (UnsupportedEncodingException ex) {
            // This should never occur
            throw new RuntimeException(ex);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * UNUSED Creates a new HTTP Basic Authentication filter using provided username
     * and password credentials.
     *
     * @param username
     * @param password
     */
    // public ServicePrincipalAuthFilter(final String username, final String
    // password) {
    // this(username, password.getBytes(CHARACTER_SET));
    // }

    private void setAuthentication() throws ClientProtocolException, IOException {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(oauthURL);

        // String json = "{/"id":1,"name":"John"}";
        UrlEncodedFormEntity body = new UrlEncodedFormEntity(oauthData, StandardCharsets.UTF_8);
        httpPost.setEntity(body);
        // httpPost.setHeader("Accept", "application/json");
        // httpPost.setHeader("content-type", "application/json");

        LOG.info("executing httpPost now");
        CloseableHttpResponse response = client.execute(httpPost);
        // assertThat(response.getStatusLine().getStatusCode(), equalTo(200));
        LOG.info("successfully executed httpPost");

        HttpEntity entity = response.getEntity();
        Header encodingHeader = entity.getContentEncoding();
        // you need to know the encoding to parse correctly
        Charset encoding = encodingHeader == null ? StandardCharsets.UTF_8
                : Charsets.toCharset(encodingHeader.getValue());

        // use org.apache.http.util.EntityUtils to read json as string
        String jsonStr = EntityUtils.toString(response.getEntity(), encoding);
        JSONObject jsonObj = new JSONObject(jsonStr);
        LOG.info("parsed response to jsonObj");

        accessToken = jsonObj.getString("access_token");
        authentication = "Bearer " + accessToken;
        expiration = Instant.ofEpochSecond(Long.parseLong(jsonObj.getString("expires_on")));
        String expireStr = utcFormatter.format(expiration);
        LOG.info("access token expiration: " + expireStr);
        LOG.info("set accessToken, authentication, and expiration");

        response.close();
        client.close();
        LOG.info("closed client and response");

        LOG.info("setAuthentication SUCCESSFUL");
    }

    @Override
    public ClientResponse handle(final ClientRequest cr) throws ClientHandlerException {

        if (!cr.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            LOG.info("handle: ADDING authentication");
            if (expiration.compareTo(Instant.now()) <= 0) {
                LOG.info("handle: token expired. getting new token.");
                try {
                    setAuthentication();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                LOG.info("handle: token not expired.");
            }

            cr.getHeaders().add(HttpHeaders.AUTHORIZATION, authentication);
        } else {
            LOG.info("handle: REPLACING authentication");
            try {
                setAuthentication();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Object authObj = authentication;
            List<Object> authList = Arrays.asList(authObj);
            cr.getHeaders().put(HttpHeaders.AUTHORIZATION, authList);
        }
        return getNext().handle(cr);
    }
}