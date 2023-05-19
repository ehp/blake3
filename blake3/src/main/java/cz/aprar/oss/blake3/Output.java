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

import static cz.aprar.oss.blake3.Blake3.*;

/**
 * Each chunk or parent node can produce either an 8-word chaining value or, by
 * setting the ROOT flag, any number of final output bytes. The Output struct
 * captures the state just prior to choosing between those two possibilities.
 */
class Output {
    private final int[] inputChainingValue;
    private final int[] blockWords;
    private final long counter;
    private final int blockLen;
    private final int flags;

    Output(
            final int[] inputChainingValue,
            final int[] blockWords,
            final long counter,
            final int blockLen,
            final int flags
    ) {
        this.inputChainingValue = inputChainingValue;
        this.blockWords = blockWords;
        this.counter = counter;
        this.blockLen = blockLen;
        this.flags = flags;
    }

    int[] chainingValue() {
        return first8Words(compress(
                inputChainingValue,
                blockWords,
                counter,
                blockLen,
                flags
        ));
    }

    byte[] rootOutputBytes(final int size) {
        final var outSlice = new byte[size];
        final var chunkSize = 2 * OUT_LEN;
        for (int outputBlockCounter = 0; outputBlockCounter * chunkSize < outSlice.length; outputBlockCounter++) {
            final var words = compress(inputChainingValue, blockWords, outputBlockCounter, blockLen, flags | ROOT);

            // The output length might not be a multiple of 4.
            final var sliceStart = outputBlockCounter * chunkSize;
            for (int i = 0; i < Math.min(outSlice.length - sliceStart + 3, chunkSize) / 4; i++) {
                var w = 0L;
                if (i < words.length) {
                    w = words[i];
                }
                outSlice[sliceStart + i * 4] = (byte) w;
                if (sliceStart + i * 4 + 1 < outSlice.length) {
                    outSlice[sliceStart + i * 4 + 1] = (byte) (w >> 8);
                }
                if (sliceStart + i * 4 + 2 < outSlice.length) {
                    outSlice[sliceStart + i * 4 + 2] = (byte) (w >> 16);
                }
                if (sliceStart + i * 4 + 3 < outSlice.length) {
                    outSlice[sliceStart + i * 4 + 3] = (byte) (w >> 24);
                }
            }
        }
        return outSlice;
    }
}
