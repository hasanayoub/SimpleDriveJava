package tech.webclouds.simpledrivejava.helpers;

import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

@Service("encryptor")
public class Encryptor {
    private String algorithm;
    private SecretKey secretKey;

    public Encryptor() {
    }

    public Encryptor(String algorithm) {
        this.algorithm = algorithm;
    }

    public Encryptor(SecretKey secretKey) {
        this.secretKey = secretKey;
    }

    public Encryptor(String algorithm, SecretKey secretKey) {
        this.algorithm = algorithm;
        this.secretKey = secretKey;
    }

    public byte[] encrypt(byte[] plainBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        if (plainBytes == null) {
            return null;
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return cipher.doFinal(plainBytes);
    }

    public final String encrypt(String plain) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        byte[] plainPasswordBytes = plain.getBytes();
        byte[] encryptedPasswordBytes = encrypt(plainPasswordBytes);
        if (encryptedPasswordBytes == null)
            return null;
        return Base64.getEncoder().encodeToString(encryptedPasswordBytes);
    }

    public byte[] decrypt(byte[] encryptedBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return cipher.doFinal(encryptedBytes);
    }

    public final void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public final void setSecretKey(String secretCode) {
        byte[] decode = Base64.getDecoder().decode(secretCode.getBytes());
        this.secretKey = new SecretKeySpec(decode, algorithm);
    }

    public Optional<String> signData(String data) {
        try {
            return Optional.of(encrypt(data));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    public boolean validateSignature(String data, String providedSignature) {
        try {
            String originalSignature = encrypt(data);
            return originalSignature.equalsIgnoreCase(providedSignature);
        } catch (Exception ex) {
            return false;
        }
    }

}
