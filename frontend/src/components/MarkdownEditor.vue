<template>
  <div ref="editorRef" class="milkdown-editor w-full min-h-56 border border-cyan-300/10 bg-slate-950/60 px-4 py-3 text-sm text-white focus-within:border-cyan-300/20 transition"></div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { Editor, rootCtx, defaultValueCtx } from '@milkdown/core'
import { commonmark } from '@milkdown/preset-commonmark'
import { history } from '@milkdown/plugin-history'
import { listener, listenerCtx } from '@milkdown/plugin-listener'

const props = defineProps<{ modelValue: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: string] }>()

const editorRef = ref<HTMLElement | null>(null)
let editorInstance: Editor | null = null
let ignoreNextUpdate = false

onMounted(async () => {
  if (!editorRef.value) return
  editorInstance = await Editor.make()
    .config((ctx) => {
      ctx.set(rootCtx, editorRef.value!)
      ctx.set(defaultValueCtx, props.modelValue)
      ctx.get(listenerCtx).markdownUpdated((_ctx, markdown) => {
        if (ignoreNextUpdate) { ignoreNextUpdate = false; return }
        emit('update:modelValue', markdown)
      })
    })
    .use(commonmark)
    .use(history)
    .use(listener)
    .create()
})

onBeforeUnmount(() => {
  editorInstance?.destroy()
})

</script>

<style>
.milkdown-editor .milkdown {
  outline: none;
}
.milkdown-editor .milkdown p {
  color: rgb(203 213 225); /* slate-300 */
  margin: 0.5em 0;
  line-height: 1.7;
}
.milkdown-editor .milkdown h1,
.milkdown-editor .milkdown h2,
.milkdown-editor .milkdown h3 {
  color: rgb(224 242 254); /* cyan-100 */
  font-weight: 700;
  margin: 1em 0 0.5em;
}
.milkdown-editor .milkdown strong { color: #fff; }
.milkdown-editor .milkdown em { color: rgb(148 163 184); }
.milkdown-editor .milkdown code {
  background: rgb(30 41 59 / 0.6);
  color: rgb(103 232 249);
  padding: 0.1em 0.4em;
  border-radius: 3px;
  font-size: 0.875em;
}
.milkdown-editor .milkdown pre {
  background: rgb(15 23 42 / 0.8);
  border: 1px solid rgb(103 232 249 / 0.1);
  padding: 1em;
  overflow-x: auto;
}
.milkdown-editor .milkdown pre code {
  background: none;
  padding: 0;
}
.milkdown-editor .milkdown blockquote {
  border-left: 3px solid rgb(103 232 249 / 0.3);
  padding-left: 1em;
  color: rgb(100 116 139);
  margin: 0.5em 0;
}
.milkdown-editor .milkdown ul,
.milkdown-editor .milkdown ol {
  padding-left: 1.5em;
  color: rgb(203 213 225);
}
.milkdown-editor .milkdown a {
  color: rgb(34 211 238);
  text-decoration: underline;
}
.milkdown-editor .milkdown hr {
  border-color: rgb(103 232 249 / 0.1);
  margin: 1em 0;
}
</style>
