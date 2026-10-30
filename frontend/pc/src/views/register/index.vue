<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  CircleCheck,
  Lock,
  Message,
  OfficeBuilding,
  Phone,
  Postcard,
  Reading,
  Ticket,
  User,
} from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { validatePassword } from '@/utils/password'
import type { RegisterForm } from '@/types/system'

const router = useRouter()
const userStore = useUserStore()

const form = reactive<RegisterForm>({
  username: '',
  password: '',
  confirmPassword: '',
  realName: '',
  studentNo: '',
  gender: 0,
  phone: '',
  email: '',
  deptName: '',
})
const loading = ref(false)
const formRef = ref<FormInstance>()

const rules: FormRules<RegisterForm> = {
  username: [
    { required: true, message: '请输入登录账号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z][A-Za-z0-9_]{3,19}$/,
      message: '账号需为 4-20 位，以字母开头，仅包含字母、数字、下划线',
      trigger: 'blur',
    },
  ],
  realName: [
    { required: true, message: '请输入真实姓名', trigger: 'blur' },
    { max: 50, message: '真实姓名不能超过 50 个字符', trigger: 'blur' },
  ],
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    {
      pattern: /^[A-Za-z0-9]{4,30}$/,
      message: '学号需为 4-30 位字母或数字',
      trigger: 'blur',
    },
  ],
  password: [
    { required: true, message: '请设置登录密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        const message = validatePassword(String(value ?? ''), [
          form.username,
          form.studentNo,
        ])
        callback(message ? new Error(message) : undefined)
      },
      trigger: 'blur',
    },
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (_rule, value, callback) => {
        callback(
          value === form.password ? undefined : new Error('两次输入的密码不一致'),
        )
      },
      trigger: 'blur',
    },
  ],
  phone: [
    {
      validator: (_rule, value, callback) => {
        if (!value || /^1[3-9]\d{9}$/.test(String(value))) {
          callback()
          return
        }
        callback(new Error('手机号格式不正确'))
      },
      trigger: 'blur',
    },
  ],
  email: [
    {
      validator: (_rule, value, callback) => {
        if (
          !value ||
          /^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$/.test(String(value))
        ) {
          callback()
          return
        }
        callback(new Error('邮箱格式不正确'))
      },
      trigger: 'blur',
    },
  ],
  deptName: [{ max: 100, message: '院系名称不能超过 100 个字符', trigger: 'blur' }],
}

