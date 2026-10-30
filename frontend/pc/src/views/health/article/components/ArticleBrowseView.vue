<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Reading,
  Search,
  View,
} from '@element-plus/icons-vue'
import {
  getEnabledCategoryTree,
  getHotArticles,
  pagePublishedArticles,
} from '@/api/knowledge'
import { formatDate } from '@/utils/format'
import type { ArticleVO, CategoryVO } from '@/types/knowledge'

interface FlatCategory {
  id: number
  label: string
  depth: number
  count: number
}

const route = useRoute()
const router = useRouter()

const categories = ref<CategoryVO[]>([])
const hotArticles = ref<ArticleVO[]>([])
const records = ref<ArticleVO[]>([])
const total = ref(0)
const pages = ref(0)
const loading = ref(false)
const hotLoading = ref(false)
const activeCategoryId = ref<number | undefined>(undefined)
const keyword = ref('')
const query = reactive({ page: 1, size: 10 })
const brokenCovers = reactive(new Set<number>())
const listAnchor = ref<HTMLElement>()

const flatCategories = computed(() => {
  const result: FlatCategory[] = []
  const walk = (nodes: CategoryVO[], depth: number) => {
    for (const node of nodes) {
      result.push({
        id: node.id,
        label: node.categoryName,
        depth,
        count: node.articleCount ?? 0,
      })
      if (node.children?.length) {
        walk(node.children, depth + 1)
      }
    }
  }
  walk(categories.value, 0)
  return result
})

const publishedTotal = computed(() =>
  categories.value.reduce((sum, item) => sum + (item.articleCount ?? 0), 0),
)

async function loadCategories(): Promise<void> {
  try {
    categories.value = await getEnabledCategoryTree()
  } catch {
    // 请求错误由拦截器统一提示
  }
}

async function loadHotArticles(): Promise<void> {
  hotLoading.value = true
  try {
    hotArticles.value = await getHotArticles(5)
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    hotLoading.value = false
  }
}

async function loadArticles(): Promise<void> {
  loading.value = true
  try {
    const page = await pagePublishedArticles({
      page: query.page,
      size: query.size,
      categoryId: activeCategoryId.value,
      keyword: keyword.value.trim() || undefined,
    })
    records.value = page.records
    total.value = page.total
    pages.value = page.pages
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    loading.value = false
  }
}

