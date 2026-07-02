export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string;
  data: T;
}

export interface CloudAccount {
  id: number;
  name: string;
  provider: string;
  accessKeyId?: string;
  billOwnerId?: number;
  enabled: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface CloudAccountCheck {
  available: boolean;
  message: string;
  requestId?: string;
  billingCycle?: string;
  accountId?: string;
  accountName?: string;
  totalCount?: number;
}

export interface AiEngine {
  id: number;
  name: string;
  model: string;
  apiKey?: string;
  apiAddr: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface AiEngineCheck {
  available: boolean;
  message: string;
}

export interface BillingItemRule {
  id: number;
  provider: string;
  matchScope: string;
  productCode?: string;
  productName?: string;
  productDetail?: string;
  commodityCode?: string;
  billingItemCode?: string;
  billingItem?: string;
  decision: string;
  note?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BillingAuditRun {
  id: number;
  cloudAccountId: number;
  status: string;
  billDate: string;
  periodStartDate: string;
  periodEndDate: string;
  itemCount: number;
  unknownItemCount: number;
  totalPretaxAmount: number;
  unknownPretaxAmount: number;
  message?: string;
  startedAt?: string;
  finishedAt?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BillingAuditItem {
  id: number;
  runId: number;
  provider: string;
  productCode?: string;
  productName?: string;
  productDetail?: string;
  commodityCode?: string;
  billingItemCode?: string;
  billingItem?: string;
  billingType?: string;
  subscriptionType?: string;
  currency?: string;
  stableDayPretaxAmount: number;
  periodPretaxAmount: number;
  instanceCount: number;
  regionCount: number;
  sampleInstanceId?: string;
  sampleRegion?: string;
  sampleUsage?: string;
  sampleUsageUnit?: string;
  decision: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface BillingAuditItemResource {
  rawLineId: number;
  billDate: string;
  instanceId?: string;
  instanceName?: string;
  region?: string;
  zone?: string;
  resourceGroup?: string;
  costUnit?: string;
  billingType?: string;
  usageAmount?: number;
  usageUnit?: string;
  pretaxAmount?: number;
  currency?: string;
}

export interface BillingItemExplanation {
  id: number;
  auditItemId: number;
  aiEngineId: number;
  promptContext: string;
  explanation: string;
  createdAt?: string;
  updatedAt?: string;
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(path, {
    headers: {
      'Content-Type': 'application/json',
      ...options.headers
    },
    ...options
  });
  const text = await response.text();
  if (!text) {
    throw new Error(response.ok ? 'API response is empty' : `HTTP ${response.status}`);
  }
  const payload = JSON.parse(text) as ApiResponse<T>;
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || payload.code || `HTTP ${response.status}`);
  }
  return payload.data;
}

export const api = {
  listCloudAccounts: () => request<CloudAccount[]>('/api/cloud-accounts'),
  createCloudAccount: (payload: Omit<CloudAccount, 'id' | 'createdAt' | 'updatedAt'> & { accessKeySecret?: string }) =>
    request<CloudAccount>('/api/cloud-accounts', { method: 'POST', body: JSON.stringify(payload) }),
  updateCloudAccount: (id: number, payload: Omit<CloudAccount, 'id' | 'createdAt' | 'updatedAt'> & { accessKeySecret?: string }) =>
    request<CloudAccount>(`/api/cloud-accounts/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  checkCloudAccount: (id: number) => request<CloudAccountCheck>(`/api/cloud-accounts/${id}/check`, { method: 'POST' }),
  deleteCloudAccount: (id: number) => request<void>(`/api/cloud-accounts/${id}`, { method: 'DELETE' }),

  listAiEngines: () => request<AiEngine[]>('/api/ai-engines'),
  createAiEngine: (payload: Omit<AiEngine, 'id' | 'createdAt' | 'updatedAt'>) =>
    request<AiEngine>('/api/ai-engines', { method: 'POST', body: JSON.stringify(payload) }),
  updateAiEngine: (id: number, payload: Omit<AiEngine, 'id' | 'createdAt' | 'updatedAt'>) =>
    request<AiEngine>(`/api/ai-engines/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  checkAiEngine: (id: number) => request<AiEngineCheck>(`/api/ai-engines/${id}/check`, { method: 'POST' }),
  deleteAiEngine: (id: number) => request<void>(`/api/ai-engines/${id}`, { method: 'DELETE' }),

  listBillingItemRules: () => request<BillingItemRule[]>('/api/billing-item-rules'),
  createBillingItemRule: (payload: Omit<BillingItemRule, 'id' | 'createdAt' | 'updatedAt'>) =>
    request<BillingItemRule>('/api/billing-item-rules', { method: 'POST', body: JSON.stringify(payload) }),
  updateBillingItemRule: (id: number, payload: Omit<BillingItemRule, 'id' | 'createdAt' | 'updatedAt'>) =>
    request<BillingItemRule>(`/api/billing-item-rules/${id}`, { method: 'PUT', body: JSON.stringify(payload) }),
  deleteBillingItemRule: (id: number) => request<void>(`/api/billing-item-rules/${id}`, { method: 'DELETE' }),

  triggerBillingAudit: (payload: { cloudAccountId: number; billDate: string; periodStartDate?: string; periodEndDate?: string }) =>
    request<BillingAuditRun>('/api/billing-audits', { method: 'POST', body: JSON.stringify(payload) }),
  listBillingAudits: () => request<BillingAuditRun[]>('/api/billing-audits'),
  listBillingAuditItems: (id: number) => request<BillingAuditItem[]>(`/api/billing-audits/${id}/items`),
  listBillingAuditItemResources: (runId: number, itemId: number) =>
    request<BillingAuditItemResource[]>(`/api/billing-audits/${runId}/items/${itemId}/resources`),
  listBillingAuditItemExplanations: (runId: number, itemId: number) =>
    request<BillingItemExplanation[]>(`/api/billing-audits/${runId}/items/${itemId}/explanations`),
  explainBillingAuditItem: (runId: number, itemId: number, aiEngineId: number) =>
    request<BillingItemExplanation>(`/api/billing-audits/${runId}/items/${itemId}/explanations`, { method: 'POST', body: JSON.stringify({ aiEngineId }) }),
  createBillingAuditItemRule: (runId: number, itemId: number, payload: { matchScope: string; decision: string; note?: string }) =>
    request<BillingItemRule>(`/api/billing-audits/${runId}/items/${itemId}/rules`, { method: 'POST', body: JSON.stringify(payload) }),
  applyBillingAuditRules: (runId: number) =>
    request<BillingAuditRun>(`/api/billing-audits/${runId}/apply-rules`, { method: 'POST' })
};