async function handleRegister(): Promise<void> {
  if (loading.value) return
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.register({
      username: form.username.trim(),
      password: form.password,
      confirmPassword: form.confirmPassword,
      realName: form.realName.trim(),
      studentNo: form.studentNo.trim(),
      gender: form.gender ?? 0,
      phone: form.phone?.trim() || undefined,
      email: form.email?.trim() || undefined,
      deptName: form.deptName?.trim() || undefined,
    })
    ElMessage.success('注册成功，已为你自动登录')
    await router.replace('/home')
  } catch {
    // 失败提示由请求拦截器统一弹出
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="register-page">
    <div class="register-shell">
      <aside class="register-brand">
        <div class="brand-orb orb-one" />
        <div class="brand-orb orb-two" />

        <div class="brand-top">
          <img src="/health-logo.svg" alt="健康管理平台" />
          <div class="brand-name">
            <span>大学生健康管理平台</span>
            <small>STUDENT HEALTH MANAGEMENT</small>
          </div>
        </div>

        <div class="brand-content">
          <p class="eyebrow">CREATE ACCOUNT</p>
          <h1>创建你的<br /><span>健康账号</span></h1>
          <p class="brand-desc">
            注册学生账号，开始记录健康档案、每日打卡，并获得适合自己的健康建议。
          </p>

          <ul class="brand-points">
            <li>
              <el-icon><CircleCheck /></el-icon>
              <span>仅创建学生账号，角色由系统自动分配</span>
            </li>
            <li>
              <el-icon><Lock /></el-icon>
              <span>密码加密存储，页面不会展示或记录明文</span>
            </li>
            <li>
              <el-icon><Reading /></el-icon>
              <span>注册成功后即可使用全部学生功能</span>
            </li>
          </ul>
        </div>

        <p class="brand-footer">健康 · 学习 · 生活</p>
      </aside>

      <main class="register-panel">
        <div class="register-card">
          <div class="register-header">
            <p class="eyebrow">JOIN US</p>
            <h2>注册学生账号</h2>
            <p>请填写真实信息，带 <em>*</em> 的为必填项</p>
          </div>

          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
            size="large"
            class="register-form"
            @keyup.enter="handleRegister"
          >
            <div class="form-grid">
              <el-form-item label="登录账号" prop="username">
                <el-input
                  v-model.trim="form.username"
                  placeholder="4-20 位，字母开头"
                  :prefix-icon="User"
                  maxlength="20"
                  autocomplete="username"
                />
              </el-form-item>

              <el-form-item label="真实姓名" prop="realName">
                <el-input
                  v-model.trim="form.realName"
                  placeholder="请输入真实姓名"
                  :prefix-icon="Postcard"
                  maxlength="50"
                />
              </el-form-item>

              <el-form-item label="学号" prop="studentNo">
                <el-input
                  v-model.trim="form.studentNo"
                  placeholder="4-30 位字母或数字"
                  :prefix-icon="Ticket"
                  maxlength="30"
                />
              </el-form-item>

              <el-form-item label="性别">
                <el-select v-model="form.gender" placeholder="请选择">
                  <el-option :value="0" label="未填写" />
                  <el-option :value="1" label="男" />
                  <el-option :value="2" label="女" />
                </el-select>
              </el-form-item>

              <el-form-item label="登录密码" prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  show-password
                  placeholder="8-32 位，至少三类字符"
                  :prefix-icon="Lock"
                  maxlength="32"
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入密码"
                  :prefix-icon="Lock"
                  maxlength="32"
                  autocomplete="new-password"
                />
              </el-form-item>

              <el-form-item label="手机号（选填）" prop="phone">
                <el-input
                  v-model.trim="form.phone"
                  placeholder="请输入手机号"
                  :prefix-icon="Phone"
                  maxlength="20"
                />
              </el-form-item>

              <el-form-item label="邮箱（选填）" prop="email">
                <el-input
                  v-model.trim="form.email"
                  placeholder="请输入常用邮箱"
                  :prefix-icon="Message"
                  maxlength="100"
                />
              </el-form-item>

              <el-form-item label="院系（选填）" prop="deptName" class="span-2">
                <el-input
                  v-model.trim="form.deptName"
                  placeholder="如 计算机学院"
                  :prefix-icon="OfficeBuilding"
                  maxlength="100"
                />
              </el-form-item>
            </div>

            <el-button
              class="register-button"
              type="primary"
              :loading="loading"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '注 册' }}
            </el-button>
          </el-form>

          <p class="register-tip">
            已有账号？<router-link to="/login">返回登录</router-link>
          </p>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.register-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  padding: 32px;
  background:
    radial-gradient(circle at 12% 10%, rgba(61, 220, 151, 0.16), transparent 32%),
    radial-gradient(circle at 88% 90%, rgba(31, 157, 122, 0.12), transparent 34%),
    #f4faf7;
}

.register-shell {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  width: min(1120px, 100%);
  min-height: 720px;
  overflow: hidden;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 30px 80px rgba(16, 64, 52, 0.16);
}

/* ---------------- 左侧品牌区 ---------------- */
.register-brand {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 44px 46px;
  overflow: hidden;
  color: #eafaf3;
  background:
    radial-gradient(circle at 85% 12%, rgba(61, 220, 151, 0.2), transparent 38%),
    linear-gradient(150deg, #072b25 0%, #0d4439 48%, #116050 100%);
}

.brand-orb {
  position: absolute;
  border-radius: 50%;
  background: rgba(61, 220, 151, 0.08);
}

.orb-one {
  width: 320px;
  height: 320px;
  top: -140px;
  right: -100px;
}

.orb-two {
  width: 220px;
  height: 220px;
  bottom: -90px;
  left: -80px;
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
  gap: 12px;
}

.brand-top img {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.12);
}

