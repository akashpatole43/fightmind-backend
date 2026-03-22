package com.fightmind.chat.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChatRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    // Optional URL to Cloudinary if the user attached an image
    private String imageUrl;
}
