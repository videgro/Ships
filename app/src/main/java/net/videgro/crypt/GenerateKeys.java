package net.videgro.crypt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class GenerateKeys {
    private final static String KEYS_DIRECTORY="app/src/main/res/raw/";
    private final static String KEY_PUBLIC=KEYS_DIRECTORY+"public_key";
    private final static String KEY_PRIVATE=KEYS_DIRECTORY+"private_key";

    private KeyPairGenerator keyGen;
    private PrivateKey privateKey;
    private PublicKey publicKey;

    private GenerateKeys(int keylength) throws NoSuchAlgorithmException {
        this.keyGen = KeyPairGenerator.getInstance(CryptConstants.ALGORITHM_KEYS);
        this.keyGen.initialize(keylength);
    }

    private void createKeys() {
        final KeyPair pair = keyGen.generateKeyPair();
        this.privateKey = pair.getPrivate();
        this.publicKey = pair.getPublic();
    }

    private PrivateKey getPrivateKey() {
        return this.privateKey;
    }

    private PublicKey getPublicKey() {
        return this.publicKey;
    }

    private void writeToFile(String path, byte[] key) throws IOException {
        final File f = new File(path);

        //noinspection ResultOfMethodCallIgnored
        f.getParentFile().mkdirs();

        FileOutputStream fos = new FileOutputStream(f);
        fos.write(key);
        fos.flush();
        fos.close();
    }

    public static void main(String[] args) {
        try {
            final GenerateKeys gk = new GenerateKeys(1024);
            gk.createKeys();
            gk.writeToFile(KEY_PUBLIC, gk.getPublicKey().getEncoded());
            gk.writeToFile(KEY_PRIVATE, gk.getPrivateKey().getEncoded());
        } catch (NoSuchAlgorithmException | IOException e) {
            System.err.println(e.getMessage());
        }
    }
}
