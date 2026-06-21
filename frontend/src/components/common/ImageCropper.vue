<script setup lang="ts">
import {nextTick, onBeforeUnmount, ref, watch} from 'vue'
import Cropper from 'cropperjs'

const props = withDefaults(defineProps<{
  /** 用户选择的原始图片 File */
  file: File | null
  /** 裁剪区域边长 */
  size?: number
}>(), {
  size: 256,
})

const emit = defineEmits<{
  /** 裁剪完成，导出压缩后的 Blob */
  cropped: [blob: Blob]
  /** 取消裁剪 */
  cancel: []
}>()

const imageRef = ref<HTMLImageElement | null>(null)
const zoomValue = ref(0)
let cropper: Cropper | null = null

/** 当前图片的 data URL */
const imageUrl = ref('')

watch(() => props.file, (file) => {
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    imageUrl.value = reader.result as string
    nextTick(initCropper)
  }
  reader.readAsDataURL(file)
}, {immediate: true})

function initCropper() {
  destroyCropper()
  if (!imageRef.value) return
  cropper = new Cropper(imageRef.value, {
    aspectRatio: 1,
    viewMode: 1,
    dragMode: 'move',
    autoCropArea: 1,
    cropBoxMovable: true,
    cropBoxResizable: true,
    zoomable: true,
    scalable: false,
    guides: true,
    center: true,
    highlight: false,
    background: true,
    responsive: true,
    minCropBoxWidth: 64,
    minCropBoxHeight: 64,
  })
}

function destroyCropper() {
  if (cropper) {
    cropper.destroy()
    cropper = null
  }
}

function onZoom(e: Event) {
  zoomValue.value = parseInt((e.target as HTMLInputElement).value)
  if (cropper) {
    const ratio = Math.pow(2, zoomValue.value / 100)
    cropper.zoomTo(ratio)
  }
}

function handleConfirm() {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas({
    width: props.size,
    height: props.size,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high',
  })
  // 导出为压缩 WebP，质量 0.8
  canvas.toBlob((blob) => {
    if (blob) {
      emit('cropped', blob)
    }
  }, 'image/webp', 0.8)
}

function handleCancel() {
  destroyCropper()
  imageUrl.value = ''
  emit('cancel')
}

onBeforeUnmount(() => {
  destroyCropper()
})
</script>

<template>
  <div class="cropper-wrap">
    <div class="cropper-container" v-if="imageUrl">
      <div class="img-wrapper">
        <img ref="imageRef" :src="imageUrl" alt="裁剪预览"/>
      </div>
      <div class="controls">
        <div class="zoom-row">
          <span class="zoom-label">缩放</span>
          <input
              type="range"
              min="-50"
              max="100"
              :value="zoomValue"
              class="zoom-slider"
              @input="onZoom"
          />
        </div>
        <div class="btn-row">
          <button class="btn btn-cancel" @click="handleCancel">重新选择</button>
          <button class="btn btn-confirm" @click="handleConfirm">确认裁剪</button>
        </div>
      </div>
    </div>
    <div v-else class="empty-hint">请选择图片</div>
  </div>
</template>

<style scoped>
.cropper-wrap {
  width: 100%;
}

.cropper-container {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.img-wrapper {
  max-height: 260px;
  overflow: hidden;
  border-radius: 8px;
  background: #f0f0f0;
}

.img-wrapper img {
  display: block;
  max-width: 100%;
}

.controls {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.zoom-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.zoom-label {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap;
}

.zoom-slider {
  flex: 1;
  height: 4px;
  accent-color: #2563eb;
}

.btn-row {
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background 0.15s;
}

.btn-cancel {
  background: #f4f5f7;
  color: #6b7280;
  border-color: #e0e2e6;
}

.btn-cancel:hover {
  background: #e8eaee;
}

.btn-confirm {
  background: #2563eb;
  color: #fff;
}

.btn-confirm:hover {
  background: #1d4ed8;
}

.empty-hint {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px;
}
</style>
