/*
 *
 * Copyright 2016 The Symphony Software Foundation
 *
 * Licensed to The Symphony Software Foundation (SSF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.symphonyoss.symphony.clients.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.exceptions.StreamsException;
import org.symphonyoss.exceptions.UsersClientException;
import org.symphonyoss.symphony.clients.UsersClient;
import org.symphonyoss.symphony.clients.UsersFactory;
import org.symphonyoss.symphony.clients.model.*;
import org.symphonyoss.symphony.pod.api.StreamsApi;
import org.symphonyoss.symphony.pod.invoker.ApiClient;
import org.symphonyoss.symphony.pod.invoker.ApiException;
import org.symphonyoss.symphony.pod.model.*;

import javax.ws.rs.client.Client;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * Supports all stream lookup and create functions.
 * This also includes ability to search and updates for rooms.
 *
 * @author Frank Tarsillo
 */
public class StreamsClientImpl implements org.symphonyoss.symphony.clients.StreamsClient {
    private final SymAuth symAuth;
    private final String serviceUrl;
    private final ApiClient apiClient;
    private Client httpClient = null;

    private final Logger logger = LoggerFactory.getLogger(StreamsClientImpl.class);


    public StreamsClientImpl(SymAuth symAuth, String serviceUrl) {

        this.symAuth = symAuth;
        this.serviceUrl = serviceUrl;


        //Get Service client to query for userID.
        apiClient = org.symphonyoss.symphony.pod.invoker.Configuration.getDefaultApiClient();
        apiClient.setBasePath(serviceUrl);

        apiClient.addDefaultHeader(symAuth.getSessionToken().getName(), symAuth.getSessionToken().getToken());
        apiClient.addDefaultHeader(symAuth.getKeyToken().getName(), symAuth.getKeyToken().getToken());

    }

    /**
     * If you need to override HttpClient.  Important for handling individual client certs.
     *
     * @param symAuth    Authorization model containing session and key tokens
     * @param serviceUrl Service URL
     * @param httpClient Custom HTTP Client to use
     */
    public StreamsClientImpl(SymAuth symAuth, String serviceUrl, Client httpClient) {
        this.symAuth = symAuth;
        this.serviceUrl = serviceUrl;
        this.httpClient = httpClient;
        //Get Service client to query for userID.
        apiClient = org.symphonyoss.symphony.pod.invoker.Configuration.getDefaultApiClient();
        apiClient.setHttpClient(httpClient);
        apiClient.setBasePath(serviceUrl);

        apiClient.addDefaultHeader(symAuth.getSessionToken().getName(), symAuth.getSessionToken().getToken());
        apiClient.addDefaultHeader(symAuth.getKeyToken().getName(), symAuth.getKeyToken().getToken());


    }

