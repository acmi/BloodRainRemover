/*
 * Copyright (c) 2016 acmi
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package acmi.l2.clientmod.rain_remove;

import acmi.l2.clientmod.io.BufferUtil;
import acmi.l2.clientmod.io.UnrealPackageFile;

import java.io.FileFilter;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Util {
    public static final FileFilter MAP_FILE_FILTER = pathname ->
            (pathname != null) && (pathname.isFile()) && (pathname.getName().endsWith(".unr"));

    public static void readStateFrame(ByteBuffer buffer) throws BufferUnderflowException {
        BufferUtil.getCompactInt(buffer);
        BufferUtil.getCompactInt(buffer);
        buffer.getLong();
        buffer.getInt();
        BufferUtil.getCompactInt(buffer);
    }

    public static void iterateProperties(ByteBuffer buffer, UnrealPackageFile up, TriConsumer<String, Integer, ByteBuffer> func) throws BufferUnderflowException {
        String name;
        while (!"None".equals(name = up.getNameTable().get(BufferUtil.getCompactInt(buffer)).getName())) {
            byte info = buffer.get();
            Type type = Type.values()[info & 15];
            int size = (info & 112) >> 4;
            boolean array = (info & 128) == 128;
            if (type == Type.STRUCT) {
                BufferUtil.getCompactInt(buffer);
            }

            size = getSize(size, buffer);
            if (array && type != Type.BOOL) {
                buffer.get();
            }

            byte[] obj = new byte[size];
            int pos = buffer.position();
            buffer.get(obj);

            func.accept(name, pos, ByteBuffer.wrap(obj).order(ByteOrder.LITTLE_ENDIAN));
        }
    }

    public static int getSize(int size, ByteBuffer buffer) throws BufferUnderflowException {
        switch (size) {
            case 0:
                return 1;
            case 1:
                return 2;
            case 2:
                return 4;
            case 3:
                return 12;
            case 4:
                return 16;
            case 5:
                return buffer.get() & 0xFF;
            case 6:
                return buffer.getShort() & 0xFFFF;
            case 7:
                return buffer.getInt();
        }
        throw new RuntimeException("invalid size " + size);
    }

    public enum Type {
        NONE,
        BYTE,
        INT,
        BOOL,
        FLOAT,
        OBJECT,
        NAME,
        DELEGATE,
        CLASS,
        ARRAY,
        STRUCT,
        VECTOR,
        ROTATOR,
        STR,
        MAP,
        FIXED_ARRAY;
    }
}
