<template>
  <scroll-view scroll-x class="seg-scroll" :show-scrollbar="false">
    <view class="seg-wrap">
      <view
        v-for="item in options"
        :key="String(item.value)"
        class="seg-item"
        :class="{ active: item.value === modelValue }"
        @click="select(item.value)"
      >
        <text class="seg-label">{{ item.label }}</text>
        <text v-if="item.badge" class="seg-badge">{{ item.badge > 99 ? '99+' : item.badge }}</text>
      </view>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
interface SegTabOption {
  value: string | number
  label: string
  badge?: number
}

const props = defineProps<{
  modelValue: string | number
  options: SegTabOption[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string | number]
  change: [value: string | number]
}>()

function select(value: string | number): void {
  if (value === props.modelValue) return
  emit('update:modelValue', value)
  emit('change', value)
}
</script>

<style scoped>
.seg-scroll {
  width: 100%;
  white-space: nowrap;
}

.seg-wrap {
  display: inline-flex;
  align-items: center;
  padding: 6rpx;
  gap: 6rpx;
  background: #eef4f2;
  border-radius: 999rpx;
}

.seg-item {
  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 128rpx;
  height: 64rpx;
  padding: 0 26rpx;
  border-radius: 999rpx;
  transition: all 0.2s ease;
}

.seg-item.active {
  background: #ffffff;
  box-shadow: 0 6rpx 18rpx rgba(15, 118, 110, 0.12);
}

.seg-label {
  font-size: 26rpx;
  color: #64748b;
  white-space: nowrap;
}

.seg-item.active .seg-label {
  color: #0f766e;
  font-weight: 600;
}

.seg-badge {
  margin-left: 8rpx;
  min-width: 30rpx;
  height: 30rpx;
  padding: 0 8rpx;
  border-radius: 999rpx;
  background: #ef4444;
  color: #ffffff;
  font-size: 19rpx;
  line-height: 30rpx;
  text-align: center;
}
</style>
