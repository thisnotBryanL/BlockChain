// Ports incremented by 1000 for each additional process added to the multicast group:
class Ports{
    public static int KeyServerPortBase = 4710;
    public static int UnverifiedBlockServerPortBase = 4820;
    public static int BlockchainServerPortBase = 4930;
    public static int BootServerPortBase = 5040;

    public static int KeyServerPort;
    public static int UnverifiedBlockServerPort;
    public static int BlockchainServerPort;
    public static int BootServerPort;

    // Sets port numbers for current process based on PID
    public void setPorts(){
        KeyServerPort = KeyServerPortBase + (BlockChain.PID * 1000);
        UnverifiedBlockServerPort = UnverifiedBlockServerPortBase + (BlockChain.PID * 1000);
        BlockchainServerPort = BlockchainServerPortBase + (BlockChain.PID * 1000);
        BootServerPort = BootServerPortBase + (BlockChain.PID * 1000);
        System.out.println("Ports for PID: " + BlockChain.PID + "\nKeyServer = " + KeyServerPort +
                "\nUVPort = " + UnverifiedBlockServerPort + "\nBlockServerPort = " + BlockchainServerPort +
                "\nBootStartPort = " + BootServerPort +"\n");
    }
}