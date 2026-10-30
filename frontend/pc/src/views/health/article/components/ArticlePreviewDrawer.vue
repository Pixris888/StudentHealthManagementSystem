<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { getManageArticleDetail } from '@/api/knowledge'
import { auditStatusTagType, articleStatusTagType } from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type { ArticleVO } from '@/types/knowledge'

const props = defineProps<{
  modelValue: boolean
  articleId?: number | null
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const loading = ref(false)
const article = ref<ArticleVO | null>(null)

async function loadArticle(): Promise<void> {
  if (!props.articleId) {
    article.value = null
    return
  }
  loading.value = true
  article.value = null
  try {
    article.value = await getManageArticleDetail(props.articleId)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

watch(
  () => [props.modelValue, props.articleId] as const,
  ([open]) => {
    if (open) loadArticle()
  },
)
</script>

<template>
  <el-drawer
    v-model="visible"
    title="文章预览"
    size="760px"
    append-to-body
    destroy-on-close
  >
    <div v-loading="loading" class="preview-body">
      <template v-if="article">
        <div class="preview-head">
          <div class="preview-tags">
            <el-tag
              :type="articleStatusTagType(article.status)"
              effect="light"
            >
              {{ article.statusName }}
            </el-tag>
            <el-tag
              :type="auditStatusTagType(article.auditStatus)"
              effect="plain"
            >
              {{ article.auditStatusName }}
            </el-tag>
            <el-tag v-if="article.categoryName" type="primary" effect="plain">
              {{ article.categoryName }}
            </el-tag>
          </div>
          <h2>{{ article.title }}</h2>
          <div class="preview-meta">
            <span>作者：{{ article.author || '—' }}</span>
            <span>发布时间：{{ formatDateTime(article.publishTime) }}</span>
            <span>浏览量：{{ article.viewCount ?? 0 }}</span>
          </div>
        </div>

        <el-alert
          v-if="article.auditStatus === 2 && article.auditRemark"
          :title="`审核不通过：${article.auditRemark}`"
          type="error"
          :closable="false"
          show-icon
          class="preview-alert"
        />

        <div v-if="article.summary" class="preview-summary">
          {{ article.summary }}
        </div>

        <img
          v-if="article.coverUrl"
          :src="article.coverUrl"
          alt=""
          class="preview-cover"
        />

        <div class="article-content">{{ article.content ?? '（暂无正文）' }}</div>

        <el-descriptions :column="2" border class="preview-info">
          <el-descriptions-item label="来源">
            {{ article.source || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="附件">
            <a
              v-if="article.docUrl"
              :href="article.docUrl"
              target="_blank"
              rel="noopener noreferrer"
            >
              查看附件
            </a>
            <span v-else>—</span>
          </el-descriptions-item>
          <el-descriptions-item label="创建时间">
            {{ formatDateTime(article.createTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDateTime(article.updateTime) }}
          </el-descriptions-item>
          <el-descriptions-item label="审核意见" :span="2">
            {{ article.auditRemark || '—' }}
          </el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">
            {{ article.remark || '—' }}
          </el-descriptions-item>
        </el-descriptions>
      </template>

      <el-empty
        v-else-if="!loading"
        description="未获取到文章内容"
        :image-size="90"
      />
    </div>
  </el-drawer>
</template>

<style scoped>
.preview-body {
  min-height: 240px;
}

.preview-head h2 {
  margin: 12px 0 10px;
  font-size: 24px;
  line-height: 1.45;
  color: var(--hp-text);
}

.preview-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preview-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 18px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
}

.preview-alert {
  margin: 18px 0 0;
}

.preview-summary {
  padding: 14px 16px;
  margin: 20px 0 0;
  font-size: 13.5px;
  line-height: 1.8;
  color: var(--hp-text-secondary);
  background: #f6faf8;
  border-left: 3px solid var(--hp-primary);
  border-radius: 0 10px 10px 0;
}

.preview-cover {
  width: 100%;
  max-height: 320px;
  margin-top: 20px;
  object-fit: cover;
  border-radius: 12px;
}

.article-content {
  padding: 24px 0;
  margin-top: 8px;
  font-size: 14.5px;
  line-height: 1.95;
  color: var(--hp-text);
  white-space: pre-wrap;
  word-break: break-word;
}

.preview-info {
  margin-top: 8px;
}

.preview-info :deep(.el-descriptions__label) {
  width: 96px;
  color: var(--hp-text-secondary);
}
</style>
