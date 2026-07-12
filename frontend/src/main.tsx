import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AiEngine, ApiError, AuthenticationConfig, BillingAuditItem, BillingAuditItemResource, BillingAuditRun, BillingItemExplanation, BillingItemRule, CloudAccount, CloudAccountCheck, CurrentUser, api } from './api';
import './styles.css';

type Tab = 'audits' | 'accounts' | 'rules' | 'ai';
type Language = 'zh' | 'en';

const TEXT = {
  zh: {
    common: {
      refresh: '刷新',
      switchLanguage: '切换语言',
      delete: '删除',
      close: '关闭',
      check: '检查',
      previous: '上一页',
      next: '下一页',
      yes: '是',
      no: '否'
    },
    tabs: {
      audits: '审计',
      accounts: '云账号',
      rules: '规则',
      ai: 'AI'
    },
    messages: {
      refreshed: '已刷新',
      loadFailed: '加载失败'
    },
    auth: {
      signIn: '登录 CostBuddy',
      google: '使用 Google 登录',
      wechat: '使用微信登录',
      noProviders: '尚未配置可用的登录方式',
      loading: '正在恢复登录状态',
      loadFailed: '加载登录配置失败',
      loginFailed: '登录失败，请重试',
      retry: '重试',
      logout: '退出登录'
    },
    audit: {
      manualAudit: '手动审计',
      cloudAccount: '云账号',
      selectAccount: '选择账号',
      stableBillDate: '稳定账单日',
      runAudit: '运行审计',
      auditRuns: '审计记录',
      id: 'ID',
      account: '账号',
      billDate: '账单日',
      executionTime: '执行时间',
      status: '状态',
      unknown: '陌生项',
      amount: '金额',
      action: '操作',
      noAuditRuns: '暂无审计记录',
      runItems: '审计项',
      aggregateDetails: '聚合明细',
      backToAuditRuns: '回到审计列表',
      resourceDetails: '资源明细',
      backToAuditItems: '回到审计项',
      aiExplain: '优化说明',
      aiExplanation: '优化说明',
      aiEngineRequired: '请先选择 AI 引擎',
      explanationCreated: '优化说明已生成',
      explainFailed: '生成优化说明失败',
      loadExplanationsFailed: '加载优化说明失败',
      noExplanations: '暂无优化说明',
      aiAnalyzing: 'AI 分析中',
      ignoreBillingItem: '忽略计费项',
      applyRules: '应用规则',
      ruleCreated: '规则已创建并应用',
      createRuleFailed: '创建规则失败',
      rulesApplied: '规则已应用',
      applyRulesFailed: '应用规则失败',
      instanceId: '资源 ID',
      instanceName: '资源名称',
      region: '地域',
      resourceGroup: '资源组',
      usage: '用量',
      billingType: '账单类型',
      product: '产品',
      detail: '明细',
      billingItem: '计费项',
      periodAmount: '账单日金额',
      decision: '处理状态',
      noAuditItems: '暂无审计项',
      noResourceDetails: '暂无资源明细',
      loadItemsFailed: '加载审计项失败',
      loadResourcesFailed: '加载资源明细失败',
      accountRequired: '请选择云账号',
      auditRunCreated: '审计记录已创建',
      createAuditFailed: '创建审计失败'
    },
    accounts: {
      cloudAccount: '云账号',
      accounts: '账号列表',
      name: '名称',
      provider: '云厂商',
      accessKeyId: 'Access Key ID',
      accessKeySecret: 'Access Key Secret',
      billOwnerId: '费用归属账号 ID',
      billOwnerIdPlaceholder: '可选，不填则使用当前账号',
      enabled: '启用',
      save: '保存账号',
      noAccounts: '暂无账号',
      saved: '账号已保存',
      saveFailed: '保存账号失败',
      checkAvailable: '账单 API 可访问',
      checkUnavailable: '账单 API 检查失败',
      checkFailed: '检查云账号失败',
      checkBillingCycle: '账期',
      checkAccountId: '账号 ID',
      checkTotalCount: '返回记录数',
      deleted: '账号已删除',
      deleteFailed: '删除账号失败'
    },
    rules: {
      billingRule: '账单规则',
      rules: '规则列表',
      scope: '匹配范围',
      productScope: '产品',
      billingItemScope: '计费项',
      decision: '处理方式',
      known: '熟悉',
      ignored: '忽略',
      productCode: '产品代码',
      productName: '产品名称',
      productDetail: '产品明细',
      billingItemCode: '计费项代码',
      billingItem: '计费项',
      note: '备注',
      save: '保存规则',
      noRules: '暂无规则',
      saved: '规则已保存',
      saveFailed: '保存规则失败',
      deleted: '规则已删除',
      deleteFailed: '删除规则失败'
    },
    ai: {
      aiEngine: 'AI 引擎',
      aiEngines: 'AI 引擎列表',
      name: '名称',
      model: '模型',
      apiKey: 'API Key',
      apiBaseUrl: 'API Base URL',
      save: '保存 AI 引擎',
      noAiEngines: '暂无 AI 引擎',
      saved: 'AI 引擎已保存',
      saveFailed: '保存 AI 引擎失败',
      checkFailed: 'AI 引擎检查失败',
      deleted: 'AI 引擎已删除',
      deleteFailed: '删除 AI 引擎失败'
    },
    auditStatuses: {
      WAITING_FOR_CONNECTOR: '等待账单连接器',
      RUNNING: '运行中',
      SUCCESS: '成功',
      FAILED: '失败'
    },
    decisions: {
      UNKNOWN: '陌生',
      KNOWN: '熟悉',
      IGNORED: '忽略'
    },
    scopes: {
      PRODUCT: '产品',
      BILLING_ITEM: '计费项'
    },
    providers: {
      ALIYUN: '阿里云'
    }
  },
  en: {
    common: {
      refresh: 'Refresh',
      switchLanguage: 'Switch language',
      delete: 'Delete',
      close: 'Close',
      check: 'Check',
      previous: 'Previous',
      next: 'Next',
      yes: 'Yes',
      no: 'No'
    },
    tabs: {
      audits: 'Audits',
      accounts: 'Accounts',
      rules: 'Rules',
      ai: 'AI'
    },
    messages: {
      refreshed: 'Refreshed',
      loadFailed: 'Load failed'
    },
    auth: {
      signIn: 'Sign in to CostBuddy',
      google: 'Continue with Google',
      wechat: 'Continue with WeChat',
      noProviders: 'No sign-in provider is configured',
      loading: 'Restoring your session',
      loadFailed: 'Failed to load sign-in configuration',
      loginFailed: 'Sign-in failed. Please try again.',
      retry: 'Retry',
      logout: 'Sign out'
    },
    audit: {
      manualAudit: 'Manual Audit',
      cloudAccount: 'Cloud account',
      selectAccount: 'Select account',
      stableBillDate: 'Stable bill date',
      runAudit: 'Run audit',
      auditRuns: 'Audit Runs',
      id: 'ID',
      account: 'Account',
      billDate: 'Bill Date',
      executionTime: 'Started At',
      status: 'Status',
      unknown: 'Unknown',
      amount: 'Amount',
      action: 'Action',
      noAuditRuns: 'No audit runs',
      runItems: 'Run Items',
      aggregateDetails: 'Details',
      backToAuditRuns: 'Back to Audit Runs',
      resourceDetails: 'Resources',
      backToAuditItems: 'Back to Items',
      aiExplain: 'Optimization Notes',
      aiExplanation: 'Optimization Notes',
      aiEngineRequired: 'Select an AI engine first',
      explanationCreated: 'Optimization notes generated',
      explainFailed: 'Generate optimization notes failed',
      loadExplanationsFailed: 'Load optimization notes failed',
      noExplanations: 'No optimization notes',
      aiAnalyzing: 'AI analyzing',
      ignoreBillingItem: 'Ignore Item',
      applyRules: 'Apply Rules',
      ruleCreated: 'Rule created and applied',
      createRuleFailed: 'Create rule failed',
      rulesApplied: 'Rules applied',
      applyRulesFailed: 'Apply rules failed',
      instanceId: 'Resource ID',
      instanceName: 'Resource Name',
      region: 'Region',
      resourceGroup: 'Resource Group',
      usage: 'Usage',
      billingType: 'Billing Type',
      product: 'Product',
      detail: 'Detail',
      billingItem: 'Billing Item',
      periodAmount: 'Bill Date Amount',
      decision: 'Decision',
      noAuditItems: 'No audit items',
      noResourceDetails: 'No resource details',
      loadItemsFailed: 'Load audit items failed',
      loadResourcesFailed: 'Load resource details failed',
      accountRequired: 'Cloud account is required',
      auditRunCreated: 'Audit run created',
      createAuditFailed: 'Create audit failed'
    },
    accounts: {
      cloudAccount: 'Cloud Account',
      accounts: 'Accounts',
      name: 'Name',
      provider: 'Provider',
      accessKeyId: 'Access Key ID',
      accessKeySecret: 'Access Key Secret',
      billOwnerId: 'Billing owner account ID',
      billOwnerIdPlaceholder: 'Optional, leave empty to use the current account',
      enabled: 'Enabled',
      save: 'Save account',
      noAccounts: 'No accounts',
      saved: 'Account saved',
      saveFailed: 'Save account failed',
      checkAvailable: 'Billing API is available',
      checkUnavailable: 'Billing API check failed',
      checkFailed: 'Check cloud account failed',
      checkBillingCycle: 'Billing cycle',
      checkAccountId: 'Account ID',
      checkTotalCount: 'Returned records',
      deleted: 'Account deleted',
      deleteFailed: 'Delete account failed'
    },
    rules: {
      billingRule: 'Billing Rule',
      rules: 'Rules',
      scope: 'Scope',
      productScope: 'Product',
      billingItemScope: 'Billing Item',
      decision: 'Decision',
      known: 'Known',
      ignored: 'Ignored',
      productCode: 'Product code',
      productName: 'Product name',
      productDetail: 'Product detail',
      billingItemCode: 'Billing item code',
      billingItem: 'Billing item',
      note: 'Note',
      save: 'Save rule',
      noRules: 'No rules',
      saved: 'Rule saved',
      saveFailed: 'Save rule failed',
      deleted: 'Rule deleted',
      deleteFailed: 'Delete rule failed'
    },
    ai: {
      aiEngine: 'AI Engine',
      aiEngines: 'AI Engines',
      name: 'Name',
      model: 'Model',
      apiKey: 'API Key',
      apiBaseUrl: 'API Base URL',
      save: 'Save AI engine',
      noAiEngines: 'No AI engines',
      saved: 'AI engine saved',
      saveFailed: 'Save AI engine failed',
      checkFailed: 'AI engine check failed',
      deleted: 'AI engine deleted',
      deleteFailed: 'Delete AI engine failed'
    },
    auditStatuses: {
      WAITING_FOR_CONNECTOR: 'Waiting for connector',
      RUNNING: 'Running',
      SUCCESS: 'Success',
      FAILED: 'Failed'
    },
    decisions: {
      UNKNOWN: 'Unknown',
      KNOWN: 'Known',
      IGNORED: 'Ignored'
    },
    scopes: {
      PRODUCT: 'Product',
      BILLING_ITEM: 'Billing Item'
    },
    providers: {
      ALIYUN: 'Alibaba Cloud'
    }
  }
};

