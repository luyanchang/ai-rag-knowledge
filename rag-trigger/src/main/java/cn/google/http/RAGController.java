package cn.google.http;

import cn.google.IRAGService;
import cn.google.response.Response;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatClient;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.PgVectorStore;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.core.io.PathResource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/**
 * RAG（检索增强生成）控制器
 * 
 * 提供知识库管理的核心功能，包括：
 * 1. 知识库标签查询
 * 2. 文件上传和向量化
 * 3. Git仓库自动分析和知识提取
 *
 */
@Slf4j
@RestController()
@CrossOrigin("*")
@RequestMapping("/api/v1/rag/")
public class RAGController implements IRAGService {

    @Resource
    private OllamaChatClient ollamaChatClient;
    @Resource
    private TokenTextSplitter tokenTextSplitter;
    @Resource
    private SimpleVectorStore simpleVectorStore;
    @Resource
    private PgVectorStore pgVectorStore;
    @Resource
    private RedissonClient redissonClient;

    /**
     * 查询所有可用的知识库标签列表
     * 
     * 从Redis中获取所有已创建的知识库标签
     * 
     * @return 包含所有知识库标签的响应
     */
    @RequestMapping(value = "query_rag_tag_list", method = RequestMethod.GET)
    @Override
    public Response<List<String>> queryRagTagList() {
        RList<String> elements = redissonClient.getList("ragTag");
        return Response.<List<String>>builder()
                .code("0000")
                .info("调用成功")
                .data(elements)
                .build();
    }

    /**
     * 上传文件到指定知识库
     * 
     * 处理流程：
     * 1. 使用Tika文档读取器解析上传的文件
     * 2. 使用文本分割器将文档分割成适合向量化的片段
     * 3. 为每个文档片段添加知识库标签元数据
     * 4. 将文档片段存储到PostgreSQL向量数据库
     * 5. 在Redis中记录知识库标签
     * 
     * @param ragTag 知识库标签，用于标识和管理文档
     * @param files 要上传的文件列表
     * @return 上传操作的结果响应
     */
    @RequestMapping(value = "file/upload", method = RequestMethod.POST, headers = "content-type=multipart/form-data")
    @Override
    // @RequestParam("ragTag")：接收前端传来的知识库标签参数。 
    public Response<String> uploadFile(@RequestParam("ragTag") String ragTag, @RequestParam("file") List<MultipartFile> files) {
        log.info("上传知识库开始 {}", ragTag);
        
        for (MultipartFile file : files) {
            // 使用Tika解析文档内容 
            // 用TikaDocumentReader（基于Apache Tika）解析每个文件内容，自动识别多种文档格式（如PDF、Word、TXT等）。
            // files.parallelStream().forEach(file -> {
            //     TikaDocumentReader reader = new TikaDocumentReader(file.getResource());
            //     List<Document> docs = reader.get();
            //     // 后续处理
            // });

            TikaDocumentReader documentReader = new TikaDocumentReader(file.getResource());
            List<Document> documents = documentReader.get();  // documentReader.get()方法将文件内容读取为Document对象列表。
            
            // 使用文本分割器将文档分割成片段
            List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

            // 为原始文档和分割后的文档片段添加知识库标签
            documents.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));
            documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", ragTag));

            // 将文档片段存储到向量数据库 
            pgVectorStore.accept(documentSplitterList);

            // 在Redis中记录知识库标签
            RList<String> elements = redissonClient.getList("ragTag");
            if (!elements.contains(ragTag)) {
                elements.add(ragTag);
            }
        }

        log.info("上传知识库完成 {}", ragTag);
        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    /**
     * 分析Git仓库并提取知识
     * 
     * 处理流程：
     * 1. 克隆指定的Git仓库到本地临时目录
     * 2. 遍历仓库中的所有文件
     * 3. 使用Tika解析支持的文件格式
     * 4. 将解析的内容分割并向量化
     * 5. 存储到PostgreSQL向量数据库
     * 6. 清理临时文件
     * 7. 在Redis中记录知识库标签
     * 
     * @param repoUrl Git仓库URL
     * @param userName Git用户名
     * @param token Git访问令牌
     * @return 分析操作的结果响应
     * @throws Exception 当克隆或解析过程中出现错误时抛出
     */
    @RequestMapping(value = "analyze_git_repository", method = RequestMethod.POST)
    @Override
    public Response<String> analyzeGitRepository(@RequestParam("repoUrl") String repoUrl, @RequestParam("userName") String userName, @RequestParam("token") String token) throws Exception {
        String localPath = "./git-cloned-repo";
        String repoProjectName = extractProjectName(repoUrl);
        log.info("克隆路径：{}", new File(localPath).getAbsolutePath());

        // 清理之前的临时目录
        FileUtils.deleteDirectory(new File(localPath));

        // 克隆Git仓库
        Git git = Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(new File(localPath))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(userName, token))
                .call();

        // 遍历仓库中的所有文件
        Files.walkFileTree(Paths.get(localPath), new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                log.info("{} 遍历解析路径，上传知识库:{}", repoProjectName, file.getFileName());
                try {
                    // 使用Tika解析文件内容
                    TikaDocumentReader reader = new TikaDocumentReader(new PathResource(file));
                    List<Document> documents = reader.get();
                    
                    // 分割文档内容
                    List<Document> documentSplitterList = tokenTextSplitter.apply(documents);

                    // 为文档添加知识库标签
                    documents.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));
                    documentSplitterList.forEach(doc -> doc.getMetadata().put("knowledge", repoProjectName));

                    // 存储到向量数据库
                    pgVectorStore.accept(documentSplitterList);
                } catch (Exception e) {
                    log.error("遍历解析路径，上传知识库失败:{}", file.getFileName());
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.info("Failed to access file: {} - {}", file.toString(), exc.getMessage());
                return FileVisitResult.CONTINUE;
            }
        });

        // 清理临时目录
        FileUtils.deleteDirectory(new File(localPath));

        // 在Redis中记录知识库标签
        RList<String> elements = redissonClient.getList("ragTag");
        if (!elements.contains(repoProjectName)) {
            elements.add(repoProjectName);
        }

        git.close();

        log.info("遍历解析路径，上传完成:{}", repoUrl);

        return Response.<String>builder().code("0000").info("调用成功").build();
    }

    /**
     * 从Git仓库URL中提取项目名称
     * 
     * @param repoUrl Git仓库URL
     * @return 项目名称（不包含.git后缀）
     */
    private String extractProjectName(String repoUrl) {
        String[] parts = repoUrl.split("/");
        String projectNameWithGit = parts[parts.length - 1];
        return projectNameWithGit.replace(".git", "");
    }

}
