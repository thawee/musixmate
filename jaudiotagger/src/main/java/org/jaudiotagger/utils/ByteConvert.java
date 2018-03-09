package org.jaudiotagger.utils;

/**
 * @author Drew Noakes http://drewnoakes.com
 */
public class ByteConvert
{
    public static int toInt32BigEndian( byte[] bytes)
    {
        return (bytes[0] << 24 & 0xFF000000) |
                (bytes[1] << 16 & 0xFF0000) |
                (bytes[2] << 8  & 0xFF00) |
                (bytes[3]       & 0xFF);
    }

    public static int toInt32LittleEndian( byte[] bytes)
    {
        return (bytes[0]       & 0xFF) |
                (bytes[1] << 8  & 0xFF00) |
                (bytes[2] << 16 & 0xFF0000) |
                (bytes[3] << 24 & 0xFF000000);
    }
}