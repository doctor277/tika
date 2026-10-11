/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.digest;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.Provider;
import java.util.Arrays;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

public class InputStreamDigesterManualTest {

    private static final Encoder BASE64_ENCODER =
            bytes -> Base64.getEncoder().encodeToString(bytes);

    @Test
    public void testDigestSetsLengthAndRewindsRawStream() throws Exception {
        byte[] data = new byte[9000];
        Arrays.fill(data, (byte) 42);
        Metadata metadata = new Metadata();
        InputStreamDigester digester =
                new InputStreamDigester("SHA-256", "custom:digest:rewind", BASE64_ENCODER);

        try (TikaInputStream stream = TikaInputStream.get(new ByteArrayInputStream(data))) {
            digester.digest(stream, metadata, new ParseContext());

            assertEquals(Integer.toString(data.length), metadata.get(Metadata.CONTENT_LENGTH));
            assertArrayEquals(data, stream.readAllBytes());
        }
    }

    @Test
    public void testDigestPreservesExistingContentLength() throws Exception {
        Metadata metadata = new Metadata();
        metadata.set(Metadata.CONTENT_LENGTH, "1234");
        InputStreamDigester digester =
                new InputStreamDigester("SHA-256", "custom:digest:length", BASE64_ENCODER);

        try (TikaInputStream stream = TikaInputStream.get(new byte[] {1, 2, 3})) {
            digester.digest(stream, metadata, new ParseContext());
        }

        assertEquals("1234", metadata.get(Metadata.CONTENT_LENGTH));
    }

    @Test
    public void testDigestUsesConfiguredProvider() throws Exception {
        Provider provider = MessageDigest.getInstance("SHA-256").getProvider();
        InputStreamDigester digester = new InputStreamDigester(
                "SHA-256", "custom:digest:provider", bytes -> Integer.toString(bytes.length)) {
            @Override
            protected Provider getProvider() {
                return provider;
            }
        };
        Metadata metadata = new Metadata();

        try (TikaInputStream stream = TikaInputStream.get(new byte[] {1, 2, 3})) {
            digester.digest(stream, metadata, new ParseContext());
        }

        assertEquals("32", metadata.get("custom:digest:provider"));
    }

    @Test
    public void testUnknownAlgorithmIsRejected() throws Exception {
        InputStreamDigester digester = new InputStreamDigester(
                "not-a-message-digest", "custom:digest:invalid", BASE64_ENCODER);

        try (TikaInputStream stream = TikaInputStream.get(new byte[0])) {
            assertThrows(IllegalArgumentException.class,
                    () -> digester.digest(stream, new Metadata(), new ParseContext()));
        }
    }
}
