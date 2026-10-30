<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {
  createArticle,
  getManageArticleDetail,
  updateArticle,
} from '@/api/knowledge'
import type {
  ArticleSaveDTO,
  ArticleVO,
  CategoryVO,
} from '@/types/knowledge'

interface ArticleFormModel {
  categoryId?: number
  title: string
  summary: string
  coverUrl: string
  author: string
  content: string
  source: string
  docUrl: string
  remark: string
}

interface CategoryOption extends CategoryVO {
  disabled?: boolean
  children?: CategoryOption[]
}

const props = defineProps<{
  modelValue: boolean
  articleId?: number | null
  categoryTree: CategoryVO[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => Boolean(props.articleId))
const dialogTitle = computed(() => (isEdit.value ? '编辑文章' : '新增文章'))

function createDefaultForm(): ArticleFormModel {
  return {
    categoryId: undefined,
    title: '',
    summary: '',
    coverUrl: '',
    author: '',
    content: '',
    source: '',
    docUrl: '',
    remark: '',
  }
}

const form = reactive<ArticleFormModel>(createDefaultForm())
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const original = ref<ArticleVO | null>(null)
const coverError = ref(false)

const locked = computed(() => original.value?.status === 1)

function decorateCategories(nodes: CategoryVO[]): CategoryOption[] {
  return nodes.map((node) => ({
    ...node,
    disabled: node.status !== 1,
    children: node.children?.length
      ? decorateCategories(node.children)
      : undefined,
  }))
}

const categoryOptions = computed<CategoryOption[]>(() =>
  decorateCategories(props.categoryTree),
)

const rules: FormRules<ArticleFormModel> = {
  categoryId: [
    { required: true, message: '请选择文章分类', trigger: 'change' },
  ],
  title: [
    { required: true, message: '请输入文章标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  summary: [{ max: 500, message: '摘要不能超过 500 个字符', trigger: 'blur' }],
  coverUrl: [
    { max: 255, message: '封面地址不能超过 255 个字符', trigger: 'blur' },
  ],
  author: [{ max: 50, message: '作者不能超过 50 个字符', trigger: 'blur' }],
  content: [
    { required: true, message: '请输入文章正文', trigger: 'blur' },
  ],
  source: [{ max: 100, message: '来源不能超过 100 个字符', trigger: 'blur' }],
  docUrl: [
    { max: 255, message: '附件地址不能超过 255 个字符', trigger: 'blur' },
  ],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  original.value = null
  coverError.value = false
  formRef.value?.clearValidate()
}

async function loadDetail(id: number): Promise<void> {
  loading.value = true
  try {
    const detail = await getManageArticleDetail(id)
    original.value = detail
    Object.assign(form, {
      categoryId: detail.categoryId,
      title: detail.title,
      summary: detail.summary ?? '',
      coverUrl: detail.coverUrl ?? '',
      author: detail.author ?? '',
      content: detail.content ?? '',
      source: detail.source ?? '',
      docUrl: detail.docUrl ?? '',
      remark: detail.remark ?? '',
    })
  } catch {
    visible.value = false
  } finally {
    loading.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForm()
    if (props.articleId) {
      loadDetail(props.articleId)
    }
  },
)

watch(
  () => form.coverUrl,
  () => {
    coverError.value = false
  },
)

function buildPayload(): ArticleSaveDTO {
  return {
    categoryId: form.categoryId as number,
    title: form.title.trim(),
    summary: form.summary.trim() || undefined,
    coverUrl: form.coverUrl.trim() || undefined,
    author: form.author.trim() || undefined,
    content: form.content,
    source: form.source.trim() || undefined,
    docUrl: form.docUrl.trim() || undefined,
    remark: form.remark.trim() || undefined,
  }
}

async function submitForm(): Promise<void> {
  if (locked.value) {
    ElMessage.warning('已发布文章不能编辑，请先下架')
    return
  }
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (
    original.value &&
    (original.value.status === 2 || original.value.auditStatus !== 0)
  ) {
    try {
      await ElMessageBox.confirm(
        original.value.status === 2
          ? '该文章已下架，保存后将重置为草稿并重新提交审核，审核通过后才能再次发布。是否继续？'
          : '保存后文章将重置为草稿并重新提交审核，审核通过后才能发布。是否继续？',
        '提交确认',
        {
          type: 'warning',
          confirmButtonText: '保存并送审',
          cancelButtonText: '取消',
        },
      )
    } catch {
      return
    }
  }

  saving.value = true
  try {
    if (props.articleId) {
      await updateArticle(props.articleId, buildPayload())
      ElMessage.success('保存成功，已提交审核')
    } else {
      await createArticle(buildPayload())
      ElMessage.success('新增成功，已提交审核')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 请求错误由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="820px"
    top="6vh"
    append-to-body
    class="article-editor-dialog"
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <el-alert
        v-if="locked"
        title="已发布文章不能直接编辑，请先在文章列表中下架。"
        type="error"
        :closable="false"
        show-icon
        class="editor-alert"
      />
      <el-alert
        v-else
        :title="
          isEdit
            ? '保存后文章会重新进入“草稿 / 待审核”状态，需超管重新审核。'
            : '新增文章将保存为草稿并提交审核，审核通过后由校医发布。'
        "
        type="info"
        :closable="false"
        show-icon
        class="editor-alert"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="96px"
        class="editor-form"
      >
        <el-form-item label="文章分类" prop="categoryId">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryOptions"
            node-key="id"
            :props="{
              label: 'categoryName',
              children: 'children',
              disabled: 'disabled',
            }"
            check-strictly
            :render-after-expand="false"
            default-expand-all
            clearable
            placeholder="请选择文章分类"
            style="width: 100%"
          />
          <div class="form-hint">已停用的分类不可选，如需使用请先在分类管理中启用。</div>
        </el-form-item>

        <el-form-item label="文章标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            placeholder="请输入文章标题"
          />
        </el-form-item>

        <el-form-item label="文章摘要" prop="summary">
          <el-input
            v-model="form.summary"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="用于列表卡片展示，建议 60-120 字"
          />
        </el-form-item>

        <el-form-item label="封面地址" prop="coverUrl">
          <el-input
            v-model="form.coverUrl"
            maxlength="255"
            placeholder="填写图片 URL；留空时使用平台默认封面"
          />
          <div
            v-if="form.coverUrl && !coverError"
            class="cover-preview"
          >
            <img
              :src="form.coverUrl"
              alt="封面预览"
              @error="coverError = true"
            />
          </div>
        </el-form-item>

        <el-form-item label="作者" prop="author">
          <el-input
            v-model="form.author"
            maxlength="50"
            placeholder="留空时自动使用当前用户真实姓名"
          />
        </el-form-item>

        <el-form-item label="文章正文" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="12"
            placeholder="请输入文章正文，支持换行。本期按纯文本展示。"
          />
          <div class="form-hint">正文按纯文本渲染，换行会在详情页原样保留。</div>
        </el-form-item>

        <el-form-item label="来源" prop="source">
          <el-input
            v-model="form.source"
            maxlength="100"
            placeholder="如：校园健康中心 / 中国居民膳食指南"
          />
        </el-form-item>

        <el-form-item label="附件地址" prop="docUrl">
          <el-input
            v-model="form.docUrl"
            maxlength="255"
            placeholder="选填，填写附件 URL"
          />
        </el-form-item>

        <el-form-item label="备注" prop="remark">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="选填，仅供后台查看"
          />
        </el-form-item>
      </el-form>
    </div>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="saving"
        :disabled="loading || locked"
        @click="submitForm"
      >
        保存并提交审核
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.editor-alert {
  margin-bottom: 20px;
}

.editor-form {
  padding-right: 8px;
}

.form-hint {
  margin-top: 5px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}

.cover-preview {
  width: 180px;
  height: 100px;
  margin-top: 10px;
  overflow: hidden;
  background: #f3f8f6;
  border: 1px solid var(--hp-border);
  border-radius: 10px;
}

.cover-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
</style>

<style>
.article-editor-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
}
</style>
