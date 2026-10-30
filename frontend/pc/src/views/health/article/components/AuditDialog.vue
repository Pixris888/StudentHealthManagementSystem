<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { InfoFilled } from '@element-plus/icons-vue'
import type { AuditDTO } from '@/types/knowledge'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    title?: string
    targetLabel?: string
    submitting?: boolean
  }>(),
  {
    title: '内容审核',
    targetLabel: '',
    submitting: false,
  },
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'submit', data: AuditDTO): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

const approved = ref(true)
const remark = ref('')

watch(
  () => props.modelValue,
  (open) => {
    if (open) {
      approved.value = true
      remark.value = ''
    }
  },
)

function handleSubmit(): void {
  const trimmed = remark.value.trim()
  if (!approved.value && !trimmed) {
    ElMessage.warning('审核不通过时请填写审核意见')
    return
  }
  emit('submit', {
    approved: approved.value,
    remark: trimmed || undefined,
  })
}
</script>

<template>
  <el-dialog
    v-model="visible"
    :title="title"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
    :close-on-press-escape="!submitting"
    :show-close="!submitting"
  >
    <el-alert
      v-if="targetLabel"
      :title="targetLabel"
      type="info"
      :closable="false"
      show-icon
      class="audit-target"
    />

    <el-form label-width="90px" class="audit-form">
      <el-form-item label="审核结果" required>
        <el-radio-group v-model="approved">
          <el-radio-button :value="true">通过</el-radio-button>
          <el-radio-button :value="false">不通过</el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="审核意见">
        <el-input
          v-model="remark"
          type="textarea"
          :rows="4"
          maxlength="255"
          show-word-limit
          :placeholder="
            approved
              ? '可选，例如：内容科学，审核通过'
              : '请说明不通过的原因，便于作者修改（必填）'
          "
        />
      </el-form-item>
    </el-form>

    <div class="audit-tip">
      <el-icon><InfoFilled /></el-icon>
      <span>审核通过后由校医发布；审核不通过时作者可修改后重新提交。</span>
    </div>

    <template #footer>
      <el-button :disabled="submitting" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">
        提交审核
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.audit-target {
  margin-bottom: 18px;
}

.audit-form {
  padding-right: 6px;
}

.audit-tip {
  display: flex;
  align-items: flex-start;
  gap: 7px;
  padding: 10px 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-muted);
  background: #f6faf8;
  border: 1px solid #e7f0ec;
  border-radius: 9px;
}

.audit-tip .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--hp-primary);
}
</style>
