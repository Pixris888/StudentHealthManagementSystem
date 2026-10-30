<script setup lang="ts">
import { reactive, ref, type Component } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  ArrowRight,
  Calendar,
  Iphone,
  Lock,
  MagicStick,
  Notebook,
  Reading,
  User,
} from '@element-plus/icons-vue'
import AppDownloadDialog from '@/components/app/AppDownloadDialog.vue'
import { useUserStore } from '@/stores/user'
import { getRememberedUsername, rememberUsername } from '@/utils/auth'
import {
  resolveAppDownloadUrl,
  shouldOpenDownloadDirectly,
} from '@/utils/app-download'
import type { LoginForm } from '@/types/system'

interface FeatureItem {
  icon: Component
  title: string
  desc: string
}

const features: FeatureItem[] = [
  {
    icon: Notebook,
    title: '健康档案',
    desc: '体测成绩与健康指标趋势，一屏掌握',
  },
  {
    icon: Calendar,
    title: '每日打卡',
    desc: '睡眠、运动、饮食、情绪记录打卡',
  },
  {
    icon: MagicStick,
    title: 'AI 健康助手',
    desc: '个性化健康问答与作息运动计划',
  },
  {
    icon: Reading,
    title: '健康科普',
    desc: '权威健康知识库与健康公告',
  },
]

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const remembered = getRememberedUsername()
const form = reactive<LoginForm>({
  username: remembered,
  password: '',
})
const rememberMe = ref(Boolean(remembered))
const loading = ref(false)
const downloadVisible = ref(false)
const formRef = ref<FormInstance>()

const rules: FormRules<LoginForm> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    { max: 50, message: '账号长度不能超过 50 个字符', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入登录密码', trigger: 'blur' },
    { max: 72, message: '密码长度不能超过 72 个字符', trigger: 'blur' },
  ],
}

function handleDownloadApp(): void {
  if (shouldOpenDownloadDirectly()) {
    window.location.href = resolveAppDownloadUrl()
    return
  }
  downloadVisible.value = true
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.login(form)
    rememberUsername(rememberMe.value ? form.username.trim() : '')
    ElMessage.success('登录成功，欢迎回来')
    const redirect =
      typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    const target =
      redirect.startsWith('/') && !redirect.startsWith('//') && redirect !== '/login'
        ? redirect
        : '/home'
    await router.replace(target)
  } catch {
    // 登录失败提示由请求拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <section class="login-brand">
      <div class="brand-orb orb-one" />
      <div class="brand-orb orb-two" />
      <div class="brand-orb orb-three" />

      <div class="brand-top">
        <img src="/health-logo.svg" alt="健康管理平台" />
        <div class="brand-name">
          <span>大学生健康管理平台</span>
          <small>STUDENT HEALTH MANAGEMENT</small>
        </div>
      </div>

      <div class="brand-content">
        <p class="eyebrow">HEALTH MANAGEMENT · 2026</p>
        <h1>
          让每一次健康记录<br />
          都成为<span class="highlight">更好的自己</span>
        </h1>
        <p class="brand-desc">
          围绕健康档案、每日打卡、AI 健康助手与健康科普，
          构建属于大学生的健康管理平台。
        </p>

        <div class="feature-grid">
          <div
            v-for="item in features"
            :key="item.title"
            class="feature-item"
          >
            <div class="feature-icon">
              <el-icon :size="20"><component :is="item.icon" /></el-icon>
            </div>
            <div class="feature-text">
              <strong>{{ item.title }}</strong>
              <span>{{ item.desc }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="brand-footer">
        <span>大学生健康管理平台</span>
        <span>健康 · 学习 · 生活</span>
      </div>
    </section>

    <section class="login-panel">
      <div class="mobile-brand">
        <img src="/health-logo.svg" alt="健康管理平台" />
        <span>大学生健康管理平台</span>
      </div>

      <div class="login-card">
        <div class="login-header">
          <p class="login-eyebrow">WELCOME BACK</p>
          <h2>欢迎回来</h2>
          <p>登录大学生健康管理平台，开启今日健康管理</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          class="login-form"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="请输入登录账号"
              :prefix-icon="User"
              clearable
              autocomplete="username"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入登录密码"
              :prefix-icon="Lock"
              show-password
              autocomplete="current-password"
            />
          </el-form-item>

          <div class="login-options">
            <el-checkbox v-model="rememberMe">记住账号</el-checkbox>
            <span class="help-text">忘记密码请联系管理员</span>
          </div>

          <el-button
            class="login-button"
            type="primary"
            :loading="loading"
            @click="handleLogin"
          >
            <span>{{ loading ? '登录中...' : '登 录' }}</span>
            <el-icon v-if="!loading" class="login-arrow">
              <ArrowRight />
            </el-icon>
          </el-button>
        </el-form>

        <div class="register-row">
          <span>还没有账号？</span>
          <router-link class="register-link" to="/register">立即注册</router-link>
        </div>

        <button class="download-app-link" type="button" @click="handleDownloadApp">
          <el-icon><Iphone /></el-icon>
          下载学生端 App
        </button>
      </div>

      <p class="login-copyright">
        © 2026 大学生健康管理平台
      </p>
    </section>

    <AppDownloadDialog v-model="downloadVisible" />
  </div>
</template>

<style scoped>
.login-page {
  display: grid;
  grid-template-columns: minmax(0, 1.15fr) minmax(440px, 0.85fr);
  min-height: 100vh;
  background: #f4faf7;
}

/* ---------------- 左侧品牌区 ---------------- */
.login-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 64px 34px;
  overflow: hidden;
  color: #eafaf3;
  background:
    radial-gradient(circle at 88% 12%, rgba(61, 220, 151, 0.2), transparent 38%),
    radial-gradient(circle at 8% 88%, rgba(61, 220, 151, 0.12), transparent 34%),
    linear-gradient(145deg, #072b25 0%, #0d4439 46%, #116050 100%);
}

.login-brand::before {
  position: absolute;
  inset: 0;
  content: '';
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.035) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.035) 1px, transparent 1px);
  background-size: 44px 44px;
  mask-image: radial-gradient(circle at 55% 42%, #000 0%, transparent 78%);
}

