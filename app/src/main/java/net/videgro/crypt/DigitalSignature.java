package net.videgro.crypt;

import android.content.res.Resources;
import android.util.Base64;
import android.util.Log;

import net.videgro.ships.R;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class DigitalSignature {
    private final static String TAG = "DigitalSignature";

    private Resources resources;

    public DigitalSignature(Resources resources) {
        this.resources = resources;
    }

    /**
     * Create a signature with the private key
     * @param data The data to sign
     * @return Base64 encoded signature
     */
    public String sign(final String data) {
        final String tag = "sign - ";

        String result = null;

        try {
            Signature rsa = Signature.getInstance(CryptConstants.ALGORITHM_SIGNATURE);
            final PrivateKey key=retrievePrivateKey();
            if (key!=null) {
                rsa.initSign(key);
                rsa.update(data.getBytes());
                result = Base64.encodeToString(rsa.sign(),Base64.DEFAULT);
            }
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, tag, e);
        }

        return result;
    }


    public boolean verify(final String data,final String base64encodedSignature) {
        final String tag="verify - ";
        boolean result=false;

        try {
            final Signature sig = Signature.getInstance(CryptConstants.ALGORITHM_SIGNATURE);
            sig.initVerify(retrievePublicKey());
            sig.update(data.getBytes());
            result = sig.verify(Base64.decode(base64encodedSignature,Base64.DEFAULT));
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException | IllegalArgumentException e) {
            Log.e(TAG, tag, e);
        }

        return result;
    }

    private PrivateKey retrievePrivateKey() {
        final String tag = "retrievePrivateKey - ";
        PrivateKey result = null;

        final byte[] keyBytes = retrieveKeyFromResources(R.raw.private_key);
        final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);

        try {
            final KeyFactory kf = KeyFactory.getInstance(CryptConstants.ALGORITHM_KEYS);
            result = kf.generatePrivate(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, tag, e);
        }
        return result;
    }

    private PublicKey retrievePublicKey(){
        final String tag = "retrievePublicKey - ";

        PublicKey result=null;
        final byte[] keyBytes = retrieveKeyFromResources(R.raw.public_key);
        final X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);

        try {
            final KeyFactory kf = KeyFactory.getInstance(CryptConstants.ALGORITHM_KEYS);
            result=kf.generatePublic(spec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            Log.e(TAG, tag, e);
        }
        return result;
    }

    private byte[] retrieveKeyFromResources(final int resourceId) {
        final String tag = "retrieveKeyFromResources - ";
        byte[] result = null;
        final InputStream inputStream = resources.openRawResource(resourceId);
        try {
            result = new byte[inputStream.available()];
            final int num = inputStream.read(result);
            Log.d(TAG, tag + num + "bytes read into buffer.");

        } catch (IOException e) {
            Log.e(TAG, tag + "Exception while reading inputstream", e);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, tag + "Exception while closing inputstream", e);
                }
            }
        }
        return result;
    }
}
