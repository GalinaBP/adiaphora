import { request } from './client';
import type {
  ApplicationResponse,
  CompletionResponse,
  CreateApplicationResponse,
  FormResponse,
  LoginRequest,
  MeResponse,
  PageResponse,
  RegisterRequest,
  RegisterResponse,
  TokenResponse,
} from './types';

// Thin, typed wrappers over the backend endpoints. No business logic — each function maps 1:1
// to an operation in the OpenAPI contract.

export const authApi = {
  register: (data: RegisterRequest) =>
    request<RegisterResponse>('/auth/register', { method: 'POST', body: data, auth: false }),
  login: (data: LoginRequest) =>
    request<TokenResponse>('/auth/login', { method: 'POST', body: data, auth: false }),
  logout: () => request<void>('/auth/logout', { method: 'POST' }),
  me: () => request<MeResponse>('/auth/me'),
};

export const applicationsApi = {
  list: (page = 0, size = 20) =>
    request<PageResponse<ApplicationResponse>>(`/applications?page=${page}&size=${size}`),
  get: (id: string) => request<ApplicationResponse>(`/applications/${id}`),
  create: () => request<CreateApplicationResponse>('/applications', { method: 'POST' }),
};

export const questionnaireApi = {
  form: (applicationId: string) =>
    request<FormResponse>(`/applications/${applicationId}/questionnaire`),
  saveAnswer: (applicationId: string, questionCode: string, value: string) =>
    request<CompletionResponse>(`/applications/${applicationId}/answers/${questionCode}`, {
      method: 'PUT',
      body: { value },
    }),
};
