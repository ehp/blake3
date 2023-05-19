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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static cz.aprar.oss.blake3.Blake3.OUT_LEN;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class HasherTest {
    @Test
    void testHashSizes() {
        final var hasher = new Hasher();
        hasher.update("abc".getBytes());
        hasher.update("def".getBytes());
        final var hash = hasher.finalizeHash();
        final var extendedHash = hasher.finalizeHash(500);
        assertArrayEquals(hash, Arrays.copyOfRange(extendedHash, 0, OUT_LEN));
    }

    @TestFactory
    Stream<DynamicTest> testVectors() throws IOException {
        final var mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final var testVector = mapper.readValue(getClass().getResourceAsStream("/test_vectors.json"), TestVector.class);
        return testVector.cases().stream().flatMap((tc) -> Stream.of(
                DynamicTest.dynamicTest("regular-" + tc.inputLen(), () -> {
                    testRegular(tc, inputBytes(tc.inputLen()));
                }),
                DynamicTest.dynamicTest("keyed-" + tc.inputLen(), () -> {
                    testKeyed(tc, inputBytes(tc.inputLen()), testVector.key().getBytes());
                }),
                DynamicTest.dynamicTest("derived-" + tc.inputLen(), () -> {
                    testDerivation(tc, inputBytes(tc.inputLen()), testVector.contextString());
                })
        ));
    }

    byte[] inputBytes(int len) {
        final var inputBytes = new byte[len];
        for (int i = 0; i < len; i++) {
            inputBytes[i] = (byte) (i % 251);
        }
        return inputBytes;
    }

    void testRegular(final Case testCase, final byte[] inputBytes) throws DecoderException {
        final var expected = Hex.decodeHex(testCase.hash());
        final var hasher = new Hasher();
        hasher.update(inputBytes);
        assertArrayEquals(expected, hasher.finalizeHash(expected.length));
    }

    void testKeyed(final Case testCase, final byte[] inputBytes, final byte[] key) throws DecoderException {
        final var expected = Hex.decodeHex(testCase.keyedHash());
        final var hasher = new Hasher(key);
        hasher.update(inputBytes);
        assertArrayEquals(expected, hasher.finalizeHash(expected.length));
    }

    void testDerivation(final Case testCase, final byte[] inputBytes, final String context) throws DecoderException {
        final var expected = Hex.decodeHex(testCase.deriveKey());
        final var hasher = new Hasher(context);
        hasher.update(inputBytes);
        assertArrayEquals(expected, hasher.finalizeHash(expected.length));
    }
}

record Case(
        @JsonProperty("input_len")
        int inputLen,
        String hash,
        @JsonProperty("keyed_hash")
        String keyedHash,
        @JsonProperty("derive_key")
        String deriveKey
) { }

record TestVector(
        String key,
        @JsonProperty("context_string")
        String contextString,
        List<Case> cases
) { }
