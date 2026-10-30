<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { type FormInstance, type FormRules } from 'element-plus'
import {
  CHECKIN_EXERCISE_INTENSITY_OPTIONS,
  CHECKIN_EXERCISE_TYPES,
} from '@/utils/checkin'

interface ExerciseStartForm {
  exerciseType: string
  exerciseIntensity: number | null
}

const visible = defineModel<boolean>({ required: true })

const props = defineProps<{
  loading?: boolean
}>()

const emit = defineEmits<{
  submit: [data: { exerciseType: string; exerciseIntensity: number }]
}>()

const formRef = ref<FormInstance>()
const form = reactive<ExerciseStartForm>({
  exerciseType: '',
  exerciseIntensity: 2,
})

const rules: FormRules<ExerciseStartForm> = {
  exerciseType: [
    { required: true, message: '请选择或输入运动类型', trigger: 'change' },
    { max: 50, message: '运动类型不能超过 50 个字符', trigger: 'blur' },
  ],
  exerciseIntensity: [
    { required: true, message: '请选择运动强度', trigger: 'change' },
  ],
}

watch(visible, (open) => {
  if (open) {
    form.exerciseType = ''
    form.exerciseIntensity = 2
    formRef.value?.clearValidate()
  }
})

async function handleSubmit(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid || form.exerciseIntensity == null) return
  emit('submit', {
    exerciseType: form.exerciseType.trim(),
    exerciseIntensity: form.exerciseIntensity,
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="开始运动"
    width="480px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      title="开始后由服务端记录开始时间，结束运动时自动计算时长并同步到今日打卡"
      type="info"
      :closable="false"
      show-icon
      class="start-tip"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="88px"
      @keyup.enter="handleSubmit"
    >
      <el-form-item label="运动类型" prop="exerciseType">
        <el-select
          v-model="form.exerciseType"
          filterable
          allow-create
          default-first-option
          placeholder="请选择或输入运动类型"
          style="width: 100%"
        >
          <el-option
            v-for="item in CHECKIN_EXERCISE_TYPES"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="运动强度" prop="exerciseIntensity">
        <el-radio-group v-model="form.exerciseIntensity">
          <el-radio
            v-for="item in CHECKIN_EXERCISE_INTENSITY_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        开始计时
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.start-tip {
  margin-bottom: 20px;
  line-height: 1.6;
}
</style>