type UiText = typeof TEXT.zh;

const emptyAccountForm = {
  name: '',
  provider: 'ALIYUN',
  accessKeyId: '',
  accessKeySecret: '',
  billOwnerId: '',
  enabled: true
};

const emptyRuleForm = {
  provider: 'ALIYUN',
  matchScope: 'BILLING_ITEM',
  productCode: '',
  productName: '',
  productDetail: '',
  commodityCode: '',
  billingItemCode: '',
  billingItem: '',
  decision: 'KNOWN',
  note: ''
};

const emptyAiEngineForm = {
  name: '',
  model: '',
  apiKey: '',
  apiAddr: ''
};

function App() {
  const [language, setLanguage] = useState<Language>('zh');
  const [authReady, setAuthReady] = useState(false);
  const [authConfig, setAuthConfig] = useState<AuthenticationConfig | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);
  const [authError, setAuthError] = useState(() => new URLSearchParams(window.location.search).get('authError') || '');
  const [authReloadKey, setAuthReloadKey] = useState(0);
  const [tab, setTab] = useState<Tab>('audits');
  const [busy, setBusy] = useState(false);
  const [status, setStatus] = useState('');
  const [accounts, setAccounts] = useState<CloudAccount[]>([]);
  const [runs, setRuns] = useState<BillingAuditRun[]>([]);
  const [rules, setRules] = useState<BillingItemRule[]>([]);
  const [aiEngines, setAiEngines] = useState<AiEngine[]>([]);

  const text = TEXT[language];

  const loadAll = useCallback(async (showSuccess = false) => {
    setBusy(true);
    try {
      const [nextAccounts, nextRuns, nextRules, nextAiEngines] = await Promise.all([
        api.listCloudAccounts(),
        api.listBillingAudits(),
        api.listBillingItemRules(),
        api.listAiEngines()
      ]);
      setAccounts(nextAccounts);
      setRuns(nextRuns);
      setRules(nextRules);
      setAiEngines(nextAiEngines);
      if (showSuccess) {
        setStatus(TEXT[language].messages.refreshed);
      }
    } catch (error) {
      setStatus(error instanceof Error ? error.message : TEXT[language].messages.loadFailed);
    } finally {
      setBusy(false);
    }
  }, [language]);

  useEffect(() => {
    document.documentElement.lang = language === 'zh' ? 'zh-CN' : 'en';
  }, [language]);

  useEffect(() => {
    let cancelled = false;
    setAuthReady(false);
    api.getAuthenticationConfig()
      .then(async nextConfig => {
        if (cancelled) {
          return;
        }
        setAuthConfig(nextConfig);
        if (!nextConfig.enabled) {
          setCurrentUser(null);
          return;
        }
        try {
          const nextUser = await api.getCurrentUser();
          if (!cancelled) {
            setCurrentUser(nextUser);
            setAuthError('');
          }
        } catch (error) {
          if (error instanceof ApiError && error.status === 401) {
            if (!cancelled) {
              setCurrentUser(null);
            }
            return;
          }
          throw error;
        }
      })
      .catch(error => {
        if (!cancelled) {
          setAuthConfig(null);
          setAuthError(error instanceof Error ? error.message : TEXT.zh.auth.loadFailed);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setAuthReady(true);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [authReloadKey]);

  const applicationReady = authReady && authConfig !== null && (!authConfig.enabled || currentUser !== null);

  useEffect(() => {
    if (applicationReady) {
      void loadAll();
    }
  }, [applicationReady, loadAll]);

  useEffect(() => {
    if (!status) {
      return;
    }
    const timeoutId = window.setTimeout(() => setStatus(''), 3200);
    return () => window.clearTimeout(timeoutId);
  }, [status]);

  async function logout() {
    setBusy(true);
    try {
      await api.logout();
      setCurrentUser(null);
      setAccounts([]);
      setRuns([]);
      setRules([]);
      setAiEngines([]);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : text.auth.loginFailed);
    } finally {
      setBusy(false);
    }
  }

  if (!authReady) {
    return <AuthenticationScreen language={language} text={text} loading />;
  }

  if (authConfig === null || (authConfig.enabled && currentUser === null)) {
    return (
      <AuthenticationScreen
        language={language}
        text={text}
        providers={authConfig?.providers ?? []}
        error={authError ? `${text.auth.loginFailed} (${authError})` : ''}
        onRetry={authConfig === null ? () => setAuthReloadKey(current => current + 1) : undefined}
        onSwitchLanguage={() => setLanguage(current => current === 'zh' ? 'en' : 'zh')}
      />
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <img className="brand-logo-mark" src="/favicon.svg" alt="" />
          <span className="brand-wordmark" aria-hidden="true">CostBuddy</span>
          <h1 className="sr-only">CostBuddy</h1>
        </div>
        <div className="topbar-actions">
          <nav className="segmented">
            <button className={tab === 'audits' ? 'active' : ''} onClick={() => setTab('audits')}>{text.tabs.audits}</button>
            <button className={tab === 'accounts' ? 'active' : ''} onClick={() => setTab('accounts')}>{text.tabs.accounts}</button>
            <button className={tab === 'rules' ? 'active' : ''} onClick={() => setTab('rules')}>{text.tabs.rules}</button>
            <button className={tab === 'ai' ? 'active' : ''} onClick={() => setTab('ai')}>{text.tabs.ai}</button>
          </nav>
          <div className="icon-actions">
            {currentUser && (
              <div className="user-session" title={currentUser.email || String(currentUser.motherboardUserId)}>
                <span>{currentUser.displayName || currentUser.email || `#${currentUser.motherboardUserId}`}</span>
                <button className="icon-button square-icon" title={text.auth.logout} aria-label={text.auth.logout} onClick={() => void logout()} disabled={busy}>
                  <LogoutIcon className="button-icon" />
                </button>
              </div>
            )}
            <button className="icon-button square-icon" title={text.common.refresh} aria-label={text.common.refresh} onClick={() => void loadAll(true)} disabled={busy}>
              <RefreshIcon className={busy ? 'button-icon spin-icon' : 'button-icon'} />
            </button>
            <button
              className="icon-button language-button"
              title={text.common.switchLanguage}
              aria-label={text.common.switchLanguage}
              onClick={() => setLanguage(current => current === 'zh' ? 'en' : 'zh')}
            >
              <GlobeIcon className="button-icon" />
              <span className="language-hint">{language === 'zh' ? 'EN' : '中'}</span>
            </button>
          </div>
        </div>
      </header>
      {status && <div className="toast">{status}</div>}
      <main className={`workspace ${tab === 'audits' ? 'audit-workspace' : ''}`}>
        {tab === 'audits' && <AuditWorkspace accounts={accounts} runs={runs} aiEngines={aiEngines} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'accounts' && <AccountWorkspace accounts={accounts} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'rules' && <RuleWorkspace rules={rules} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'ai' && <AiWorkspace aiEngines={aiEngines} onReload={loadAll} onStatus={setStatus} text={text} />}
      </main>
    </div>
  );
}

function AuthenticationScreen({
  language,
  text,
  providers = [],
  loading = false,
  error = '',
  onRetry,
  onSwitchLanguage
}: {
  language: Language;
  text: UiText;
  providers?: string[];
  loading?: boolean;
  error?: string;
  onRetry?: () => void;
  onSwitchLanguage?: () => void;
}) {
  return (
    <main className="authentication-page">
      {onSwitchLanguage && (
        <button
          className="icon-button language-button authentication-language"
          title={text.common.switchLanguage}
          aria-label={text.common.switchLanguage}
          onClick={onSwitchLanguage}
        >
          <GlobeIcon className="button-icon" />
          <span className="language-hint">{language === 'zh' ? 'EN' : '中'}</span>
        </button>
      )}
      <section className="authentication-panel">
        <div className="authentication-brand">
          <img src="/favicon.svg" alt="" />
          <span>CostBuddy</span>
        </div>
        <h1>{text.auth.signIn}</h1>
        {loading ? (
          <div className="authentication-loading">
            <span className="large-spinner" aria-hidden="true" />
            <p>{text.auth.loading}</p>
          </div>
        ) : (
          <div className="authentication-actions">
            {providers.includes('GOOGLE') && (
              <button className="primary" onClick={() => window.location.assign('/api/auth/login/GOOGLE')}>{text.auth.google}</button>
            )}
            {providers.includes('WECHAT') && (
              <button onClick={() => window.location.assign('/api/auth/login/WECHAT')}>{text.auth.wechat}</button>
            )}
            {!providers.length && !onRetry && <p className="authentication-empty">{text.auth.noProviders}</p>}
            {error && <p className="authentication-error">{error}</p>}
            {onRetry && <button onClick={onRetry}>{text.auth.retry}</button>}
          </div>
        )}
      </section>
    </main>
  );
}

function AuditWorkspace({
  accounts,
  runs,
  aiEngines,
  onReload,
  onStatus,
  text
}: {
  accounts: CloudAccount[];
  runs: BillingAuditRun[];
  aiEngines: AiEngine[];
  onReload: (showSuccess?: boolean) => Promise<void>;
  onStatus: (message: string) => void;
  text: UiText;
}) {
  const pageSize = 10;
  const [form, setForm] = useState(() => defaultAuditForm(accounts[0]?.id));
  const [detailRunId, setDetailRunId] = useState<number | null>(null);
  const [items, setItems] = useState<BillingAuditItem[]>([]);
  const [resourceItemId, setResourceItemId] = useState<number | null>(null);
  const [resources, setResources] = useState<BillingAuditItemResource[]>([]);
  const [explanationItemId, setExplanationItemId] = useState<number | null>(null);
  const [explanations, setExplanations] = useState<BillingItemExplanation[]>([]);
  const [explanationError, setExplanationError] = useState('');
  const [explanationModalOpen, setExplanationModalOpen] = useState(false);
  const [selectedAiEngineId, setSelectedAiEngineId] = useState('');
  const [explainingItemId, setExplainingItemId] = useState<number | null>(null);
  const [page, setPage] = useState(1);

  const accountById = useMemo(() => new Map(accounts.map(account => [account.id, account])), [accounts]);
  const pageCount = Math.max(1, Math.ceil(runs.length / pageSize));
  const pagedRuns = useMemo(() => runs.slice((page - 1) * pageSize, page * pageSize), [page, runs]);
  const detailRun = useMemo(() => runs.find(run => run.id === detailRunId) ?? null, [detailRunId, runs]);
  const resourceItem = useMemo(() => items.find(item => item.id === resourceItemId) ?? null, [items, resourceItemId]);
  const explanationItem = useMemo(() => items.find(item => item.id === explanationItemId) ?? null, [explanationItemId, items]);

  useEffect(() => {
    if (!form.cloudAccountId && accounts[0]) {
      setForm(current => ({ ...current, cloudAccountId: String(accounts[0].id) }));
    }
  }, [accounts, form.cloudAccountId]);

  useEffect(() => {
    if (aiEngines.length === 0) {
      setSelectedAiEngineId('');
      return;
    }
    if (!aiEngines.some(aiEngine => String(aiEngine.id) === selectedAiEngineId)) {
      setSelectedAiEngineId(String(aiEngines[0].id));
    }
  }, [aiEngines, selectedAiEngineId]);

  useEffect(() => {
    if (page > pageCount) {
      setPage(pageCount);
    }
  }, [page, pageCount]);

  useEffect(() => {
    if (detailRunId === null) {
      setItems([]);
      setResourceItemId(null);
      setExplanationItemId(null);
      setExplanationModalOpen(false);
      return;
    }
    api.listBillingAuditItems(detailRunId)
      .then(setItems)
      .catch(error => onStatus(error instanceof Error ? error.message : text.audit.loadItemsFailed));
  }, [detailRunId, onStatus, text.audit.loadItemsFailed]);

  useEffect(() => {
    if (detailRunId === null || resourceItemId === null) {
      setResources([]);
      return;
    }
    api.listBillingAuditItemResources(detailRunId, resourceItemId)
      .then(setResources)
      .catch(error => onStatus(error instanceof Error ? error.message : text.audit.loadResourcesFailed));
  }, [detailRunId, resourceItemId, onStatus, text.audit.loadResourcesFailed]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.cloudAccountId) {
      onStatus(text.audit.accountRequired);
      return;
    }
    try {
      const run = await api.triggerBillingAudit({
        cloudAccountId: Number(form.cloudAccountId),
        billDate: form.billDate
      });
      if (run.status === 'FAILED') {
        onStatus(run.message || text.audit.createAuditFailed);
      } else {
        onStatus(text.audit.auditRunCreated);
      }
      setPage(1);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.audit.createAuditFailed);
    }
  }

  async function reloadAuditItems(runId: number) {
    const nextItems = await api.listBillingAuditItems(runId);
    setItems(nextItems);
  }

  async function createRuleFromItem(item: BillingAuditItem, matchScope: string, decision: string) {
    if (detailRunId === null) {
      return;
    }
    try {
      await api.createBillingAuditItemRule(detailRunId, item.id, { matchScope, decision });
      await reloadAuditItems(detailRunId);
      await onReload(false);
      onStatus(text.audit.ruleCreated);
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.audit.createRuleFailed);
    }
  }

  async function applyCurrentRules() {
    if (detailRunId === null) {
      return;
    }
    try {
      await api.applyBillingAuditRules(detailRunId);
      await reloadAuditItems(detailRunId);
      await onReload(false);
      onStatus(text.audit.rulesApplied);
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.audit.applyRulesFailed);
    }
  }

  async function explainItem(item: BillingAuditItem) {
    if (detailRunId === null) {
      return;
    }
    if (!selectedAiEngineId) {
      onStatus(text.audit.aiEngineRequired);
      return;
    }
    try {
      setExplanationItemId(item.id);
      setExplanationModalOpen(true);
      setExplanations([]);
      setExplanationError('');
      setExplainingItemId(item.id);
      setResourceItemId(null);
      const explanation = await api.explainBillingAuditItem(detailRunId, item.id, Number(selectedAiEngineId));
      setExplanations([explanation]);
      onStatus(text.audit.explanationCreated);
    } catch (error) {
      const message = error instanceof Error ? error.message : text.audit.explainFailed;
      setExplanationError(message);
      onStatus(message);
    } finally {
      setExplainingItemId(null);
    }
  }

  function closeExplanationModal() {
    setExplanationModalOpen(false);
    if (explainingItemId === null) {
      setExplanationItemId(null);
      setExplanations([]);
      setExplanationError('');
    }
  }

  if (detailRunId !== null) {
    if (resourceItemId !== null) {
      return (
        <section className="panel table-panel audit-detail-panel">
          <div className="panel-title">
            <h2>{text.audit.resourceDetails}{resourceItem ? ` #${resourceItem.id}` : ''}</h2>
            <button onClick={() => setResourceItemId(null)}>{text.audit.backToAuditItems}</button>
          </div>
          <div className="table-wrap">
            <table className="resource-detail-table">
              <thead>
                <tr>
                  <th>{text.audit.instanceId}</th>
                  <th>{text.audit.instanceName}</th>
                  <th>{text.audit.region}</th>
                  <th>{text.audit.resourceGroup}</th>
                  <th>{text.audit.usage}</th>
                  <th>{text.audit.amount}</th>
                  <th>{text.audit.billingType}</th>
                </tr>
              </thead>
              <tbody>
                {resources.map(resource => (
                  <tr key={resource.rawLineId}>
                    <td>{resource.instanceId || '-'}</td>
                    <td>{resource.instanceName || '-'}</td>
                    <td>{resource.region || resource.zone || '-'}</td>
                    <td>{resource.resourceGroup || resource.costUnit || '-'}</td>
                    <td>{formatUsage(resource.usageAmount, resource.usageUnit)}</td>
                    <td>{formatPreciseMoney(resource.pretaxAmount)}</td>
                    <td>{resource.billingType || '-'}</td>
                  </tr>
                ))}
                {!resources.length && <EmptyRow colSpan={7} label={text.audit.noResourceDetails} />}
              </tbody>
            </table>
          </div>
        </section>
      );
    }
    return (
      <>
        <section className="panel table-panel audit-detail-panel">
          <div className="panel-title">
            <h2>{text.audit.runItems}{detailRun ? ` #${detailRun.id}` : ''}</h2>
            <div className="panel-title-actions">
              {aiEngines.length > 0 && (
                <select className="compact-select" value={selectedAiEngineId} onChange={event => setSelectedAiEngineId(event.target.value)}>
                  {aiEngines.map(aiEngine => (
                    <option value={aiEngine.id} key={aiEngine.id}>{aiEngine.name}</option>
                  ))}
                </select>
              )}
              <button onClick={() => void applyCurrentRules()}>{text.audit.applyRules}</button>
              <button onClick={() => setDetailRunId(null)}>{text.audit.backToAuditRuns}</button>
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{text.audit.product}</th>
                  <th>{text.audit.detail}</th>
                  <th>{text.audit.billingItem}</th>
                  <th>{text.audit.periodAmount}</th>
                  <th>{text.audit.decision}</th>
                  <th>{text.audit.action}</th>
                </tr>
              </thead>
              <tbody>
                {items.map(item => {
                  const isNonZeroUnknown = item.decision === 'UNKNOWN' && Number(item.periodPretaxAmount) !== 0;
                  return (
                    <tr className={item.decision === 'UNKNOWN' ? (isNonZeroUnknown ? 'unknown-cost-row' : '') : 'resolved-audit-row'} key={item.id}>
                      <td>{item.productName || item.productCode || '-'}</td>
                      <td>{item.productDetail || item.commodityCode || '-'}</td>
                      <td>{item.billingItem || item.billingItemCode || '-'}</td>
                      <td className="audit-amount-cell">{formatAuditItemMoney(item.periodPretaxAmount)}</td>
                      <td className="audit-decision-cell">{formatDecision(item.decision, text)}</td>
                      <td>
                        <div className="row-actions">
                          <button onClick={() => void createRuleFromItem(item, 'BILLING_ITEM', 'IGNORED')}>{text.audit.ignoreBillingItem}</button>
                          <button disabled={!aiEngines.length || explainingItemId !== null} onClick={() => void explainItem(item)}>{text.audit.aiExplain}</button>
                          <button onClick={() => {
                            setExplanationItemId(null);
                            setResourceItemId(item.id);
                          }}>{text.audit.resourceDetails}</button>
                        </div>
                      </td>
                    </tr>
                  );
                })}
                {!items.length && <EmptyRow colSpan={6} label={text.audit.noAuditItems} />}
              </tbody>
            </table>
          </div>
        </section>
        {explanationModalOpen && (
          <OptimizationExplanationModal
            error={explanationError}
            explanation={explanations[0]}
            item={explanationItem}
            loading={explainingItemId !== null}
            onClose={closeExplanationModal}
            text={text}
          />
        )}
      </>
    );
  }

  return (
    <>
      <section className="panel form-panel">
        <div className="panel-title">
          <h2>{text.audit.manualAudit}</h2>
        </div>
        <form className="form-grid" onSubmit={submit}>
          <label>
            {text.audit.cloudAccount}
            <select value={form.cloudAccountId} onChange={event => setForm({ ...form, cloudAccountId: event.target.value })}>
              <option value="">{text.audit.selectAccount}</option>
              {accounts.map(account => (
                <option value={account.id} key={account.id}>{account.name}</option>
              ))}
            </select>
          </label>
          <label>
            {text.audit.stableBillDate}
            <input type="date" value={form.billDate} onChange={event => setForm({ ...form, billDate: event.target.value })} />
          </label>
          <div className="form-actions">
            <button className="primary" type="submit" disabled={!accounts.length}>{text.audit.runAudit}</button>
          </div>
        </form>
      </section>
      <section className="panel table-panel">
        <div className="panel-title">
          <h2>{text.audit.auditRuns}</h2>
        </div>
        <div className="table-wrap">
          <table className="audit-run-table">
            <thead>
              <tr>
                <th>{text.audit.id}</th>
                <th>{text.audit.account}</th>
                <th>{text.audit.billDate}</th>
                <th>{text.audit.executionTime}</th>
                <th>{text.audit.status}</th>
                <th>{text.audit.unknown}</th>
                <th>{text.audit.amount}</th>
                <th>{text.audit.action}</th>
              </tr>
            </thead>
            <tbody>
              {pagedRuns.map(run => (
                <tr key={run.id}>
                  <td>{run.id}</td>
                  <td>{accountById.get(run.cloudAccountId)?.name ?? run.cloudAccountId}</td>
                  <td>{run.billDate}</td>
                  <td>{formatDateTime(run.startedAt)}</td>
                  <td><span className="status-pill">{formatAuditStatus(run.status, text)}</span></td>
                  <td>{run.unknownItemCount} / {run.itemCount}</td>
                  <td>{formatMoney(run.unknownPretaxAmount)} / {formatMoney(run.totalPretaxAmount)}</td>
                  <td><button onClick={() => setDetailRunId(run.id)}>{text.audit.aggregateDetails}</button></td>
                </tr>
              ))}
              {!runs.length && <EmptyRow colSpan={8} label={text.audit.noAuditRuns} />}
            </tbody>
          </table>
        </div>
        <div className="pagination">
          <button onClick={() => setPage(current => Math.max(1, current - 1))} disabled={page <= 1}>{text.common.previous}</button>
          <span>{page} / {pageCount}</span>
          <button onClick={() => setPage(current => Math.min(pageCount, current + 1))} disabled={page >= pageCount}>{text.common.next}</button>
        </div>
      </section>
    </>
  );
}