.brand-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(2px);
  opacity: 0.55;
  animation: float 9s ease-in-out infinite;
}

.orb-one {
  top: 18%;
  right: 9%;
  width: 150px;
  height: 150px;
  background: radial-gradient(circle at 35% 35%, rgba(123, 240, 187, 0.5), transparent 68%);
}

.orb-two {
  right: 24%;
  bottom: 12%;
  width: 90px;
  height: 90px;
  background: radial-gradient(circle at 35% 35%, rgba(61, 220, 151, 0.42), transparent 70%);
  animation-delay: -3s;
}

.orb-three {
  top: 48%;
  left: -40px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, rgba(61, 220, 151, 0.14), transparent 70%);
  animation-delay: -6s;
}

@keyframes float {
  0%,
  100% {
    transform: translate3d(0, 0, 0) scale(1);
  }
  50% {
    transform: translate3d(0, -18px, 0) scale(1.04);
  }
}

.brand-top,
.brand-content,
.brand-footer {
  position: relative;
  z-index: 1;
}

.brand-top {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-top img {
  width: 46px;
  height: 46px;
  border-radius: 13px;
  box-shadow: 0 10px 26px rgba(61, 220, 151, 0.24);
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.brand-name span {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
  color: #fff;
}

.brand-name small {
  font-size: 9px;
  letter-spacing: 2px;
  color: rgba(255, 255, 255, 0.45);
}

.brand-content {
  max-width: 640px;
  margin: 48px 0;
}

.eyebrow {
  margin-bottom: 20px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 3px;
  color: #7bf0bb;
}

.brand-content h1 {
  font-size: clamp(38px, 4.1vw, 58px);
  font-weight: 700;
  line-height: 1.22;
  letter-spacing: 0.5px;
  color: #f4fffb;
}

.highlight {
  color: transparent;
  background: linear-gradient(90deg, #7bf0bb, #3ddc97 60%, #a2f7cf);
  background-clip: text;
  -webkit-background-clip: text;
}

.brand-desc {
  max-width: 520px;
  margin-top: 22px;
  font-size: 14.5px;
  line-height: 1.9;
  color: rgba(234, 250, 243, 0.62);
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
  max-width: 620px;
  margin-top: 38px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 15px 16px;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  backdrop-filter: blur(8px);
  transition: background 0.25s ease, transform 0.25s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.09);
  transform: translateY(-2px);
}

.feature-icon {
  display: grid;
  flex-shrink: 0;
  place-items: center;
  width: 38px;
  height: 38px;
  color: #7bf0bb;
  background: rgba(61, 220, 151, 0.14);
  border-radius: 11px;
}

.feature-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.feature-text strong {
  font-size: 13.5px;
  font-weight: 600;
  color: #f0fff9;
}

.feature-text span {
  font-size: 11.5px;
  line-height: 1.55;
  color: rgba(234, 250, 243, 0.5);
}

.brand-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  font-size: 11.5px;
  color: rgba(255, 255, 255, 0.36);
}

/* ---------------- 右侧登录区 ---------------- */
.login-panel {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background:
    radial-gradient(circle at 100% 0%, rgba(61, 220, 151, 0.12), transparent 30%),
    #f4faf7;
}

.login-card {
  position: relative;
  width: 100%;
  max-width: 420px;
  padding: 40px 38px 30px;
  overflow: hidden;
  background: #fff;
  border: 1px solid #e5efeb;
  border-radius: 24px;
  box-shadow: 0 28px 80px rgba(15, 74, 64, 0.11);
}

.login-card::before {
  position: absolute;
  top: 0;
  right: 0;
  left: 0;
  height: 4px;
  content: '';
  background: linear-gradient(90deg, #1f9d7a, #3ddc97, #7bf0bb);
}

.login-header {
  margin-bottom: 28px;
}

.login-eyebrow {
  margin-bottom: 10px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 2.5px;
  color: var(--hp-primary);
}

.login-header h2 {
  margin-bottom: 8px;
  font-size: 27px;
  font-weight: 700;
  color: var(--hp-text);
}

.login-header > p:last-child {
  font-size: 13px;
  color: var(--hp-text-secondary);
}

.login-form :deep(.el-form-item) {
  margin-bottom: 20px;
}

.login-form :deep(.el-input__wrapper) {
  height: 48px;
  padding: 0 14px;
  background: #f8fbfa;
  border-radius: 11px;
  box-shadow: 0 0 0 1px #e3ece8 inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c9e2d9 inset;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  background: #fff;
  box-shadow: 0 0 0 1px var(--hp-primary) inset;
}

.login-form :deep(.el-input__prefix) {
  color: #8aa198;
}

.login-options {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -2px 0 22px;
}

.help-text {
  font-size: 12px;
  color: var(--hp-text-muted);
}

.login-button {
  width: 100%;
  height: 48px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #1f9d7a, #17b37f);
  border: none;
  border-radius: 11px;
  box-shadow: 0 12px 26px rgba(31, 157, 122, 0.26);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.login-button:hover {
  background: linear-gradient(135deg, #19906f, #14a875);
  box-shadow: 0 16px 32px rgba(31, 157, 122, 0.32);
  transform: translateY(-1px);
}

.login-arrow {
  margin-left: 8px;
}

.register-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 20px;
  font-size: 13px;
  color: var(--hp-text-muted);
}

.register-link {
  color: var(--hp-primary);
  font-weight: 600;
}

.register-link:hover {
  color: var(--hp-primary-dark);
}

.download-app-link {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  margin-top: 12px;
  padding: 8px;
  font-size: 12.5px;
  color: var(--hp-text-muted);
  cursor: pointer;
  background: transparent;
  border: none;
  border-radius: 8px;
  transition: color 0.2s ease, background 0.2s ease;
}

.download-app-link:hover {
  color: var(--hp-primary);
  background: #f4faf7;
}

.login-copyright {
  margin-top: 24px;
  font-size: 11.5px;
  color: #9bb0a8;
}

.mobile-brand {
  display: none;
}

/* ---------------- 响应式 ---------------- */
@media (max-width: 1100px) {
  .login-brand {
    padding: 40px 40px 30px;
  }

  .feature-grid {
    gap: 10px;
    max-width: 560px;
    margin-top: 30px;
  }

  .feature-item {
    padding: 12px 13px;
  }
}

@media (max-width: 960px) {
  .login-page {
    display: block;
    min-height: 100vh;
    background:
      linear-gradient(180deg, #0b3b33 0%, #0f5145 34%, #f4faf7 34%, #f4faf7 100%);
  }

  .login-brand {
    display: none;
  }

  .login-panel {
    justify-content: flex-start;
    min-height: 100vh;
    padding: 44px 20px 28px;
    background: transparent;
  }

  .mobile-brand {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 28px;
    font-size: 16px;
    font-weight: 700;
    color: #fff;
  }

  .mobile-brand img {
    width: 36px;
    height: 36px;
    border-radius: 10px;
  }

  .login-card {
    max-width: 440px;
    padding: 34px 26px 26px;
  }

  .login-copyright {
    color: #7d988e;
  }
}
</style>
