/**
 * @author Eddie
 * @date 2026-06-20
 */

/// <reference types="vite/client" />

declare module '*.vue' {
  import type {DefineComponent} from 'vue'
  const component: DefineComponent<{}, {}, any>
    export default component
}
