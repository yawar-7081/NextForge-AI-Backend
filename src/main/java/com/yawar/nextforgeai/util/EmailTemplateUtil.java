package com.yawar.nextforgeai.util;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;

public class EmailTemplateUtil {

    public static String loadTemplate(String fileName) {
        try {
            ClassPathResource resource =
                    new ClassPathResource("templates/" + fileName);

            return StreamUtils.copyToString(
                    resource.getInputStream(),
                    StandardCharsets.UTF_8
            );
        } catch (Exception e) {
            throw new RuntimeException("Unable to load email template");
        }
    }
}
