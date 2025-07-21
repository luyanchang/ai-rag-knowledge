package cn.google;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * AI RAG知识库应用主启动类
 * 
 * 这是一个基于Spring Boot的AI检索增强生成（RAG）知识库管理系统。
 * 系统支持：
 * 1. 多种AI模型集成（Ollama、OpenAI）
 * 2. 文档上传和向量化存储
 * 3. Git仓库自动分析和知识提取
 * 4. 基于知识库的智能问答
 *
 */
@SpringBootApplication
@Configurable
public class Application {

    /**
     * 应用程序入口点
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        SpringApplication.run(Application.class);
    }

}