function AccountWorkspace({
  accounts,
  onReload,
  onStatus,
  text
}: {
  accounts: CloudAccount[];
  onReload: (showSuccess?: boolean) => Promise<void>;
  onStatus: (message: string) => void;
  text: UiText;
}) {
  const [form, setForm] = useState(emptyAccountForm);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await api.createCloudAccount({
        name: form.name,
        provider: form.provider,
        accessKeyId: emptyToUndefined(form.accessKeyId),
        accessKeySecret: emptyToUndefined(form.accessKeySecret),
        billOwnerId: form.billOwnerId ? Number(form.billOwnerId) : undefined,
        enabled: form.enabled
      });
      setForm(emptyAccountForm);
      onStatus(text.accounts.saved);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.accounts.saveFailed);
    }
  }

  async function deleteAccount(id: number) {
    try {
      await api.deleteCloudAccount(id);
      onStatus(text.accounts.deleted);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.accounts.deleteFailed);
    }
  }

  async function checkAccount(id: number) {
    try {
      const result = await api.checkCloudAccount(id);
      onStatus(formatCloudAccountCheck(result, text));
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.accounts.checkFailed);
    }
  }

  return (
    <>
      <section className="panel form-panel">
        <div className="panel-title">
          <h2>{text.accounts.cloudAccount}</h2>
        </div>
        <form className="form-grid" onSubmit={submit}>
          <label>
            {text.accounts.name}
            <input value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} required />
          </label>
          <label>
            {text.accounts.provider}
            <select value={form.provider} onChange={event => setForm({ ...form, provider: event.target.value })}>
              <option value="ALIYUN">{formatProvider('ALIYUN', text)}</option>
            </select>
          </label>
          <label>
            {text.accounts.accessKeyId}
            <input value={form.accessKeyId} onChange={event => setForm({ ...form, accessKeyId: event.target.value })} />
          </label>
          <label>
            {text.accounts.accessKeySecret}
            <input type="password" value={form.accessKeySecret} onChange={event => setForm({ ...form, accessKeySecret: event.target.value })} />
          </label>
          <label>
            {text.accounts.billOwnerId}
            <input placeholder={text.accounts.billOwnerIdPlaceholder} value={form.billOwnerId} onChange={event => setForm({ ...form, billOwnerId: event.target.value })} />
          </label>
          <label className="checkbox-label">
            <input type="checkbox" checked={form.enabled} onChange={event => setForm({ ...form, enabled: event.target.checked })} />
            {text.accounts.enabled}
          </label>
          <div className="form-actions">
            <button className="primary" type="submit">{text.accounts.save}</button>
          </div>
        </form>
      </section>
      <section className="panel table-panel">
        <div className="panel-title">
          <h2>{text.accounts.accounts}</h2>
        </div>
        <SimpleTable
          headers={[text.accounts.name, text.accounts.provider, text.accounts.accessKeyId, text.accounts.billOwnerId, text.accounts.enabled, '', '']}
          rows={accounts.map(account => [
            account.name,
            formatProvider(account.provider, text),
            account.accessKeyId || '-',
            account.billOwnerId ?? '-',
            account.enabled ? text.common.yes : text.common.no,
            <button onClick={() => void checkAccount(account.id)}>{text.common.check}</button>,
            <button className="danger" onClick={() => void deleteAccount(account.id)}>{text.common.delete}</button>
          ])}
          emptyLabel={text.accounts.noAccounts}
        />
      </section>
    </>
  );
}

