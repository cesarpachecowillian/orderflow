export type Role = 'USER' | 'ADMIN'

export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'CANCELLED'

export interface AuthResponse {
  token: string
  type: string
  expiresInMs: number
}

export interface UserResponse {
  id: number
  name: string
  email: string
  role: Role
  createdAt: string
}

export interface OrderItem {
  id: number
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: number
  customerName: string
  customerEmail: string | null
  status: OrderStatus
  totalAmount: number
  items: OrderItem[]
  confirmedAt: string | null
  confirmedByName: string | null
  confirmedByEmail: string | null
  confirmationMethod: string | null
  cancelledAt: string | null
  cancelledByName: string | null
  cancelledByEmail: string | null
  cancellationReason: string | null
  cancellationMethod: string | null
  createdAt: string
  updatedAt: string
}

export interface UserListItem {
  id: number
  name: string
  email: string
  role: Role
  superAdmin: boolean
  createdAt: string
}

export interface AdminActivitySummary {
  adminName: string
  adminEmail: string
  confirmedCount: number
  confirmedAmount: number
  cancelledCount: number
  cancelledAmount: number
}

export interface OrderSummary {
  totalOrders: number
  totalAmount: number
  pendingCount: number
  confirmedCount: number
  cancelledCount: number
  pendingAmount: number
  confirmedAmount: number
}

export interface UserOrdersSummary {
  customerName: string
  customerEmail: string
  totalOrders: number
  totalAmount: number
  pendingAmount: number
  confirmedAmount: number
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export interface CreateOrderItemRequest {
  productName: string
  quantity: number
  unitPrice: number
}

export interface CreateOrderRequest {
  items: CreateOrderItemRequest[]
}

export interface ApiError {
  status: number
  error: string
  message: string
  details?: string[]
}
