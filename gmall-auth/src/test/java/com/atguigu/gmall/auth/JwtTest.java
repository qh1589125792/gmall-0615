package com.atguigu.gmall.auth;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.core.utils.RsaUtils;
import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
public class JwtTest {
	private static final String pubKeyPath = "D:\\develop\\Code\\workspace-idea\\tmp\\rsa.pub";

    private static final String priKeyPath = "D:\\develop\\Code\\workspace-idea\\tmp\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "1151as3faff,./,/^&fds");
    }

    @Before
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath);
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath);
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "qinhan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 5);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJxaW5oYW4iLCJleHAiOjE1NzM1MzUwMzZ9.Ai2kbDOCyhEE01pBrgfHiXvvRSXYWBqFmjanRB0PwC_4Lyj0rbb0kllCCSQwPStZmdHLXNirieku1fNCnrSpn6kTb62oIPM0xLfPo1UXVJVFtdcmqB7OdopflZfDpTIW4lxnBP8YZZzvOODNmMgarmng_PzsCwCuJ8TLumpJPevKA6bixND3BW4CW4Ro6B0vJbfYsaKJYUzed66eUlKXfiN0MDgxsWoMbgZRZLeWIqfyQRz9veOLY-fXn8k0bvrEJvoM6RhVmQxi3OlJYsseRYa-dX4Hnp_ze2fnLmAZdBYyXm9wya8DiyZd_C-dTBXdr7yWgqa-KAAN0wl9VflsxQ";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}