function RuleWorkspace({
  rules,
  onReload,
  onStatus,
  text
}: {
  rules: BillingItemRule[];
  onReload: (showSuccess?: boolean) => Promise<void>;
  onStatus: (message: string) => void;
  text: UiText;
}) {
  const [form, setForm] = useState(emptyRuleForm);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await api.createBillingItemRule({
        ...form,
        productCode: emptyToUndefined(form.productCode),
        productName: emptyToUndefined(form.productName),
        productDetail: emptyToUndefined(form.productDetail),
        commodityCode: emptyToUndefined(form.commodityCode),
        billingItemCode: emptyToUndefined(form.billingItemCode),
        billingItem: emptyToUndefined(form.billingItem),
        note: emptyToUndefined(form.note)
      });
      setForm(emptyRuleForm);
      onStatus(text.rules.saved);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.rules.saveFailed);
    }
  }

  async function deleteRule(id: number) {
    try {
      await api.deleteBillingItemRule(id);
      onStatus(text.rules.deleted);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.rules.deleteFailed);
    }
  }

  return (
    <>
      <section className="panel form-panel">
        <div className="panel-title">
          <h2>{text.rules.billingRule}</h2>
        </div>
        <form className="form-grid" onSubmit={submit}>
          <label>
            {text.rules.scope}
            <select value={form.matchScope} onChange={event => setForm({ ...form, matchScope: event.target.value })}>
              <option value="PRODUCT">{text.rules.productScope}</option>
              <option value="BILLING_ITEM">{text.rules.billingItemScope}</option>
            </select>
          </label>
          <label>
            {text.rules.decision}
            <select value={form.decision} onChange={event => setForm({ ...form, decision: event.target.value })}>
              <option value="KNOWN">{text.rules.known}</option>
              <option value="IGNORED">{text.rules.ignored}</option>
            </select>
          </label>
          <label>
            {text.rules.productCode}
            <input value={form.productCode} onChange={event => setForm({ ...form, productCode: event.target.value })} />
          </label>
          <label>
            {text.rules.productName}
            <input value={form.productName} onChange={event => setForm({ ...form, productName: event.target.value })} />
          </label>
          <label>
            {text.rules.productDetail}
            <input value={form.productDetail} onChange={event => setForm({ ...form, productDetail: event.target.value })} />
          </label>
          <label>
            {text.rules.billingItemCode}
            <input value={form.billingItemCode} onChange={event => setForm({ ...form, billingItemCode: event.target.value })} />
          </label>
          <label>
            {text.rules.billingItem}
            <input value={form.billingItem} onChange={event => setForm({ ...form, billingItem: event.target.value })} />
          </label>
          <label className="wide-field">
            {text.rules.note}
            <input value={form.note} onChange={event => setForm({ ...form, note: event.target.value })} />
          </label>
          <div className="form-actions">
            <button className="primary" type="submit">{text.rules.save}</button>
          </div>
        </form>
      </section>
      <section className="panel table-panel">
        <div className="panel-title">
          <h2>{text.rules.rules}</h2>
        </div>
        <SimpleTable
          headers={[text.rules.scope, text.rules.decision, text.audit.product, text.audit.detail, text.audit.billingItem, '']}
          rows={rules.map(rule => [
            formatScope(rule.matchScope, text),
            formatDecision(rule.decision, text),
            rule.productName || rule.productCode || '-',
            rule.productDetail || rule.commodityCode || '-',
            rule.billingItem || rule.billingItemCode || '-',
            <button className="danger" onClick={() => void deleteRule(rule.id)}>{text.common.delete}</button>
          ])}
          emptyLabel={text.rules.noRules}
        />
      </section>
    </>
  );
}

