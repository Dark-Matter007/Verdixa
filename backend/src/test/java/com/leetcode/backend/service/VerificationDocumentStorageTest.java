package com.leetcode.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class VerificationDocumentStorageTest {
    @TempDir Path directory;

    @Test
    void storedDocumentIsEncryptedAndNotMeaningfullyReadableFromDisk() throws Exception {
        byte[] plain = "%PDF-1.7 private identity document".getBytes();
        VerificationDocumentStorage storage = new VerificationDocumentStorage(directory.toString(), key((byte) 1));
        var stored = storage.store(new MockMultipartFile("document", "identity.pdf", "application/pdf", plain));
        byte[] disk = Files.readAllBytes(directory.resolve(stored.name()));

        assertFalse(java.util.Arrays.equals(plain, disk));
        assertFalse(new String(disk).startsWith("%PDF"));
        assertArrayEquals(plain, storage.read(stored.name()));
    }

    @Test
    void pathTraversalIsRejected() {
        VerificationDocumentStorage storage = new VerificationDocumentStorage(directory.toString(), key((byte) 1));
        assertThrows(IllegalStateException.class, () -> storage.read("../outside.bin"));
        assertThrows(IllegalStateException.class, () -> storage.delete("../outside.bin"));
    }

    @Test
    void declaredMimeMustMatchMagicBytes() {
        VerificationDocumentStorage storage = new VerificationDocumentStorage(directory.toString(), key((byte) 1));
        var disguised = new MockMultipartFile("document", "identity.png", "image/png", "%PDF-1.7".getBytes());
        assertThrows(IllegalArgumentException.class, () -> storage.store(disguised));
    }

    @Test
    void replacingEncryptionKeyMakesExistingDocumentUnreadable() {
        byte[] plain = {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 1};
        VerificationDocumentStorage original = new VerificationDocumentStorage(directory.toString(), key((byte) 1));
        var stored = original.store(new MockMultipartFile("document", "identity.png", "image/png", plain));
        VerificationDocumentStorage rotated = new VerificationDocumentStorage(directory.toString(), key((byte) 2));
        assertThrows(IllegalStateException.class, () -> rotated.read(stored.name()));
    }

    private static String key(byte value) {
        byte[] bytes = new byte[32];
        java.util.Arrays.fill(bytes, value);
        return Base64.getEncoder().encodeToString(bytes);
    }
}
