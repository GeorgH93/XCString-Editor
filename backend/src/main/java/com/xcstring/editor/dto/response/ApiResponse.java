package com.xcstring.editor.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private boolean success;
    private String error;

    public static ApiResponse ok() {
        ApiResponse r = new ApiResponse();
        r.success = true;
        return r;
    }

    public static ApiResponse error(String message) {
        ApiResponse r = new ApiResponse();
        r.success = false;
        r.error = message;
        return r;
    }
}
