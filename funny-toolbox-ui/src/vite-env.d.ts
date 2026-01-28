/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_DEPLOYMENT_MODE: 'standalone' | 'embedded'
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
