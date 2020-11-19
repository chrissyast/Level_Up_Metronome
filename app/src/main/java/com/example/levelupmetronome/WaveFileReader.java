package com.example.levelupmetronome;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;


public class WaveFileReader {

    private final WavFileProperty CHUNK_SIZE = new WavFileProperty(4,4,WavFilePropertyFormat.HEX_LITTLE_ENDIAN, null, "chunkSize");
    private final WavFileProperty NUM_CHANNELS = new WavFileProperty(10,2,WavFilePropertyFormat.HEX_LITTLE_ENDIAN, "fmt ", "numChannels");
    private final WavFileProperty SAMPLE_RATE = new WavFileProperty(12,4,WavFilePropertyFormat.HEX_LITTLE_ENDIAN, "fmt ", "sampleRate");
    private final WavFileProperty BITS_PER_SAMPLE = new WavFileProperty(22,2,WavFilePropertyFormat.HEX_LITTLE_ENDIAN, "fmt ", "bitsPerSample");
    private final WavFileProperty DATA_SIZE = new WavFileProperty(4,4,WavFilePropertyFormat.HEX_LITTLE_ENDIAN, "data", "dataSize");

    private final List<WavFileProperty> properties = Arrays.asList(CHUNK_SIZE, NUM_CHANNELS, SAMPLE_RATE, DATA_SIZE, BITS_PER_SAMPLE);


    private int chunkSize;
    private int numChannels;
    private int sampleRate;
    private int dataSize;
    private int bitsPerSample;
    private byte[] dataChunk;

    public WaveFileReader (InputStream beat) throws NoSuchFieldException, IllegalAccessException {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] b = new byte[1024];
            try {
                for (int readNum; (readNum = beat.read(b)) != -1; ) {
                    bos.write(b, 0, readNum);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            byte[] fileByteArray = bos.toByteArray();

            for (WavFileProperty property : properties) {
                int chunkStart = 0;
                if (null != property.chunk) {
                    chunkStart = startOfChunk(fileByteArray, property.chunk);
                }
                property.setIndex(chunkStart + property.offsetWithinChunk);
                int start = property.index;
                int length = property.length;
                byte[] propertyData = Arrays.copyOfRange(fileByteArray, start, start+length);
                int value = (int) makePropertyLegible(property, propertyData);
                Field field = getClass().getDeclaredField(property.propertyName);
                field.set(this, value);
            }
            int startOfData = startOfChunk(fileByteArray, "data");
            byte[] dataChunk = Arrays.copyOfRange(fileByteArray, startOfData, fileByteArray.length);
            this.dataChunk = dataChunk;
    }

    public static Object makePropertyLegible(WavFileProperty property, byte[] data) {
        if (property.format == WavFilePropertyFormat.ASCII) {
            StringBuilder sb = new StringBuilder();
            for (byte datum : data) {
                sb.append((char) datum);
            }
            return sb.toString();
        }
        if (property.format == WavFilePropertyFormat.HEX_LITTLE_ENDIAN) {
            ByteBuffer bb = ByteBuffer.wrap(data);
            bb.order( ByteOrder.LITTLE_ENDIAN);
            int output = 0;
            int multiplier = 1;
            while( bb.hasRemaining()) {

                short v = bb.getShort();
                long segment = (v & 0XFFFF) * multiplier;
                output += segment;
                multiplier *= (256 * 256);
            }
            return output;
        }
        // TODO hex big endian
        return null;
    }

    private int startOfChunk(byte[] bytes, String chunkName) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
        baos.write(bytes, 0, bytes.length);
        return baos.toString().indexOf(chunkName);
    }

    public static class WavFileProperty {
        private int offsetWithinChunk;
        private int length;
        private int index;
        private WavFilePropertyFormat format;
        private String chunk;
        private String propertyName;


        public WavFileProperty(Integer offsetWithinChunk, int length, WavFilePropertyFormat format, String chunk, String propertyName) {
            if (offsetWithinChunk != null) {
                this.offsetWithinChunk = offsetWithinChunk;
            }
            this.length = length;
            this.format = format;
            this.chunk = chunk;
            this.propertyName = propertyName;
        }

        public void setIndex(int index) {
            this.index = index;
        }
    }

    private enum WavFilePropertyFormat {
        ASCII,
        HEX_LITTLE_ENDIAN,
        HEX_BIG_ENDIAN
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public int getNumChannels() {
        return numChannels;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public int getDataSize() {
        return dataSize;
    }

    public int getBitsPerSample() {
        return bitsPerSample;
    }

    public int fileLengthInMilliseconds() {
        return (int) ((double) dataSize / (bitsPerSample / 8) / numChannels / sampleRate * 1000);
    }

    public double dataSizePerMillisecond() {
        return ((double) sampleRate * numChannels * bitsPerSample  / 8 / 1000);
    }

    public byte[] getDataChunk() {
        return dataChunk;
    }
}
