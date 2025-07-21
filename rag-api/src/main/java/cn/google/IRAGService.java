package cn.google;

import cn.google.response.Response;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * RAG（检索增强生成）服务接口
 * 定义了知识库管理、文档上传和Git仓库分析的核心功能
 *
 */
public interface IRAGService {

    /**
     * 查询所有可用的知识库标签列表
     * 
     * @return 包含所有知识库标签的响应
     */
    Response<List<String>> queryRagTagList();

    /**
     * 上传文件到指定知识库
     * 
     * @param ragTag 知识库标签，用于标识和管理文档
     * @param files 要上传的文件列表
     * @return 上传操作的结果响应
     */
    Response<String> uploadFile(String ragTag, List<MultipartFile> files);

    /**
     * 分析Git仓库并提取知识
     * 克隆指定的Git仓库，解析其中的文档文件，并将内容存储到向量数据库中
     * 
     * @param repoUrl Git仓库URL
     * @param userName Git用户名
     * @param token Git访问令牌
     * @return 分析操作的结果响应
     * @throws Exception 当克隆或解析过程中出现错误时抛出
     */
    Response<String> analyzeGitRepository(String repoUrl, String userName, String token) throws Exception;

}
