import type { SVGProps } from 'react'

type Props = SVGProps<SVGSVGElement>

const base = 'inline-block'

export function ChevronRight(props: Props) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
         strokeWidth={2} stroke="currentColor" className={`${base} w-5 h-5`} aria-hidden {...props}>
      <path strokeLinecap="round" strokeLinejoin="round" d="m8.25 4.5 7.5 7.5-7.5 7.5" />
    </svg>
  )
}

export function ArrowLeft(props: Props) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
         strokeWidth={2} stroke="currentColor" className={`${base} w-4 h-4`} aria-hidden {...props}>
      <path strokeLinecap="round" strokeLinejoin="round" d="M10.5 19.5 3 12m0 0 7.5-7.5M3 12h18" />
    </svg>
  )
}

export function ShieldCheck(props: Props) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
         strokeWidth={2} stroke="currentColor" className={`${base} w-4 h-4`} aria-hidden {...props}>
      <path strokeLinecap="round" strokeLinejoin="round"
            d="M9 12.75 11.25 15 15 9.75M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9c0-1.31.28-2.55.78-3.67L12 3l8.22 5.33c.5 1.12.78 2.36.78 3.67Z"/>
    </svg>
  )
}

export function ShieldOff(props: Props) {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"
         strokeWidth={2} stroke="currentColor" className={`${base} w-4 h-4`} aria-hidden {...props}>
      <path strokeLinecap="round" strokeLinejoin="round"
            d="M9.75 9.75 14.25 14.25M14.25 9.75 9.75 14.25M21 12c0 4.97-4.03 9-9 9s-9-4.03-9-9c0-1.31.28-2.55.78-3.67L12 3l8.22 5.33c.5 1.12.78 2.36.78 3.67Z"/>
    </svg>
  )
}
