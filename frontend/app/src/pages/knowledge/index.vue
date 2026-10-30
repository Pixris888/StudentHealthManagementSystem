<template>
  <view class="page page-with-tab">
    <view class="search-row">
      <view class="search-box">
        <uni-icons type="search" size="17" color="#94a3b8" />
        <input
          v-model="keyword"
          class="search-input"
          placeholder="搜索健康知识"
          placeholder-class="input-placeholder"
          confirm-type="search"
          @confirm="handleSearch"
        />
        <view v-if="keyword" class="search-clear" @click="clearKeyword">
          <uni-icons type="clear" size="16" color="#b1bfba" />
        </view>
      </view>
      <view class="search-btn" @click="handleSearch">搜索</view>
    </view>

    <scroll-view scroll-x class="category-scroll" :show-scrollbar="false">
      <view class="category-row">
        <view
          class="category-chip"
          :class="{ active: categoryId === 0 }"
          @click="selectCategory(0)"
        >
          全部
        </view>
        <view
          v-for="category in categories"
          :key="category.id"
          class="category-chip"
          :class="{ active: categoryId === category.id }"
          @click="selectCategory(category.id)"
        >
          {{ category.categoryName }}
        </view>
      </view>
    </scroll-view>

    <view v-if="showHot && hotArticles.length" class="hot-section">
      <view class="section-head">
        <view>
          <text class="section-title">热门阅读</text>
          <text class="section-desc">同学们都在看的健康知识</text>
        </view>
      </view>
      <view class="hot-list">
        <view
          v-for="(article, index) in hotArticles"
          :key="article.id"
          class="hot-card"
          @click="openArticle(article.id)"
        >
          <view class="hot-rank" :class="`rank-${index + 1}`">{{ index + 1 }}</view>
          <view class="grow">
            <text class="hot-title text-clamp-2">{{ article.title }}</text>
            <view class="hot-meta">
              <text>{{ article.categoryName || '健康科普' }}</text>
              <text>·</text>
              <text>{{ article.viewCount }} 阅读</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view class="section-head">
      <view>
        <text class="section-title">{{ keyword ? `“${keyword}”的搜索结果` : '全部文章' }}</text>
        <text class="section-desc">共 {{ total }} 篇已发布文章</text>
      </view>
    </view>

    <view v-if="loading && !articles.length" class="card loading-card">
      <uni-load-more status="loading" color="#0f9d8a" />
    </view>

    <template v-else>
      <view
        v-for="article in articles"
        :key="article.id"
        class="card article-card"
        @click="openArticle(article.id)"
      >
        <view v-if="article.coverUrl" class="article-cover-wrap">
          <image
            :src="resolveFileUrl(article.coverUrl)"
            class="article-cover"
            mode="aspectFill"
          />
        </view>
        <view class="article-body">
          <view class="row" style="gap: 8rpx">
            <text class="tag tag-primary">{{ article.categoryName || '健康科普' }}</text>
            <text v-if="article.author" class="tag">{{ article.author }}</text>
          </view>
          <text class="article-title text-clamp-2">{{ article.title }}</text>
          <text v-if="article.summary" class="article-summary text-clamp-2">
            {{ article.summary }}
          </text>
          <view class="article-meta">
            <text>{{ formatDate(article.publishTime || article.createTime, 'YYYY-MM-DD') }}</text>
            <text>·</text>
            <text>{{ article.viewCount }} 次阅读</text>
            <view class="grow" />
            <text class="article-more">阅读全文</text>
            <uni-icons type="arrowright" size="12" color="#0f9d8a" />
          </view>
        </view>
      </view>

      <EmptyState
        v-if="!articles.length"
        icon="list"
        title="没有找到相关文章"
        desc="换个关键词或分类试试看"
        action-text="查看全部"
        @action="resetFilters"
      />

      <view v-if="articles.length" class="load-more">
        <uni-load-more
          :status="loading ? 'loading' : hasMore ? 'more' : 'noMore'"
          color="#94a3b8"
        />
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import {
  onLoad,
  onPullDownRefresh,
  onReachBottom,
  onUnload,
} from '@dcloudio/uni-app'
import EmptyState from '@/components/EmptyState.vue'
import {
  getHotArticles,
  listEnabledCategories,
  pagePublishedArticles,
} from '@/api/knowledge'
import { getAccessToken } from '@/utils/auth'
import { formatDate } from '@/utils/format'
import { resolveFileUrl } from '@/utils/url'
import type { ArticleVO, CategoryVO } from '@/types/knowledge'

const keyword = ref('')
const activeKeyword = ref('')
const categoryId = ref(0)
const categories = ref<CategoryVO[]>([])
const hotArticles = ref<ArticleVO[]>([])
const articles = ref<ArticleVO[]>([])
const page = ref(1)
const size = 10
const total = ref(0)
const loading = ref(false)
let searchTimer: ReturnType<typeof setTimeout> | null = null

const hasMore = computed(() => articles.value.length < total.value)
const showHot = computed(
  () => !activeKeyword.value && categoryId.value === 0 && hotArticles.value.length > 0,
)

function ensureLogin(): boolean {
  if (!getAccessToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return false
  }
  return true
}

async function loadCategories(): Promise<void> {
  try {
    const list = await listEnabledCategories()
    categories.value = flattenCategories(list ?? [])
  } catch {
    categories.value = []
  }
}

function flattenCategories(list: CategoryVO[]): CategoryVO[] {
  const result: CategoryVO[] = []
  const walk = (items: CategoryVO[], depth: number): void => {
    for (const item of items) {
      result.push({ ...item, categoryName: `${depth > 0 ? '· ' : ''}${item.categoryName}` })
      if (item.children?.length) walk(item.children, depth + 1)
    }
  }
  walk(list, 0)
  return result
}

