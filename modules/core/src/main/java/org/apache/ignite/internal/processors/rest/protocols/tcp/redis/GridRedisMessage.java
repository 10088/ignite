/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.processors.rest.protocols.tcp.redis;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.ignite.internal.processors.rest.client.message.GridClientMessage;

/**
 * Message to communicate with Redis client. Contains command, its attributes and response.
 */
public class GridRedisMessage implements GridClientMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** Request byte. */
    public static final byte RESP_REQ_FLAG = GridRedisProtocolParser.ARRAY;

    private static final int CMD_POS = 0;

    private static final int KEY_POS = 1;

    /** Request message parts. */
    private final List<String> msgParts;

    private ByteBuffer response;

    public GridRedisMessage(int msgLen) {
        msgParts = new ArrayList<>(msgLen);
    }

    public void append(String part) {
        msgParts.add(part);
    }

    public void setResponse(ByteBuffer response) {
        this.response = response;
    }

    public ByteBuffer getResponse() {
        return response;
    }

    /**
     * @return {@link GridRedisCommand}.
     */
    public GridRedisCommand command() {
        return GridRedisCommand.valueOf(msgParts.get(CMD_POS).toUpperCase());
    }

    /**
     * @return Key for the command.
     */
    public String key() {
        if (msgParts.size() <= KEY_POS)
            return null;

        return msgParts.get(KEY_POS);
    }

    @Override public String toString() {
        return "GridRedisMessage [msg: " + msgParts + "]";
    }

    /** {@inheritDoc} */
    @Override public long requestId() {
        return 0;
    }

    /** {@inheritDoc} */
    @Override public void requestId(long reqId) {

    }

    /** {@inheritDoc} */
    @Override public UUID clientId() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void clientId(UUID id) {

    }

    /** {@inheritDoc} */
    @Override public UUID destinationId() {
        return null;
    }

    /** {@inheritDoc} */
    @Override public void destinationId(UUID id) {

    }

    /** {@inheritDoc} */
    @Override public byte[] sessionToken() {
        return new byte[0];
    }

    /** {@inheritDoc} */
    @Override public void sessionToken(byte[] sesTok) {

    }
}
