/*
 * COPYRIGHT Ericsson 2024
 *
 *
 *
 * The copyright to the computer program(s) herein is the property of
 *
 * Ericsson Inc. The programs may be used and/or copied only with written
 *
 * permission from Ericsson Inc. or in accordance with the terms and
 *
 * conditions stipulated in the agreement/contract under which the
 *
 * program(s) have been supplied.
 */
package com.ericsson.amcommonwfs.services.crypto;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import com.ericsson.am.shared.vnfd.service.CryptoService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.google.common.base.Strings;

/**
 * Helper Class to mimick CryptoService
 * Service to encrypt and decrypt sensitive data
 */
@Profile({ "dev", "test" })
@Service
public class DevCryptoService implements CryptoService {
    private static final String AES = "AES";
    private static final String SECRET = "secret-key-12345";

    private final Key key;
    private final Cipher cipher;
    private final Lock cipherInitializationLock;

    public DevCryptoService() throws NoSuchPaddingException, NoSuchAlgorithmException {
        key = new SecretKeySpec(SECRET.getBytes(), AES); // NOSONAR
        cipher = Cipher.getInstance(AES); // NOSONAR
        cipherInitializationLock = new ReentrantLock();
    }

    @Override
    public String encryptString(final String data) {
        if (Strings.isNullOrEmpty(data)) {
            return data;
        }
        try {
            cipherInitializationLock.lock();
            cipher.init(Cipher.ENCRYPT_MODE, key);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes())); // NOSONAR
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException e) {
            throw new IllegalStateException(e);
        } finally {
            cipherInitializationLock.unlock();
        }
    }

    @Override
    public String decryptString(final String data) {
        if (Strings.isNullOrEmpty(data)) {
            return data;
        }
        try {
            cipherInitializationLock.lock();
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.getDecoder().decode(data))); // NOSONAR
        } catch (InvalidKeyException | BadPaddingException | IllegalBlockSizeException e) {
            throw new IllegalStateException(e);
        } finally {
            cipherInitializationLock.unlock();
        }
    }
}