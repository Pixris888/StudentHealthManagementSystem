<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {
  createMaterial,
  getMaterialDetail,
  updateMaterial,
} from '@/api/knowledge'
import type {
  CategoryVO,
  EnableStatus,
  MaterialSaveDTO,
} from '@/types/knowledge'

interface MaterialFormModel {
  categoryId?: number
  title: string
  content: string
  keyword: string
  source: string
  status: EnableStatus
  remark: string
}

interface CategoryOption extends CategoryVO {
  disabled?: boolean
  children?: CategoryOption[]
}

const props = defineProps<{
  modelValue: boolean
  materialId?: number | null
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

const isEdit = computed(() => Boolean(props.materialId))
const dialogTitle = computed(() =>
  isEdit.value ? '编辑 AI 知识素材' : '新增 AI 知识素材',
)

function createDefaultForm(): MaterialFormModel {
  return {
    categoryId: undefined,
    title: '',
    content: '',
    keyword: '',
    source: '',
    status: 1,
    remark: '',
  }
}

const form = reactive<MaterialFormModel>(createDefaultForm())
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)

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

const rules: FormRules<MaterialFormModel> = {
  categoryId: [
    { required: true, message: '请选择素材分类', trigger: 'change' },
  ],
  title: [
    { required: true, message: '请输入素材标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入素材内容', trigger: 'blur' },
  ],
  keyword: [
    { max: 255, message: '关键词不能超过 255 个字符', trigger: 'blur' },
  ],
  source: [
    { max: 100, message: '来源不能超过 100 个字符', trigger: 'blur' },
  ],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  formRef.value?.clearValidate()
}

async function loadDetail(id: number): Promise<void> {
  loading.value = true
  try {
    const detail = await getMaterialDetail(id)
    Object.assign(form, {
      categoryId: detail.categoryId,
      title: detail.title,
      content: detail.content ?? '',
      keyword: detail.keyword ?? '',
      source: detail.source ?? '',
      status: detail.status ?? 1,
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
    if (props.materialId) {
      loadDetail(props.materialId)
    }
  },
)

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: MaterialSaveDTO = {
    categoryId: form.categoryId as number,
    title: form.title.trim(),
    content: form.content,
    keyword: form.keyword.trim() || undefined,
    source: form.source.trim() || undefined,
    status: form.status,
    remark: form.remark.trim() || undefined,
  }

  saving.value = true
  try {
    if (props.materialId) {
      await updateMaterial(props.materialId, payload)
      ElMessage.success('保存成功，已重新提交审核')
    } else {
      await createMaterial(payload)
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
    width="760px"
    top="7vh"
    append-to-body
    class="material-editor-dialog"
    :close-on-click-modal="false"
  >
    <div v-loading="loading">
      <el-alert
        :title="
          isEdit
            ? '保存后素材会重新进入待审核状态，并清空上次审核意见。'
            : '新增素材会自动提交审核；审核通过并启用后才会进入 AI 知识召回。'
        "
        type="info"
        :closable="false"
        show-icon
        class="material-alert"
      />

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="96px"
      >
        <el-form-item label="素材分类" prop="categoryId">
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
            placeholder="请选择素材分类"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="素材标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="200"
            show-word-limit
            placeholder="请输入便于检索的素材标题"
          />
        </el-form-item>

        <el-form-item label="素材内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="请输入权威、准确的健康知识内容，供 AI 健康助手检索引用"
          />
        </el-form-item>

        <el-form-item label="关键词" prop="keyword">
          <el-input
            v-model="form.keyword"
            maxlength="255"
            placeholder="如：睡眠,熬夜,作息,失眠（逗号分隔）"
          />
        </el-form-item>

        <el-form-item label="来源" prop="source">
          <el-input
            v-model="form.source"
            maxlength="100"
            placeholder="如：中国居民膳食指南（2022）"
          />
        </el-form-item>

        <el-form-item label="启用状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
          <div class="form-hint">
            未审核通过的素材即使选择启用，也不会被 AI 健康助手召回。
          </div>
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
        :disabled="loading"
        @click="submitForm"
      >
        保存并提交审核
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.material-alert {
  margin-bottom: 20px;
}

.form-hint {
  margin-top: 5px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}
</style>

<style>
.material-editor-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
}
</style>
