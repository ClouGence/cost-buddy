import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { AiEngine, BillingAuditItem, BillingAuditRun, BillingItemRule, CloudAccount, CloudAccountCheck, api } from './api';
import './styles.css';

type Tab = 'audits' | 'accounts' | 'rules' | 'ai';
type Language = 'zh' | 'en';

const TEXT = {
  zh: {
    common: {
      refresh: '刷新',
      switchLanguage: '切换语言',
      delete: '删除',
      check: '检查',
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
    audit: {
      manualAudit: '手动审计',
      cloudAccount: '云账号',
      selectAccount: '选择账号',
      stableBillDate: '稳定账单日',
      periodStart: '统计开始',
      periodEnd: '统计结束',
      runAudit: '运行审计',
      auditRuns: '审计记录',
      id: 'ID',
      account: '账号',
      billDate: '账单日',
      window: '窗口',
      status: '状态',
      unknown: '陌生项',
      amount: '金额',
      noAuditRuns: '暂无审计记录',
      runItems: '审计项',
      product: '产品',
      detail: '明细',
      billingItem: '计费项',
      periodAmount: '周期金额',
      decision: '处理状态',
      noAuditItems: '暂无审计项',
      loadItemsFailed: '加载审计项失败',
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
      check: 'Check',
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
    audit: {
      manualAudit: 'Manual Audit',
      cloudAccount: 'Cloud account',
      selectAccount: 'Select account',
      stableBillDate: 'Stable bill date',
      periodStart: 'Period start',
      periodEnd: 'Period end',
      runAudit: 'Run audit',
      auditRuns: 'Audit Runs',
      id: 'ID',
      account: 'Account',
      billDate: 'Bill Date',
      window: 'Window',
      status: 'Status',
      unknown: 'Unknown',
      amount: 'Amount',
      noAuditRuns: 'No audit runs',
      runItems: 'Run Items',
      product: 'Product',
      detail: 'Detail',
      billingItem: 'Billing Item',
      periodAmount: 'Period Amount',
      decision: 'Decision',
      noAuditItems: 'No audit items',
      loadItemsFailed: 'Load audit items failed',
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
    void loadAll();
  }, [loadAll]);

  useEffect(() => {
    if (!status) {
      return;
    }
    const timeoutId = window.setTimeout(() => setStatus(''), 3200);
    return () => window.clearTimeout(timeoutId);
  }, [status]);

  return (
    <div className="app-shell">
      <header className="topbar">
        <div className="brand-lockup">
          <span className="brand-mark">C</span>
          <h1>Cost Buddy</h1>
        </div>
        <div className="topbar-actions">
          <nav className="segmented">
            <button className={tab === 'audits' ? 'active' : ''} onClick={() => setTab('audits')}>{text.tabs.audits}</button>
            <button className={tab === 'accounts' ? 'active' : ''} onClick={() => setTab('accounts')}>{text.tabs.accounts}</button>
            <button className={tab === 'rules' ? 'active' : ''} onClick={() => setTab('rules')}>{text.tabs.rules}</button>
            <button className={tab === 'ai' ? 'active' : ''} onClick={() => setTab('ai')}>{text.tabs.ai}</button>
          </nav>
          <div className="icon-actions">
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
      <main className="workspace">
        {tab === 'audits' && <AuditWorkspace accounts={accounts} runs={runs} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'accounts' && <AccountWorkspace accounts={accounts} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'rules' && <RuleWorkspace rules={rules} onReload={loadAll} onStatus={setStatus} text={text} />}
        {tab === 'ai' && <AiWorkspace aiEngines={aiEngines} onReload={loadAll} onStatus={setStatus} text={text} />}
      </main>
    </div>
  );
}

function AuditWorkspace({
  accounts,
  runs,
  onReload,
  onStatus,
  text
}: {
  accounts: CloudAccount[];
  runs: BillingAuditRun[];
  onReload: (showSuccess?: boolean) => Promise<void>;
  onStatus: (message: string) => void;
  text: UiText;
}) {
  const [form, setForm] = useState(() => defaultAuditForm(accounts[0]?.id));
  const [selectedRunId, setSelectedRunId] = useState<number | null>(null);
  const [items, setItems] = useState<BillingAuditItem[]>([]);

  const accountById = useMemo(() => new Map(accounts.map(account => [account.id, account])), [accounts]);

  useEffect(() => {
    if (!form.cloudAccountId && accounts[0]) {
      setForm(current => ({ ...current, cloudAccountId: String(accounts[0].id) }));
    }
  }, [accounts, form.cloudAccountId]);

  useEffect(() => {
    if (selectedRunId === null) {
      setItems([]);
      return;
    }
    api.listBillingAuditItems(selectedRunId)
      .then(setItems)
      .catch(error => onStatus(error instanceof Error ? error.message : text.audit.loadItemsFailed));
  }, [selectedRunId, onStatus, text.audit.loadItemsFailed]);

  async function submit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!form.cloudAccountId) {
      onStatus(text.audit.accountRequired);
      return;
    }
    try {
      const run = await api.triggerBillingAudit({
        cloudAccountId: Number(form.cloudAccountId),
        billDate: form.billDate,
        periodStartDate: form.periodStartDate,
        periodEndDate: form.periodEndDate
      });
      setSelectedRunId(run.id);
      onStatus(text.audit.auditRunCreated);
      await onReload();
    } catch (error) {
      onStatus(error instanceof Error ? error.message : text.audit.createAuditFailed);
    }
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
          <label>
            {text.audit.periodStart}
            <input type="date" value={form.periodStartDate} onChange={event => setForm({ ...form, periodStartDate: event.target.value })} />
          </label>
          <label>
            {text.audit.periodEnd}
            <input type="date" value={form.periodEndDate} onChange={event => setForm({ ...form, periodEndDate: event.target.value })} />
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
          <table>
            <thead>
              <tr>
                <th>{text.audit.id}</th>
                <th>{text.audit.account}</th>
                <th>{text.audit.billDate}</th>
                <th>{text.audit.window}</th>
                <th>{text.audit.status}</th>
                <th>{text.audit.unknown}</th>
                <th>{text.audit.amount}</th>
              </tr>
            </thead>
            <tbody>
              {runs.map(run => (
                <tr className={selectedRunId === run.id ? 'selected-row' : ''} key={run.id} onClick={() => setSelectedRunId(run.id)}>
                  <td>{run.id}</td>
                  <td>{accountById.get(run.cloudAccountId)?.name ?? run.cloudAccountId}</td>
                  <td>{run.billDate}</td>
                  <td>{run.periodStartDate} / {run.periodEndDate}</td>
                  <td><span className="status-pill">{formatAuditStatus(run.status, text)}</span></td>
                  <td>{run.unknownItemCount} / {run.itemCount}</td>
                  <td>{formatMoney(run.unknownPretaxAmount)} / {formatMoney(run.totalPretaxAmount)}</td>
                </tr>
              ))}
              {!runs.length && <EmptyRow colSpan={7} label={text.audit.noAuditRuns} />}
            </tbody>
          </table>
        </div>
        {selectedRunId !== null && (
          <div className="detail-block">
            <h3>{text.audit.runItems}</h3>
            <div className="table-wrap compact">
              <table>
                <thead>
                  <tr>
                    <th>{text.audit.product}</th>
                    <th>{text.audit.detail}</th>
                    <th>{text.audit.billingItem}</th>
                    <th>{text.audit.periodAmount}</th>
                    <th>{text.audit.decision}</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map(item => (
                    <tr key={item.id}>
                      <td>{item.productName || item.productCode || '-'}</td>
                      <td>{item.productDetail || item.commodityCode || '-'}</td>
                      <td>{item.billingItem || item.billingItemCode || '-'}</td>
                      <td>{formatMoney(item.periodPretaxAmount)}</td>
                      <td>{formatDecision(item.decision, text)}</td>
                    </tr>
                  ))}
                  {!items.length && <EmptyRow colSpan={5} label={text.audit.noAuditItems} />}
                </tbody>
              </table>
            </div>
          </div>
        )}
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

function defaultAuditForm(cloudAccountId?: number) {
  const billDate = offsetDate(-2);
  return {
    cloudAccountId: cloudAccountId ? String(cloudAccountId) : '',
    billDate,
    periodStartDate: offsetDate(-31),
    periodEndDate: billDate
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
