package cn.google;

import org.springframework.ai.chat.ChatResponse;
import reactor.core.publisher.Flux;

/**
 * AI服务接口
 * 定义了与AI模型交互的核心方法，支持同步和流式响应
 *
 */
public interface IAiService {

    /**
     * 生成AI回复（同步方式）
     * 
     * @param model AI模型名称，如 "deepseek-r1:1.5b" 或 "gpt-4o"
     * @param message 用户输入的消息
     * @return AI生成的回复
     */
    ChatResponse generate(String model, String message);

    /**
     * 生成AI回复（流式方式）
     * 
     * @param model AI模型名称
     * @param message 用户输入的消息
     * @return 流式AI回复
     */
    Flux<ChatResponse> generateStream(String model, String message);

    /**
     * 基于RAG（检索增强生成）的流式AI回复
     * 
     * @param model AI模型名称
     * @param ragTag 知识库标签，用于检索相关文档
     * @param message 用户输入的消息
     * @return 基于知识库的流式AI回复
     */
    Flux<ChatResponse> generateStreamRag(String model, String ragTag, String message);

}
