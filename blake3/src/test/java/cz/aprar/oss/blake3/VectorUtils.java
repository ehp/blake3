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

import java.io.IOException;
import java.util.List;

public class VectorUtils {
    static TestVector testVector() throws IOException {
        final var mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        final var testVector = mapper.readValue(
            VectorUtils.class.getResourceAsStream("/test_vectors.json"),
            TestVector.class
        );
        return testVector;
    }

    static byte[] inputBytes(int len) {
        final var inputBytes = new byte[len];
        for (int i = 0; i < len; i++) {
            inputBytes[i] = (byte) (i % 251);
        }
        return inputBytes;
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