function scrollToList(): void {
  window.requestAnimationFrame(() => {
    listAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

function selectCategory(id: number): void {
  activeCategoryId.value = id
  query.page = 1
  loadArticles()
  scrollToList()
}

function selectAll(): void {
  activeCategoryId.value = undefined
  query.page = 1
  loadArticles()
  scrollToList()
}

function handleSearch(): void {
  query.page = 1
  loadArticles()
}

function resetSearch(): void {
  keyword.value = ''
  activeCategoryId.value = undefined
  query.page = 1
  loadArticles()
}

function handlePageChange(page: number): void {
  query.page = page
  loadArticles()
  scrollToList()
}

function openArticle(id: number): void {
  // 管理员学生端预览时保留预览标记，详情页返回仍回到预览列表
  if (route.query.preview === '1') {
    router.push({ path: `/health/article/${id}`, query: { preview: '1' } })
  } else {
    router.push(`/health/article/${id}`)
  }
}

onMounted(() => {
  loadCategories()
  loadHotArticles()
  loadArticles()
})
</script>

<template>
  <div class="browse-page">
    <section class="knowledge-hero">
      <div class="hero-glow glow-a" />
      <div class="hero-glow glow-b" />
      <div class="hero-content">
        <span class="hero-eyebrow">HEALTH KNOWLEDGE LIBRARY</span>
        <h2>健康知识库</h2>
        <p>
          汇集校园健康科普与权威健康知识，从睡眠、饮食、运动到心理调适，
          帮助你用科学的方式照顾好自己。
        </p>
      </div>
      <div class="hero-stats">
        <div class="hero-stat">
          <strong>{{ categories.length }}</strong>
          <span>健康分类</span>
        </div>
        <div class="hero-divider" />
        <div class="hero-stat">
          <strong>{{ publishedTotal }}</strong>
          <span>科普文章</span>
        </div>
      </div>
    </section>

    <div class="browse-grid">
      <aside class="panel-card category-card">
        <div class="card-heading">
          <span>文章分类</span>
          <el-icon><Reading /></el-icon>
        </div>

        <div class="category-list">
          <button
            type="button"
            class="category-item"
            :class="{ active: activeCategoryId == null }"
            @click="selectAll"
          >
            <span class="category-name">全部文章</span>
            <span class="category-count">{{ publishedTotal }}</span>
          </button>

          <button
            v-for="item in flatCategories"
            :key="item.id"
            type="button"
            class="category-item"
            :class="{ active: activeCategoryId === item.id }"
            :style="{ paddingLeft: `${14 + item.depth * 16}px` }"
            @click="selectCategory(item.id)"
          >
            <span class="category-name">{{ item.label }}</span>
            <span class="category-count">{{ item.count }}</span>
          </button>
        </div>

        <div v-if="!flatCategories.length" class="category-empty">
          暂无可用分类
        </div>
      </aside>

      <section ref="listAnchor" class="article-column">
        <div class="panel-card search-card">
          <div class="search-row">
            <el-input
              v-model="keyword"
              placeholder="搜索文章标题或摘要"
              clearable
              maxlength="100"
              class="search-input"
              @keyup.enter="handleSearch"
              @clear="handleSearch"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
            <el-button type="primary" :icon="Search" @click="handleSearch">
              搜索
            </el-button>
            <el-button
              v-if="keyword || activeCategoryId != null"
              @click="resetSearch"
            >
              重置
            </el-button>
          </div>
          <div class="search-meta">
            <span>共找到 <strong>{{ total }}</strong> 篇文章</span>
            <span v-if="activeCategoryId != null" class="filter-tip">
              已按分类筛选
            </span>
          </div>
        </div>

        <div v-if="loading && !records.length" class="article-grid">
          <div v-for="index in 4" :key="index" class="article-card skeleton-card">
            <el-skeleton animated :rows="4" />
          </div>
        </div>

        <div v-else-if="records.length" v-loading="loading" class="article-grid">
          <article
            v-for="article in records"
            :key="article.id"
            class="article-card"
            role="button"
            tabindex="0"
            @click="openArticle(article.id)"
            @keyup.enter="openArticle(article.id)"
          >
            <div class="article-cover">
              <img
                v-if="article.coverUrl && !brokenCovers.has(article.id)"
                :src="article.coverUrl"
                :alt="article.title"
                loading="lazy"
                @error="brokenCovers.add(article.id)"
              />
              <div v-else class="cover-fallback">
                <span class="cover-char">
                  {{ (article.categoryName || article.title).charAt(0) }}
                </span>
                <el-icon :size="30"><Reading /></el-icon>
              </div>
              <span v-if="article.categoryName" class="cover-category">
                {{ article.categoryName }}
              </span>
            </div>

            <div class="article-body">
              <h3>{{ article.title }}</h3>
              <p class="article-summary">
                {{ article.summary || '点击查看文章完整内容' }}
              </p>
              <div class="article-meta">
                <span>{{ article.author || '校医' }}</span>
                <span>{{ formatDate(article.publishTime) }}</span>
                <span class="view-count">
                  <el-icon><View /></el-icon>
                  {{ article.viewCount ?? 0 }}
                </span>
              </div>
            </div>

            <div class="read-more">
              阅读全文
              <el-icon><ArrowRight /></el-icon>
            </div>
          </article>
        </div>

        <div v-else class="panel-card empty-card">
          <el-empty description="暂无符合条件的文章" :image-size="96">
            <el-button v-if="keyword || activeCategoryId != null" @click="resetSearch">
              清空筛选条件
            </el-button>
          </el-empty>
        </div>

        <div v-if="total > 0" class="pagination-bar">
          <span class="pagination-info">
            第 {{ query.page }} / {{ pages }} 页，共 {{ total }} 篇
          </span>
          <el-pagination
            :current-page="query.page"
            :page-size="query.size"
            :total="total"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </section>

      <aside class="panel-card hot-card">
        <div class="hot-heading">
          <span>热门文章</span>
          <el-tag type="warning" effect="plain" size="small">TOP 5</el-tag>
        </div>

        <div v-loading="hotLoading" class="hot-list">
          <button
            v-for="(item, index) in hotArticles"
            :key="item.id"
            type="button"
            class="hot-item"
            @click="openArticle(item.id)"
          >
            <span class="hot-rank" :class="`rank-${index + 1}`">
              {{ index + 1 }}
            </span>
            <span class="hot-info">
              <strong>{{ item.title }}</strong>
              <small>
                <el-icon><View /></el-icon>
                {{ item.viewCount ?? 0 }} 次浏览
              </small>
            </span>
          </button>

          <el-empty
            v-if="!hotLoading && !hotArticles.length"
            description="暂无热门文章"
            :image-size="64"
          />
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.browse-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* ---------------- 顶部横幅 ---------------- */
.knowledge-hero {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 28px;
  padding: 30px 38px;
  overflow: hidden;
  color: #eafff7;
  background:
    radial-gradient(circle at 84% 12%, rgba(61, 220, 151, 0.24), transparent 38%),
    linear-gradient(120deg, #0a352d 0%, #0f4f42 55%, #14745a 100%);
  border-radius: 20px;
  box-shadow: 0 16px 40px rgba(12, 59, 51, 0.16);
}

.hero-glow {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.glow-a {
  top: -110px;
  right: 22%;
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.2), transparent 68%);
}

.glow-b {
  right: -60px;
  bottom: -120px;
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, rgba(123, 240, 187, 0.12), transparent 66%);
}

.hero-content {
  position: relative;
  z-index: 1;
  max-width: 720px;
}

.hero-eyebrow {
  font-size: 9.5px;
  font-weight: 700;
  letter-spacing: 2.4px;
  color: rgba(123, 240, 187, 0.7);
}

.hero-content h2 {
  margin: 8px 0 10px;
  font-size: clamp(24px, 2.4vw, 31px);
  font-weight: 700;
  color: #fff;
}

.hero-content p {
  max-width: 640px;
  font-size: 13px;
  line-height: 1.85;
  color: rgba(234, 255, 247, 0.68);
}

.hero-stats {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 22px;
  flex-shrink: 0;
  padding: 18px 24px;
  background: rgba(255, 255, 255, 0.07);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  backdrop-filter: blur(8px);
}

.hero-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.hero-stat strong {
  font-size: 25px;
  font-weight: 700;
  line-height: 1.15;
  color: #7bf0bb;
}

.hero-stat span {
  font-size: 11px;
  color: rgba(234, 255, 247, 0.6);
}

.hero-divider {
  width: 1px;
  height: 34px;
  background: rgba(255, 255, 255, 0.14);
}

/* ---------------- 三栏布局 ---------------- */
.browse-grid {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr) 268px;
  gap: 18px;
  align-items: start;
}

.category-card,
.hot-card {
  position: sticky;
  top: calc(var(--header-height) + 18px);
  padding: 18px 14px;
}

.card-heading,
.hot-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 6px 12px;
  margin-bottom: 8px;
  font-size: 13.5px;
  font-weight: 700;
  color: var(--hp-text);
  border-bottom: 1px solid #eef4f1;
}