function AiWorkspace({
  aiEngines,
  onReload,
  onStatus,
  text
}: {
  aiEngines: AiEngine[];
  onReload: (showSuccess?: boolean) => Promise<void>;
  onStatus: (message: string) => void;
  text: UiText;
}) {
  const [form, setForm] = useState(emptyAiEngineForm);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    try {
      await api.createAiEngine(form);
      setForm(emptyAiEngineForm);
      onStatus(text.ai.saved);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.ai.saveFailed);
    }
  }

  async function check(id: number) {
    try {
      const result = await api.checkAiEngine(id);
      onStatus(result.message);
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.ai.checkFailed);
    }
  }

  async function deleteAiEngine(id: number) {
    try {
      await api.deleteAiEngine(id);
      onStatus(text.ai.deleted);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.ai.deleteFailed);
    }
  }

  return (
    <>
      <section className="panel form-panel">
        <div className="panel-title">
          <h2>{text.ai.aiEngine}</h2>
        </div>
        <form className="form-grid" onSubmit={submit}>
          <label>
            {text.ai.name}
            <input value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} required />
          </label>
          <label>
            {text.ai.model}
            <input value={form.model} onChange={event => setForm({ ...form, model: event.target.value })} required />
          </label>
          <label>
            {text.ai.apiKey}
            <input type="password" value={form.apiKey} onChange={event => setForm({ ...form, apiKey: event.target.value })} required />
          </label>
          <label>
            {text.ai.apiBaseUrl}
            <input value={form.apiAddr} onChange={event => setForm({ ...form, apiAddr: event.target.value })} required />
          </label>
          <div className="form-actions">
            <button className="primary" type="submit">{text.ai.save}</button>
          </div>
        </form>
      </section>
      <section className="panel table-panel">
        <div className="panel-title">
          <h2>{text.ai.aiEngines}</h2>
        </div>
        <SimpleTable
          headers={[text.ai.name, text.ai.model, text.ai.apiBaseUrl, '', '']}
          rows={aiEngines.map(aiEngine => [
            aiEngine.name,
            aiEngine.model,
            aiEngine.apiAddr,
            <button onClick={() => void check(aiEngine.id)}>{text.common.check}</button>,
            <button className="danger" onClick={() => void deleteAiEngine(aiEngine.id)}>{text.common.delete}</button>
          ])}
          emptyLabel={text.ai.noAiEngines}
        />
      </section>
    </>
  );
}

