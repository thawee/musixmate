package org.jaudiotagger.utils;

import java.nio.charset.Charset;
import java.io.UnsupportedEncodingException;

/**
 * @author Drew Noakes https://drewnoakes.com
 */
public final class StringValue
{

    private final byte[] _bytes;

    private final Charset _charset;

    public StringValue( byte[] bytes,  Charset charset)
    {
        _bytes = bytes;
        _charset = charset;
    }


    public byte[] getBytes()
    {
        return _bytes;
    }


    public Charset getCharset()
    {
        return _charset;
    }

    @Override
    public String toString()
    {
        return toString(_charset);
    }

    public String toString( Charset charset)
    {
        if (charset != null) {
            try {
                return new String(_bytes, charset.name());
            } catch (UnsupportedEncodingException ex) {
                // fall through
            }
        }

        return new String(_bytes);
    }
}