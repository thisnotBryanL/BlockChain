import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.*;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

/*
    Main class used to store the entire blockchain, including it's suporting methods and helper classes
 */
public class BlockChain {
    static int numProcesses = 3; // Number of processes we expect to get
    static int PID = 0; // Current process ID
    static String serverName = "localhost"; // Name of server that we will be running on
    static LinkedList<BlockRecord> blockChainLedger; // This is the main BlockChain that is shared between processes
    static List<BlockRecord> unverifiedList; // List that will contain all unverified blocks when we consume from files
    private static String FILENAME; // Name of the file we will be consuming from
    private static PrivateKey privateKey; // Stored Private key used to sign blocks that are completed
    private static PublicKeyWrapper publicKeyWrapper; // Stored Public Key
    private static volatile Boolean StartState = false; // Flag used to determine when to start the blockchain
    static List<PublicKeyWrapper> publicKeyList = new ArrayList<>(); // Used to store all known public keys from other processes

    /* Basically ENUMS used for better understanding of code when tokenizing*/
    private static final int FIRST_NAME = 0;
    private static final int LAST_NAME = 1;
    private static final int DOB = 2;
    private static final int SOCIAL_SEC = 3;
    private static final int DIAGNOSIS = 4;
    private static final int TREATMENT = 5;
    private static final int RX = 6;

    /*
        Class used to contain how to do work for each block along with helper functions
     */
    static class Work {
        public static String ByteArrayToString(byte[] ba){
            StringBuilder hex = new StringBuilder(ba.length * 2);
            for(int i=0; i < ba.length; i++){
                hex.append(String.format("%02X", ba[i]));
            }
            return hex.toString();
        }

        // Returns random String used for seeding
        public static String randomAlphaNumeric(int count) {
            StringBuilder builder = new StringBuilder();
            while (count-- != 0) {
                int character = (int)(Math.random()*ALPHA_NUMERIC_STRING.length());
                builder.append(ALPHA_NUMERIC_STRING.charAt(character));
            }
            return builder.toString();
        }

        // Pool of alphanumeric characters
        private static final String ALPHA_NUMERIC_STRING = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        static String someText = "one two three";
        static String randString;

