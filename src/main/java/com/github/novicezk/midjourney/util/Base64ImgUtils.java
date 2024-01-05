package com.github.novicezk.midjourney.util;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class Base64ImgUtils {
    /**
     * base64前缀
     */
    public static final String BASE64_PREFIX = "data:image/png;base64,";

    private static final int RETRY_COUNT = 4;

    private final StringRedisTemplate stringRedisTemplate;
    /**
     * 图片链接转为base64编码
     * @param netImagePath
     * @return
     */
    public String generateFileUrlToBase64(String netImagePath) throws Exception {
        log.info("generateFileUrlToBase64:netImagePath->{}", netImagePath);
        int retry = 0;
        while (retry < RETRY_COUNT){
            try {
                URL url = new URL(netImagePath);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10 * 1000);
                InputStream is = conn.getInputStream();
                return this.inputStream2Base64(is);
            } catch (Exception e) {
                log.warn("generateFileUrlToBase64:retry->{},netImagePath->{}", retry, netImagePath, e);
                retry++;
                Thread.sleep(1000);
            }
        }
        throw new RuntimeException("generateFileUrlToBase64:重试多次失败.netImagePath->"+netImagePath);
    }

    /**
     * 图片链接转为MD5编码
     * @param netImagePath
     * @return
     */
    private static final String REAL_URL_MD5_PREFIX = "real-url-md5:";
    Digester md5Digester = new Digester(DigestAlgorithm.MD5);
    public String generateFileUrlToMd5(String netImagePath){
        String key = REAL_URL_MD5_PREFIX + netImagePath;
        String md5HexRedis = stringRedisTemplate.opsForValue().get(key);
        if(StringUtils.isNotBlank(md5HexRedis)){
            return md5HexRedis;
        }
        String md5Hex = netImagePath;
        try {
            String base64 = generateFileUrlToBase64(netImagePath);
            md5Hex = md5Digester.digestHex(base64);
            stringRedisTemplate.opsForValue().set(key, md5Hex);
            stringRedisTemplate.expire(key, 20, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("MD5转换异常", e);
        }
        log.info("MD5转换：{}->{}",netImagePath, md5Hex);
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
