package com.atguigu.gmall.order.config;

import com.alipay.api.AlipayApiException;
import com.alipay.api.AlipayClient;
import com.alipay.api.DefaultAlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.atguigu.gmall.order.vo.PayVo;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Component
@Data
public class AlipayTemplate {

    //在支付宝创建的应用的id
    private   String app_id = "2016101200669649";

    // 商户私钥，您的PKCS8格式RSA2私钥
    private  String merchant_private_key = "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCgxNLFoe56aICMU8ayAFNWA7uK8mZ6Thdzhsf3f7deUl86iizQ+KZM/UI0nZtOP+3cW+jgrb8CeuHi/3b0o0WuiTZoFH6kIWypSfiBFoe13F4ZxcO/ME/EAhY/CmNG/IOJ11iZSRwwznD+8XH/J1gbRNiTk8VGwlkNYTcnI5d0Nq1Jnqkes0I4O+MjL0Ft1jZWl/GhGYODIVdDSPXhcYoic43FBXLzO++iKE0wk8Fx5z/lEZ1xNZIsxcM8AALvOyYYMMZ1MhvRyQu/sWuW18t1CUvMP9IHKAxRYHntkidKVBm4aGU+3BQBKii4yc5cxUDgu1BnnVzppcQc9Qj6bYe/AgMBAAECggEALM2tT5aJN2PYL0NJpGGi193Cbj6PgJYywudU4wyctZSPJWlMhBBHiD0sLi2eEniOeVLD25mQGcjVGsXHCPZXfQSQPQE5KdSGy8bpklANEgmPz+7lVmrIT/+k43ypnibRIaD59/3/9ToTE3mGI217iF3vXXFZERcoaioljKrGkoQZJ99w35yFT0jpde/tlM/5XplUINpz5xsE/tmxlOBcipyGlJU51WysyK1nFG6bOUP3kQ9CNfD34jPiWIT+8k+04uCfQ+G44XsivjYFd0MafXfZg6u74uYjh7A4UhyWP+X0ooPBxdmDzf9BB9ISJtLpsaIaiynqCrey0k4n3MacgQKBgQDT9WyjJmm7F45bqSM3oVtnJEBr5ZS/0OhWwMHsm0sihMZvcTf02k6dfvqqPEvdG6Gds723q5AfrNBY21P4LO8eZubtBDk/yLPQz3MGkzH/PuKTm9Fg+olrT8o+lyG2ccKTBCWnhJeXG1tliTiaKopdd9DHKOE2FKHt0qcZas8wRwKBgQDCLH3tUFkoUmC1IY3zh6oK+86uhLSUIT/lkltMWYWV/1RK1CROSp94MAr9pOxIsq5asYMsAbSA66pyY7V3/INSrGO43AuhkbeKMp2n/u4ZWeOQ5Kr/7RTzkRCRpcK90Y6oyTlfvMzuRWoUm4IJzB4syxdxZiSy7wFnljRzgCpgyQKBgQCABnCtBwm8ARlykfH8qxDLduEWiuBTD/sWU22O3an/WpWFQKycz7PGe6kqZCsqjYML/0Iri4wNJdPep/PLJlD0WxFGvlNaDH9YM8V1hjkfxDsVD3vjiNCyKcquDZlGWuY86LdZvMPXeKW26bBvphxXIoTPlwiZItNAFAnsV1B4BwKBgHPxlzwGV0aRcfeWUqnRH0MxuudQjHdLJ1lQvD1p0y+O5+lKpWcufeyJW3a7bO+36g5zoqy7U+oLTprBWRGNcWkDUQSIOFXAqgTWVneC7O+sUgspz8XD7lfGDku52//2SqYUibLWDNRWFJK7jLmG7Zm3p4bqBA3oU0FkWmuBY0ypAoGAG00RjGF5gF0RyPF9Pnrz+buVPAc1kmnIgTepQ7mbUOhXEEFamUQPVBD8utgEHHOWCyeAlV36s6E+Hge6Ttu1urd+F4N9M+HWWCIMoFm8y6bFCxtF8zJFnNSMara5hcvac6F4Hsru8ciuZBu7eLmSqbsYHvu6Me1iPCe7+4yS4Rk=";
    // 支付宝公钥,查看地址：https://openhome.alipay.com/platform/keyManage.htm 对应APPID下的支付宝公钥。
    private  String alipay_public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAgNxgADNI+FMaueyuSUtzvlP5KkTBiTwJx8kHL8Km3SyS7kZvZALt8XKc1/jiUr6SrogJoNTsrqnWddAfU/IBJkbMtLbeekChypi4koVMYllxBQdY3iimVgFMcPQpUx5H+MYW+9S8j0dTfk3qjZ/DoYmAfGEAbQjzVorN4C4d632YMGvtuWQEYIIsUFv4UZkWAt1JtaioGBRk5doGYZIrNfG4eg9VN3hs12bwSJWGNY0rxkUepx5VNG2t5A8jHmL6CzQ/U2zDymqOOYY1uu5KlWvzpQ63DEAEfEdQcY+d9sRqC6uA/D+F62Epm8QmBmiGfvJWHaQ51EdYuTh6xSoHYwIDAQAB";
    // 服务器[异步通知]页面路径  需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    // 支付宝会悄悄的给我们发送一个请求，告诉我们支付成功的信息
    private  String notify_url = "http://wrxrdj68t1.52http.net/api/order/pay/success";

    // 页面跳转同步通知页面路径 需http://格式的完整路径，不能加?id=123这类自定义参数，必须外网可以正常访问
    //同步通知，支付成功，一般跳转到成功页
    private  String return_url = null;

    // 签名方式
    private  String sign_type = "RSA2";

    // 字符编码格式
    private  String charset = "utf-8";

    // 支付宝网关； https://openapi.alipaydev.com/gateway.do
    private  String gatewayUrl = "https://openapi.alipaydev.com/gateway.do";

    public  String pay(PayVo vo) throws AlipayApiException {

        //AlipayClient alipayClient = new DefaultAlipayClient(AlipayTemplate.gatewayUrl, AlipayTemplate.app_id, AlipayTemplate.merchant_private_key, "json", AlipayTemplate.charset, AlipayTemplate.alipay_public_key, AlipayTemplate.sign_type);
        //1、根据支付宝的配置生成一个支付客户端
        AlipayClient alipayClient = new DefaultAlipayClient(gatewayUrl,
                app_id, merchant_private_key, "json",
                charset, alipay_public_key, sign_type);

        //2、创建一个支付请求 //设置请求参数
        AlipayTradePagePayRequest alipayRequest = new AlipayTradePagePayRequest();
        alipayRequest.setReturnUrl(return_url);
        alipayRequest.setNotifyUrl(notify_url);

        //商户订单号，商户网站订单系统中唯一订单号，必填
        String out_trade_no = vo.getOut_trade_no();
        //付款金额，必填
        String total_amount = vo.getTotal_amount();
        //订单名称，必填
        String subject = vo.getSubject();
        //商品描述，可空
        String body = vo.getBody();

        alipayRequest.setBizContent("{\"out_trade_no\":\""+ out_trade_no +"\","
                + "\"total_amount\":\""+ total_amount +"\","
                + "\"subject\":\""+ subject +"\","
                + "\"body\":\""+ body +"\","
                + "\"product_code\":\"FAST_INSTANT_TRADE_PAY\"}");

        String result = alipayClient.pageExecute(alipayRequest).getBody();

        //会收到支付宝的响应，响应的是一个页面，只要浏览器显示这个页面，就会自动来到支付宝的收银台页面
        System.out.println("支付宝的响应："+result);

        return result;

    }
}
