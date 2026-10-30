<script setup lang="ts">
import { onHide, onShow } from '@dcloudio/uni-app'
import { useMessageStore } from '@/stores/message'
import { useUserStore } from '@/stores/user'

onShow(() => {
  const userStore = useUserStore()
  if (userStore.accessToken) {
    useMessageStore().startPolling()
  }
})

onHide(() => {
  useMessageStore().stopPolling()
})
</script>

<style>
page {
  background: #f4f7f6;
  color: #0f172a;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei',
    'Helvetica Neue', Arial, sans-serif;
  font-size: 28rpx;
  line-height: 1.5;
}

view,
text,
button,
input,
textarea,
scroll-view,
swiper,
swiper-item,
image {
  box-sizing: border-box;
}

button::after {
  border: none;
}

.page {
  min-height: 100vh;
  padding: 24rpx 24rpx 48rpx;
}

.page-with-fixed {
  padding-bottom: calc(190rpx + env(safe-area-inset-bottom));
}

.page-with-tab {
  padding-bottom: calc(140rpx + env(safe-area-inset-bottom));
}

.card {
  width: 100%;
  padding: 28rpx;
  border-radius: 28rpx;
  background: #ffffff;
  box-shadow: 0 10rpx 34rpx rgba(15, 118, 110, 0.06);
}

.card + .card {
  margin-top: 24rpx;
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.card-title {
  display: flex;
  align-items: center;
  font-size: 31rpx;
  font-weight: 700;
  color: #0f172a;
}

.card-subtitle {
  margin-top: 8rpx;
  font-size: 24rpx;
  color: #94a3b8;
  line-height: 1.6;
}

.row {
  display: flex;
  align-items: center;
}

.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.grow {
  flex: 1;
  min-width: 0;
}

.muted {
  color: #94a3b8;
}

.primary-text {
  color: #0f9d8a;
}

.text-ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.text-clamp-2 {
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 40rpx;
  padding: 0 14rpx;
  border-radius: 999rpx;
  background: #eef4f2;
  color: #64748b;
  font-size: 21rpx;
  line-height: 1;
}

.tag-primary {
  background: #e6f7f3;
  color: #0f766e;
}

.tag-success {
  background: #e8f8ef;
  color: #15803d;
}

.tag-warning {
  background: #fff7e6;
  color: #b45309;
}

.tag-danger {
  background: #fef0f0;
  color: #dc2626;
}

.tag-info {
  background: #eef2f7;
  color: #64748b;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 88rpx;
  padding: 0 36rpx;
  border-radius: 999rpx;
  font-size: 29rpx;
  font-weight: 600;
  line-height: 1;
  border: none;
}

.btn::after {
  border: none;
}

.btn-primary {
  color: #ffffff;
  background: linear-gradient(135deg, #12b39c 0%, #0b7f70 100%);
  box-shadow: 0 12rpx 26rpx rgba(13, 148, 136, 0.24);
}

.btn-primary[disabled] {
  opacity: 0.55;
  box-shadow: none;
}

.btn-ghost {
  color: #0f766e;
  background: #e6f7f3;
}

.btn-plain {
  color: #64748b;
  background: #f1f5f4;
}

.btn-danger {
  color: #dc2626;
  background: #fef2f2;
}

.btn-block {
  width: 100%;
}

.btn-sm {
  height: 64rpx;
  padding: 0 24rpx;
  font-size: 25rpx;
}

.form-item {
  margin-top: 26rpx;
}

.form-label {
  display: flex;
  align-items: center;
  margin-bottom: 12rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #475569;
}

.form-label .required {
  margin-left: 6rpx;
  color: #ef4444;
}

.input-box {
  display: flex;
  align-items: center;
  width: 100%;
  min-height: 80rpx;
  padding: 0 22rpx;
  border-radius: 18rpx;
  background: #f6f9f8;
  border: 2rpx solid transparent;
}

.input-box:focus-within {
  border-color: #bbf0e6;
  background: #ffffff;
}

.input {
  flex: 1;
  height: 76rpx;
  font-size: 28rpx;
  color: #0f172a;
}

.input-placeholder {
  color: #b1bfba;
}

.textarea-box {
  width: 100%;
  padding: 18rpx 22rpx;
  border-radius: 18rpx;
  background: #f6f9f8;
  border: 2rpx solid transparent;
}

.textarea {
  width: 100%;
  min-height: 150rpx;
  font-size: 28rpx;
  line-height: 1.65;
  color: #0f172a;
}

.picker-value {
  flex: 1;
  font-size: 28rpx;
  color: #0f172a;
}

.picker-placeholder {
  color: #b1bfba;
}

.divider {
  height: 1rpx;
  background: #edf3f1;
}

.loading-block {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60rpx 0;
}

.section-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 34rpx 4rpx 18rpx;
}

.section-title {
  display: block;
  font-size: 33rpx;
  font-weight: 700;
  color: #0f172a;
}

.section-desc {
  display: block;
  margin-top: 4rpx;
  font-size: 23rpx;
  color: #94a3b8;
}

.section-link {
  font-size: 24rpx;
  color: #0f9d8a;
}

.hero-gradient {
  background: linear-gradient(135deg, #0f9d8a 0%, #0b7a6b 58%, #0a6a61 100%);
}

.safe-bottom {
  padding-bottom: env(safe-area-inset-bottom);
}

.fade-in {
  animation: fadeIn 0.28s ease both;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(8rpx);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

::-webkit-scrollbar {
  width: 0;
  height: 0;
  background: transparent;
}
</style>
