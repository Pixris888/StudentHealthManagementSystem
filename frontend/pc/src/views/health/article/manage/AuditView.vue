<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  CircleCheck,
  Collection,
  Document,
  View,
} from '@element-plus/icons-vue'
import {
  auditArticle,
  auditMaterial,
  pageManageArticles,
  pageMaterials,
} from '@/api/knowledge'
import { auditStatusTagType } from '@/utils/knowledge'
import { formatDateTime } from '@/utils/format'
import type {
  ArticleVO,
  AuditDTO,
  MaterialVO,
} from '@/types/knowledge'
import ArticlePreviewDrawer from '../components/ArticlePreviewDrawer.vue'
import MaterialPreviewDrawer from '../components/MaterialPreviewDrawer.vue'
import AuditDialog from '../components/AuditDialog.vue'

const activeTab = ref<'articles' | 'materials'>('articles')

/* ---------------- 待审核文章 ---------------- */
const articleRecords = ref<ArticleVO[]>([])
const articleTotal = ref(0)
const articleLoading = ref(false)
const articlePage = reactive({ page: 1, size: 10 })

async function loadArticles(): Promise<void> {
  articleLoading.value = true
  try {
    const page = await pageManageArticles({
      page: articlePage.page,
      size: articlePage.size,
      auditStatus: 0,
    })
    articleRecords.value = page.records
    articleTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    articleLoading.value = false
  }
}

function handleArticlePage(page: number): void {
  articlePage.page = page
  loadArticles()
}

function handleArticleSize(size: number): void {
  articlePage.size = size
  articlePage.page = 1
  loadArticles()
}

/* ---------------- 待审核素材 ---------------- */
const materialRecords = ref<MaterialVO[]>([])
const materialTotal = ref(0)
const materialLoading = ref(false)
const materialPage = reactive({ page: 1, size: 10 })

async function loadMaterials(): Promise<void> {
  materialLoading.value = true
  try {
    const page = await pageMaterials({
      page: materialPage.page,
      size: materialPage.size,
      auditStatus: 0,
    })
    materialRecords.value = page.records
    materialTotal.value = page.total
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    materialLoading.value = false
  }
}

function handleMaterialPage(page: number): void {
  materialPage.page = page
  loadMaterials()
}

function handleMaterialSize(size: number): void {
  materialPage.size = size
  materialPage.page = 1
  loadMaterials()
}

/* ---------------- 预览 ---------------- */
const articlePreviewVisible = ref(false)
const articlePreviewId = ref<number | null>(null)
const materialPreviewVisible = ref(false)
const materialPreviewId = ref<number | null>(null)

function openArticlePreview(row: ArticleVO): void {
  articlePreviewId.value = row.id
  articlePreviewVisible.value = true
}

function openMaterialPreview(row: MaterialVO): void {
  materialPreviewId.value = row.id
  materialPreviewVisible.value = true
}

/* ---------------- 审核 ---------------- */
type AuditTarget =
  | { type: 'article'; item: ArticleVO }
  | { type: 'material'; item: MaterialVO }

const auditVisible = ref(false)
const auditTarget = ref<AuditTarget | null>(null)
const auditSubmitting = ref(false)

const auditTargetLabel = computed(() => {
  if (!auditTarget.value) return ''
  return auditTarget.value.type === 'article'
    ? `文章：${auditTarget.value.item.title}`
    : `素材：${auditTarget.value.item.title}`
})

function openArticleAudit(row: ArticleVO): void {
  auditTarget.value = { type: 'article', item: row }
  auditVisible.value = true
}

function openMaterialAudit(row: MaterialVO): void {
  auditTarget.value = { type: 'material', item: row }
  auditVisible.value = true
}

async function submitAudit(data: AuditDTO): Promise<void> {
  if (!auditTarget.value) return
  auditSubmitting.value = true
  try {
    if (auditTarget.value.type === 'article') {
      await auditArticle(auditTarget.value.item.id, data)
      articlePage.page = 1
    } else {
      await auditMaterial(auditTarget.value.item.id, data)
      materialPage.page = 1
    }
    ElMessage.success(data.approved ? '审核已通过' : '已退回修改')
    auditVisible.value = false
    await Promise.all([loadArticles(), loadMaterials()])
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    auditSubmitting.value = false
  }
}

