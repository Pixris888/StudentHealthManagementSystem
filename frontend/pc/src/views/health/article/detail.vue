<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowLeft,
  Calendar,
  Document,
  User,
  View,
} from '@element-plus/icons-vue'
import { getArticleDetail } from '@/api/knowledge'
import { formatDateTime } from '@/utils/format'
import type { ArticleVO } from '@/types/knowledge'

const route = useRoute()
const router = useRouter()

const articleId = computed(() => {
  const value = Array.isArray(route.params.id)
    ? route.params.id[0]
    : route.params.id
  return Number(value)
})

const article = ref<ArticleVO | null>(null)
const loading = ref(false)
const errorMessage = ref('')

async function loadArticle(): Promise<void> {
  if (!Number.isFinite(articleId.value) || articleId.value <= 0) {
    errorMessage.value = '文章地址无效'
    return
  }
  loading.value = true
  errorMessage.value = ''
  article.value = null
  try {
    const data = await getArticleDetail(articleId.value)
    article.value = data
    document.title = `${data.title} · 大学生健康管理平台`
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '文章不存在或已下架'
  } finally {
    loading.value = false
  }
}

watch(articleId, loadArticle, { immediate: true })

function goBack(): void {
  router.push({
    path: '/health/article',
    query: route.query.preview === '1' ? { preview: '1' } : undefined,
  })
}
</script>

<template>
  <div class="article-detail-page">
    <div class="detail-toolbar">
      <el-button text :icon="ArrowLeft" class="back-button" @click="goBack">
        返回知识库
      </el-button>
    </div>

    <div v-if="loading" class="panel-card detail-loading">
      <el-skeleton animated :rows="10" />
    </div>

    <div v-else-if="errorMessage" class="panel-card detail-error">
      <el-result
        icon="warning"
        title="文章无法查看"
        :sub-title="errorMessage"
      >
        <template #extra>
          <el-button type="primary" @click="goBack">返回知识库</el-button>
        </template>
      </el-result>
    </div>

    <template v-else-if="article">
      <article class="article-paper panel-card">
        <header class="paper-header">
          <div class="paper-tags">
            <el-tag v-if="article.categoryName" type="primary" effect="light">
              {{ article.categoryName }}
            </el-tag>
            <el-tag v-if="article.source" type="info" effect="plain">
              {{ article.source }}
            </el-tag>
          </div>

          <h1>{{ article.title }}</h1>

          <div class="paper-meta">
            <span>
              <el-icon><User /></el-icon>
              {{ article.author || '校医' }}
            </span>
            <span>
              <el-icon><Calendar /></el-icon>
              {{ formatDateTime(article.publishTime) }}
            </span>
            <span>
              <el-icon><View /></el-icon>
              {{ article.viewCount ?? 0 }} 次浏览
            </span>
          </div>

          <p v-if="article.summary" class="paper-summary">
            {{ article.summary }}
          </p>
        </header>

        <img
          v-if="article.coverUrl"
          :src="article.coverUrl"
          :alt="article.title"
          class="paper-cover"
        />

        <div class="article-content">{{ article.content }}</div>

        <footer class="paper-footer">
          <div v-if="article.source" class="source-line">
            <el-icon><Document /></el-icon>
            <span>来源：{{ article.source }}</span>
          </div>
          <a
            v-if="article.docUrl"
            :href="article.docUrl"
            target="_blank"
            rel="noopener noreferrer"
            class="doc-link"
          >
            <el-icon><Document /></el-icon>
            查看相关附件
          </a>

          <el-divider />

          <div class="paper-actions">
            <el-button :icon="ArrowLeft" @click="goBack">返回知识库</el-button>
          </div>
        </footer>
      </article>

      <p class="detail-hint">
        本文内容仅用于校园健康科普，不能替代专业医疗诊断。如有不适，请及时就医。
      </p>
    </template>
  </div>
</template>

<style scoped>
.article-detail-page {
  width: min(880px, 100%);
  margin: 0 auto;
}

.detail-toolbar {
  margin-bottom: 12px;
}

.back-button {
  padding-left: 0;
  color: var(--hp-text-secondary);
}

.back-button:hover {
  color: var(--hp-primary);
}

.detail-loading,
.detail-error {
  padding: 32px;
}

.article-paper {
  padding: 38px 46px 30px;
}

.paper-header {
  padding-bottom: 22px;
  border-bottom: 1px solid #edf3f0;
}

.paper-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.paper-header h1 {
  margin-bottom: 16px;
  font-size: clamp(24px, 2.8vw, 32px);
  line-height: 1.4;
  color: var(--hp-text);
  letter-spacing: 0.2px;
}

.paper-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px 20px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
}

.paper-meta span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.paper-summary {
  padding: 14px 16px;
  margin-top: 20px;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
  background: #f6faf8;
  border-left: 3px solid var(--hp-primary);
  border-radius: 0 10px 10px 0;
}

.paper-cover {
  width: 100%;
  max-height: 360px;
  margin-top: 26px;
  object-fit: cover;
  border-radius: 14px;
}

.article-content {
  padding: 30px 0 14px;
  font-size: 15px;
  line-height: 1.95;
  color: #233f37;
  white-space: pre-wrap;
  word-break: break-word;
}

.paper-footer {
  padding-top: 12px;
}

.source-line,
.doc-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-right: 18px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
}

.doc-link {
  color: var(--hp-primary);
}

.paper-actions {
  display: flex;
  justify-content: center;
}

.detail-hint {
  padding: 16px;
  margin-top: 16px;
  font-size: 11.5px;
  line-height: 1.7;
  color: var(--hp-text-muted);
  text-align: center;
}

@media (max-width: 680px) {
  .article-paper {
    padding: 24px 20px 20px;
  }

  .paper-header h1 {
    font-size: 23px;
  }
}
</style>
