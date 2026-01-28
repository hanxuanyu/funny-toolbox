export type GradientStop = {
  id: string
  offset: number
  color: string
}

export type GradientForm = {
  enabled: boolean
  type: "linear" | "radial"
  rotation: number // degrees for easier UX
  colorStops: GradientStop[]
}

export type ColorableSection = {
  color: string
  gradient: GradientForm
}

export type CornerShape = "dot" | "square" | "extra-rounded" | "rounded" | "dots" | "classy" | "classy-rounded"
export type DotShape = "rounded" | "dots" | "classy" | "classy-rounded" | "square" | "extra-rounded"

export interface QrFormState {
  width: number
  height: number
  type: "canvas" | "svg"
  shape: "square" | "circle"
  margin: number
  data: string
  image: string
  qrOptions: {
    typeNumber: number
    mode: "Numeric" | "Alphanumeric" | "Byte" | "Kanji"
    errorCorrectionLevel: "L" | "M" | "Q" | "H"
  }
  imageOptions: {
    hideBackgroundDots: boolean
    imageSize: number
    margin: number
    crossOrigin: "none" | "anonymous" | "use-credentials"
    saveAsBlob: boolean
  }
  dotsOptions: {
    type: DotShape
    color: string
    roundSize: boolean
    gradient: GradientForm
  }
  backgroundOptions: ColorableSection
  cornersSquareOptions: {
    type: CornerShape
    color: string
    gradient: GradientForm
  }
  cornersDotOptions: {
    type: CornerShape
    color: string
    gradient: GradientForm
  }
}

export type QrPreset = {
  name: string
  description: string
  config: () => Partial<QrFormState>
}
