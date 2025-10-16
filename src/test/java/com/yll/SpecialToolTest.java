package com.yll;

import junit.framework.TestCase;

public class SpecialToolTest extends TestCase {

    public void testGenerateKey() {
        String key = new String(SpecialTool.generateKey());
        System.out.println("key ---> " + key);
    }

    public void testEncode() {
        String encode = SpecialTool.encode("123".getBytes());
        System.out.println("123 ---> " + encode);
    }

    public void testDecode() {
        String encode = SpecialTool.encode("123".getBytes());
        String decode = new String(SpecialTool.decode(encode));
        System.out.println(encode+" ---> " + decode);
    }

    public void testGetJavaExePath() {
        System.out.println("java.home ---> " + SpecialTool.getJavaExePath());
    }
}