package com.yll;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class SpecialToolTest {

    @Test
    public void testGenerateKey() {
        byte[] key = SpecialTool.generateKey();
        assertNotNull(key);
        assertEquals(32, key.length);
    }

    @Test
    public void testEncodeDecode() {
        String original = "123";
        String encoded = SpecialTool.encode(original.getBytes(StandardCharsets.UTF_8));
        String decoded = new String(SpecialTool.decode(encoded), StandardCharsets.UTF_8);
        assertEquals(original, decoded);
    }

    @Test
    public void testGetJavaExePath() {
        String javaExePath = SpecialTool.getJavaExePath();
        assertNotNull(javaExePath);
        assertFalse(javaExePath.trim().isEmpty());
    }
}
