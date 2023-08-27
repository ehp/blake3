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
package cz.aprar.oss.blake3.jca;

import cz.aprar.oss.blake3.Hasher;

import java.security.MessageDigestSpi;

/**
 * Blake3 Service Provider Interface
 */
abstract class Blake3Spi extends MessageDigestSpi {
    private static final int MAX_CHUNK_LENGTH = 1024 * 1024;

    private Hasher hasher = new Hasher();
    private final int hashSize;

    protected Blake3Spi(final int hashSize) {
        this.hashSize = hashSize;
    }

    @Override
    protected void engineUpdate(final byte input) {
        hasher.update(new byte[] {input});
    }

    @Override
    protected void engineUpdate(final byte[] input, final int offset, final int len) {
        var movingOffset = offset;

        while (movingOffset < offset + len) {
            var movingLength = offset + len - movingOffset;
            // limit chunk length
            if (movingLength > MAX_CHUNK_LENGTH) {
                movingLength = MAX_CHUNK_LENGTH;
            }

            var buffer = new byte[movingLength];
            System.arraycopy(input, movingOffset, buffer, 0, movingLength);
            hasher.update(buffer);
            movingOffset += movingLength;
        }
    }

    @Override
    protected byte[] engineDigest() {
        return hasher.finalizeHash(hashSize);
    }

    @Override
    protected void engineReset() {
        hasher = new Hasher();
    }
}
