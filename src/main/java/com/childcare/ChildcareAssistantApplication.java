package com.childcare;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChildcareAssistantApplication {
    
    public static void main(String[] args) {
        // Load environment variables from .env file
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));

        // 디버깅: 로드된 DB_URL 확인 (비밀번호 등 민감 정보는 제외하고 URL만 확인)
        String dbUrl = System.getProperty("DB_URL");
        System.out.println("==================================================");
        System.out.println("DEBUG: Loaded DB_URL from .env: " + dbUrl);
        System.out.println("==================================================");

        SpringApplication.run(ChildcareAssistantApplication.class, args);
    }
}