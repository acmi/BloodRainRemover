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

import acmi.l2.clientmod.io.UnrealPackage;

import java.io.File;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

import static acmi.l2.clientmod.io.BufferUtil.getCompactInt;
import static acmi.l2.clientmod.io.BufferUtil.putCompactInt;

public class Main {
    private static void handleFile(File unr) {
        try (UnrealPackage up = new UnrealPackage(unr, false)) {
            System.out.println(unr);
            UnrealPackage.ExportEntry level = (UnrealPackage.ExportEntry) up.objectReference(up.objectReference("myLevel", "Engine.Level"));
            ByteBuffer levelBuffer = ByteBuffer.wrap(level.getObjectRawData()).order(ByteOrder.LITTLE_ENDIAN);

            ByteBuffer newLevelBuffer = ByteBuffer.allocate(level.getSize()).order(ByteOrder.LITTLE_ENDIAN);

            putCompactInt(newLevelBuffer, getCompactInt(levelBuffer));
            if (up.getLicensee() > 20) {
                newLevelBuffer.putInt(levelBuffer.getInt());
                int count = levelBuffer.getInt();
                newLevelBuffer.putInt(count);
                for (int i = 0; i < count; i++) {
                    putCompactInt(newLevelBuffer, getCompactInt(levelBuffer));
                }
            }

            levelBuffer.getInt();
            int[] refs = new int[levelBuffer.getInt()];
            for (int i = 0; i < refs.length; i++) {
                refs[i] = getCompactInt(levelBuffer);
            }
            List<Integer> entries = new ArrayList<>();
            for (int ref : refs) {
                if (ref == 0) {
                    entries.add(ref);
                } else {
                    UnrealPackage.ExportEntry exportEntry = (UnrealPackage.ExportEntry) up.objectReference(ref);

                    switch (exportEntry.getObjectClass().getObjectFullName()) {
                        case "Engine.Emitter": {
                            boolean[] holder = new boolean[]{true};
                            ByteBuffer data = ByteBuffer.wrap(exportEntry.getObjectRawData());
                            Util.readStateFrame(data);
                            Util.iterateProperties(data, up, (name, pos, pData) -> {
                                if (name.equalsIgnoreCase("Group") && up.nameReference(getCompactInt(pData)).toLowerCase().contains("bloodrain")) {
                                    System.out.println("remove from level: " + exportEntry);
                                    holder[0] = false;
                                }
                            });
                            if (holder[0]) {
                                entries.add(ref);
                            }
                            break;
                        }
                        default:
                            entries.add(ref);
                    }
                }
            }

            newLevelBuffer.putInt(entries.size());
            newLevelBuffer.putInt(entries.size());
            for (Integer ref : entries) {
                putCompactInt(newLevelBuffer, ref);
            }

            newLevelBuffer.put(levelBuffer);
            newLevelBuffer.flip();

            byte[] data = new byte[newLevelBuffer.limit()];
            newLevelBuffer.get(data);
            level.setObjectRawData(data);
        } catch (UncheckedIOException e) {
            System.err.println("Couldn't open " + unr + ": " + e.getCause().getMessage());
        }
    }

    public static void main(String[] args) {
        File folder = args.length > 0 ? new File(args[0]) : new File(System.getProperty("user.dir"));

        for (File unr : folder.listFiles(Util.MAP_FILE_FILTER)) {
            handleFile(unr);
        }
    }
}
