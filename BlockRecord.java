import java.io.Serializable;
import java.util.UUID;

// Class that will hold all the information for a single Block in the blockchain
// It is Serializable so that it can be passed around to different servers
class BlockRecord implements Serializable {
    String BlockID;
    String TimeStamp;
    String VerificationProcessID;
    String PreviousHash;
    UUID uuid;
    String Fname;
    String Lname;
    String SSNum;
    String DOB;
    String Diag;
    String Treat;
    String Rx;
    String RandomSeed; // The guess that we use each time we do work
    String WinningHash; // The hash we get when the work is complete and we combine with previous hash, current block data,
    // and the random guess
    String SignedSignature; // This is how we identify if the block has been modified, we sign with our private key
    String CreatingProcessID;

    /* Examples of accessors for the BlockRecord fields: */
    public String getBlockID() {return BlockID;}
    public void setBlockID(String BID){this.BlockID = BID;}

    public String getVerificationProcessID() {return VerificationProcessID;}
    public void setVerificationProcessID(String VID){this.VerificationProcessID = VID;}

    public String getPreviousHash() {return this.PreviousHash;}
    public void setPreviousHash (String PH){this.PreviousHash = PH;}

    public UUID getUUID() {return uuid;}
    public void setUUID (UUID ud){this.uuid = ud;}

    public String getLname() {return Lname;}
    public void setLname (String LN){this.Lname = LN;}

    public String getFname() {return Fname;}
    public void setFname (String FN){this.Fname = FN;}

    public String getSSNum() {return SSNum;}
    public void setSSNum (String SS){this.SSNum = SS;}

    public String getDOB() {return DOB;}
    public void setDOB (String RS){this.DOB = RS;}

    public String getDiag() {return Diag;}
    public void setDiag (String D){this.Diag = D;}

    public String getTreat() {return Treat;}
    public void setTreat (String Tr){this.Treat = Tr;}

    public String getRx() {return Rx;}
    public void setRx (String Rx){this.Rx = Rx;}

    public String getRandomSeed() {return RandomSeed;}
    public void setRandomSeed (String RS){this.RandomSeed = RS;}

    public String getWinningHash() {return WinningHash;}
    public void setWinningHash (String WH){this.WinningHash = WH;}

    public String getTimeStamp() {return TimeStamp;}
    public void setTimeStamp(String TS){this.TimeStamp = TS;}

    public String getSignedSignature() {return SignedSignature;}
    public void setSignedSignature(String signedSignature) {SignedSignature = signedSignature;}

    public String getCreatingProcessID() {
        return CreatingProcessID;
    }

    public void setCreatingProcessID(String creatingProcessID) {
        CreatingProcessID = creatingProcessID;
    }

    // Used to display or pass only Block data information as a string
    public String blockDataToString(String blocknumber){
        return "{BlockNumber='" + blocknumber + '\'' +
                ", ProcessID='" + CreatingProcessID + '\'' +
                ", Data='" + PreviousHash + uuid +
                "{Fname='" + Fname + '\'' +
                ", Lname='" + Lname + '\'' +
                ", SSNum='" + SSNum + '\'' +
                ", DOB='" + DOB + '\'' +
                ", Diag='" + Diag + '\'' +
                ", Treat='" + Treat + '\'' +
                ", Rx='" + Rx + '\'' +
                ", RandomSeed='" + RandomSeed + '\'' +"'}";
    }

    // Used to display or pass ALL information of the block
    @Override
    public String toString() {
        return "BlockRecord{" +
                "BlockID='" + BlockID + '\'' +
                ", TimeStamp='" + TimeStamp + '\'' +
                ", VerificationProcessID='" + VerificationProcessID + '\'' +
                ", PreviousHash='" + PreviousHash + '\'' +
                ", uuid=" + uuid +
                ", Fname='" + Fname + '\'' +
                ", Lname='" + Lname + '\'' +
                ", SSNum='" + SSNum + '\'' +
                ", DOB='" + DOB + '\'' +
                ", Diag='" + Diag + '\'' +
                ", Treat='" + Treat + '\'' +
                ", Rx='" + Rx + '\'' +
                ", RandomSeed='" + RandomSeed + '\'' +
                ", WinningHash='" + WinningHash + '\'' +
                ", CreatingProcessID=" + CreatingProcessID +'\'' +
                '}';
    }
}