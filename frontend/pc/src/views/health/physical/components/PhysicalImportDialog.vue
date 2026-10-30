<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, type UploadRequestOptions } from 'element-plus'
import { Download, UploadFilled } from '@element-plus/icons-vue'
import { importPhysicalTests } from '@/api/health-physical'
import type { PhysicalImportResult } from '@/types/health'

const visible = defineModel<boolean>({ required: true })
const emit = defineEmits<{ imported: [] }>()

const importing = ref(false)
const result = ref<PhysicalImportResult | null>(null)

const columns = [
  '学号',
  '测试年份',
  '学期',
  '身高',
  '体重',
  '肺活量',
  '50米跑',
  '坐位体前屈',
  '立定跳远',
  '耐力跑',
  '力量项目',
  '总分',
  '等级',
  '测试日期',
]

watch(visible, (open) => {
  if (open) {
    result.value = null
    importing.value = false
  }
})

function failUpload(
  options: UploadRequestOptions,
  message: string,
): void {
  ElMessage.error(message)
  options.onError(new Error(message) as Parameters<typeof options.onError>[0])
}

async function handleUpload(options: UploadRequestOptions): Promise<void> {
  const file = options.file as File
  if (!/\.(xlsx|xls)$/i.test(file.name)) {
    failUpload(options, '仅支持 .xlsx 或 .xls 格式的 Excel 文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    failUpload(options, '文件大小不能超过 5MB')
    return
  }

  importing.value = true
  result.value = null
  try {
    const response = await importPhysicalTests(file)
    result.value = response
    options.onSuccess(response)
    emit('imported')
    if (response.failed > 0) {
      ElMessage.warning(
        `导入完成：新增 ${response.inserted} 条，更新 ${response.updated} 条，失败 ${response.failed} 条`,
      )
    } else {
      ElMessage.success(
        `导入成功：新增 ${response.inserted} 条，更新 ${response.updated} 条`,
      )
    }
  } catch (error) {
    options.onError(error as Parameters<typeof options.onError>[0])
  } finally {
    importing.value = false
  }
}

async function downloadTemplate(): Promise<void> {
  const XLSX = await import('xlsx')
  const header = [
    '学号',
    '测试年份',
    '学期',
    '身高(cm)',
    '体重(kg)',
    '肺活量(ml)',
    '50米跑(秒)',
    '坐位体前屈(cm)',
    '立定跳远(cm)',
    '耐力跑(秒)',
    '力量项目(个)',
    '总分',
    '等级',
    '测试日期',
  ]
  const sample = [
    '',
    2025,
    '上学期',
    175,
    65,
    3800,
    7.2,
    12.5,
    220,
    240,
    10,
    82,
    '良好',
    '2025-10-15',
  ]
  const sheet = XLSX.utils.aoa_to_sheet([header, sample])
  sheet['!cols'] = header.map(() => ({ wch: 14 }))
  const workbook = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(workbook, sheet, '体测成绩')
  XLSX.writeFile(workbook, '体测成绩导入模板.xlsx')
  ElMessage.success('模板已开始下载，请按模板列顺序填写')
}
</script>

<template>
  <el-dialog
    v-model="visible"
    title="导入体测成绩"
    width="640px"
    append-to-body
    :close-on-click-modal="false"
  >
    <el-alert
      title="支持 .xlsx / .xls 文件，单次最多导入 500 行，文件大小不超过 5MB"
      type="info"
      :closable="false"
      show-icon
      class="import-tip"
    />

    <div class="import-guide">
      <div class="guide-head">
        <div>
          <strong>导入列顺序（共 {{ columns.length }} 列）</strong>
          <p>学号可留空；如填写，必须与当前登录账号的学号一致</p>
        </div>
        <el-button link type="primary" :icon="Download" @click="downloadTemplate">
          下载 Excel 模板
        </el-button>
      </div>
      <div class="column-tags">
        <span v-for="(column, index) in columns" :key="column">
          <em>{{ index + 1 }}</em>
          {{ column }}
        </span>
      </div>
      <p class="guide-note">
        同一学年学期已存在记录时会覆盖更新；Excel 中留空的项目会保留已有值，不会清空。
      </p>
    </div>

    <el-upload
      drag
      action="#"
      accept=".xlsx,.xls"
      :show-file-list="false"
      :disabled="importing"
      :http-request="handleUpload"
      class="import-upload"
    >
      <el-icon class="upload-icon"><UploadFilled /></el-icon>
      <div class="el-upload__text">
        {{ importing ? '正在导入，请稍候...' : '拖拽 Excel 文件到此处，或点击选择文件' }}
      </div>
      <template #tip>
        <div class="el-upload__tip">
          建议先下载模板填写，避免列顺序或日期格式错误
        </div>
      </template>
    </el-upload>

    <div v-if="result" class="import-result">
      <div class="result-grid">
        <div>
          <span>有效行数</span>
          <strong>{{ result.total }}</strong>
        </div>
        <div class="result-inserted">
          <span>新增</span>
          <strong>{{ result.inserted }}</strong>
        </div>
        <div class="result-updated">
          <span>覆盖更新</span>
          <strong>{{ result.updated }}</strong>
        </div>
        <div :class="{ 'result-failed': result.failed > 0 }">
          <span>失败</span>
          <strong>{{ result.failed }}</strong>
        </div>
      </div>

      <el-alert
        v-if="result.failed > 0"
        :title="`${result.failed} 条数据导入失败`"
        type="warning"
        :closable="false"
        show-icon
        class="result-alert"
      >
        <ul class="error-list">
          <li v-for="(error, index) in result.errors" :key="index">
            {{ error }}
          </li>
        </ul>
      </el-alert>
      <el-alert
        v-else
        title="全部数据导入成功"
        type="success"
        :closable="false"
        show-icon
        class="result-alert"
      />
    </div>

    <template #footer>
      <el-button type="primary" @click="visible = false">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.import-tip {
  margin-bottom: 16px;
}

.import-guide {
  padding: 16px;
  margin-bottom: 16px;
  background: #f8fbfa;
  border: 1px solid #e7f0ec;
  border-radius: 12px;
}

.guide-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.guide-head strong {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  color: var(--hp-text);
}

.guide-head p {
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.column-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.column-tags span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 7px;
  font-size: 10.5px;
  color: #4f6b62;
  background: #fff;
  border: 1px solid #e4eee9;
  border-radius: 5px;
}

.column-tags em {
  font-style: normal;
  font-weight: 700;
  color: var(--hp-primary);
}

.guide-note {
  margin-top: 10px;
  font-size: 11px;
  line-height: 1.6;
  color: var(--hp-text-muted);
}

.import-upload {
  width: 100%;
}

.import-upload :deep(.el-upload-dragger) {
  padding: 26px 20px;
  background: #fbfdfc;
  border-color: #d9e9e3;
}

.upload-icon {
  margin-bottom: 8px;
  font-size: 40px;
  color: #9fc8ba;
}

.import-upload :deep(.el-upload__text) {
  font-size: 13px;
  color: var(--hp-text-secondary);
}

.import-upload :deep(.el-upload__tip) {
  color: var(--hp-text-muted);
}

.import-result {
  margin-top: 18px;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.result-grid > div {
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 12px 14px;
  background: #f7faf9;
  border: 1px solid #edf3f0;
  border-radius: 10px;
}

.result-grid span {
  font-size: 10.5px;
  color: var(--hp-text-muted);
}

.result-grid strong {
  font-size: 21px;
  font-weight: 700;
  color: var(--hp-text);
}

.result-inserted strong {
  color: var(--hp-primary);
}

.result-updated strong {
  color: #3f7ff0;
}

.result-failed {
  background: #fff5f5 !important;
  border-color: #f4dede !important;
}

.result-failed strong {
  color: var(--hp-danger);
}

.result-alert {
  margin-top: 12px;
}

.error-list {
  max-height: 160px;
  padding-left: 16px;
  margin: 6px 0 0;
  overflow: auto;
  font-size: 11.5px;
  line-height: 1.7;
  color: #8a681d;
}

.error-list li + li {
  margin-top: 3px;
}

@media (max-width: 600px) {
  .result-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .guide-head {
    flex-direction: column;
  }
}
</style>
