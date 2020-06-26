package src.core;

import java.io.*;
import java.net.*;
import java.util.Arrays;

public class Client {
    /*
     * This class implements application's client
     */

    private DatagramSocket socket;
    private byte[] buffer;
    private int[] receivedAck;
    private InetAddress hostIP;
    private final int hostPort = 3000;

    public Client() {
        /*
         * Constructor
         */

        this.log("Starting client...");

        try {
            this.hostIP = InetAddress.getByName("localhost");
            this.socket = new DatagramSocket();
            this.buffer = new byte[512];
        } catch (UnknownHostException error) {
            this.log("Unknown host exception: " + error.getMessage());
        } catch (SocketException error) {
            this.log("An error occurred while building socket: " + error.getMessage());
        }
    }

    public void slowStart(byte[][] datagrams) {
        /*
         * This method implements slow start technique
         */

        this.log("Slow start...");

        receivedAck = new int[datagrams.length];
        int slowCount = -1;
        int status = -1;

        while (true) {
            int[] packetsToSend;
            if (status == -1) {
                slowCount = slowCount == -1 ? 1 : Math.min(slowCount * 2, 4096);
                packetsToSend = getNextPackets(slowCount);
            } else {
                slowCount = 1;
                packetsToSend = new int[] { status };
                receivedAck[status] = 0;
            }

            if (packetsToSend == null) {
                break;
            }

            for (int i : packetsToSend) {
                try {
                    sendData(datagrams[i]);
                    this.log("Sending data " + i + "...");
                } catch (IOException e) {
                    this.log("Error occurred while sending data: " + e.getMessage());
                }
            }

            status = receiveAck();
            if (status == -2) {
                endClient();
                break;
            }
        }
        this.log("Upload completed!");
    }

    public int[] getNextPackets(int count) {
        /*
         * This method gets next packets to send to the server
         */

        for (int i = receivedAck.length - 1; i >= 0; i--) {
            if (receivedAck[i] != 0) {
                i++;
                int end = Math.min(count, receivedAck.length - i);
                int[] aux = new int[end];
                for (int j = 0; j < end; j++) {
                    aux[j] = i + j;
                }
                return aux;
            }
        }
        return receivedAck[0] == 0 ? new int[] { 0 } : null;
    }

    public int receiveAck() {
        /*
         * This method receives and interprets acknowledgements from the server
         */

        DatagramPacket getAck = new DatagramPacket(this.buffer, this.buffer.length);

        try {
            this.socket.setSoTimeout(100);

            while (true) {
                this.socket.receive(getAck);

                byte[] data = getAck.getData();
                for (int i = 0; i < data.length; i++) {
                    if (data[i] == 0) {
                        data = Arrays.copyOfRange(data, 0, i);
                    }
                }

                int ack = Integer.parseInt(new String(data));
                receivedAck[ack - 1]++;

                if (receivedAck.length == ack) {
                    return -2;
                }
                if (receivedAck[ack - 1] == 3) {
                    return ack;
                }
            }
        } catch (SocketException e) {
            this.log("Connection with server timed out, not receiving packets...");
        } catch (IOException e) {
            this.log("Error while receiving packets: " + e.getMessage());
        }
        return -1;
    }

    public void sendData(byte[] data) throws IOException {
        this.log("Sending data...");

        DatagramPacket sendPacket = new DatagramPacket(data, data.length, this.hostIP, this.hostPort);
        this.socket.send(sendPacket);
    }

    public void endClient() {
        byte[] data = "end".getBytes();
        DatagramPacket sendPacket = new DatagramPacket(data, data.length, this.hostIP, this.hostPort);
        
        try {
            this.socket.send(sendPacket);
        } catch (IOException e) {
            this.log("Error while sending end packet " + e);
        }
        
        socket.close();
    }

    public void log(String message) {
        /*
         * This method standardize logging style
         */

        System.out.print("[CLIENTE] ");
        System.out.println(message);
    }
}
