import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
    Wrapper class used to hold public key string and to make it easier to create public keys from string
 */
class PublicKeyWrapper{
    String publicKeyString;
    int pid;

    public PublicKeyWrapper(String publicKeyString, int pid) {
        this.publicKeyString = publicKeyString;
        this.pid = pid;
    }

    public String getPublicKeyString() {
        return publicKeyString;
    }

    public int getPid() {
        return pid;
    }

    public void setPid(int pid) {
        this.pid = pid;
    }

    public void setPublicKeyString(String publicKeyString) {
        this.publicKeyString = publicKeyString;
    }

    // Used to re-create public key from stored key string
    public PublicKey getPublicKeyClass(){
        byte[] bytePubkey2  = Base64.getDecoder().decode(publicKeyString);
        System.out.println("Key in Byte[] form again: " + bytePubkey2);
        PublicKey restoredKey = null;
        X509EncodedKeySpec pubSpec = new X509EncodedKeySpec(bytePubkey2);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            restoredKey = keyFactory.generatePublic(pubSpec);
        }catch(Exception e){
            e.printStackTrace();
        }
        return restoredKey;
    }
}