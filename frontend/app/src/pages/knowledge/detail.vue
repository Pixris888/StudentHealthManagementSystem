<template>
  <view class="page">
    <view v-if="loading" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <EmptyState
      v-else-if="error"
      icon="info"
      title="文章加载失败"
      :desc="error"
      action-text="返回"
      @action="goBack"
    />

    <template v-else-if="article">
      <view class="card article-head">
        <view class="row" style="gap: 8rpx">
          <text class="tag tag-primary">{{ article.categoryName || '健康科普' }}</text>
          <text v-if="article.author" class="tag">{{ article.author }}</text>
        </view>
        <text class="article-title">{{ article.title }}</text>
        <view class="article-meta">
          <text>{{ formatDate(article.publishTime || article.createTime, 'YYYY-MM-DD') }}</text>
          <text>·</text>
          <text>{{ article.viewCount }} 次阅读</text>
          <text v-if="article.source">· 来源：{{ article.source }}</text>
        </view>
      </view>

      <view v-if="article.coverUrl" class="card cover-card">
        <image
          :src="resolveFileUrl(article.coverUrl)"
          class="cover-image"
          mode="widthFix"
          @click="previewCover"
        />
      </view>

      <view v-if="article.summary" class="card summary-card">
        <text class="summary-label">内容摘要</text>
        <text class="summary-text">{{ article.summary }}</text>
      </view>

      <view class="card content-card">
        <text v-if="article.content" class="article-content" selectable>
          {{ article.content }}
        </text>
        <EmptyState
          v-else
          icon="list"
          title="暂无正文"
          desc="该文章还没有正文内容"
        />
      </view>

      <view class="article-footer">
        <view class="row" style="gap: 8rpx">
          <uni-icons type="info" size="14" color="#b1bfba" />
          <text>内容仅供健康科普参考，不能替代专业医疗建议</text>
        </view>
        <button class="btn btn-ghost btn-block back-btn" @click="goBack">
          返回知识列表
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import { getArticleDetail } from '@/api/knowledge'
import { getAccessToken } from '@/utils/auth'
import { formatDate } from '@/utils/format'
import { resolveFileUrl } from '@/utils/url'
import type { ArticleVO } from '@/types/knowledge'

const article = ref<ArticleVO | null>(null)
const loading = ref(true)
const error = ref('')

function goBack(): void {
  if (getCurrentPages().length > 1) {
    uni.navigateBack({ delta: 1 })
  } else {
    uni.switchTab({ url: '/pages/knowledge/index' })
  }
}

function previewCover(): void {
  if (!article.value?.coverUrl) return
  uni.previewImage({ urls: [resolveFileUrl(article.value.coverUrl)] })
}

async function load(id: number): Promise<void> {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loading.value = true
  error.value = ''
  try {
    article.value = await getArticleDetail(id)
  } catch (err) {
    error.value = (err as Error | null)?.message || '文章不存在或已下架'
  } finally {
    loading.value = false
  }
}

onLoad((options) => {
  const id = Number(options?.id)
  if (!id) {
    loading.value = false
    error.value = '缺少文章编号'
    return
  }
  load(id)
})
</script>

<style scoped>
.loading-card {
  min-height: 300rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-head {
  padding-bottom: 24rpx;
}

.article-title {
  display: block;
  margin-top: 18rpx;
  font-size: 42rpx;
  font-weight: 700;
  line-height: 1.45;
  color: #0f172a;
}

.article-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8rpx;
  margin-top: 18rpx;
  font-size: 21rpx;
  color: #94a3b8;
}

.cover-card {
  padding: 0;
  overflow: hidden;
}

.cover-image {
  width: 100%;
  display: block;
}

.summary-card {
  background: linear-gradient(120deg, #f1fbf8, #e8f8f2);
}

.summary-label {
  display: block;
  font-size: 21rpx;
  font-weight: 700;
  letter-spacing: 1rpx;
  color: #0f9d8a;
}

.summary-text {
  display: block;
  margin-top: 10rpx;
  font-size: 25rpx;
  line-height: 1.75;
  color: #417a70;
}

.content-card {
  padding: 30rpx 28rpx;
}

.article-content {
  display: block;
  font-size: 29rpx;
  line-height: 1.9;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.article-footer {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
  padding: 34rpx 8rpx 10rpx;
  font-size: 20rpx;
  color: #b1bfba;
}

.back-btn {
  height: 80rpx;
  font-size: 26rpx;
}
</style>
