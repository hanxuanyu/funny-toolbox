# Funny Toolbox – QR Generator

Fully featured QR design studio powered by Vue 3 + TypeScript. It wraps the `qr-code-styling` renderer with shadcn-vue components, so you can paste text, import existing QR images for beautifying, tweak gradients/corners/logo, and export optimized assets.

## ✨ Feature Highlights

- Live preview fixed at 300×300 px; export PNG/JPEG/WEBP/SVG up to 1024 px.
- Scrollable configuration panel with quick presets plus range sliders + text inputs for every numeric field.
- Gradient tooling for dots/corners/backgrounds, including rotation/offset sliders and color pickers.
- Logo controls: image scale locked to 0.1–0.6 (step 0.1) and margin 0–17 px with automatic clamping.
- “Beautify existing QR”: upload any QR image, `jsqr` decodes it locally, and the text field updates instantly.
- Responsive layout with sticky preview column to keep both panes within a single viewport on wide screens.

## 🧱 Tech Stack & Open-Source Components

| Component | Purpose |
| --- | --- |
| [Vue 3](https://vuejs.org/) + [TypeScript](https://www.typescriptlang.org/) | Application framework & typing |
| [Vite](https://vitejs.dev/) | Dev server & build tooling |
| [qr-code-styling](https://github.com/kozakdenys/qr-code-styling) | QR rendering, gradients, exports |
| [shadcn-vue](https://www.shadcn-vue.com/) + [reka-ui](https://reka-ui.netlify.app/) | UI primitives (Button, Card, Switch, Select, etc.) |
| [jsqr](https://github.com/cozmo/jsQR) | Decode uploaded QR images |
| [Tailwind CSS](https://tailwindcss.com/) via `@tailwindcss/vite` | Utility styling |
| [@vueuse/core](https://vueuse.org/), `clsx`, `class-variance-authority`, `tailwind-merge`, `tw-animate-css` | Composition helpers & styling utilities |

All dependencies keep their original licenses (MIT-compatible at the time of writing). Review them before redistribution.

## 🚀 Getting Started

```bash
npm install
npm run dev
npm run build
npm run preview
```

The dev server prints `http://localhost:5173` by default.

## 📁 Project Highlights

- `src/App.vue` – complete QR studio (state, presets, existing-QR upload, download logic, layout).
- `src/components/GradientControls.vue` – shared gradient editor with responsive slider + input pairs.
- `src/components/ui/*` – shadcn-vue generated components.

## 📝 Notes

- Circle shapes always enforce `roundSize = true` to satisfy qr-code-styling.
- Logo size/margin are clamped to 0.1–0.6 and 0–17 px to keep the code scannable.
- Existing QR decoding runs entirely in the browser; nothing is uploaded externally.
- When adding presets, ensure gradients contain ≥2 stops and keep width/height tied to the `PREVIEW_SIZE` constant.

## 📄 License

This repository follows the license declared in `package.json`; all third-party libraries retain their original licenses.
