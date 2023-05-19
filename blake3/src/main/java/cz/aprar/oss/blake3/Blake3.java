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

class Blake3 {
    static final int OUT_LEN = 32;
    static final int KEY_LEN = 32;
    static final int BLOCK_LEN = 64;
    static final int CHUNK_LEN = 1024;

    static final int CHUNK_START = 1 << 0;
    static final int CHUNK_END = 1 << 1;
    static final int PARENT = 1 << 2;
    static final int ROOT = 1 << 3;
    static final int KEYED_HASH = 1 << 4;
    static final int DERIVE_KEY_CONTEXT = 1 << 5;
    static final int DERIVE_KEY_MATERIAL = 1 << 6;

    static final int[] IV = {
        0x6A09E667, 0xBB67AE85, 0x3C6EF372, 0xA54FF53A, 0x510E527F, 0x9B05688C, 0x1F83D9AB, 0x5BE0CD19,
    };

    static final int[] MSG_PERMUTATION = {2, 6, 3, 10, 7, 0, 4, 13, 1, 11, 12, 5, 9, 14, 15, 8};

    static int[] wordsFromLittleEndianBytes(final byte[] bytes) {
        final var words = new int[bytes.length / 4];

        for (int i = 0; i < words.length; i++) {
            words[i] = (bytes[i * 4] & 0xFF) |
                    ((bytes[i * 4 + 1] & 0xFF) << 8) |
                    ((bytes[i * 4 + 2] & 0xFF) << 16) |
                    ((bytes[i * 4 + 3] & 0xFF) << 24);
        }

        return words;
    }

    // The mixing function, G, which mixes either a column or a diagonal.
    private static void g(
            final int[] state,
            final int a,
            final int b,
            final int c,
            final int d,
            final int mx,
            final int my
    ) {
        var state_a = state[a];
        var state_b = state[b];
        var state_c = state[c];
        var state_d = state[d];

        state_a = state_a + state_b + mx;
        var da_xor = state_d ^ state_a;
        state_d = (da_xor >>> 16) | (da_xor << 16);

        state_c = state_c + state_d;
        var bc_xor = state_b ^ state_c;
        state_b = (bc_xor >>> 12) | (bc_xor << 20);

        state_a = state_a + state_b + my;
        da_xor = state_d ^ state_a;
        state_d = (da_xor >>> 8) | (da_xor << 24);

        state_c = state_c + state_d;
        bc_xor = state_b ^ state_c;
        state_b = (bc_xor >>> 7) | (bc_xor << 25);

        state[a] = state_a;
        state[b] = state_b;
        state[c] = state_c;
        state[d] = state_d;
    }

    private static void round(final int[] state, final int[] m) {
        // Mix the columns.
        g(state, 0, 4, 8, 12, m[0], m[1]);
        g(state, 1, 5, 9, 13, m[2], m[3]);
        g(state, 2, 6, 10, 14, m[4], m[5]);
        g(state, 3, 7, 11, 15, m[6], m[7]);
        // Mix the diagonals.
        g(state, 0, 5, 10, 15, m[8], m[9]);
        g(state, 1, 6, 11, 12, m[10], m[11]);
        g(state, 2, 7, 8, 13, m[12], m[13]);
        g(state, 3, 4, 9, 14, m[14], m[15]);
    }

    private static int[] permute(final int[] m) {
        final var permuted = new int[16];

        for (int i = 0; i < 16; i++) {
            permuted[i] = m[MSG_PERMUTATION[i]];
        }

        return permuted;
    }

    static int[] compress(
            final int[] chainingValue,
            final int[] blockWords,
            final long counter,
            final int blockLen,
            final int flags
    ) {
        final var state = new int[] {
                chainingValue[0],
                chainingValue[1],
                chainingValue[2],
                chainingValue[3],
                chainingValue[4],
                chainingValue[5],
                chainingValue[6],
                chainingValue[7],
                IV[0],
                IV[1],
                IV[2],
                IV[3],
                (int)counter,
                (int)(counter >> 32),
                blockLen,
                flags,
        };

        var block = blockWords;

        round(state, block); // round 1
        block = permute(block);
        round(state, block); // round 2
        block = permute(block);
        round(state, block); // round 3
        block = permute(block);
        round(state, block); // round 4
        block = permute(block);
        round(state, block); // round 5
        block = permute(block);
        round(state, block); // round 6
        block = permute(block);
        round(state, block); // round 7

        for (int i = 0; i < 8; i++) {
            state[i] ^= state[i + 8];
            state[i + 8] ^= chainingValue[i];
        }

        return state;
    }

    static int[] first8Words(final int[] compressionOutput) {
        return Arrays.copyOf(compressionOutput, 8);
    }
}