    /**
     * Get stream by {@link SymUser}
     *
     * @param symUser {@link SymUser}
     * @return {@link Stream}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public Stream getStream(SymUser symUser) throws StreamsException {

        if (symUser == null) {
            throw new NullPointerException("User was not provided...");
        }

        UserIdList userIdList = new UserIdList();
        userIdList.add(symUser.getId());

        Stream stream = getStream(userIdList);
        logger.debug("Stream ID for one to one chat: {}:{} ", symUser.getEmailAddress(), stream.getId());

        return stream;


    }

    /**
     * Get stream by set of {@link SymUser}.  This represents a stream related to multi-party conversation.
     *
     * @param symUsers A set {@link SymUser}
     * @return {@link Stream}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public Stream getStream(Set<SymUser> symUsers) throws StreamsException {
        if (symUsers == null) {
            throw new NullPointerException("Users were not provided...");
        }

        UserIdList userIdList = new UserIdList();
        StringBuilder usersPrint = new StringBuilder();

        for (SymUser user : symUsers) {
            userIdList.add(user.getId());
            usersPrint.append(" [").append(user.getEmailAddress()).append("] ");
        }


        Stream stream = getStream(userIdList);

        logger.debug("Stream ID for chat: {}:{} ", usersPrint.toString(), stream.getId());

        return stream;


    }


    /**
     * Get stream by list of user IDs.
     *
     * @param userIdList A arraylist of user IDs
     * @return {@link Stream}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public Stream getStream(UserIdList userIdList) throws StreamsException {
        if (userIdList == null) {
            throw new NullPointerException("UsersIds were not provided...");
        }

        StreamsApi streamsApi = new StreamsApi(apiClient);
        try {
            return streamsApi.v1ImCreatePost(userIdList, symAuth.getSessionToken().getToken());
        } catch (ApiException e) {
            throw new StreamsException("Failed to retrieve stream for given user ids...", e);
        }

    }


    /**
     * Retrieve all known streams based on filter as an Admin user
     *
     * @param skip  Skip number of stream entries
     * @param limit Limit number of results
     * @return {@link SymAdminStreamFilter}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public SymAdminStreamList getStreams(Integer skip, Integer limit, SymAdminStreamFilter symAdminStreamFilter) throws StreamsException {

        if (symAdminStreamFilter == null) {
            throw new NullPointerException("NO filter provided ...");
        }

        StreamsApi streamsApi = new StreamsApi(apiClient);
        try {

            AdminStreamFilter filter = SymAdminStreamFilter.toAdminStreamFilter(symAdminStreamFilter);

            AdminStreamList adminStreamList = streamsApi.v1AdminStreamsListPost(symAuth.getSessionToken().getToken(), skip, limit, filter);

            return SymAdminStreamList.toSymStreamList(adminStreamList);


        } catch (ApiException e) {
            throw new StreamsException("Failed to retrieve stream for given user ids...", e);
        }


    }


    /**
     * Retrieve all known streams based on filter.  User level access.
     *
     * @param skip  Skip number of stream entries
     * @param limit Limit number of results
     * @return {@link SymAdminStreamFilter}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public List<SymStreamAttributes> getStreams(Integer skip, Integer limit, SymStreamFilter symStreamFilter) throws StreamsException {

        if (symStreamFilter == null) {
            throw new NullPointerException("NO filter provided ...");
        }

        StreamsApi streamsApi = new StreamsApi(apiClient);
        try {

            StreamFilter filter = SymStreamFilter.toStreamFilter(symStreamFilter);

            StreamList streamList = streamsApi.v1StreamsListPost(symAuth.getSessionToken().getToken(), skip, limit, filter);

            List<SymStreamAttributes> symStreamAttributes = new ArrayList<>();

            for(StreamAttributes streamAttributes: streamList){

                symStreamAttributes.add(SymStreamAttributes.toStreamAttributes(streamAttributes));

            }

            return symStreamAttributes;


        } catch (ApiException e) {
            throw new StreamsException("Failed to retrieve stream for given user ids...", e);
        }


    }


    /**
     * Get stream by set of {@link SymUser}.  This represents a stream related to multi-party conversation.
     *
     * @param email email address
     * @return {@link Stream}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public Stream getStreamFromEmail(String email) throws StreamsException {

        if (email == null) {
            throw new NullPointerException("Email was not provided...");
        }

        UsersClient usersClient;
        if (httpClient == null) {
            usersClient = UsersFactory.getClient(symAuth, serviceUrl, UsersFactory.TYPE.DEFAULT);
        } else {
            //not pretty..
            usersClient = UsersFactory.getClient(symAuth, serviceUrl, httpClient);
        }


        try {
            return getStream(usersClient.getUserFromEmail(email));
        } catch (UsersClientException e) {
            throw new StreamsException("Failed to find user from email : " + email, e);
        }
    }


    /**
     * Get stream by set of {@link SymUser}.  This represents a stream related to multi-party conversation.
     *
     * @param roomId roomId, this is the same as a streamID
     * @return {@link Stream}
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public SymRoomDetail getRoomDetail(String roomId) throws StreamsException {

        if (roomId == null) {
            throw new NullPointerException("Room ID was not provided..");
        }
        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {
            return SymRoomDetail.toSymRoomDetail(streamsApi.v2RoomIdInfoGet(roomId, symAuth.getSessionToken().getToken()));
        } catch (ApiException e) {
            throw new StreamsException("Failed to obtain room information from ID: " + roomId, e);
        }
    }

    /**
     * Create or retrieve existing chat room
     *
     * @param roomAttributes Room attributes that define a room
     * @return Room details
     * @throws StreamsException Generated by Symphony API exceptions
     */
    @Override
    public SymRoomDetail createChatRoom(SymRoomAttributes roomAttributes) throws StreamsException {

        if (roomAttributes == null) {
            throw new NullPointerException("Room Attributes were not provided..");
        }
        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {
            return SymRoomDetail.toSymRoomDetail(streamsApi.v2RoomCreatePost(
                    SymRoomAttributes.toV2RoomAttributes(roomAttributes), symAuth.getSessionToken().getToken())
            );
        } catch (ApiException e) {
            throw new StreamsException("Failed to obtain room information while creating room: " + roomAttributes.getName(), e);
        }
    }

