<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { createCategory, updateCategory } from '@/api/knowledge'
import type {
  CategorySaveDTO,
  CategoryVO,
  EnableStatus,
} from '@/types/knowledge'

interface CategoryFormModel {
  categoryName: string
  parentId: number
  orderNum: number
  status: EnableStatus
  remark: string
}

interface CategoryOption {
  id: number
  categoryName: string
  children?: CategoryOption[]
}

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    category?: CategoryVO | null
    parentId?: number
    tree: CategoryVO[]
  }>(),
  {
    category: null,
    parentId: 0,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'saved'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const isEdit = computed(() => Boolean(props.category?.id))
const dialogTitle = computed(() =>
  isEdit.value ? '编辑分类' : '新增分类',
)

function createDefaultForm(): CategoryFormModel {
  return {
    categoryName: '',
    parentId: props.parentId ?? 0,
    orderNum: 0,
    status: 1,
    remark: '',
  }
}

const form = reactive<CategoryFormModel>(createDefaultForm())
const formRef = ref<FormInstance>()
const saving = ref(false)

const rules: FormRules<CategoryFormModel> = {
  categoryName: [
    { required: true, message: '请输入分类名称', trigger: 'blur' },
    { max: 50, message: '分类名称不能超过 50 个字符', trigger: 'blur' },
  ],
  orderNum: [
    { required: true, message: '请输入显示顺序', trigger: 'change' },
  ],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function collectSubtreeIds(node: CategoryVO): number[] {
  return [node.id, ...(node.children ?? []).flatMap(collectSubtreeIds)]
}

const excludedIds = computed(() => {
  if (!props.category) return new Set<number>()
  return new Set(collectSubtreeIds(props.category))
})

function buildOptions(
  nodes: CategoryVO[],
  excluded: Set<number>,
): CategoryOption[] {
  const result: CategoryOption[] = []
  for (const node of nodes) {
    if (excluded.has(node.id)) continue
    result.push({
      id: node.id,
      categoryName:
        node.status === 1 ? node.categoryName : `${node.categoryName}（已停用）`,
      children: buildOptions(node.children ?? [], excluded),
    })
  }
  return result
}

const parentOptions = computed<CategoryOption[]>(() => [
  {
    id: 0,
    categoryName: '顶级分类',
    children: buildOptions(props.tree, excludedIds.value),
  },
])

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForm()
    if (props.category) {
      Object.assign(form, {
        categoryName: props.category.categoryName,
        parentId: props.category.parentId ?? 0,
        orderNum: props.category.orderNum ?? 0,
        status: props.category.status ?? 1,
        remark: props.category.remark ?? '',
      })
    }
  },
)

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: CategorySaveDTO = {
    categoryName: form.categoryName.trim(),
    parentId: form.parentId ?? 0,
    orderNum: form.orderNum,
    status: form.status,
    remark: form.remark.trim() || undefined,
  }

  saving.value = true
  try {
    if (props.category) {
      await updateCategory(props.category.id, payload)
      ElMessage.success('分类修改成功')
    } else {
      await createCategory(payload)
      ElMessage.success('分类新增成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 请求错误由拦截器统一提示（同父级重名等 409）
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="600px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      title="同一父分类下不能重名；父分类不能选择自己或自己的子分类。"
      type="info"
      :closable="false"
      show-icon
      class="category-alert"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
    >
      <el-form-item label="分类名称" prop="categoryName">
        <el-input
          v-model="form.categoryName"
          maxlength="50"
          show-word-limit
          placeholder="请输入分类名称"
        />
      </el-form-item>

      <el-form-item label="上级分类" prop="parentId">
        <el-tree-select
          v-model="form.parentId"
          :data="parentOptions"
          node-key="id"
          :props="{ label: 'categoryName', children: 'children' }"
          check-strictly
          :render-after-expand="false"
          default-expand-all
          placeholder="请选择上级分类"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="显示顺序" prop="orderNum">
        <el-input-number
          v-model="form.orderNum"
          :min="0"
          :max="9999"
          controls-position="right"
          style="width: 180px"
        />
        <span class="order-hint">数值越小越靠前</span>
      </el-form-item>

      <el-form-item label="分类状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">启用</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-alert
        v-if="form.status === 0"
        title="停用后该分类下文章不能新发布，已有内容仍可正常浏览；AI 素材也不能再挂到该分类。"
        type="warning"
        :closable="false"
        show-icon
        class="status-alert"
      />

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="3"
          maxlength="500"
          show-word-limit
          placeholder="选填，分类说明"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        保存
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.category-alert {
  margin-bottom: 20px;
}

.status-alert {
  margin: -4px 0 18px 96px;
}

.order-hint {
  margin-left: 12px;
  font-size: 12px;
  color: var(--hp-text-muted);
}
</style>
