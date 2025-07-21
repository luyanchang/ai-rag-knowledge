# AI RAG知识库系统

## 项目简介

AI RAG知识库系统是一个基于Spring Boot的AI检索增强生成（RAG, Retrieval-Augmented Generation）知识库管理平台。系统支持多种AI模型集成、文档上传与向量化存储、Git仓库自动分析，以及基于知识库的智能问答，适用于企业级知识管理与智能问答场景。

## 技术栈
- **后端框架**：Spring Boot 3.2.3
- **AI框架**：Spring AI 0.8.1
- **AI模型**：Ollama（本地）、OpenAI（远程）
- **向量数据库**：PostgreSQL + pgvector
- **缓存**：Redis + Redisson
- **文档解析**：Apache Tika
- **Git操作**：JGit
- **构建工具**：Maven
- **容器化**：Docker + Docker Compose

## 项目结构
```
ai-rag-knowledge/
├── rag-api/      # API接口定义模块
├── rag-app/      # 应用主模块
├── rag-trigger/  # 控制器模块
└── docs/         # 部署和配置文档
```

### 模块说明
- **rag-api**：定义AI服务、RAG服务等接口及统一响应结构。
- **rag-app**：Spring Boot主应用及核心配置（AI模型、Redis等）。
- **rag-trigger**：Ollama、OpenAI、RAG等控制器，提供REST API。

## 核心流程
- **应用启动**：加载配置，初始化AI模型、向量数据库、Redis等。
- **文件上传/知识库构建**：解析文档、分割文本、向量化、存储、标签管理。
- **Git仓库分析**：克隆仓库、遍历文件、解析与存储知识。
- **AI对话与RAG问答**：支持普通AI对话与基于知识库的智能问答。

## 数据流
- 文档 → 解析 → 分割 → 向量化 → PostgreSQL存储
- 用户问题 → 向量化 → 相似度搜索 → 文档检索 → 上下文构建 → AI生成 → 回复

## 主要API接口
- `GET /api/v1/ollama/generate`：Ollama同步对话
- `GET /api/v1/ollama/generate_stream`：Ollama流式对话
- `GET /api/v1/ollama/generate_stream_rag`：Ollama RAG对话
- `GET /api/v1/openai/generate`：OpenAI同步对话
- `GET /api/v1/openai/generate_stream`：OpenAI流式对话
- `GET /api/v1/openai/generate_stream_rag`：OpenAI RAG对话
- `GET /api/v1/rag/query_rag_tag_list`：查询知识库标签
- `POST /api/v1/rag/file/upload`：上传文件到知识库
- `POST /api/v1/rag/analyze_git_repository`：分析Git仓库

## 部署架构
```
用户请求 → Nginx → Spring Boot应用 → AI模型/向量数据库/Redis
```
- 支持Docker Compose多环境部署
- Nginx前端代理
- 提供数据库初始化脚本

## 扩展性与优化
- **模块化设计**：接口与实现分离，便于扩展新AI模型
- **配置灵活**：支持多种AI模型和向量存储
- **插件化**：支持不同文档解析器和文本分割器
- **分布式支持**：可部署Redis和数据库集群
- **性能优化**：连接池、缓存、异步与批量处理

## 安全与合规
- API密钥管理
- CORS跨域配置
- 文件上传安全解析
- Git仓库认证支持

---
本系统为企业和开发者提供了完整的RAG知识库解决方案，支持高效的知识管理与智能问答，具备良好的扩展性和可维护性。 