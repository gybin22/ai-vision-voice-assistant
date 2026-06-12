package com.example.assistant.service;

import com.example.assistant.exception.CostLimitExceededException;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Set;

@Service
public class ImagePreprocessService {
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final int MAX_LONG_SIDE = 1600;

    public void validateImage(byte[] imageBytes, String mimeType, Integer clientImageWidth, Integer clientImageHeight) {
        if (!ALLOWED_MIME_TYPES.contains(mimeType)) {
            throw new CostLimitExceededException("UNSUPPORTED_IMAGE_TYPE", "仅支持 JPEG、PNG 或 WebP 图片。");
        }

        if ("image/webp".equals(mimeType)) {
            // JDK 默认 ImageIO 不一定能读取 WebP。MVP 对 WebP 只做 MIME 与大小校验。
            return;
        }

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (image == null) {
                throw new CostLimitExceededException("INVALID_IMAGE", "图片文件无法解析。");
            }
            int width = image.getWidth();
            int height = image.getHeight();
            if (Math.max(width, height) > MAX_LONG_SIDE) {
                throw new CostLimitExceededException("IMAGE_DIMENSION_TOO_LARGE", "图片分辨率过高，请在前端压缩后再上传。");
            }
        } catch (CostLimitExceededException e) {
            throw e;
        } catch (Exception e) {
            throw new CostLimitExceededException("INVALID_IMAGE", "图片文件无法解析。");
        }
    }
}