async function loadHot(): Promise<void> {
  try {
    hotArticles.value = (await getHotArticles(3)) ?? []
  } catch {
    hotArticles.value = []
  }
}

async function loadArticles(reset = false): Promise<void> {
  if (loading.value) return
  if (!ensureLogin()) return
  if (reset) {
    page.value = 1
    total.value = 0
    articles.value = []
  }
  loading.value = true
  try {
    const result = await pagePublishedArticles({
      page: page.value,
      size,
      categoryId: categoryId.value || undefined,
      keyword: activeKeyword.value || undefined,
    })
    const list = result.records ?? []
    articles.value = reset ? list : [...articles.value, ...list]
    total.value = result.total ?? articles.value.length
  } catch {
    // 请求层已提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  activeKeyword.value = keyword.value.trim()
  loadArticles(true)
}

function clearKeyword(): void {
  keyword.value = ''
  if (activeKeyword.value) {
    activeKeyword.value = ''
    loadArticles(true)
  }
}

function selectCategory(id: number): void {
  if (categoryId.value === id) return
  categoryId.value = id
  loadArticles(true)
}

function resetFilters(): void {
  keyword.value = ''
  activeKeyword.value = ''
  categoryId.value = 0
  loadArticles(true)
}

function openArticle(id: number): void {
  uni.navigateTo({ url: `/pages/knowledge/detail?id=${id}` })
}

function loadMore(): void {
  if (loading.value || !hasMore.value) return
  page.value += 1
  loadArticles(false)
}

watch(keyword, () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    const value = keyword.value.trim()
    if (value !== activeKeyword.value) {
      activeKeyword.value = value
      loadArticles(true)
    }
  }, 450)
})

onLoad(async () => {
  if (!ensureLogin()) return
  await Promise.all([loadCategories(), loadHot()])
  await loadArticles(true)
})

onPullDownRefresh(async () => {
  await Promise.all([loadCategories(), loadHot(), loadArticles(true)])
  uni.stopPullDownRefresh()
})

onReachBottom(loadMore)

onUnload(() => {
  if (searchTimer) clearTimeout(searchTimer)
})
</script>

<style scoped>
.search-row {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.search-box {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12rpx;
  height: 82rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #ffffff;
  border: 2rpx solid #eaf1ee;
}

.search-input {
  flex: 1;
  height: 76rpx;
  font-size: 27rpx;
  color: #0f172a;
}

.search-clear {
  width: 44rpx;
  height: 44rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.search-btn {
  height: 82rpx;
  padding: 0 28rpx;
  border-radius: 999rpx;
  background: #0f9d8a;
  color: #ffffff;
  font-size: 26rpx;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
}

.category-scroll {
  width: 100%;
  margin-top: 22rpx;
  white-space: nowrap;
}

.category-row {
  display: inline-flex;
  align-items: center;
  gap: 12rpx;
  padding: 4rpx 0;
}

.category-chip {
  height: 58rpx;
  padding: 0 24rpx;
  border-radius: 999rpx;
  background: #ffffff;
  color: #64748b;
  font-size: 23rpx;
  display: inline-flex;
  align-items: center;
  border: 2rpx solid transparent;
}

.category-chip.active {
  background: #e6f7f3;
  border-color: #a8e5d9;
  color: #0f766e;
  font-weight: 600;
}

.hot-section {
  margin-top: 8rpx;
}

.hot-list {
  display: flex;
  flex-direction: column;
  gap: 14rpx;
}

.hot-card {
  display: flex;
  align-items: center;
  gap: 18rpx;
  padding: 22rpx 24rpx;
  border-radius: 22rpx;
  background: #ffffff;
  box-shadow: 0 8rpx 24rpx rgba(15, 118, 110, 0.05);
}

.hot-rank {
  width: 52rpx;
  height: 52rpx;
  border-radius: 16rpx;
  background: #f2f7f5;
  color: #94a3b8;
  font-size: 24rpx;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.hot-rank.rank-1 {
  background: #fff2e6;
  color: #ea580c;
}

.hot-rank.rank-2 {
  background: #eef4ff;
  color: #3b82f6;
}

.hot-rank.rank-3 {
  background: #f0fdf4;
  color: #16a34a;
}

.hot-title {
  font-size: 26rpx;
  font-weight: 600;
  line-height: 1.5;
  color: #1e293b;
}

.hot-meta {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}

.loading-card {
  min-height: 260rpx;
  display: flex;
  align-items: center;
  justify-content: center;
}

.article-card {
  padding: 0;
  overflow: hidden;
  margin-bottom: 20rpx;
}

.article-cover-wrap {
  width: 100%;
  height: 280rpx;
  background: #eef6f3;
}

.article-cover {
  width: 100%;
  height: 100%;
}

.article-body {
  padding: 24rpx 26rpx 26rpx;
}

.article-title {
  display: block;
  margin-top: 14rpx;
  font-size: 31rpx;
  font-weight: 700;
  line-height: 1.5;
  color: #0f172a;
}

.article-summary {
  display: block;
  margin-top: 10rpx;
  font-size: 23rpx;
  line-height: 1.65;
  color: #7b8f89;
}

.article-meta {
  display: flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 18rpx;
  font-size: 20rpx;
  color: #a8b7b2;
}

.article-more {
  color: #0f9d8a;
  font-size: 20rpx;
}

.load-more {
  padding: 10rpx 0 20rpx;
}
</style>