        // Main Work function used to do work for each block
        public static void doWork(BlockRecord br) {
            int blockchainSize = blockChainLedger.size();
            String concatString = "";  // Combines existing block data with random seed string
            String stringOut = ""; // Contains the hash value result

            // Gets random string
            randString = randomAlphaNumeric((int)(Math.random()*ALPHA_NUMERIC_STRING.length()));

            // Sets random seed for current block
            br.setRandomSeed(randString);

            System.out.println("Starting work for block: " + br.getBlockID());
            int workNumber = 0;

            // This will begin the work, which will be getting a random string, taking the first 16 bits and comparing to
            // a target value, if reached then work is over, if not then re-do for set amount of times
            try {
                Random r = new Random();
                for(int i=1; i<40; i++){ // Set amount of time to guess

                    Thread.sleep((r.nextInt(9) * 100));
                    randString = randomAlphaNumeric(8); // Get random string

                    // Sets random seed for block
                    br.setRandomSeed(randString);

                    // Initialize block data as string
                    String stringIn = br.blockDataToString(Integer.toString(blockchainSize+1));

                    concatString = stringIn + randString; // Block data with our random seed

                    MessageDigest MD = MessageDigest.getInstance("SHA-256");
                    byte[] bytesHash = MD.digest(concatString.getBytes("UTF-8")); // Hash value in bytes

                    stringOut = ByteArrayToString(bytesHash); // Convert bytes to hex value string

                    // Retrieves first 16 bytes
                    workNumber = Integer.parseInt(stringOut.substring(0,4),16);

                    // Case where guess is wrong
                    if (!(workNumber < 14000)){
                    }

                    // Case where guess is correct
                    if (workNumber < 14000){
                        System.out.println("Work Complete. The seed (puzzle answer) is: " + randString);

                        // Sets the correct hash
                        br.setWinningHash(stringOut);

                        // SIGN THE BLOCKS DATA WITH ANSWER USING PRIVATE KEY
                        System.out.println("Signing winning hash of: " + stringOut);
                        br.setSignedSignature(createSignatureString(br,true));


                        // CHECK TO SEE IF BLOCKCHAIN HAS BEEN UPDATED,and CHECK IF BLOCK IS ALREADY IN LEDGER
                        if(blockChainLedger.size() != blockchainSize && findBlock(br.getBlockID())){
                            break;
                        }else{
                            // Gets last block in ledger
                            BlockRecord prevBlock = blockChainLedger.get(blockchainSize-1);

                            // Links previous correct hash to current block
                            br.setPreviousHash(prevBlock.getWinningHash());

                            // Used to skip first dummy block
                            int counter = 0;

                            // Verifies that all blocks have not been modified, if fails once, then all verified is false
                            boolean allVerified = false;
                            for(BlockRecord blockRecord :blockChainLedger){
                                if(counter > 0) {
                                    allVerified = isNotModified(blockRecord);
                                }
                                counter++;
                            }
                            System.out.println("Verifying if all previous blocks have not been modified..." + allVerified);

                            // Check signature if block has been modified, if not then can add
                            if(isNotModified(br)) {
                                // Set Process ID of process that has verified this block
                                br.setVerificationProcessID(Integer.toString(PID));

                                // Add to ledger
                                blockChainLedger.add(br);

                                // Sends Updated ledger to all other processes
                                multiCastVerifedBlock();

                                // Writes to Json file
                                if (PID == 0) {
                                    System.out.println("\nWriting blockchain with " + blockChainLedger.size() + " blocks to blockRecord.json..");
                                    Gson gson = new GsonBuilder().setPrettyPrinting().create();
                                    try (FileWriter writer = new FileWriter("blockRecord.json")) {
                                        gson.toJson(blockChainLedger, writer);
                                        writer.flush();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            break;
                        }
                    }
                }
            }catch(Exception ex) {ex.printStackTrace();}
        }
    }



    /*
        Used to replace old ledgers with new current ledger and output to console
     */
    static class BlockchainWorker extends Thread {
        Socket sock;
        BlockchainWorker (Socket s) {sock = s;}
        public void run(){
            LinkedList<BlockRecord> blockRecords = new LinkedList<>();
            try{
                // Object stream used to read in serialized data
                ObjectInputStream in = new ObjectInputStream(sock.getInputStream());

                // Deserialize blockchain
                blockRecords = (LinkedList<BlockRecord>) in.readObject();

                // Update Blockchain
                blockChainLedger = blockRecords;
                System.out.println("         --NEW BLOCKCHAIN--\n");

                Iterator<BlockRecord> iterator = blockChainLedger.iterator();
                int counter = 0;

                // Traverse new Blockchain and display new records
                while(iterator.hasNext()) {
                    BlockRecord tempRec = iterator.next();
                    if (counter > 0) {
                        System.out.println(("[(Block#" + tempRec.getBlockID() + " from P" + tempRec.getCreatingProcessID() + ") verified by P" + tempRec.getVerificationProcessID() + " at time "
                                + ThreadLocalRandom.current().nextInt(100, 1000) + "]\n"));
                    }
                    counter++;
                }
                sock.close();
            } catch (Exception x){x.printStackTrace();}
        }
    }

    /*
        Used to listen to new blockchains coming in
     */
    static class BlockchainServer implements Runnable {
        public void run(){
            int q_len = 6;
            Socket sock;
            System.out.println("Starting the Blockchain server input thread using " + Integer.toString(Ports.BlockchainServerPort));
            try{
                ServerSocket servsock = new ServerSocket(Ports.BlockchainServerPort, q_len);
                while (true) {
                    sock = servsock.accept();
                    new BlockchainWorker(sock).start();
                }
            }catch (IOException ioe) {System.out.println(ioe);}
        }
    }

    // Thread used to coordinate work for each block
    static class UnverifiedBlockConsumer implements Runnable {
        PriorityBlockingQueue<BlockRecord> queue; // Shared Priority queue
        UnverifiedBlockConsumer(PriorityBlockingQueue<BlockRecord> queue){
            this.queue = queue;
        }

        public void run(){
            BlockRecord tempRec;

            System.out.println("Starting the Unverified Block Priority Queue Consumer thread.\n");
            try{
                while(true){ // Blocking thread
                    tempRec = queue.take(); //Retrieve highest priority block
                    Work.doWork(tempRec);
                    Thread.sleep(1500);
                }
            }catch (Exception e) {System.out.println(e);}
        }
    }

    // Used to serach if block exists already in blockchain
    public static boolean findBlock(String blockID){
        Iterator<BlockRecord> iterator = blockChainLedger.iterator();
        while(iterator.hasNext()){
            BlockRecord tempRec = iterator.next();
            if(tempRec.getBlockID().equals(blockID)){
                return true;
            }
        }
        return false;
    }
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

    // Shared Queue for unverified blocks
    static final PriorityBlockingQueue<BlockRecord> ourPriorityQueue = new PriorityBlockingQueue<BlockRecord>(200, BlockTSComparator);


    // Get Private/Public Key pair
    public static KeyPair generateKeyPair(long seed) throws Exception {
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        SecureRandom rng = SecureRandom.getInstance("SHA1PRNG", "SUN");
        rng.setSeed(seed);
        keyGenerator.initialize(1024, rng);

        return (keyGenerator.generateKeyPair());
    }

    // Signs data
    public static byte[] signData(byte[] data, PrivateKey key) throws Exception {
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initSign(key);
        signer.update(data);
        return (signer.sign());
    }

    // Verifies signature
    public static boolean verifySig(byte[] data, PublicKey key, byte[] sig) throws Exception {
        Signature signer = Signature.getInstance("SHA1withRSA");
        signer.initVerify(key);
        signer.update(data);

        return (signer.verify(sig));
    }

    // Intialize Keypair
    public static void createKeys(){
        try {
            System.out.println("\nCreating keys..");
            // Random number generator I used from my JokeServer
            int randomNum = (int) (Math.random() * (1000));
            KeyPair keyPair = generateKeyPair(randomNum);
            privateKey = keyPair.getPrivate();
            byte[] bytePubKey = keyPair.getPublic().getEncoded();
            publicKeyWrapper = new PublicKeyWrapper(Base64.getEncoder().encodeToString(bytePubKey), PID);
            System.out.println("Process"+ PID +" Public Key: " + publicKeyWrapper.getPublicKeyString());
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    // Used to Start server by flipping start flag
    class BootWorker extends Thread { // Worker thread to process incoming public keys
        Socket sock; // Class member, socket, local to Worker.
        BootWorker (Socket s) {sock = s;} // Constructor, assign arg s to local sock
        public void run(){
            try{
                BufferedReader in = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                StartState = Boolean.parseBoolean(in.readLine ());
                sock.close();
            } catch (IOException x){x.printStackTrace();}
        }
    }

    // Listens to start server from Process 2
    class BootServer implements Runnable {
        public void run(){
            int q_len = 6;
            Socket sock;
            System.out.println("Starting Boot Server waiting for process 2... ");
            try{
                ServerSocket servsock = new ServerSocket(Ports.BootServerPort, q_len);
                while (true) {
                    sock = servsock.accept();
                    new BootWorker(sock).start();
                }
            }catch (IOException ioe) {System.out.println(ioe);}
        }
    }

    // Sends public key to all proccess
    public static void multiCastPublicKey(){
        Socket sock;
        PrintStream toServer;

        try{
            // Converts to JSON format
            Gson gson = new GsonBuilder().create();
            String json = gson.toJson(publicKeyWrapper);
            for(int i=0; i< numProcesses; i++){// Send our public key to all servers.
                sock = new Socket(serverName, Ports.KeyServerPortBase + (i * 1000));
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println(json);
                toServer.flush();
                sock.close();
            }
        }catch (Exception x) {x.printStackTrace ();}
    }

    // Sends START command to all processes
    public static void multiCastStartCommand(){
        Socket sock;
        PrintStream toServer;
        try{
            for(int i=0; i< numProcesses; i++){// Send our public key to all servers.
                sock = new Socket(serverName, Ports.BootServerPortBase + (i * 1000));
                toServer = new PrintStream(sock.getOutputStream());
                toServer.println("true");
                toServer.flush();
                sock.close();
            }
        }catch (Exception x) {x.printStackTrace ();}
    }


    // Sends new Blockchain to all processes
    public static void multiCastVerifedBlock (){ // Multicast our public key to the other processes
        Socket sock;
        ObjectOutputStream toServer = null;
        try{
            for(int i=0; i< numProcesses; i++){// Send our public key to all servers.
                sock = new Socket(serverName, Ports.BlockchainServerPortBase + (i * 1000));
                toServer = new ObjectOutputStream(sock.getOutputStream());
                toServer.writeObject(blockChainLedger);
                toServer.flush();
                sock.close();
            }
        }catch (Exception x) {x.printStackTrace ();}
    }

    // Sends unverified blocks to all processes
    public static void multiCastUnverifiedBlocks (){

        Socket UVBsock;
        BlockRecord tempRec;
        Random r = new Random();

        try{
            Iterator<BlockRecord> iterator = unverifiedList.iterator();

            ObjectOutputStream toServerOOS = null;
            for(int i = 0; i < numProcesses; i++){
                System.out.println("Sending UVBs to process " + i + "...");
                iterator = unverifiedList.iterator();
                while(iterator.hasNext()){
                    UVBsock = new Socket(serverName, Ports.UnverifiedBlockServerPortBase + (i * 1000));
                    toServerOOS = new ObjectOutputStream(UVBsock.getOutputStream());
                    Thread.sleep((r.nextInt(9) * 100)); // Sleep up to a second to randominze when sent.
                    tempRec = iterator.next();
                    toServerOOS.writeObject(tempRec);
                    toServerOOS.flush();
                    UVBsock.close();
                }
            }
            Thread.sleep((r.nextInt(9) * 100));
        }catch (Exception x) {x.printStackTrace ();}
    }

    // Creates signature in string format
    static String createSignatureString(BlockRecord blockRecord,boolean winningHash){
        String signature = "";
        try {
            StringBuffer data = new StringBuffer();
            data.append(blockRecord.getBlockID());

            MessageDigest md = MessageDigest.getInstance("SHA-256");
            if(winningHash) {
                md.update(blockRecord.getWinningHash().getBytes());
            }else{
                md.update(blockRecord.getBlockID().getBytes());
            }
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }

            String SHA256String = sb.toString();
            byte[] digitalSignature = signData(SHA256String.getBytes(), privateKey);
            signature = Base64.getEncoder().encodeToString(digitalSignature);

        }catch(Exception e){e.printStackTrace();}
        return signature;
    }


    // checks to see if signature has been broken and if the block has been modified
    static boolean isNotModified(BlockRecord block){
        boolean verified = false;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(block.getSignedSignature().getBytes());
            byte byteData[] = md.digest();
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < byteData.length; i++) {
                sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
            }
            String SHA256String = sb.toString();

            byte[] testSignature = Base64.getDecoder().decode(block.getSignedSignature());
            PublicKeyWrapper pk = null;

            // Was not able to set up fully and check for signature so we are checking by matching PID's to Creating proccesses
            for(PublicKeyWrapper pubKey : publicKeyList){
                if(pubKey.getPid() == Integer.parseInt(block.getCreatingProcessID())){
                    pk = pubKey;
                    verified = true;
                    break;
                }
            }
//            verified = verifySig(SHA256String.getBytes(), pk.getPublicKeyClass(), testSignature);
//            System.out.println("Has the signature been verified: " + verified + "\n");
        }catch(Exception e){
            e.printStackTrace();
        }
        return verified;
    }

    // Listens to new public keys
    class PublicKeyServer implements Runnable {

        public void run(){
            int q_len = 6;
            Socket keySock;
            System.out.println("Starting Key Server input thread using " + Integer.toString(Ports.KeyServerPort));
            try{
                ServerSocket servsock = new ServerSocket(Ports.KeyServerPort, q_len);
                while (true) {
                    keySock = servsock.accept();
                    new PublicKeyWorker (keySock).start();
                }
            }catch (IOException ioe) {System.out.println(ioe);}
        }
    }
    class PublicKeyWorker extends Thread { // Worker thread to process incoming public keys
        Socket keySock; // Class member, socket, local to Worker.
        PublicKeyWorker (Socket s) {keySock = s;} // Constructor, assign arg s to local sock
        public void run(){
            try{
                Gson gson = new Gson();
                BufferedReader in = new BufferedReader(new InputStreamReader(keySock.getInputStream()));
                String data = in.readLine ();
                PublicKeyWrapper publicKeyWrapper = gson.fromJson(data,PublicKeyWrapper.class);
                publicKeyList.add(publicKeyWrapper);
                System.out.println("Got key: " + publicKeyWrapper.publicKeyString);
                keySock.close();
            } catch (IOException x){x.printStackTrace();}
        }
    }

    // Reads in data from input files
    public static void consumeBlockData() {
        int pnum = 0;
        int UnverifiedBlockPort;
        int BlockChainPort;


        if (PID == 0) pnum = 0;
        if (PID == 1) pnum = 1;
        if (PID == 2) pnum = 2;

        UnverifiedBlockPort = 4820 + pnum;
        BlockChainPort = 4930 + pnum;


        switch(pnum){
            case 1: FILENAME = "/Users/TheBeast/Desktop/DePaul/CSC-435-Distributed-Systems-1/Blockchain_Assign3/src/BlockInput1.txt"; break;
            case 2: FILENAME = "/Users/TheBeast/Desktop/DePaul/CSC-435-Distributed-Systems-1/Blockchain_Assign3/src/BlockInput2.txt"; break;
            default: FILENAME= "/Users/TheBeast/Desktop/DePaul/CSC-435-Distributed-Systems-1/Blockchain_Assign3/src/BlockInput0.txt"; break;
        }
        System.out.println("Beginning to consume data from: " + FILENAME);

        try {
            BufferedReader br = new BufferedReader(new FileReader(FILENAME));
            String[] tokens = new String[10];
            String InputLineStr;
            String suuid;
            UUID idA;
            BlockRecord tempRec;
            StringWriter sw = new StringWriter();
            int n = 0;
            unverifiedList = new LinkedList<>();

            while ((InputLineStr = br.readLine()) != null) {
                BlockRecord BR = new BlockRecord(); // Careful

                /* CDE For the timestamp in the block entry: */
                try{Thread.sleep(1001);}catch(InterruptedException e){}
                Date date = new Date();
                //String T1 = String.format("%1$s %2$tF.%2$tT", "Timestamp:", date);
                String T1 = String.format("%1$s %2$tF.%2$tT", "", date);
                String TimeStampString = T1 + "." + pnum; // No timestamp collisions!
//                System.out.println("Timestamp: " + TimeStampString);
                BR.setTimeStamp(TimeStampString); // Will be able to priority sort by TimeStamp

                /* CDE: Generate a unique blockID. This would also be signed by creating process: */
                suuid = new String(UUID.randomUUID().toString());
                BR.setBlockID(suuid);

                /* CDE put the file data into the block record: */
                tokens = InputLineStr.split(" +"); // Tokenize the input
                BR.setFname(tokens[FIRST_NAME]);
                BR.setLname(tokens[LAST_NAME]);
                BR.setSSNum(tokens[SOCIAL_SEC]);
                BR.setDOB(tokens[DOB]);
                BR.setDiag(tokens[DIAGNOSIS]);
                BR.setTreat(tokens[TREATMENT]);
                BR.setRx(tokens[RX]);
                BR.setCreatingProcessID(Integer.toString(PID));
                BR.setSignedSignature(createSignatureString(BR,false));
                unverifiedList.add(BR);
                n++;
            }
            System.out.println(unverifiedList.size() + " records read.");

            Iterator<BlockRecord> iterator = unverifiedList.iterator();
            while(iterator.hasNext()){
                tempRec = iterator.next();
                System.out.println(tempRec.getTimeStamp() + " " + tempRec.getFname() + " " + tempRec.getLname());
            }

            multiCastUnverifiedBlocks();

        } catch(FileNotFoundException fileNotFoundException){
            System.err.println("ERROR - File handling: " + fileNotFoundException);
        }catch (Exception e) {e.printStackTrace();}

        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        try{Thread.sleep(5000);}catch(Exception e){}
    }
    public static void doWork(){
        int size = ourPriorityQueue.size();
        System.out.print("SIZE = " + size);
        for(int i =0; i < size; i++){
            BlockRecord result =  ourPriorityQueue.remove();
            Work.doWork(result);
        }
    }

    // Initialize dummy block
    public static BlockRecord getInitialBlock(){
        System.out.println("Initializing First Block...");
        Date date = new Date();
        String T1 = String.format("%1$s %2$tF.%2$tT", "", date);
        String TimeStampString = T1 + "." + PID;

        BlockRecord blockRecord = new BlockRecord();
        blockRecord.setBlockID(UUID.randomUUID().toString());
        blockRecord.setTimeStamp(TimeStampString);
        blockRecord.setVerificationProcessID("null");
        blockRecord.setPreviousHash("00000000000000000000");
        blockRecord.setUUID(UUID.randomUUID());
        blockRecord.setFname("null");
        blockRecord.setLname("null");
        blockRecord.setSSNum("null");
        blockRecord.setDOB("null");
        blockRecord.setDiag("null");
        blockRecord.setTreat("null");
        blockRecord.setRx("null");
        blockRecord.setRandomSeed("null");
        blockRecord.setWinningHash("00000000000000000000");
        blockRecord.setSignedSignature("null");
        blockRecord.setCreatingProcessID("null");

        return blockRecord;
    }

    public static void setPID(int PID) {
        BlockChain.PID = PID;
    }

    public static void main(String [] args){
        BlockChain blockChain = new BlockChain();
        blockChain.run(args);
    }

    public void run(String [] args){
        int pid = 0;
        if (args.length == 1){pid = Integer.parseInt(args[0]);}

        System.out.println("Process: " + pid);

        BlockChain.setPID(pid);
        BlockChain.createKeys();
        blockChainLedger = new LinkedList<>();
        blockChainLedger.add(getInitialBlock());

        // Start server and ports
        new Ports().setPorts();
        new Thread(new PublicKeyServer()).start();
        new Thread(new BootServer()).start();
        new Thread(new UnverifiedBlockServer(ourPriorityQueue)).start(); // New thread to process incoming unverified blocks
        new Thread(new BlockchainServer()).start(); // New thread to process incomming new blockchains

        // Simulate Proccess start at differnt times
        if(pid == 0) {try {Thread.sleep(3000);} catch (Exception e) {e.printStackTrace();}}
        if(pid == 1){try {Thread.sleep(4000);} catch (Exception e) {e.printStackTrace();}}
        if(pid == 2){try {Thread.sleep(7000);} catch (Exception e) {e.printStackTrace();}}

        // Send public keys to all proccess
        multiCastPublicKey();

        // Start blockchain server
        if(pid == 2) {
            System.out.println("Process 2 Boot up complete, sending Start command..");
            BlockChain.multiCastStartCommand();
        }

        // Keeps proccess waiting until blockchain server starts
        while (!StartState) {Thread.onSpinWait();}

        System.out.println("\nBlockchain STARTED\n");
        // Read in input files
        BlockChain.consumeBlockData();

        // Begin Work and creating blockchain
        UnverifiedBlockConsumer unverifiedBlockConsumer = new UnverifiedBlockConsumer(ourPriorityQueue);
        unverifiedBlockConsumer.run();
    }
}
