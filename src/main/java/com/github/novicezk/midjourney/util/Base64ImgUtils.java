package com.github.novicezk.midjourney.util;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;

@Slf4j
@Component
public class Base64ImgUtils {
    /**
     * base64前缀
     */
    public static final String BASE64_PREFIX = "data:image/png;base64,";

    /**
     * 图片链接转为base64编码
     * @param netImagePath
     * @return
     */
    public String generateFileUrlToBase64(String netImagePath) throws Exception {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        // 创建URL
        URL url = new URL(netImagePath);
        final byte[] by = new byte[1024];
        // 创建链接
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(20 * 1000);


        InputStream is = conn.getInputStream();
        String stream2Base64 = this.inputStream2Base64(is);

        return stream2Base64;
    }

    /**
     * 图片链接转为MD5编码
     * @param netImagePath
     * @return
     */
    Digester md5Digester = new Digester(DigestAlgorithm.MD5);
    public String generateFileUrlToMd5(String netImagePath){
        String md5Hex = netImagePath;
        try {
            String base64 = generateFileUrlToBase64(netImagePath);
            md5Hex = md5Digester.digestHex(base64);
        } catch (Exception e) {
            log.error("MD5转换异常", e);
        }
        log.info("MD5转换：{}->{}",netImagePath,md5Hex);
        return md5Hex;
    }

    private String inputStream2Base64(InputStream is) throws Exception {
        byte[] data = null;
        try {
            ByteArrayOutputStream swapStream = new ByteArrayOutputStream();
            byte[] buff = new byte[100];
            int rc = 0;
            while ((rc = is.read(buff, 0, 100)) > 0) {
                swapStream.write(buff, 0, rc);
            }
            data = swapStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    throw new Exception("输入流关闭异常");
                }
            }
        }

        return Base64.getEncoder().encodeToString(data);
    }

}
