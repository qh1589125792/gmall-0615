package com.atguigu.gmall.auth.config;

import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Slf4j
@Data
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /**
     *     publicKeyPath: D:\\develop\\Code\\workspace-idea\\tmp\\rsa.pub
     *     privateKeyPath: D:\\develop\\Code\\workspace-idea\\tmp\\rsa.pri
     *     secret: asf,./sa135%^%$
     *     expire: 180   #分期单位为分钟
     *     cookieName: GMALL_TOKEN
     */
    private String publicKeyPath;

    private String privateKeyPath;

    private String secret;

    private Integer expire;

    private String cookieName;

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @PostConstruct
    public void init(){

        try {
            //1.初始化公钥私钥文件
            File publicFile = new File(publicKeyPath);
            File privateFile = new File(privateKeyPath);

            //2.检查文件对象是否为空
            if(!publicFile.exists() || !privateFile.exists()){
                RsaUtils.generateKey(publicKeyPath,privateKeyPath,secret);
            }

            //3.读取密钥
            publicKey = RsaUtils.getPublicKey(publicKeyPath);
            privateKey = RsaUtils.getPrivateKey(privateKeyPath);
        } catch (Exception e) {
            log.error("初始化公钥和私钥失败！！！");
            e.printStackTrace();
        }
    }


}
