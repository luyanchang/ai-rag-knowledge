package cn.google.config;

import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.ollama.OllamaEmbeddingClient;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.OpenAiEmbeddingClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * AI模型配置类
 * 
 * 负责配置和管理各种AI模型客户端，包括：
 * 1. Ollama本地模型客户端
 * 2. OpenAI远程模型客户端
 * 3. 文本分割器
 * 4. 向量存储（简单存储和PostgreSQL向量存储）
 *
 */
@Configuration
public class OllamaConfig {

    /**
     * 配置Ollama API客户端
     * 
     * @param baseUrl Ollama服务的基础URL
     * @return OllamaApi实例
     */
    @Bean
    public OllamaApi ollamaApi(@Value("${spring.ai.ollama.base-url}") String baseUrl) {
        return new OllamaApi(baseUrl);
    }

    /**
     * 配置OpenAI API客户端
     * 
     * @param baseUrl OpenAI服务的基础URL
     * @param apikey OpenAI API密钥
     * @return OpenAiApi实例
     */
    @Bean
    public OpenAiApi openAiApi(@Value("${spring.ai.openai.base-url}") String baseUrl, @Value("${spring.ai.openai.api-key}") String apikey) {
        return new OpenAiApi(baseUrl, apikey);
    }

    /**
     * 配置Ollama聊天客户端
     * 
     * @param ollamaApi Ollama API实例
     * @return OllamaChatClient实例
     */
    @Bean
    public OllamaChatClient ollamaChatClient(OllamaApi ollamaApi) {
        return new OllamaChatClient(ollamaApi);
    }

    /**
     * 配置文本分割器
     * 用于将长文档分割成适合向量化的片段
     * 
     * @return TokenTextSplitter实例
     */
    @Bean
    public TokenTextSplitter tokenTextSplitter() {
        return new TokenTextSplitter();
    }

    /**
     * 配置简单向量存储
     * 根据配置的嵌入模型选择使用Ollama或OpenAI的嵌入客户端
     * 
     * @param model 嵌入模型名称
     * @param ollamaApi Ollama API实例
     * @param openAiApi OpenAI API实例
     * @return SimpleVectorStore实例
     */
    @Bean
    public SimpleVectorStore vectorStore(@Value("${spring.ai.rag.embed}") String model, OllamaApi ollamaApi, OpenAiApi openAiApi) {
        if ("nomic-embed-text".equalsIgnoreCase(model)) {
            // 使用Ollama的nomic-embed-text模型进行文本嵌入
            OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
            embeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
            return new SimpleVectorStore(embeddingClient);
        } else {
            // 使用OpenAI的嵌入模型
            OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(openAiApi);
            return new SimpleVectorStore(embeddingClient);
        }
    }

    /**
     * 配置PostgreSQL向量存储
     * 提供持久化的向量存储能力，支持复杂的查询和过滤
     * 
     * @param model 嵌入模型名称
     * @param ollamaApi Ollama API实例
     * @param openAiApi OpenAI API实例
     * @param jdbcTemplate JDBC模板
     * @return PgVectorStore实例
     */
    @Bean
    public PgVectorStore pgVectorStore(@Value("${spring.ai.rag.embed}") String model, OllamaApi ollamaApi, OpenAiApi openAiApi, JdbcTemplate jdbcTemplate) {
        if ("nomic-embed-text".equalsIgnoreCase(model)) {
            // 使用Ollama的nomic-embed-text模型
            OllamaEmbeddingClient embeddingClient = new OllamaEmbeddingClient(ollamaApi);
            embeddingClient.withDefaultOptions(OllamaOptions.create().withModel("nomic-embed-text"));
            return new PgVectorStore(jdbcTemplate, embeddingClient);
        } else {
            // 使用OpenAI的嵌入模型
            OpenAiEmbeddingClient embeddingClient = new OpenAiEmbeddingClient(openAiApi);
            return new PgVectorStore(jdbcTemplate, embeddingClient);
        }
    }

}
