/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyright [year] [name of copyright owner]".
 *
 * Portions Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.json.resource.http;

import static org.forgerock.http.io.IO.newBranchingInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.http.io.BranchingInputStream;
import org.forgerock.http.io.Buffer;
import org.forgerock.http.io.IO;
import org.forgerock.util.Factory;

/**
 * Represents a pipe for transferring bytes from an {@link java.io.OutputStream} to a
 * {@link org.forgerock.http.io.BranchingInputStream}.
 */
final class PipeBufferedStream {
    private final OutputStream outputStream;
    private final BranchingInputStream inputStream;
    /** The buffer will be closed once both the input and output stream are closed. */
    private final AtomicInteger bufferRefCount = new AtomicInteger(2);
    private final Buffer buffer;
    private int position = 0;

    PipeBufferedStream() {
        this(IO.newTemporaryStorage());
    }

    PipeBufferedStream(final Factory<Buffer> bufferFactory) {
        outputStream = new PipeOutputStream();
        inputStream = newBranchingInputStream(new PipeInputStream(), bufferFactory);
        this.buffer = bufferFactory.newInstance();
    }

    /**
     * Returns the output stream which writes to the pipe.
     *
     * @return The output stream.
     */
    public OutputStream getIn() {
        return outputStream;
    }

    /**
     * Returns the input stream which reads from the pipe.
     *
     * @return The input stream.
     */
    public BranchingInputStream getOut() {
        return inputStream;
    }

    private void closeBufferIfNeeded() throws IOException {
        if (bufferRefCount.decrementAndGet() == 0) {
            buffer.close();
        }
    }

    private class PipeOutputStream extends OutputStream {
        @Override
        public void write(int i) throws IOException {
            buffer.append(new byte[]{(byte) i}, 0, 1);
        }

        @Override
        public void close() throws IOException {
            closeBufferIfNeeded();
        }
    }

    private class PipeInputStream extends InputStream {
        @Override
        public int read() throws IOException {
            if (position >= buffer.length()) {
                return -1;
            } else {
                byte[] b = new byte[1];
                buffer.read(position++, b, 0, 1);
                return b[0];
            }
        }

        @Override
        public void close() throws IOException {
            closeBufferIfNeeded();
        }
    }
}