function OptimizationExplanationModal({
  error,
  explanation,
  item,
  loading,
  onClose,
  text
}: {
  error: string;
  explanation?: BillingItemExplanation;
  item: BillingAuditItem | null;
  loading: boolean;
  onClose: () => void;
  text: UiText;
}) {
  return (
    <div className="modal-backdrop" role="presentation">
      <section className="modal-panel" role="dialog" aria-modal="true" aria-labelledby="optimization-modal-title">
        <div className="modal-title">
          <div>
            <h2 id="optimization-modal-title">{text.audit.aiExplanation}</h2>
            <p>{item ? `${item.productName || item.productCode || '-'} / ${item.billingItem || item.billingItemCode || '-'}` : '-'}</p>
          </div>
          <button onClick={onClose}>{text.common.close}</button>
        </div>
        <div className="modal-body">
          {loading && (
            <div className="modal-loading">
              <span className="large-spinner" aria-hidden="true" />
              <strong>{text.audit.aiAnalyzing}</strong>
            </div>
          )}
          {!loading && error && <div className="modal-error">{error}</div>}
          {!loading && !error && explanation && (
            <article className="explanation-block">
              <div className="explanation-meta">#{explanation.id} / {formatDateTime(explanation.createdAt)}</div>
              <MarkdownContent markdown={explanation.explanation} />
            </article>
          )}
          {!loading && !error && !explanation && <div className="empty-card">{text.audit.noExplanations}</div>}
        </div>
      </section>
    </div>
  );
}