.card-heading .el-icon {
  color: var(--hp-primary);
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.category-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  width: 100%;
  padding: 9px 12px;
  font-family: inherit;
  font-size: 12.5px;
  color: var(--hp-text-secondary);
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 9px;
  transition: all 0.18s ease;
}

.category-item:hover {
  color: var(--hp-primary-dark);
  background: #f2faf7;
}

.category-item.active {
  font-weight: 600;
  color: var(--hp-primary-dark);
  background: var(--hp-mint);
}

.category-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.category-count {
  flex-shrink: 0;
  min-width: 20px;
  padding: 1px 6px;
  font-size: 10.5px;
  color: var(--hp-text-muted);
  text-align: center;
  background: #f1f6f4;
  border-radius: 999px;
}

.category-item.active .category-count {
  color: var(--hp-primary-dark);
  background: #d3ede4;
}

.category-empty {
  padding: 18px 0;
  font-size: 12px;
  color: var(--hp-text-muted);
  text-align: center;
}

/* ---------------- 文章列表 ---------------- */
.article-column {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
  scroll-margin-top: calc(var(--header-height) + 16px);
}

.search-card {
  padding: 16px 18px;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.search-input {
  flex: 1;
  min-width: 220px;
}

.search-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
  font-size: 12px;
  color: var(--hp-text-muted);
}

.search-meta strong {
  font-weight: 600;
  color: var(--hp-primary-dark);
}

.filter-tip {
  padding: 2px 9px;
  color: var(--hp-primary-dark);
  background: var(--hp-mint);
  border-radius: 999px;
}

.article-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.article-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  cursor: pointer;
  background: var(--hp-card);
  border: 1px solid var(--hp-border);
  border-radius: 15px;
  outline: none;
  box-shadow: 0 8px 24px rgba(23, 51, 44, 0.04);
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
}

