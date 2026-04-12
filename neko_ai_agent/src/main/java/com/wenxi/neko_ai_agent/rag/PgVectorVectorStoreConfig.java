//package com.wenxi.neko_ai_agent.rag;
//
//import jakarta.annotation.Resource;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.core.JdbcTemplate;
//
//import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
//import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;
//
///**
// * 基于 PGVectorVectorStore 的向量存储配置  （暂时不用）
// */
//@Configuration
//public class PgVectorVectorStoreConfig {
//
//    // 文档加载器
//    @Resource
//    private LoveAppDocumentLoader loveAppDocumentLoader;
//
//    @Bean
//    public VectorStore pgVectorVectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel dashScopeEmbeddingModel) {
//        PgVectorStore vectorStore = PgVectorStore.builder(jdbcTemplate, dashScopeEmbeddingModel)
//                .dimensions(1536)
//                .distanceType(COSINE_DISTANCE)
//                .indexType(HNSW)
//                .initializeSchema(true)
//                .schemaName("public")
//                .vectorTableName("vector_store")
//                .maxDocumentBatchSize(10000)
//                .build();
//        return vectorStore;
//    }
//
//}
