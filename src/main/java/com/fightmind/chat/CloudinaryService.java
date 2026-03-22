package com.fightmind.chat;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    /**
     * Uploads an image to Cloudinary and returns the secure URL.
     */
    public String uploadImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        
        try {
            log.debug("Uploading an image of size {} bytes to Cloudinary", file.getSize());
            Map uploadResult = cloudinary.uploader().upload(file.getBytes(), 
                ObjectUtils.asMap(
                    "folder", "fightmind_chat", 
                    "public_id", UUID.randomUUID().toString()
                ));
                
            return uploadResult.get("secure_url").toString();
            
        } catch (IOException e) {
            log.error("Failed to upload image to Cloudinary", e);
            throw new RuntimeException("Image upload failed", e);
        }
    }
}