    /**
     * Update chat room attributes
     * @param streamId Stream Id representing room
     * @param roomAttributes Room attributes to update
     * @return Room detail from changes
     * @throws StreamsException Exception from underlying API call
     */
    @Override
    public SymRoomDetail updateChatRoom(String streamId, SymRoomAttributes roomAttributes) throws StreamsException {

        if (roomAttributes == null) {
            throw new NullPointerException("Room Attributes were not provided..");
        }
        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {
            return SymRoomDetail.toSymRoomDetail(streamsApi.v2RoomIdUpdatePost(streamId,
                    SymRoomAttributes.toV2RoomAttributes(roomAttributes), symAuth.getSessionToken().getToken())
            );
        } catch (ApiException e) {
            throw new StreamsException("Failed to obtain room information while updating attributes on room: " + roomAttributes.getName(), e);
        }
    }


    /**
     * Search for rooms using search criteria.
     *
     * @param searchCriteria Search criteria
     * @param skip Skip number of results
     * @param limit Limit number of results
     * @return Room search results
     * @throws StreamsException Exception from underlying API call
     */
    @Override
    public SymRoomSearchResults roomSearch(SymRoomSearchCriteria searchCriteria, Integer skip, Integer limit) throws StreamsException {

        if (searchCriteria == null) {
            throw new NullPointerException("Room search criteria was not provided..");
        }
        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {

            return SymRoomSearchResults.toSymRoomSearchResults(streamsApi.v2RoomSearchPost(symAuth.getSessionToken().getToken(), SymRoomSearchCriteria.toRoomSearchCriteria(searchCriteria), skip, limit));


        } catch (ApiException e) {
            throw new StreamsException("Failed room search...", e);
        }
    }


    /**
     * Retrieve stream attributes, which provide communication types like ROOM, IM
     *
     * @param streamId Stream Id
     * @return Stream Attributes
     * @throws StreamsException Thrown from underlying API calls
     */
    @Override
    public SymStreamAttributes getStreamAttributes(String streamId) throws StreamsException {


        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {
            return SymStreamAttributes.toStreamAttributes(streamsApi.v1StreamsSidInfoGet(streamId, symAuth.getSessionToken().getToken()));
        } catch (ApiException e) {
            throw new StreamsException("Failed to obtain stream attributes while updating attributes on stream: {}" + streamId, e);
        }


    }

    @Override
    public void deactivateRoom(String roomId) throws StreamsException {
        if (roomId == null) {
            throw new IllegalArgumentException("Argument roomId must not be null");
        }

        StreamsApi streamsApi = new StreamsApi(apiClient);

        try {
            String sessionToken = symAuth.getSessionToken().getToken();

            streamsApi.v1RoomIdSetActivePost(roomId, false, sessionToken);
        } catch (Exception e) {
            String message = "Failed to deactivate room for roomId: " + roomId;
            logger.error(message, e);
            throw new StreamsException(message, e);
        }
    }
}
