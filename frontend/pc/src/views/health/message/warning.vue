<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Clock,
  DataAnalysis,
  Promotion,
  Refresh,
  Warning,
} from '@element-plus/icons-vue'
import { runWarning } from '@/api/message'
import type { WarningRunResultVO } from '@/types/message'
import MessageNav from './components/MessageNav.vue'

function toDateText(date: Date): string {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

const yesterday = new Date()
yesterday.setDate(yesterday.getDate() - 1)

const scanDate = ref(toDateText(yesterday))
const running = ref(false)
const result = ref<WarningRunResultVO | null>(null)

function disabledDate(date: Date): boolean {
  const today = new Date()
  today.setHours(23, 59, 59, 999)
  return date.getTime() > today.getTime()
}

async function handleRun(): Promise<void> {
  if (!scanDate.value) {
    ElMessage.warning('请选择要扫描的日期')
    return
  }
  try {
    await ElMessageBox.confirm(
      `将扫描 ${scanDate.value} 的打卡数据并生成健康预警。重复执行不会重复发送，确定继续吗？`,
      '执行预警扫描',
      {
        type: 'warning',
        confirmButtonText: '立即扫描',
        cancelButtonText: '取消',
      },
    )
  } catch {
    return
  }

  running.value = true
  try {
    result.value = await runWarning(scanDate.value)
    ElMessage.success('预警扫描完成')
  } catch {
    // 未来日期或权限错误由拦截器展示后端 msg
  } finally {
    running.value = false
  }
}
</script>

<template>
  <div class="message-page">
    <div class="page-heading">
      <div>
        <span class="message-eyebrow">WARNING TASK</span>
        <h2>预警任务</h2>
        <p>按日期扫描健康打卡，为熬夜或运动不足的学生生成健康预警消息。</p>
      </div>
    </div>

    <MessageNav />

    <el-alert
      title="同一学生同一天同类型只生成一条预警，重复执行不会重复发送，可安全用于补跑。"
      type="info"
      :closable="false"
      show-icon
      class="warning-tip"
    />

    <section class="panel-card scan-card">
      <div class="scan-head">
        <div class="scan-title">
          <span class="scan-icon">
            <el-icon :size="22"><Clock /></el-icon>
          </span>
          <div>
            <h3>手动执行健康预警</h3>
            <p>默认扫描昨天，可选择今天或历史日期；未来日期不可选择。</p>
          </div>
        </div>

        <div class="scan-actions">
          <el-date-picker
            v-model="scanDate"
            type="date"
            value-format="YYYY-MM-DD"
            format="YYYY-MM-DD"
            placeholder="选择扫描日期"
            :disabled-date="disabledDate"
            :clearable="false"
            style="width: 180px"
          />
          <el-button
            type="primary"
            :icon="Promotion"
            :loading="running"
            @click="handleRun"
          >
            立即扫描
          </el-button>
        </div>
      </div>

      <div class="scan-note">
        <el-icon><Warning /></el-icon>
        <span>
          预警基于打卡中的“是否熬夜”和“运动是否达标”标记生成；数据质量异常的打卡会被跳过。
        </span>
      </div>
    </section>

    <section v-if="result" class="panel-card result-card">
      <div class="result-head">
        <div>
          <h3>扫描结果</h3>
          <p>扫描日期：{{ result.date }}</p>
        </div>
        <el-tag
          :type="result.locked ? 'success' : 'warning'"
          effect="light"
        >
          {{ result.locked ? '任务锁已获取' : '未获取任务锁' }}
        </el-tag>
      </div>

      <div class="result-stats">
        <div class="result-stat tone-blue">
          <span class="stat-icon">
            <el-icon :size="20"><DataAnalysis /></el-icon>
          </span>
          <div>
            <span class="stat-label">扫描打卡数</span>
            <strong class="stat-value">{{ result.scannedCount }}</strong>
          </div>
        </div>
        <div class="result-stat tone-green">
          <span class="stat-icon">
            <el-icon :size="20"><Promotion /></el-icon>
          </span>
          <div>
            <span class="stat-label">生成预警数</span>
            <strong class="stat-value">{{ result.generatedCount }}</strong>
          </div>
        </div>
        <div class="result-stat tone-orange">
          <span class="stat-icon">
            <el-icon :size="20"><Refresh /></el-icon>
          </span>
          <div>
            <span class="stat-label">跳过数</span>
            <strong class="stat-value">{{ result.skippedCount }}</strong>
          </div>
        </div>
        <div class="result-stat tone-teal">
          <span class="stat-icon">
            <el-icon :size="20"><Clock /></el-icon>
          </span>
          <div>
            <span class="stat-label">是否获取任务锁</span>
            <strong class="stat-value stat-text">
              {{ result.locked ? '是' : '否' }}
            </strong>
          </div>
        </div>
      </div>

      <el-alert
        :title="result.message"
        :type="result.locked ? 'success' : 'warning'"
        :closable="false"
        show-icon
        class="result-message"
      />

      <p v-if="!result.locked" class="lock-hint">
        该日期的预警任务正在其他请求中执行，请稍后重试。若确认没有其他任务在跑，可稍后重新扫描。
      </p>
    </section>
  </div>
</template>

<style scoped>
.message-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.message-eyebrow {
  display: block;
  margin-bottom: 6px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 2.2px;
  color: #9cb6ac;
}

.warning-tip {
  border-radius: 12px;
}

.scan-card {
  padding: 24px;
}

.scan-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.scan-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.scan-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 48px;
  height: 48px;
  color: var(--hp-primary);
  background: #e5f6ef;
  border-radius: 13px;
}

.scan-title h3 {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
}

.scan-title p {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.scan-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.scan-note {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px 14px;
  margin-top: 20px;
  font-size: 12px;
  line-height: 1.65;
  color: var(--hp-text-secondary);
  background: #f7faf9;
  border: 1px solid #e9f1ed;
  border-radius: 10px;
}

.scan-note .el-icon {
  flex-shrink: 0;
  margin-top: 2px;
  color: var(--hp-warning);
}

.result-card {
  padding: 24px;
}

.result-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 20px;
}

.result-head h3 {
  margin-bottom: 4px;
  font-size: 16px;
  font-weight: 700;
  color: var(--hp-text);
}

.result-head p {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.result-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.result-stat {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  background: #fbfdfc;
  border: 1px solid #edf3f0;
  border-radius: 13px;
}

.stat-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 11px;
}

.tone-blue .stat-icon {
  color: #3f7ff0;
  background: #eaf1ff;
}

.tone-green .stat-icon {
  color: #1f9d7a;
  background: #e3f6ef;
}

.tone-orange .stat-icon {
  color: #d98b2b;
  background: #fff3e2;
}

.tone-teal .stat-icon {
  color: #2f9d9d;
  background: #e6f6f6;
}

.stat-label {
  display: block;
  font-size: 11.5px;
  color: var(--hp-text-muted);
}

.stat-value {
  font-size: 24px;
  font-weight: 700;
  line-height: 1.3;
  color: var(--hp-text);
}

.stat-text {
  font-size: 18px;
}

.result-message {
  margin-top: 18px;
  border-radius: 10px;
}

.lock-hint {
  margin-top: 12px;
  font-size: 12px;
  line-height: 1.6;
  color: var(--hp-text-muted);
}

@media (max-width: 1000px) {
  .result-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .scan-actions {
    width: 100%;
  }

  .scan-actions :deep(.el-date-editor) {
    flex: 1;
  }

  .result-stats {
    grid-template-columns: 1fr;
  }
}
</style>