function MarkdownContent({ markdown }: { markdown: string }) {
  const blocks: React.ReactNode[] = [];
  let listItems: React.ReactNode[] = [];
  let listType: 'ul' | 'ol' | null = null;

  function flushList(key: string) {
    if (!listType || listItems.length === 0) {
      return;
    }
    const ListTag = listType;
    blocks.push(<ListTag key={key}>{listItems}</ListTag>);
    listItems = [];
    listType = null;
  }

  markdown.split(/\r?\n/).forEach((line, index) => {
    const trimmed = line.trim();
    if (!trimmed) {
      flushList(`list-${index}`);
      return;
    }
    const heading = /^(#{1,4})\s+(.+)$/.exec(trimmed);
    if (heading) {
      flushList(`list-${index}`);
      blocks.push(renderMarkdownHeading(index, Math.min(heading[1].length + 2, 6), heading[2]));
      return;
    }
    const unordered = /^[-*]\s+(.+)$/.exec(trimmed);
    if (unordered) {
      if (listType !== 'ul') {
        flushList(`list-${index}`);
        listType = 'ul';
      }
      listItems.push(<li key={index}>{renderInlineMarkdown(unordered[1])}</li>);
      return;
    }
    const ordered = /^\d+\.\s+(.+)$/.exec(trimmed);
    if (ordered) {
      if (listType !== 'ol') {
        flushList(`list-${index}`);
        listType = 'ol';
      }
      listItems.push(<li key={index}>{renderInlineMarkdown(ordered[1])}</li>);
      return;
    }
    flushList(`list-${index}`);
    blocks.push(<p key={index}>{renderInlineMarkdown(trimmed)}</p>);
  });
  flushList('list-end');

  return <div className="markdown-body">{blocks}</div>;
}

function renderMarkdownHeading(key: number, level: number, content: string) {
  const children = renderInlineMarkdown(content);
  const className = content.includes('释放/关闭路径') ? 'markdown-danger-heading' : undefined;
  if (level === 3) {
    return <h3 className={className} key={key}>{children}</h3>;
  }
  if (level === 4) {
    return <h4 className={className} key={key}>{children}</h4>;
  }
  if (level === 5) {
    return <h5 className={className} key={key}>{children}</h5>;
  }
  return <h6 className={className} key={key}>{children}</h6>;
}

function renderInlineMarkdown(text: string) {
  const nodes: React.ReactNode[] = [];
  const pattern = /(`[^`]+`|\*\*[^*]+\*\*|https?:\/\/[^\s)]+)/g;
  let lastIndex = 0;
  text.replace(pattern, (match, _group, offset: number) => {
    if (offset > lastIndex) {
      nodes.push(text.slice(lastIndex, offset));
    }
    if (match.startsWith('`')) {
      nodes.push(<code key={offset}>{match.slice(1, -1)}</code>);
    } else if (match.startsWith('**')) {
      nodes.push(<strong key={offset}>{match.slice(2, -2)}</strong>);
    } else {
      nodes.push(<a key={offset} href={match} target="_blank" rel="noreferrer">{match}</a>);
    }
    lastIndex = offset + match.length;
    return match;
  });
  if (lastIndex < text.length) {
    nodes.push(text.slice(lastIndex));
  }
  return nodes;
}

