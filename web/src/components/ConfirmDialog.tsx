import { useEffect, useRef, type ReactNode } from 'react'

export type DialogVariant = 'primary' | 'danger'

interface Props {
  open: boolean
  title: string
  description?: ReactNode
  children?: ReactNode
  confirmLabel?: string
  cancelLabel?: string
  variant?: DialogVariant
  loading?: boolean
  onConfirm: () => void
  onCancel: () => void
}

export function ConfirmDialog({
  open,
  title,
  description,
  children,
  confirmLabel = 'Confirmar',
  cancelLabel = 'Cancelar',
  variant = 'primary',
  loading = false,
  onConfirm,
  onCancel,
}: Props) {
  const confirmBtnRef = useRef<HTMLButtonElement>(null)

  // Foco inicial no botão de confirmar — só dispara quando o diálogo abre,
  // não a cada re-render (evita roubar o foco de inputs internos).
  useEffect(() => {
    if (open) confirmBtnRef.current?.focus()
  }, [open])

  // Listener de Esc — pode ser re-registrado livremente sem afetar foco.
  useEffect(() => {
    if (!open) return
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !loading) onCancel()
    }
    document.addEventListener('keydown', onKey)
    return () => document.removeEventListener('keydown', onKey)
  }, [open, loading, onCancel])

  if (!open) return null

  const confirmCls = variant === 'danger' ? 'btn-danger' : 'btn-primary'

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center px-4"
      role="dialog"
      aria-modal="true"
      aria-labelledby="confirm-dialog-title"
    >
      <div
        className="absolute inset-0 bg-black/40 backdrop-blur-sm"
        onClick={loading ? undefined : onCancel}
      />
      <div className="relative card max-w-md w-full p-6 shadow-xl">
        <h2 id="confirm-dialog-title" className="text-lg font-semibold text-gray-900">
          {title}
        </h2>
        {description && (
          <div className="mt-2 text-sm text-gray-600">{description}</div>
        )}
        {children && <div className="mt-4">{children}</div>}
        <div className="mt-6 flex justify-end gap-3">
          <button
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="btn-secondary"
          >
            {cancelLabel}
          </button>
          <button
            ref={confirmBtnRef}
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className={confirmCls}
          >
            {loading ? 'Processando…' : confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
