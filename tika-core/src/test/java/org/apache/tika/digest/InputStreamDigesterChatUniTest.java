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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.junit.jupiter.api.Test;

import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;

/**
 * Corrected version of the test generated locally by ChatUniTest and CodeQwen.
 * The unmodified failed generations are kept under tache2-results/chatunitest.
 */
public class InputStreamDigesterChatUniTest {

    @Test
    public void testDigestStoresEncodedSha256() throws Exception {
        String key = "custom:digest:sha256";
        InputStreamDigester digester = new InputStreamDigester("SHA-256", key,
                bytes -> Base64.getEncoder().encodeToString(bytes));
        Metadata metadata = new Metadata();

        try (TikaInputStream stream =
                     TikaInputStream.get("hello world".getBytes(StandardCharsets.UTF_8))) {
            digester.digest(stream, metadata, new ParseContext());
        }

        assertEquals("uU0nuZNNPgilLlLX2n2r+sSE7+N6U4DukIj3rOLvzek=", metadata.get(key));
    }
}