onMounted(() => {
  loadArticles()
  loadMaterials()
})
</script>

<template>
  <div class="audit-view">
    <div class="audit-stats">
      <div class="audit-stat">
        <span class="stat-icon article">
          <el-icon :size="20"><Document /></el-icon>
        </span>
        <div>
          <span class="stat-label">待审核文章</span>
          <strong class="stat-value">{{ articleTotal }}</strong>
        </div>
      </div>
      <div class="audit-stat">
        <span class="stat-icon material">
          <el-icon :size="20"><Collection /></el-icon>
        </span>
        <div>
          <span class="stat-label">待审核素材</span>
          <strong class="stat-value">{{ materialTotal }}</strong>
        </div>
      </div>
      <div class="audit-note">
        <el-icon><CircleCheck /></el-icon>
        <span>审核通过后由校医发布文章；素材审核通过后才可启用并进入 AI 知识召回。</span>
      </div>
    </div>

    <section class="panel-card audit-panel">
      <el-tabs v-model="activeTab" class="audit-tabs">
        <el-tab-pane name="articles">
          <template #label>
            <span class="tab-label">
              待审核文章
              <el-badge
                :value="articleTotal"
                :hidden="articleTotal === 0"
                type="warning"
              />
            </span>
          </template>

          <el-table
            v-loading="articleLoading"
            :data="articleRecords"
            row-key="id"
            style="width: 100%"
          >
            <el-table-column label="文章标题" min-width="300">
              <template #default="{ row }">
                <div class="audit-title-cell">
                  <button
                    type="button"
                    class="title-link"
                    @click="openArticlePreview(row)"
                  >
                    {{ row.title }}
                  </button>
                  <span class="audit-sub">
                    {{ row.categoryName || '未分类' }} ·
                    {{ row.author || '—' }}
                  </span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="审核状态" width="118" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="auditStatusTagType(row.auditStatus)"
                  effect="plain"
                >
                  {{ row.auditStatusName }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    link
                    type="primary"
                    :icon="View"
                    @click="openArticlePreview(row)"
                  >
                    查看
                  </el-button>
                  <el-button
                    link
                    type="warning"
                    :icon="CircleCheck"
                    @click="openArticleAudit(row)"
                  >
                    审核
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty
                description="没有待审核文章，内容都很新鲜"
                :image-size="88"
              />
            </template>
          </el-table>

          <div class="audit-footer">
            <el-pagination
              :current-page="articlePage.page"
              :page-size="articlePage.size"
              :page-sizes="[10, 20, 50]"
              :total="articleTotal"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="handleArticlePage"
              @size-change="handleArticleSize"
            />
          </div>
        </el-tab-pane>

        <el-tab-pane name="materials">
          <template #label>
            <span class="tab-label">
              待审核素材
              <el-badge
                :value="materialTotal"
                :hidden="materialTotal === 0"
                type="warning"
              />
            </span>
          </template>

          <el-table
            v-loading="materialLoading"
            :data="materialRecords"
            row-key="id"
            style="width: 100%"
          >
            <el-table-column label="素材标题" min-width="300">
              <template #default="{ row }">
                <div class="audit-title-cell">
                  <button
                    type="button"
                    class="title-link"
                    @click="openMaterialPreview(row)"
                  >
                    {{ row.title }}
                  </button>
                  <span class="audit-sub">
                    {{ row.categoryName || '未分类' }} ·
                    {{ row.keyword || '未设置关键词' }}
                  </span>
                </div>
              </template>
            </el-table-column>

            <el-table-column label="来源" min-width="180">
              <template #default="{ row }">
                <span class="source-cell">{{ row.source || '—' }}</span>
              </template>
            </el-table-column>

            <el-table-column label="审核状态" width="118" align="center">
              <template #default="{ row }">
                <el-tag
                  :type="auditStatusTagType(row.auditStatus)"
                  effect="plain"
                >
                  {{ row.auditStatusName }}
                </el-tag>
              </template>
            </el-table-column>

            <el-table-column label="提交时间" width="160">
              <template #default="{ row }">
                <span class="time-cell">{{ formatDateTime(row.createTime) }}</span>
              </template>
            </el-table-column>

            <el-table-column label="操作" width="190" fixed="right">
              <template #default="{ row }">
                <div class="table-actions">
                  <el-button
                    link
                    type="primary"
                    :icon="View"
                    @click="openMaterialPreview(row)"
                  >
                    查看
                  </el-button>
                  <el-button
                    link
                    type="warning"
                    :icon="CircleCheck"
                    @click="openMaterialAudit(row)"
                  >
                    审核
                  </el-button>
                </div>
              </template>
            </el-table-column>

            <template #empty>
              <el-empty
                description="没有待审核素材，内容都很新鲜"
                :image-size="88"
              />
            </template>
          </el-table>

          <div class="audit-footer">
            <el-pagination
              :current-page="materialPage.page"
              :page-size="materialPage.size"
              :page-sizes="[10, 20, 50]"
              :total="materialTotal"
              layout="total, sizes, prev, pager, next"
              background
              @current-change="handleMaterialPage"
              @size-change="handleMaterialSize"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </section>

    <ArticlePreviewDrawer
      v-model="articlePreviewVisible"
      :article-id="articlePreviewId"
    />

    <MaterialPreviewDrawer
      v-model="materialPreviewVisible"
      :material-id="materialPreviewId"
    />

    <AuditDialog
      v-model="auditVisible"
      :title="auditTarget?.type === 'material' ? '素材审核' : '文章审核'"
      :target-label="auditTargetLabel"
      :submitting="auditSubmitting"
      @submit="submitAudit"
    />
  </div>
</template>

<style scoped>
.audit-view {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.audit-stats {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.audit-stat {
  display: flex;
  align-items: center;
  gap: 13px;
  min-width: 180px;
  padding: 16px 20px;
  background: var(--hp-card);
  border: 1px solid var(--hp-border);
  border-radius: 14px;
  box-shadow: 0 8px 28px rgba(23, 51, 44, 0.04);
}

.stat-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border-radius: 12px;
}

.stat-icon.article {
  color: #3f7ff0;
  background: #eaf1ff;
}

.stat-icon.material {
  color: #8a63e8;
  background: #f1ecff;
}

.stat-label {
  display: block;
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.25;
  color: var(--hp-text);
}

.audit-note {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-muted);
  background: #f6faf8;
  border: 1px solid #e7f0ec;
  border-radius: 12px;
}

.audit-note .el-icon {
  flex-shrink: 0;
  color: var(--hp-primary);
}

.audit-panel {
  padding: 10px 20px 16px;
}

.audit-tabs :deep(.el-tabs__header) {
  margin-bottom: 10px;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.audit-title-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.title-link {
  overflow: hidden;
  padding: 0;
  font-family: inherit;
  font-size: 13.5px;
  font-weight: 600;
  color: var(--hp-text);
  text-align: left;
  text-overflow: ellipsis;
  white-space: nowrap;
  cursor: pointer;
  background: none;
  border: none;
}

.title-link:hover {
  color: var(--hp-primary);
}

.audit-sub {
  overflow: hidden;
  font-size: 11.5px;
  color: var(--hp-text-muted);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-cell {
  font-size: 12.5px;
  color: var(--hp-text-secondary);
}

.time-cell {
  font-size: 12px;
  color: var(--hp-text-secondary);
}

.table-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}

.table-actions :deep(.el-button + .el-button) {
  margin-left: 6px;
}

.audit-footer {
  display: flex;
  justify-content: flex-end;
  padding: 16px 2px 4px;
}

@media (max-width: 768px) {
  .audit-panel {
    padding: 10px 12px 12px;
  }

  .audit-footer {
    justify-content: center;
  }
}
</style>
