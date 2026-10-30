<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {
  createDictData,
  updateDictData,
} from '@/api/dict'
import type {
  DictDataForm,
  SysDictData,
  SystemEnableStatus,
} from '@/types/system'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    dictType: string
    data?: SysDictData | null
  }>(),
  {
    dictType: '',
    data: null,
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

const isEdit = computed(() => Boolean(props.data?.id))
const dialogTitle = computed(() =>
  isEdit.value ? '编辑字典数据' : '新增字典数据',
)

function createDefaultForm(): DictDataForm {
  return {
    dictType: props.dictType,
    dictLabel: '',
    dictValue: '',
    dictSort: 0,
    status: 1,
    remark: '',
  }
}

const form = reactive<DictDataForm>(createDefaultForm())
const formRef = ref<FormInstance>()
const saving = ref(false)

const rules: FormRules<DictDataForm> = {
  dictLabel: [
    { required: true, message: '请输入字典标签', trigger: 'blur' },
    { max: 100, message: '字典标签不能超过 100 个字符', trigger: 'blur' },
  ],
  dictValue: [
    { required: true, message: '请输入字典键值', trigger: 'blur' },
    { max: 100, message: '字典键值不能超过 100 个字符', trigger: 'blur' },
  ],
  dictSort: [
    { required: true, message: '请输入显示顺序', trigger: 'change' },
    {
      type: 'number',
      min: 0,
      message: '显示顺序不能小于 0',
      trigger: 'change',
    },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  delete form.id
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForm()
    form.dictType = props.dictType || props.data?.dictType || ''
    if (props.data) {
      Object.assign(form, {
        id: props.data.id,
        dictType: props.data.dictType,
        dictLabel: props.data.dictLabel,
        dictValue: props.data.dictValue,
        dictSort: props.data.dictSort ?? 0,
        status: props.data.status ?? 1,
        remark: props.data.remark ?? '',
      })
    }
  },
)

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: DictDataForm = {
    dictType: form.dictType,
    dictLabel: form.dictLabel.trim(),
    dictValue: form.dictValue.trim(),
    dictSort: Number(form.dictSort ?? 0),
    status: form.status as SystemEnableStatus,
    remark: form.remark?.trim() || undefined,
  }
  if (form.id) payload.id = form.id

  saving.value = true
  try {
    if (form.id) {
      await updateDictData(payload)
      ElMessage.success('字典数据修改成功')
    } else {
      await createDictData(payload)
      ElMessage.success('字典数据新增成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 同类型下键值重复等由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="560px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="同一字典类型下键值不能重复"
      description="新增数据会归属左侧当前选中的字典类型；只有启用状态的数据会出现在下拉接口中。"
      class="dict-data-alert"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
    >
      <el-form-item label="字典类型">
        <el-input :model-value="form.dictType" disabled />
      </el-form-item>

      <el-form-item label="字典标签" prop="dictLabel">
        <el-input
          v-model="form.dictLabel"
          placeholder="展示给用户的文本"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="字典键值" prop="dictValue">
        <el-input
          v-model="form.dictValue"
          placeholder="程序中使用的值"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="显示顺序" prop="dictSort">
        <el-input-number
          v-model="form.dictSort"
          :min="0"
          :max="9999"
          :controls="false"
          style="width: 140px"
        />
        <span class="form-tip">数字越小越靠前。</span>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="可选"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ isEdit ? '保存修改' : '新增数据' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dict-data-alert {
  margin-bottom: 16px;
}

.form-tip {
  margin-left: 8px;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}
</style>