function SimpleTable({
  headers,
  rows,
  emptyLabel
}: {
  headers: string[];
  rows: React.ReactNode[][];
  emptyLabel: string;
}) {
  return (
    <div className="table-wrap">
      <table>
        <thead>
          <tr>
            {headers.map((header, index) => <th key={`${header}-${index}`}>{header}</th>)}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, rowIndex) => (
            <tr key={rowIndex}>
              {row.map((cell, cellIndex) => <td key={`${rowIndex}-${cellIndex}`}>{cell}</td>)}
            </tr>
          ))}
          {!rows.length && <EmptyRow colSpan={headers.length} label={emptyLabel} />}
        </tbody>
      </table>
    </div>
  );
}

function EmptyRow({ colSpan, label }: { colSpan: number; label: string }) {
  return (
    <tr>
      <td className="empty-cell" colSpan={colSpan}>{label}</td>
    </tr>
  );
}

function RefreshIcon({ className }: { className?: string }) {
  return (
    <svg className={className} aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 12a9 9 0 0 1-15.2 6.5" />
      <path d="M3 12a9 9 0 0 1 15.2-6.5" />
      <path d="M18 2v4h-4" />
      <path d="M6 22v-4h4" />
    </svg>
  );
}

function GlobeIcon({ className }: { className?: string }) {
  return (
    <svg className={className} aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <path d="M2 12h20" />
      <path d="M12 2a15.3 15.3 0 0 1 0 20" />
      <path d="M12 2a15.3 15.3 0 0 0 0 20" />
    </svg>
  );
}

function LogoutIcon({ className }: { className?: string }) {
  return (
    <svg className={className} aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M10 17l5-5-5-5" />
      <path d="M15 12H3" />
      <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4" />
    </svg>
  );
}

function defaultAuditForm(cloudAccountId?: number) {
  const billDate = offsetDate(-2);
  return {
    cloudAccountId: cloudAccountId ? String(cloudAccountId) : '',
    billDate
  };
}

function offsetDate(offsetDays: number) {
  const date = new Date();
  date.setDate(date.getDate() + offsetDays);
  return date.toISOString().slice(0, 10);
}

function emptyToUndefined(value: string) {
  const trimmed = value.trim();
  return trimmed ? trimmed : undefined;
}

function formatMoney(value: number | undefined) {
  return Number(value ?? 0).toFixed(2);
}

function formatAuditItemMoney(value: number | undefined) {
  const amount = Number(value ?? 0);
  if (amount !== 0 && Math.abs(amount) < 0.01) {
    return amount.toFixed(6);
  }
  return amount.toFixed(2);
}

function formatPreciseMoney(value: number | undefined) {
  return Number(value ?? 0).toFixed(6);
}

function formatUsage(value: number | undefined, unit: string | undefined) {
  if (value === undefined || value === null) {
    return unit || '-';
  }
  const amount = Number(value).toLocaleString(undefined, { maximumFractionDigits: 8 });
  return unit ? `${amount} ${unit}` : amount;
}

function formatDateTime(value: string | undefined) {
  if (!value) {
    return '-';
  }
  return value.replace('T', ' ').slice(0, 19);
}

function formatAuditStatus(status: string, text: UiText) {
  return (text.auditStatuses as Record<string, string>)[status] ?? status;
}

function formatDecision(decision: string, text: UiText) {
  return (text.decisions as Record<string, string>)[decision] ?? decision;
}

function formatScope(scope: string, text: UiText) {
  return (text.scopes as Record<string, string>)[scope] ?? scope;
}

function formatProvider(provider: string, text: UiText) {
  return (text.providers as Record<string, string>)[provider] ?? provider;
}

function formatCloudAccountCheck(result: CloudAccountCheck, text: UiText) {
  if (!result.available) {
    return `${text.accounts.checkUnavailable}: ${result.message}`;
  }
  const details = [
    result.billingCycle ? `${text.accounts.checkBillingCycle}: ${result.billingCycle}` : '',
    result.accountId ? `${text.accounts.checkAccountId}: ${result.accountId}` : '',
    result.totalCount !== undefined ? `${text.accounts.checkTotalCount}: ${result.totalCount}` : ''
  ].filter(Boolean);
  if (!details.length) {
    return text.accounts.checkAvailable;
  }
  return `${text.accounts.checkAvailable}: ${details.join(' / ')}`;
}

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
