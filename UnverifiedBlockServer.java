import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.BlockingQueue;

/*
    This class will be the server that listens to when an unverified block is multicasted and add the block to the
    local queue
 */
class UnverifiedBlockServer implements Runnable {
    BlockingQueue<BlockRecord> queue;
    UnverifiedBlockServer(BlockingQueue<BlockRecord> queue){
        this.queue = queue;
    }

    // Used to compare blockrecords in the priority queue based on timestamp
    public static Comparator<BlockRecord> BlockTSComparator = new Comparator<BlockRecord>()
    {
        @Override
        public int compare(BlockRecord b1, BlockRecord b2)
        {
            String s1 = b1.getTimeStamp();
            String s2 = b2.getTimeStamp();
            if (s1 == s2) {return 0;}
            if (s1 == null) {return -1;}
            if (s2 == null) {return 1;}
            return s1.compareTo(s2);
        }
    };

    /* Worker thread that reads in unverified blocks and adds to our priority queue */
    class UnverifiedBlockWorker extends Thread {
        Socket sock;
        UnverifiedBlockWorker (Socket s) {sock = s;}

        // Initialize
        BlockRecord BR = new BlockRecord();

        public void run(){
            try{
                // Create object stream to read in serialized BlockRecords
                ObjectInputStream unverifiedIn = new ObjectInputStream(sock.getInputStream());

                // Deserialize Blockrecord
                BR = (BlockRecord) unverifiedIn.readObject(); // Read in the UVB as an object
                System.out.println("Received UVB: " + BR.getTimeStamp() + " " + BR.getFname() + " " + BR.getLname() + " " + BR.getBlockID());

                // Inserts Blockrecord into shared queue
                queue.put(BR);
                sock.close();
            } catch (Exception x){x.printStackTrace();}
        }
    }

    // Starts UnverifiedBlockserver listening
    public void run(){
        int q_len = 6; /* Number of requests for OpSys to queue */
        Socket sock;
        System.out.println("Starting the Unverified Block Server input thread using " +
                Integer.toString(Ports.UnverifiedBlockServerPort));
        try{
            // Creates sockets with ports respective of current process
            ServerSocket UVBServer = new ServerSocket(Ports.UnverifiedBlockServerPort, q_len);
            while (true) {
                // Retrieves block when port is hit
                sock = UVBServer.accept();

                // Thread start
                new UnverifiedBlockServer.UnverifiedBlockWorker(sock).start();
            }
        }catch (IOException ioe) {System.out.println(ioe);}
    }
}