.brand-name {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-name span {
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.brand-name small {
  font-size: 9.5px;
  letter-spacing: 1.5px;
  color: rgba(234, 250, 243, 0.6);
}

.eyebrow {
  margin-bottom: 16px;
  font-size: 11px;
  font-weight: 600;
  letter-spacing: 2.5px;
  color: #3ddc97;
}

.brand-content h1 {
  font-size: 40px;
  font-weight: 700;
  line-height: 1.25;
  letter-spacing: -0.5px;
}

.brand-content h1 span {
  color: #3ddc97;
}

.brand-desc {
  max-width: 360px;
  margin-top: 20px;
  font-size: 14px;
  line-height: 1.9;
  color: rgba(234, 250, 243, 0.72);
}

.brand-points {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 0;
  margin: 34px 0 0;
  list-style: none;
}

.brand-points li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: rgba(234, 250, 243, 0.78);
}

.brand-points .el-icon {
  margin-top: 2px;
  color: #3ddc97;
}

.brand-footer {
  font-size: 11px;
  letter-spacing: 2px;
  color: rgba(234, 250, 243, 0.45);
}

/* ---------------- 右侧表单区 ---------------- */
.register-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 44px 54px;
  background: #ffffff;
}

.register-card {
  width: 100%;
  max-width: 640px;
}

.register-header {
  margin-bottom: 26px;
}

.register-header .eyebrow {
  margin-bottom: 10px;
  color: var(--hp-primary);
}

.register-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--hp-text);
}

.register-header p {
  margin-top: 8px;
  font-size: 13px;
  color: var(--hp-text-muted);
}

.register-header em {
  font-style: normal;
  color: var(--hp-danger);
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 18px;
}

.form-grid .span-2 {
  grid-column: 1 / -1;
}

.register-form :deep(.el-form-item__label) {
  padding-bottom: 6px;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--hp-text-secondary);
}

.register-form :deep(.el-input__wrapper),
.register-form :deep(.el-select__wrapper) {
  min-height: 44px;
  padding: 0 13px;
  background: #f8fbfa;
  border-radius: 10px;
  box-shadow: 0 0 0 1px var(--hp-border) inset;
  transition: box-shadow 0.2s ease, background 0.2s ease;
}

.register-form :deep(.el-input__wrapper:hover),
.register-form :deep(.el-select__wrapper:hover) {
  box-shadow: 0 0 0 1px #c9e2d9 inset;
}

.register-form :deep(.el-input__wrapper.is-focus),
.register-form :deep(.el-select__wrapper.is-focused) {
  background: #ffffff;
  box-shadow: 0 0 0 1px var(--hp-primary) inset;
}

.register-form :deep(.el-select) {
  width: 100%;
}

.register-button {
  width: 100%;
  height: 48px;
  margin-top: 6px;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 3px;
  background: linear-gradient(135deg, #1f9d7a, #17b37f);
  border: none;
  border-radius: 11px;
  box-shadow: 0 12px 26px rgba(31, 157, 122, 0.24);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.register-button:hover {
  background: linear-gradient(135deg, #19906f, #14a875);
  box-shadow: 0 16px 32px rgba(31, 157, 122, 0.3);
  transform: translateY(-1px);
}

.register-tip {
  margin-top: 20px;
  font-size: 13px;
  text-align: center;
  color: var(--hp-text-muted);
}

.register-tip a {
  color: var(--hp-primary);
  font-weight: 600;
}

@media (max-width: 900px) {
  .register-page {
    align-items: flex-start;
    padding: 18px;
  }

  .register-shell {
    grid-template-columns: 1fr;
    min-height: auto;
  }

  .register-brand {
    display: none;
  }

  .register-panel {
    padding: 30px 22px;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .form-grid .span-2 {
    grid-column: auto;
  }

  .register-header h2 {
    font-size: 24px;
  }
}
</style>
