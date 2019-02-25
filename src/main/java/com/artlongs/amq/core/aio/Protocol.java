package com.artlongs.amq.core.aio;

import java.nio.ByteBuffer;

/**
 * Func :
 *
 * @author: leeton on 2019/2/22.
 */
public interface Protocol<T> {
    ByteBuffer encode(T message);
    T decode(final ByteBuffer buffer);


}
