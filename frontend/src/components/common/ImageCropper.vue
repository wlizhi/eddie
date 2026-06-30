<!--
 * @author Eddie
 * @date 2026-06-21
-->

<script setup lang="ts">
import {onBeforeUnmount, ref, watch} from 'vue'

const props = withDefaults(defineProps<{ file: File | null; size?: number }>(), {size: 256})
const emit = defineEmits<{ cropped: [blob: Blob]; cancel: [] }>()

const S = 320
const canvasRef = ref<HTMLCanvasElement | null>(null)
const imageUrl = ref('')
let imgObj: HTMLImageElement | null = null
let scale = 1, ox = 0, oy = 0, r = 115
let dragging: 'move' | 'n' | 's' | 'e' | 'w' | null = null
let dsx = 0, dsy = 0, dsox = 0, dsoy = 0, dsr = 0
const zv = ref(0)

watch(() => props.file, (file) => {
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    imageUrl.value = reader.result as string
    const img = new Image()
    img.onload = () => {
      imgObj = img;
      fit();
      render()
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)
}, {immediate: true})

function fit() {
  if (!imgObj) return
  scale = Math.min(S / imgObj.width, S / imgObj.height) * 1.05
  ox = 0;
  oy = 0;
  r = S / 2 * 0.7;
  zv.value = 0
}

function render() {
  const c = canvasRef.value;
  if (!c) return
  const ctx = c.getContext('2d');
  if (!ctx) return
  const cx = S / 2, cy = S / 2
  ctx.clearRect(0, 0, S, S)

  // 1. 整张图片（圆外也能看到轮廓）
  if (imgObj) {
    const iw = imgObj.width * scale, ih = imgObj.height * scale;
    ctx.drawImage(imgObj, cx - iw / 2 + ox, cy - ih / 2 + oy, iw, ih)
  }

  // 2. 圆形外半透明遮罩
  ctx.save();
  ctx.beginPath();
  ctx.rect(0, 0, S, S);
  ctx.arc(cx, cy, r, 0, Math.PI * 2, true);
  ctx.closePath();
  ctx.fillStyle = 'rgba(0,0,0,0.5)';
  ctx.fill();
  ctx.restore()

  // 3. 白色圆环
  ctx.beginPath();
  ctx.arc(cx, cy, r, 0, Math.PI * 2);
  ctx.strokeStyle = 'rgba(255,255,255,0.85)';
  ctx.lineWidth = 2;
  ctx.stroke()

  // 4. 四个手柄
  const hr = 6;
  [{x: cx, y: cy - r}, {x: cx, y: cy + r}, {x: cx - r, y: cy}, {x: cx + r, y: cy}].forEach(p => {
    ctx.beginPath();
    ctx.arc(p.x, p.y, hr, 0, Math.PI * 2);
    ctx.fillStyle = '#fff';
    ctx.fill();
    ctx.strokeStyle = 'rgba(0,0,0,0.3)';
    ctx.lineWidth = 1;
    ctx.stroke()
  })
}

function cp(e: MouseEvent) {
  const rc = canvasRef.value!.getBoundingClientRect();
  return {x: (e.clientX - rc.left) * S / rc.width, y: (e.clientY - rc.top) * S / rc.height}
}

function hit(p: { x: number, y: number }): string | null {
  const cx = S / 2, cy = S / 2, d = Math.sqrt((p.x - cx) ** 2 + (p.y - cy) ** 2), hd = 14
  if (Math.abs(p.y - (cy - r)) < hd && Math.abs(p.x - cx) < hd) return 'n'
  if (Math.abs(p.y - (cy + r)) < hd && Math.abs(p.x - cx) < hd) return 's'
  if (Math.abs(p.x - (cx - r)) < hd && Math.abs(p.y - cy) < hd) return 'w'
  if (Math.abs(p.x - (cx + r)) < hd && Math.abs(p.y - cy) < hd) return 'e'
  if (Math.abs(d - r) < 14) return 'move'
  if (d < r - 8) return 'move'
  return null
}

function onDown(e: MouseEvent) {
  const p = cp(e), a = hit(p);
  if (!a) return;
  dragging = a;
  dsx = p.x;
  dsy = p.y;
  dsox = ox;
  dsoy = oy;
  dsr = r;
  e.preventDefault()
}

function onMove(e: MouseEvent) {
  if (!dragging) {
    const c = canvasRef.value;
    if (!c) return;
    const a = hit(cp(e));
    c.style.cursor = a === 'n' || a === 's' ? 'ns-resize' : a === 'e' || a === 'w' ? 'ew-resize' : a === 'move' ? 'grab' : 'default';
    return
  }
  const p = cp(e), dx = p.x - dsx, dy = p.y - dsy
  if (dragging === 'move') {
    ox = dsox + dx;
    oy = dsoy + dy
  } else if (dragging === 'n') r = Math.max(40, dsr - dy)
  else if (dragging === 's') r = Math.max(40, dsr + dy)
  else if (dragging === 'w') r = Math.max(40, dsr - dx)
  else if (dragging === 'e') r = Math.max(40, dsr + dx)
  r = Math.min(r, S / 2);
  render()
}

