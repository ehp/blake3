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

public class Hasher {
    private ChunkState chunkState;
    private final int[] keyWords;
    private final int[][] cvStack; // Space for 54 subtree chaining values:
    private byte cvStackLen;  // 2^54 * CHUNK_LEN = 2^64
    private final int flags;

    private Hasher(final int[] keyWords, final int flags) {
        this.chunkState = new ChunkState(keyWords, 0, flags);
        this.keyWords = keyWords;
        this.cvStack = new int[54][8];
        this.cvStackLen = 0;
        this.flags = flags;
    }

    /**
     * private static finalruct a new `Hasher` for the regular hash function.
     */
    public Hasher() {
        this(IV, 0);
    }

    /**
     * private static finalruct a new `Hasher` for the keyed hash function.
     * @param key hash key
     */
    public Hasher(final byte[] key) {
        this(first8Words(wordsFromLittleEndianBytes(key)), KEYED_HASH);
    }

    /**
     * private static finalruct a new `Hasher` for the key derivation function. The context
     * string should be hardcoded, globally unique, and application-specific.
     * @param context Context string
     */
    public Hasher(final String context) {
        final var contextHasher = new Hasher(IV, DERIVE_KEY_CONTEXT);
        contextHasher.update(context.getBytes());
        final var contextKey = contextHasher.finalizeHash(KEY_LEN);

        this.keyWords = wordsFromLittleEndianBytes(contextKey);
        this.chunkState = new ChunkState(this.keyWords, 0, DERIVE_KEY_MATERIAL);
        this.cvStack = new int[8][54];
        this.cvStackLen = 0;
        this.flags = DERIVE_KEY_MATERIAL;
    }

    private void pushStack(final int[] cv) {
        cvStack[cvStackLen] = cv;
        cvStackLen++;
    }

    private int[] popStack() {
        cvStackLen--;
        return cvStack[cvStackLen];
    }

    private static Output parentOutput(
            final int[] leftChildCv,
            final int[] rightChildCv,
            final int[] keyWords,
            final int flags
    ) {
        final var blockWords = new int[16];
        System.arraycopy(leftChildCv, 0, blockWords, 0, 8);
        System.arraycopy(rightChildCv, 0, blockWords, 8, 8);

        return new Output(
                keyWords,
                blockWords,
                0L, // Always 0 for parent nodes.
                BLOCK_LEN, // Always BLOCK_LEN (64) for parent nodes.
                PARENT | flags
        );
    }

    private static int[] parentCv(
            final int[] leftChildCv,
            final int[] rightChildCv,
            final int[] keyWords,
            final int flags
    ) {
        return parentOutput(leftChildCv, rightChildCv, keyWords, flags).chainingValue();
    }

    // Section 5.1.2 of the BLAKE3 spec explains this algorithm in more detail.
    private void addChunkChainingValue(int[] newCv, long totalChunks) {
        // This chunk might complete some subtrees. For each completed subtree,
        // its left child will be the current top entry in the CV stack, and
        // its right child will be the current value of `new_cv`. Pop each left
        // child off the stack, merge it with `new_cv`, and overwrite `new_cv`
        // with the result. After all these merges, push the final value of
        // `new_cv` onto the stack. The number of completed subtrees is given
        // by the number of trailing 0-bits in the new total number of chunks.
        while ((totalChunks & 1) == 0) {
            newCv = parentCv(popStack(), newCv, keyWords, flags);
            totalChunks >>= 1;
        }
        pushStack(newCv);
    }

    /**
     * Add input to the hash state. This can be called any number of times.
     * @param input Hash input
     */
    public void update(final byte[] input) {
        var counter = 0;
        while (counter < input.length) {
            // If the current chunk is complete, finalize it and reset the
            // chunk state. More input is coming, so this chunk is not ROOT.
            if (chunkState.length() == CHUNK_LEN) {
                final var chunkCv = chunkState.output().chainingValue();
                final var totalChunks = chunkState.chunkCounter + 1L;
                addChunkChainingValue(chunkCv, totalChunks);
                chunkState = new ChunkState(keyWords, totalChunks, flags);
            }

            // Compress input bytes into the current chunk state.
            final var want = CHUNK_LEN - chunkState.length();
            final var take = Math.min(want, input.length - counter);

            final var chunk = Arrays.copyOfRange(input, counter, counter + take);
            chunkState.update(chunk);
            counter += take;
        }
    }

    /**
     * Finalize the hash and write any number of output bytes.
     * @param size Hash size
     * @return Hash data
     */
    public byte[] finalizeHash(final int size) {
        // Starting with the Output from the current chunk, compute all the
        // parent chaining values along the right edge of the tree, until we
        // have the root Output.
        var output = chunkState.output();
        var parentNodesRemaining = cvStackLen;
        while (parentNodesRemaining > 0) {
            parentNodesRemaining--;
            output = parentOutput(
                    cvStack[parentNodesRemaining],
                    output.chainingValue(),
                    keyWords,
                    flags
            );
        }
        return output.rootOutputBytes(size);
    }

    /**
     * Finalize the hash and write 32 output bytes.
     * @return Hash data
     */
    public byte[] finalizeHash() {
        return finalizeHash(OUT_LEN);
    }
}
