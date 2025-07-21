package cn.google.http;

import cn.google.IAiService;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.openai.OpenAiChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OpenAI AI模型控制器
 * 
 * 提供基于OpenAI远程模型的AI对话服务，支持：
 * 1. 同步对话生成
 * 2. 流式对话生成
 * 3. 基于RAG的智能问答
 *
 */
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/openai/")
public class OpenAiController implements IAiService {

    @Resource
    private OpenAiChatClient chatClient;
    @Resource
    private PgVectorStore pgVectorStore;

    /**
     * 同步生成AI回复
     * 
     * 使用指定的OpenAI模型生成对用户消息的回复
     * 
     * @param model OpenAI模型名称，如 "gpt-4o"
     * @param message 用户输入的消息
     * @return AI生成的回复
     */
    @RequestMapping(value = "generate", method = RequestMethod.GET)
    @Override
    public ChatResponse generate(@RequestParam("model") String model, @RequestParam("message") String message) {
        return chatClient.call(new Prompt(
                message,
                OpenAiChatOptions.builder()
                        .withModel(model)
                        .build()
        ));
    }

    /**
     * 流式生成AI回复
     * 
     * 使用指定的OpenAI模型流式生成对用户消息的回复
     * 
     * @param model OpenAI模型名称
     * @param message 用户输入的消息
     * @return 流式AI回复
     * 
     * @example curl http://localhost:8090/api/v1/openai/generate_stream?model=gpt-4o&message=1+1
     */
    @RequestMapping(value = "generate_stream", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStream(@RequestParam("model") String model, @RequestParam("message") String message) {
        return chatClient.stream(new Prompt(
                message,
                OpenAiChatOptions.builder()
                        .withModel(model)
                        .build()
        ));
    }

    /**
     * 基于RAG的流式AI回复生成
     * 
     * 结合知识库检索和AI生成，提供更准确的回答。
     * 流程：
     * 1. 根据用户问题在指定知识库中检索相关文档
     * 2. 将检索到的文档作为上下文提供给AI模型
     * 3. 生成基于知识库的回复
     * 
     * @param model OpenAI模型名称
     * @param ragTag 知识库标签，用于检索相关文档
     * @param message 用户输入的消息
     * @return 基于知识库的流式AI回复
     */
    @RequestMapping(value = "generate_stream_rag", method = RequestMethod.GET)
    @Override
    public Flux<ChatResponse> generateStreamRag(@RequestParam("model") String model, @RequestParam("ragTag") String ragTag, @RequestParam("message") String message) {

        // 系统提示词模板，要求AI基于文档内容回答，并使用中文回复
        String SYSTEM_PROMPT = """
                Use the information from the DOCUMENTS section to provide accurate answers but act as if you knew this information innately.
                If unsure, simply state that you don't know.
                Another thing you need to note is that your reply must be in Chinese!
                DOCUMENTS:
                    {documents}
                """;

        // 在指定知识库中检索相关文档
        SearchRequest request = SearchRequest.query(message)
                .withTopK(5)  // 检索前5个最相关的文档
                .withFilterExpression("knowledge == '" + ragTag + "'");  // 过滤指定知识库

        List<Document> documents = pgVectorStore.similaritySearch(request);
        String documentCollectors = documents.stream().map(Document::getContent).collect(Collectors.joining());
        
        // 创建系统消息，包含检索到的文档内容
        Message ragMessage = new SystemPromptTemplate(SYSTEM_PROMPT).createMessage(Map.of("documents", documentCollectors));

        // 构建消息列表：用户消息 + 系统消息（包含文档上下文）
        List<Message> messages = new ArrayList<>();
        messages.add(new UserMessage(message));
        messages.add(ragMessage);

        // 使用OpenAI模型生成基于知识库的回复
        return chatClient.stream(new Prompt(
                messages,
                OpenAiChatOptions.builder()
                        .withModel(model)
                        .build()
        ));
    }

}