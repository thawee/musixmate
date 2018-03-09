package org.jaudiotagger.file;

import java.io.IOException;

/**
 * A checked replacement for {@link IndexOutOfBoundsException}.  Used by {@link RandomAccessReader}.
 *
 * @author Drew Noakes https://drewnoakes.com
 */
public final class BufferBoundsException extends IOException
{
    private static final long serialVersionUID = 2911102837808946396L;

    public BufferBoundsException(int index, int bytesRequested, long bufferLength)
    {
        super(getMessage(index, bytesRequested, bufferLength));
    }

    public BufferBoundsException(final String message)
    {
        super(message);
    }

    private static String getMessage(int index, int bytesRequested, long bufferLength)
    {
        if (index < 0)
            return String.format("Attempt to read from buffer using a negative index (%d)", index);

        if (bytesRequested < 0)
            return String.format("Number of requested bytes cannot be negative (%d)", bytesRequested);

        if ((long)index + (long)bytesRequested - 1L > (long)Integer.MAX_VALUE)
            return String.format("Number of requested bytes summed with starting index exceed maximum range of signed 32 bit integers (requested index: %d, requested count: %d)", index, bytesRequested);

        return String.format("Attempt to read from beyond end of underlying data source (requested index: %d, requested count: %d, max index: %d)",
                index, bytesRequested, bufferLength - 1);
    }
}