function onUp() {
  dragging = null;
  if (canvasRef.value) canvasRef.value.style.cursor = 'default'
}

function onWheel(e: WheelEvent) {
  e.preventDefault();
  if (!imgObj) return
  const p = cp(e), old = scale;
  scale *= e.deltaY < 0 ? 1.06 : 1 / 1.06;
  scale = Math.max(0.2, Math.min(5, scale))
  const cx = S / 2, cy = S / 2, ratio = scale / old
  ox = p.x - (p.x - cx + imgObj.width * old / 2 - ox) * ratio - cx + imgObj.width * scale / 2
  oy = p.y - (p.y - cy + imgObj.height * old / 2 - oy) * ratio - cy + imgObj.height * scale / 2
  zv.value = Math.round((scale - 0.2) * 50);
  render()
}

function onZoom(e: Event) {
  zv.value = parseInt((e.target as HTMLInputElement).value);
  if (!imgObj) return;
  const ns = 0.2 + zv.value / 50;
  if (ns < 0.2 || ns > 5) return;
  const rt = ns / scale;
  ox *= rt;
  oy *= rt;
  scale = ns;
  render()
}

function confirm() {
  if (!imgObj) return
  const sz = props.size, dc = document.createElement('canvas');
  dc.width = sz;
  dc.height = sz
  const ctx = dc.getContext('2d')!, h = sz / 2
  ctx.beginPath();
  ctx.arc(h, h, h, 0, Math.PI * 2);
  ctx.closePath();
  ctx.clip()
  const rt = sz / (r * 2)
  ctx.drawImage(imgObj, h - imgObj.width * scale * rt / 2 + ox * rt, h - imgObj.height * scale * rt / 2 + oy * rt, imgObj.width * scale * rt, imgObj.height * scale * rt)
  dc.toBlob(b => {
    if (b) emit('cropped', b)
  }, 'image/webp', 0.9)
}

function cancel() {
  imageUrl.value = '';
  imgObj = null;
  scale = 1;
  ox = 0;
  oy = 0;
  r = 115;
  zv.value = 0;
  emit('cancel')
}

function init() {
  const c = canvasRef.value;
  if (!c) return;
  c.width = c.height = S;
  c.addEventListener('wheel', onWheel, {passive: false})
}

watch(canvasRef, el => {
  if (el) init()
}, {immediate: true})
onBeforeUnmount(() => canvasRef.value?.removeEventListener('wheel', onWheel))
</script>

<template>
  <div class="cw">
    <div v-if="imageUrl" class="cc">
      <div class="cw2">
        <canvas ref="canvasRef" class="cv" @mousedown="onDown" @mousemove="onMove" @mouseup="onUp" @mouseleave="onUp"/>
      </div>
      <div class="ctrl">
        <div class="zr"><span class="zl">缩放</span><input type="range" min="0" max="240" :value="zv" class="zs"
                                                           @input="onZoom"/></div>
        <div class="br">
          <button class="bt bc" @click="cancel">重新选择</button>
          <button class="bt bf" @click="confirm">确认裁剪</button>
        </div>
      </div>
    </div>
    <div v-else class="eh">请选择图片</div>
  </div>
</template>

<style scoped>
.cw {
  width: 100%
}

.cc {
  display: flex;
  flex-direction: column;
  gap: 10px
}

.cw2 {
  display: flex;
  justify-content: center;
  background: #1a1a1a;
  border-radius: 8px;
  overflow: hidden
}

.cv {
  width: 320px;
  height: 320px;
  max-width: 100%;
  cursor: default;
  user-select: none
}

.ctrl {
  display: flex;
  flex-direction: column;
  gap: 8px
}

.zr {
  display: flex;
  align-items: center;
  gap: 8px
}

.zl {
  font-size: 12px;
  color: #6b7280;
  white-space: nowrap
}

.zs {
  flex: 1;
  height: 4px;
  accent-color: #2563eb
}

.br {
  display: flex;
  gap: 8px;
  justify-content: flex-end
}

.bt {
  padding: 6px 16px;
  border-radius: 6px;
  font-size: 12px;
  font-family: inherit;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: background .15s
}

.bc {
  background: #f4f5f7;
  color: #6b7280;
  border-color: #e0e2e6
}

.bc:hover {
  background: #e8eaee
}

.bf {
  background: #2563eb;
  color: #fff
}

.bf:hover {
  background: #1d4ed8
}

.eh {
  text-align: center;
  color: #9ca3af;
  font-size: 13px;
  padding: 20px
}
</style>
