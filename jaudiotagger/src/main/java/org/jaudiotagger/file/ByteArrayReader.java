package org.jaudiotagger.file;

import java.io.IOException;

/**
 * Provides methods to read specific values from a byte array, with a consistent, checked exception structure for
 * issues.
 * <p>
 * By default, the reader operates with Motorola byte order (big endianness).  This can be changed by calling
 * <code>setMotorolaByteOrder(boolean)</code>.
 *
 * @author Drew Noakes https://drewnoakes.com
 * */
public class ByteArrayReader extends RandomAccessReader
{
    private final byte[] _buffer;
    private final int _baseOffset;

    @SuppressWarnings({ "ConstantConditions" })
    public ByteArrayReader( byte[] buffer)
    {
        this(buffer, 0);
    }

    @SuppressWarnings({ "ConstantConditions" })
    public ByteArrayReader( byte[] buffer, int baseOffset)
    {
        if (buffer == null)
            throw new NullPointerException();
        if (baseOffset < 0)
            throw new IllegalArgumentException("Must be zero or greater");

        _buffer = buffer;
        _baseOffset = baseOffset;
    }

    @Override
    public int toUnshiftedOffset(int localOffset)
    {
        return localOffset + _baseOffset;
    }

    @Override
    public long getLength()
    {
        return _buffer.length - _baseOffset;
    }

    @Override
    public byte getByte(int index) throws IOException
    {
        validateIndex(index, 1);
        return _buffer[index + _baseOffset];
    }

    @Override
    protected void validateIndex(int index, int bytesRequested) throws IOException
    {
        if (!isValidIndex(index, bytesRequested))
            throw new BufferBoundsException(toUnshiftedOffset(index), bytesRequested, _buffer.length);
    }

    @Override
    protected boolean isValidIndex(int index, int bytesRequested) throws IOException
    {
        return bytesRequested >= 0
                && index >= 0
                && (long)index + (long)bytesRequested - 1L < getLength();
    }

    @Override
    public byte[] getBytes(int index, int count) throws IOException
    {
        validateIndex(index, count);

        byte[] bytes = new byte[count];
        System.arraycopy(_buffer, index + _baseOffset, bytes, 0, count);
        return bytes;
    }
}