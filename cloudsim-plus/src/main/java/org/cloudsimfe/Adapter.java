package org.cloudsimfe;

import java.util.LinkedList;
import java.util.Queue;

public class Adapter {

    public static final int READ_BUFFER = 0;
    public static final int WRITE_BUFFER = 1;

    private Queue<Payload> readBuffer;
    private Queue<Payload> writeBuffer;
    private int inputChannels;
    private int outputChannels;

    public Adapter(Queue<Payload> readBuffer, Queue<Payload> writeBuffer, int inputChannels, int outputChannels) {
        this.readBuffer = readBuffer;
        this.writeBuffer = writeBuffer;
        this.inputChannels = inputChannels;
        this.outputChannels = outputChannels;
    }

    public Adapter(int inputChannels, int outputChannels){
        this(new LinkedList<>(), new LinkedList<>(), inputChannels, outputChannels);
    }

    public Adapter() {
        this( 3, 3);
    }

    public void writeToBuffer(Payload payload, int bufferOption) {
        if (bufferOption == READ_BUFFER)
            readBuffer.add(payload);
        else if (bufferOption == WRITE_BUFFER)
            writeBuffer.add(payload);
    }

    public Payload readFromBuffer(int bufferOption) {
        if (bufferOption == READ_BUFFER)
            return readBuffer.poll();
        else if (bufferOption == WRITE_BUFFER)
            return writeBuffer.poll();
        else
            return null;
    }

    public void clearBuffer(int bufferOption) {
        if (bufferOption == READ_BUFFER)
            readBuffer.clear();
        else if (bufferOption == WRITE_BUFFER)
            writeBuffer.clear();
    }

    public Payload serializeData(Payload payload){
        return payload;
    }

    public Payload deserializeData(Payload payload){
        return payload;
    }

    public Payload packBits(Payload payload){
        return payload;
    }

    public Payload unpackBits(Payload payload){
        return payload;
    }
}
