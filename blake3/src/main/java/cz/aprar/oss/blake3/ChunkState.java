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

import java.util.Arrays;

import static cz.aprar.oss.blake3.Blake3.*;

class ChunkState {
    private int[] chainingValue;
    final long chunkCounter;
    private final byte[] block;
    private byte blockLen;
    private byte blocksCompressed;
    private final int flags;

    ChunkState(final int[] keyWords, final long chunkCounter, final int flags) {
        this.chainingValue = keyWords;
        this.chunkCounter = chunkCounter;
        this.block = new byte[BLOCK_LEN];
        this.blockLen = 0;
        this.blocksCompressed = 0;
        this.flags = flags;
    }

    int length() {
        return BLOCK_LEN * blocksCompressed + blockLen;
    }

    private int startFlag() {
        if (blocksCompressed == 0) {
            return CHUNK_START;
        } else {
            return 0;
        }
    }

    void update(final byte[] input) {
        var counter = 0;
        while (counter < input.length) {
            // If the block buffer is full, compress it and clear it. More
            // input is coming, so this compression is not CHUNK_END.
            if (blockLen == BLOCK_LEN) {
                final var blockWords = wordsFromLittleEndianBytes(block);
                chainingValue = first8Words(compress(
                        chainingValue,
                        blockWords,
                        chunkCounter,
                        BLOCK_LEN,
                        flags | startFlag()
                ));
                blocksCompressed++;
                Arrays.fill(block, (byte)0);
                blockLen = 0;
            }

            // Copy input bytes into the block buffer.
            final var want = BLOCK_LEN - blockLen;
            final var take = Math.min(want, input.length - counter);

            System.arraycopy(input, counter, block, blockLen, take);
            blockLen += take;
            counter += take;
        }
    }

    Output output() {
        final var blockWords = wordsFromLittleEndianBytes(block);
        return new Output(
                chainingValue,
                blockWords,
                chunkCounter,
                blockLen,
                flags | startFlag() | CHUNK_END
        );
    }
}
