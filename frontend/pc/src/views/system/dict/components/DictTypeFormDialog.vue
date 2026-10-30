<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import {
  createDictType,
  pageDictData,
  updateDictType,
} from '@/api/dict'
import type {
  DictTypeForm,
  SysDictType,
  SystemEnableStatus,
} from '@/types/system'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    dictType?: SysDictType | null
  }>(),
  {
    dictType: null,
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

const isEdit = computed(() => Boolean(props.dictType?.id))
const dialogTitle = computed(() =>
  isEdit.value ? '编辑字典类型' : '新增字典类型',
)

function createDefaultForm(): DictTypeForm {
  return {
    dictName: '',
    dictType: '',
    status: 1,
    remark: '',
  }
}

const form = reactive<DictTypeForm>(createDefaultForm())
const formRef = ref<FormInstance>()
const saving = ref(false)
const checkingData = ref(false)
const hasData = ref(false)

const lockTypeCode = computed(() => isEdit.value && hasData.value)

const rules: FormRules<DictTypeForm> = {
  dictName: [
    { required: true, message: '请输入字典名称', trigger: 'blur' },
    { max: 100, message: '字典名称不能超过 100 个字符', trigger: 'blur' },
  ],
  dictType: [
    { required: true, message: '请输入字典类型编码', trigger: 'blur' },
    {
      pattern: /^[a-z][a-z0-9_]{1,99}$/,
      message:
        '必须为小写字母开头，只能包含小写字母、数字、下划线，长度 2-100',
      trigger: 'blur',
    },
  ],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  remark: [{ max: 500, message: '备注不能超过 500 个字符', trigger: 'blur' }],
}

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  delete form.id
  hasData.value = false
  checkingData.value = false
  formRef.value?.clearValidate()
}

async function checkDataUsage(): Promise<void> {
  if (!props.dictType) return
  checkingData.value = true
  try {
    const page = await pageDictData({
      page: 1,
      size: 1,
      dictType: props.dictType.dictType,
    })
    hasData.value = page.total > 0
  } catch {
    hasData.value = false
  } finally {
    checkingData.value = false
  }
}

watch(
  () => props.modelValue,
  (open) => {
    if (!open) return
    resetForm()
    if (props.dictType) {
      Object.assign(form, {
        id: props.dictType.id,
        dictName: props.dictType.dictName,
        dictType: props.dictType.dictType,
        status: props.dictType.status ?? 1,
        remark: props.dictType.remark ?? '',
      })
      checkDataUsage()
    }
  },
)

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const payload: DictTypeForm = {
    dictName: form.dictName.trim(),
    dictType: form.dictType.trim(),
    status: form.status as SystemEnableStatus,
    remark: form.remark?.trim() || undefined,
  }
  if (form.id) payload.id = form.id

  saving.value = true
  try {
    if (form.id) {
      await updateDictType(payload)
      ElMessage.success('字典类型修改成功')
    } else {
      await createDictType(payload)
      ElMessage.success('字典类型新增成功')
    }
    visible.value = false
    emit('saved')
  } catch {
    // 编码重复、类型下已有数据等由拦截器统一提示
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="dialogTitle"
    width="580px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      type="info"
      :closable="false"
      show-icon
      title="字典类型编码唯一"
      :description="
        isEdit
          ? '字典类型下存在字典数据时，不能修改字典类型编码。'
          : '编码必须为小写字母开头，只能包含小写字母、数字、下划线。'
      "
      class="dict-type-alert"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="110px"
      v-loading="checkingData"
    >
      <el-form-item label="字典名称" prop="dictName">
        <el-input
          v-model="form.dictName"
          placeholder="如：健康状况"
          maxlength="100"
          show-word-limit
        />
      </el-form-item>

      <el-form-item label="字典类型" prop="dictType">
        <el-input
          v-model="form.dictType"
          placeholder="如：health_status"
          maxlength="100"
          :disabled="lockTypeCode"
        />
        <span v-if="lockTypeCode" class="form-tip">
          该类型下已有字典数据，字典类型编码不可修改。
        </span>
      </el-form-item>

      <el-form-item label="状态" prop="status">
        <el-radio-group v-model="form.status">
          <el-radio :value="1">正常</el-radio>
          <el-radio :value="0">停用</el-radio>
        </el-radio-group>
        <span class="form-tip">停用后，该字典类型不会出现在下拉接口中。</span>
      </el-form-item>

      <el-form-item label="备注" prop="remark">
        <el-input
          v-model="form.remark"
          type="textarea"
          :rows="2"
          placeholder="说明字典用途"
          maxlength="500"
          show-word-limit
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        {{ isEdit ? '保存修改' : '新增类型' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.dict-type-alert {
  margin-bottom: 16px;
}

.form-tip {
  display: block;
  width: 100%;
  margin-top: 4px;
  font-size: 11.5px;
  line-height: 1.5;
  color: var(--hp-text-muted);
}
</style>
