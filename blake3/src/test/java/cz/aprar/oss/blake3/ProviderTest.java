/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package cz.aprar.oss.blake3;

import cz.aprar.oss.blake3.jca.Blake3Provider;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.stream.Stream;

import static cz.aprar.oss.blake3.VectorUtils.inputBytes;
import static cz.aprar.oss.blake3.VectorUtils.testVector;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class ProviderTest {
    @BeforeAll
    static void setUp() {
        Security.addProvider(new Blake3Provider());
    }

    @TestFactory
    Stream<DynamicTest> testFactory() throws IOException {
        var vector = testVector();
        return vector.cases().stream()
            .map((tc) ->
                DynamicTest.dynamicTest("provider-" + tc.inputLen(), () -> testProvider(tc, inputBytes(tc.inputLen())))
        );
    }

    void testProvider(final Case testCase, final byte[] inputBytes) throws DecoderException, NoSuchAlgorithmException {
        final var expected = Hex.decodeHex(testCase.hash());
        final var digest = MessageDigest.getInstance("Blake3ExtendedHashSize");
        digest.update(inputBytes);
        assertArrayEquals(expected, digest.digest());
    }
}
