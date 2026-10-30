<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type FormInstance,
  type FormRules,
} from 'element-plus'
import { sendMessage } from '@/api/message'
import { SEND_MESSAGE_TYPE_OPTIONS } from '@/utils/message'
import type { MessageSendDTO, SendMessageType } from '@/types/message'
import ReceiverSelect from './ReceiverSelect.vue'

interface SendFormModel {
  messageType: SendMessageType
  title: string
  content: string
  receiverIds: number[]
}

const props = defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'sent', count: number): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function createDefaultForm(): SendFormModel {
  return {
    messageType: 3,
    title: '',
    content: '',
    receiverIds: [],
  }
}

const form = reactive<SendFormModel>(createDefaultForm())
const formRef = ref<FormInstance>()
const saving = ref(false)

const isBroadcast = computed(() => form.messageType === 3)
const typeHint = computed(() => {
  if (form.messageType === 3) {
    return '校园公告会以全体广播的方式发送，所有学生都能在消息中心看到，无需选择接收人。'
  }
  if (form.messageType === 4) {
    return '健康关怀留言只对所选学生可见，单次最多选择 100 人。'
  }
  return '其他消息按所选学生逐人发送，单次最多选择 100 人。'
})

const rules = computed<FormRules<SendFormModel>>(() => ({
  messageType: [
    { required: true, message: '请选择消息类型', trigger: 'change' },
  ],
  title: [
    { required: true, message: '请输入消息标题', trigger: 'blur' },
    { max: 200, message: '标题不能超过 200 个字符', trigger: 'blur' },
  ],
  content: [
    { required: true, message: '请输入消息正文', trigger: 'blur' },
    { max: 5000, message: '正文不能超过 5000 个字符', trigger: 'blur' },
  ],
  receiverIds: isBroadcast.value
    ? []
    : [
        {
          validator: (
            _rule: unknown,
            value: number[],
            callback: (error?: Error) => void,
          ) => {
            if (!Array.isArray(value) || value.length === 0) {
              callback(new Error('请选择至少一名接收人'))
              return
            }
            callback()
          },
          trigger: 'change',
        },
      ],
}))

function resetForm(): void {
  Object.assign(form, createDefaultForm())
  formRef.value?.clearValidate()
}

watch(
  () => props.modelValue,
  (open) => {
    if (open) resetForm()
  },
)

watch(
  () => form.messageType,
  (type) => {
    if (type === 3) {
      form.receiverIds = []
      formRef.value?.clearValidate('receiverIds')
    }
  },
)

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  const receiverCount = form.receiverIds.length
  const confirmText = isBroadcast.value
    ? `确定向全体学生发布校园公告“${form.title.trim()}”吗？发布后所有学生可见。`
    : `确定向 ${receiverCount} 名学生发送“${form.title.trim()}”吗？`

  try {
    await ElMessageBox.confirm(confirmText, '发送确认', {
      type: 'warning',
      confirmButtonText: '确认发送',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }

  const payload: MessageSendDTO = {
    messageType: form.messageType,
    title: form.title.trim(),
    content: form.content,
    receiverIds: isBroadcast.value ? [] : [...form.receiverIds],
  }

  saving.value = true
  try {
    const count = await sendMessage(payload)
    ElMessage.success(`已发送 ${count} 条消息`)
    visible.value = false
    emit('sent', count)
  } catch {
    // 接收人不合法、权限不足等由拦截器展示后端 msg
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="发送消息"
    width="720px"
    top="7vh"
    append-to-body
    class="message-send-dialog"
    :close-on-click-modal="false"
  >
    <el-alert
      :title="typeHint"
      type="info"
      :closable="false"
      show-icon
      class="send-alert"
    />

    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="96px"
    >
      <el-form-item label="消息类型" prop="messageType">
        <el-radio-group v-model="form.messageType">
          <el-radio-button
            v-for="item in SEND_MESSAGE_TYPE_OPTIONS"
            :key="item.value"
            :value="item.value"
          >
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="消息标题" prop="title">
        <el-input
          v-model="form.title"
          maxlength="200"
          show-word-limit
          placeholder="请输入消息标题"
        />
      </el-form-item>

      <el-form-item label="消息正文" prop="content">
        <el-input
          v-model="form.content"
          type="textarea"
          :rows="8"
          maxlength="5000"
          show-word-limit
          placeholder="请输入消息正文，支持换行。消息内容按纯文本展示。"
        />
      </el-form-item>

      <el-form-item
        v-if="!isBroadcast"
        label="接收人"
        prop="receiverIds"
      >
        <ReceiverSelect v-model="form.receiverIds" :max="100" />
      </el-form-item>

      <el-form-item v-else label="接收人">
        <el-tag type="warning" effect="plain">全体学生（广播）</el-tag>
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button :disabled="saving" @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitForm">
        确认发送
      </el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.send-alert {
  margin-bottom: 20px;
}
</style>

<style>
.message-send-dialog .el-dialog__body {
  max-height: 68vh;
  overflow-y: auto;
}
</style>
