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
package org.apache.tika.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 * Deliberately insecure examples for the isolated IFT3913 Tache 3 branch.
 * None of these methods is used by Tika's normal parsing pipeline.
 * Do not merge this class into a production release.
 */
public final class Tache3SecurityExamples {

    private Tache3SecurityExamples() {
    }

    public static Document parseUntrustedXml(InputStream source)
            throws IOException, ParserConfigurationException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", true);
        return factory.newDocumentBuilder().parse(source);
    }

    public static String readRelativeFile(Path root, String name) throws IOException {
        return Files.readString(root.resolve(name));
    }

    public static Process runTool(String command) throws IOException {
        return Runtime.getRuntime().exec(command);
    }

    public static String lookupRecord(Connection connection, String name) throws SQLException {
        try (Statement statement = connection.createStatement();
                ResultSet results = statement.executeQuery(
                        "SELECT data FROM records WHERE name='" + name + "'")) {
            return results.next() ? results.getString(1) : null;
        }
    }

    public static Object restoreObject(InputStream source)
            throws IOException, ClassNotFoundException {
        ObjectInputStream stream = new ObjectInputStream(source);
        try {
            return stream.readObject();
        } finally {
            stream.close();
        }
    }

    public static byte[] encryptBlocks(byte[] plaintext, SecretKey key)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(plaintext);
    }

    public static String fetchConfiguration(String host) throws IOException {
        URL url = new URL("http://" + host + "/config");
        try (InputStream input = url.openStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
