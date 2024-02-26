package com.github.novicezk.midjourney.util;

import cn.hutool.crypto.digest.DigestAlgorithm;
import cn.hutool.crypto.digest.Digester;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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

    private final OkHttpClient client = new OkHttpClient().newBuilder().build();

    /**
     * 图片URL转Base64编码
     * @param imgUrl 图片URL
     * @return Base64编码
     */
    public String generateImgUrlToBase64(String imgUrl) throws Exception {
        log.info("generateImgUrlToBase64:netImagePath->{}", imgUrl);
        int retry = 0;
        while (retry < RETRY_COUNT){
            try {
                Request request = new Request.Builder()
                        .url(imgUrl)
                        .method("GET", null)
                        .addHeader("Referer","https://cdn.midjourney.com")
                        .addHeader("Cookie", "__cf_bm=bzh36JR5qrmmtJMTbx5bXYjfGsF4sP6uN26LvgnRvQw-1706752711-1-AZHMsrMOdpOxoeTOsytV4Zgou+o3TWzLpmtKwQ2XHBnlUPpk/tuAEsGyiwIXJ0fz/7WAVFOZ/Ys2cWzwDkXfJy0=")
                        .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                        .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                        .addHeader("Accept-Encoding", "gzip, deflate, br")
                        .addHeader("Accept-Language", "zh-CN,zh;q=0.9,zh-TW;q=0.8,en;q=0.7,en-US;q=0.6")
                        .addHeader("Referer", "gzip, deflate, br")
                        .build();
                Response response = client.newCall(request).execute();
                InputStream inputStream = response.body().byteStream();
                return convertToPixelData(inputStream);
            } catch (Exception e) {
                log.warn("generateImgUrlToBase64:retry->{},netImagePath->{}", retry, imgUrl, e);
                retry++;
                Thread.sleep(2000);
            }
        }
        throw new RuntimeException("generateImgUrlToBase64:重试多次失败.netImagePath->"+imgUrl);
    }

    /**
     * 计算图片base64，只考虑像素不考虑图片信息
     */
    private static String convertToPixelData(InputStream is) throws IOException {
        BufferedImage image = ImageIO.read(is);
        // 获取图片的宽度和高度
        int width = image.getWidth();
        int height = image.getHeight();

        // 获取图片的像素类型
        int pixelType = image.getType() == BufferedImage.TYPE_BYTE_GRAY ? BufferedImage.TYPE_BYTE_GRAY : BufferedImage.TYPE_INT_ARGB;

        // 创建一个仅包含像素信息的BufferedImage
        BufferedImage pixelImage = new BufferedImage(width, height, pixelType);

        // 将原始图片的像素复制到新图片中
        pixelImage.getGraphics().drawImage(image, 0, 0, null);

        // 将图片转换为字节数组
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(pixelImage, "png", byteArrayOutputStream); // 使用PNG格式，因为它是无损的，并且通常比其他格式更小
        // 返回字节数组
        return Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
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
            String base64 = generateImgUrlToBase64(netImagePath);
            md5Hex = md5Digester.digestHex(base64);
            stringRedisTemplate.opsForValue().set(key, md5Hex);
            stringRedisTemplate.expire(key, 20, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.error("MD5转换异常", e);
        }
        log.info("MD5转换：{}->{}",netImagePath, md5Hex);
        return md5Hex;
    }
}
