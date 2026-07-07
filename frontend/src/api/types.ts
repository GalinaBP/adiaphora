// TypeScript mirrors of the response/request shapes from the backend OpenAPI contract
// (../../openapi.yaml). These are transport DTOs only — they carry no business logic.

export type UserRole = 'USER' | 'OPERATOR' | 'LAWYER' | 'ADMIN' | 'AUDITOR';
export type UserStatus = 'PENDING_ACTIVATION' | 'ACTIVE';

export type BankruptcyApplicationStatus =
  | 'DRAFT'
  | 'QUESTIONNAIRE_IN_PROGRESS'
  | 'READY_FOR_EVALUATION'
  | 'NEEDS_INFORMATION'
  | 'MANUAL_REVIEW_REQUIRED'
  | 'UNDER_REVIEW'
  | 'APPROVED_FOR_DOCUMENT_GENERATION'
  | 'DOCUMENTS_READY'
  | 'COMPLETED'
  | 'CANCELLED';

export type BankruptcyRoute =
  | 'NOT_EVALUATED'
  | 'MFC_PRELIMINARY'
  | 'COURT_PRELIMINARY'
  | 'MANUAL_REVIEW'
  | 'INSUFFICIENT_INFORMATION';

export type QuestionType =
  | 'TEXT'
  | 'TEXTAREA'
  | 'INTEGER'
  | 'MONEY'
  | 'BOOLEAN'
  | 'SINGLE_CHOICE'
  | 'MULTIPLE_CHOICE'
  | 'DATE';

export interface ApiFieldError {
  field: string;
  message: string;
}

export interface ApiErrorBody {
  timestamp: string;
  status: number;
  code: string;
  message: string;
  path: string;
  correlationId: string;
  fieldErrors?: ApiFieldError[];
}

export interface PageResponse<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// --- Auth ---
export interface RegisterRequest {
  email: string;
  password: string;
}
export interface RegisterResponse {
  userId: string;
}
export interface LoginRequest {
  email: string;
  password: string;
}
export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  expiresIn: number;
}
export interface MeResponse {
  userId: string;
  email: string;
  role: UserRole;
  status: UserStatus;
}

// --- Applications ---
export interface CreateApplicationResponse {
  applicationId: string;
}
export interface ApplicationResponse {
  applicationId: string;
  ownerId: string;
  status: BankruptcyApplicationStatus;
  route: BankruptcyRoute;
  submittedAt: string | null;
}

// --- Questionnaire ---
export interface OptionResponse {
  value: string;
  label: string;
  displayOrder: number;
}
export interface QuestionResponse {
  code: string;
  sectionCode: string;
  type: QuestionType;
  label: string;
  helpText: string | null;
  required: boolean;
  displayOrder: number;
  options: OptionResponse[];
}
export interface SectionResponse {
  code: string;
  title: string;
  displayOrder: number;
}
export interface CompletionResponse {
  requiredTotal: number;
  requiredAnswered: number;
  missingRequired: string[];
  complete: boolean;
}
export interface FormResponse {
  applicationId: string;
  versionCode: string;
  label: string;
  sections: SectionResponse[];
  questions: QuestionResponse[];
  answers: Record<string, string>;
  completion: CompletionResponse;
}

// --- Estate: creditors ---
export type CreditorType =
  | 'BANK'
  | 'MICROFINANCE'
  | 'INDIVIDUAL'
  | 'TAX_AUTHORITY'
  | 'UTILITY'
  | 'OTHER';

export interface CreditorRequest {
  name: string;
  type: CreditorType;
  inn?: string | null;
  claimBasis?: string | null;
  currency?: string | null;
  totalAmount: number;
  principalAmount?: number | null;
  interestAmount?: number | null;
  penaltyAmount?: number | null;
  overdue?: boolean;
  secured?: boolean;
}
export interface CreditorResponse {
  creditorId: string;
  applicationId: string;
  name: string;
  type: CreditorType;
  inn: string | null;
  claimBasis: string | null;
  currency: string;
  totalAmount: number;
  principalAmount: number | null;
  interestAmount: number | null;
  penaltyAmount: number | null;
  overdue: boolean;
  secured: boolean;
  duplicateWarning: boolean;
}

// --- Estate: assets ---
export type AssetType =
  | 'REAL_ESTATE'
  | 'VEHICLE'
  | 'BANK_ACCOUNT'
  | 'SECURITIES'
  | 'CASH'
  | 'RECEIVABLE'
  | 'MOVABLE_PROPERTY'
  | 'OTHER';

export interface AssetRequest {
  type: AssetType;
  description: string;
  currency?: string | null;
  estimatedValue: number;
  ownershipShare?: string | null;
  registrationNumber?: string | null;
  acquiredOn?: string | null;
  pledged?: boolean;
  pledgeCreditorId?: string | null;
}
export interface AssetResponse {
  assetId: string;
  applicationId: string;
  type: AssetType;
  description: string;
  currency: string;
  estimatedValue: number;
  ownershipShare: string | null;
  registrationNumber: string | null;
  acquiredOn: string | null;
  pledged: boolean;
  pledgeCreditorId: string | null;
  duplicateWarning: boolean;
}