.article-card:hover,
.article-card:focus-visible {
  border-color: #c5e6da;
  box-shadow: 0 16px 36px rgba(23, 51, 44, 0.09);
  transform: translateY(-3px);
}

.skeleton-card {
  padding: 20px;
  cursor: default;
}

.article-cover {
  position: relative;
  height: 132px;
  overflow: hidden;
  background: linear-gradient(135deg, #dff3ec, #c7e9de);
}

.article-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.article-card:hover .article-cover img {
  transform: scale(1.04);
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  width: 100%;
  height: 100%;
  color: rgba(31, 157, 122, 0.5);
  background:
    radial-gradient(circle at 84% 18%, rgba(255, 255, 255, 0.5), transparent 36%),
    linear-gradient(135deg, #e2f6ef, #c9ecdf);
}

.cover-char {
  font-size: 40px;
  font-weight: 700;
  line-height: 1;
  color: rgba(31, 157, 122, 0.35);
}

.cover-category {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 3px 9px;
  font-size: 10.5px;
  font-weight: 600;
  color: #0f4f42;
  background: rgba(255, 255, 255, 0.86);
  border-radius: 999px;
  backdrop-filter: blur(6px);
}

.article-body {
  display: flex;
  flex: 1;
  flex-direction: column;
  padding: 15px 17px 8px;
}

.article-body h3 {
  display: -webkit-box;
  overflow: hidden;
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 650;
  line-height: 1.5;
  color: var(--hp-text);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-summary {
  display: -webkit-box;
  flex: 1;
  overflow: hidden;
  margin-bottom: 12px;
  font-size: 12px;
  line-height: 1.75;
  color: var(--hp-text-muted);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.article-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px 14px;
  font-size: 11px;
  color: var(--hp-text-muted);
}

.view-count {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  margin-left: auto;
}

.read-more {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 4px;
  padding: 0 17px 14px;
  font-size: 11.5px;
  font-weight: 600;
  color: var(--hp-primary);
  opacity: 0;
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.article-card:hover .read-more {
  opacity: 1;
  transform: translateX(2px);
}

.empty-card {
  padding: 20px;
}

.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  flex-wrap: wrap;
  padding: 2px 4px;
}

.pagination-info {
  font-size: 12px;
  color: var(--hp-text-muted);
}

/* ---------------- 热门文章 ---------------- */
.hot-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-height: 120px;
}

.hot-item {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  width: 100%;
  padding: 10px 6px;
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 9px;
  transition: background 0.18s ease;
}

.hot-item:hover {
  background: #f4faf7;
}

.hot-rank {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 20px;
  height: 20px;
  margin-top: 1px;
  font-size: 10.5px;
  font-weight: 700;
  color: #9bb0a8;
  background: #f1f6f4;
  border-radius: 6px;
}

.rank-1 {
  color: #b0560d;
  background: #ffe9d2;
}

.rank-2 {
  color: #8a6a1f;
  background: #fbf0d6;
}

.rank-3 {
  color: #8a6a4f;
  background: #f4e8de;
}

.hot-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.hot-info strong {
  display: -webkit-box;
  overflow: hidden;
  font-size: 12px;
  font-weight: 500;
  line-height: 1.55;
  color: var(--hp-text);
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.hot-info small {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1280px) {
  .browse-grid {
    grid-template-columns: 200px minmax(0, 1fr);
  }

  .hot-card {
    position: static;
    grid-column: 1 / -1;
  }

  .hot-list {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 4px 12px;
  }
}

@media (max-width: 900px) {
  .knowledge-hero {
    padding: 24px;
  }

  .hero-stats {
    display: none;
  }

  .browse-grid {
    grid-template-columns: 1fr;
  }

  .category-card {
    position: static;
    padding: 14px;
  }

  .category-list {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 6px;
  }

  .category-item {
    width: auto;
    padding: 7px 12px !important;
    background: #f7fbf9;
    border: 1px solid #e8f1ed;
    border-radius: 999px;
  }

  .card-heading {
    padding-bottom: 10px;
  }
}

@media (max-width: 680px) {
  .article-grid {
    grid-template-columns: 1fr;
  }

  .article-cover {
    height: 150px;
  }

  .search-row {
    align-items: stretch;
  }

  .search-input {
    flex-basis: 100%;
  }

  .read-more {
    opacity: 1;
  }

  .hot-list {
    grid-template-columns: 1fr;
  }

  .pagination-bar {
    justify-content: center;
  }
}
</style>
