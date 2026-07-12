package com.costbuddy.service;

import com.costbuddy.ai.AiChatClient;
import com.costbuddy.common.exception.NotFoundException;
import com.costbuddy.domain.AiEngineDO;
import com.costbuddy.domain.BillingAuditItemDO;
import com.costbuddy.domain.BillingAuditRunDO;
import com.costbuddy.domain.BillingItemExplanationDO;
import com.costbuddy.domain.CloudAccountDO;
import com.costbuddy.dto.response.BillingAuditItemResourceResponse;
import com.costbuddy.dto.response.MeteredOperationResponse;
import com.costbuddy.mapper.BillingAuditItemMapper;
import com.costbuddy.mapper.BillingAuditRawLineMapper;
import com.costbuddy.mapper.BillingItemExplanationMapper;
import com.costbuddy.metering.MeterItemCodes;
import com.costbuddy.metering.UsageMeteringResult;
import com.costbuddy.metering.UsageMeteringService;
import com.motherboard.sdk.model.ResourceType;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class BillingItemExplanationService {

    private static final String                RESOURCE_NAME = "billing_item_explanation";

    private final BillingAuditItemMapper       billingAuditItemMapper;
    private final BillingAuditRawLineMapper    billingAuditRawLineMapper;
    private final BillingItemExplanationMapper billingItemExplanationMapper;
    private final BillingAuditService          billingAuditService;
    private final AiEngineService              aiEngineService;
    private final AiChatClient                 aiChatClient;
    private final CloudAccountService          cloudAccountService;
    private final UsageMeteringService         usageMeteringService;

    public BillingItemExplanationService(BillingAuditItemMapper billingAuditItemMapper, BillingAuditRawLineMapper billingAuditRawLineMapper,
                                         BillingItemExplanationMapper billingItemExplanationMapper, BillingAuditService billingAuditService, AiEngineService aiEngineService,
                                         AiChatClient aiChatClient, CloudAccountService cloudAccountService, UsageMeteringService usageMeteringService){
        this.billingAuditItemMapper = billingAuditItemMapper;
        this.billingAuditRawLineMapper = billingAuditRawLineMapper;
        this.billingItemExplanationMapper = billingItemExplanationMapper;
        this.billingAuditService = billingAuditService;
        this.aiEngineService = aiEngineService;
        this.aiChatClient = aiChatClient;
        this.cloudAccountService = cloudAccountService;
        this.usageMeteringService = usageMeteringService;
    }

    private BillingItemExplanationDO get(Long id) {
        BillingItemExplanationDO explanation = billingItemExplanationMapper.selectById(id);
        if (explanation == null) {
            throw new NotFoundException(RESOURCE_NAME, id);
        }
        return explanation;
    }

    public List<BillingItemExplanationDO> listByAuditItem(Long runId, Long itemId) {
        getItemInRun(billingAuditService.get(runId), itemId);
        return billingItemExplanationMapper.selectByAuditItemId(itemId);
    }

    public MeteredOperationResponse<BillingItemExplanationDO> explain(Long runId, Long itemId, Long aiEngineId, String idempotencyKey) {
        BillingAuditRunDO run = billingAuditService.get(runId);
        BillingAuditItemDO item = getItemInRun(run, itemId);
        CloudAccountDO cloudAccount = cloudAccountService.get(run.getCloudAccountId());
        AiEngineDO aiEngine = aiEngineService.get(aiEngineId);
        String promptContext = buildPromptContext(item, billingAuditRawLineMapper.selectResourcesByAuditItem(item));
        UsageMeteringResult metering = usageMeteringService
            .report(MeterItemCodes.AI_OPTIMIZATION_NOTE, idempotencyKey, ResourceType.ALIYUN_CREDENTIAL, cloudAccount.getCredentialResourceId());
        if (!metering.allowed()) {
            return MeteredOperationResponse.rejected(metering);
        }
        String explanationText = aiChatClient.chat(aiEngine, systemPrompt(), promptContext);
        BillingItemExplanationDO explanation = new BillingItemExplanationDO();
        explanation.setAuditItemId(itemId);
        explanation.setAiEngineId(aiEngineId);
        explanation.setPromptContext(promptContext);
        explanation.setExplanation(explanationText);
        billingItemExplanationMapper.insert(explanation);
        return MeteredOperationResponse.allowed(metering, get(explanation.getId()));
    }

    private BillingAuditItemDO getItemInRun(BillingAuditRunDO run, Long itemId) {
        BillingAuditItemDO item = billingAuditItemMapper.selectById(itemId);
        if (item == null || !run.getId().equals(item.getRunId())) {
            throw new NotFoundException("billing_audit_item", itemId);
        }
        return item;
    }

    private String systemPrompt() {
        return """
                你是一个公共云账单排查和成本优化助手。请基于给定账单上下文，说明这个计费项对应的产品做什么用、计费项为什么产生费用，以及用户如果要释放或关闭相关资源应从哪里进入。
                要求：
                1. 使用中文回答。
                2. 不要编造确定事实；不确定时明确写“需要结合控制台或产品文档确认”。
                3. 输出精简 Markdown，必须包含：产品用途、计费项含义、费用来源、释放/关闭路径（重点）、排查建议、忽略建议。
                4. “释放/关闭路径（重点）”是重点章节，请使用这个完整标题。优先给阿里云控制台里的导航路径，例如“阿里云控制台 -> 产品名称 -> 资源/实例列表 -> 选择资源 -> 释放/删除/停用”。如果不能确认准确菜单，给可操作的搜索路径，并标明需要在控制台或产品文档确认。
                5. 如果上下文里有资源样例，结合资源 ID、地域、资源组、用量说明，并提示可以按资源 ID 在对应产品控制台搜索定位。
                6. 如果这个费用不是直接释放资源能消除的类型，请说明可能需要调整配置、关闭能力、删除关联资源或降低用量。
                """;
    }

    private String buildPromptContext(BillingAuditItemDO item, List<BillingAuditItemResourceResponse> resources) {
        StringBuilder builder = new StringBuilder();
        builder.append("云厂商: ").append(item.getProvider()).append('\n');
        builder.append("产品代码: ").append(value(item.getProductCode())).append('\n');
        builder.append("产品名称: ").append(value(item.getProductName())).append('\n');
        builder.append("产品明细: ").append(value(item.getProductDetail())).append('\n');
        builder.append("商品代码: ").append(value(item.getCommodityCode())).append('\n');
        builder.append("计费项代码: ").append(value(item.getBillingItemCode())).append('\n');
        builder.append("计费项名称: ").append(value(item.getBillingItem())).append('\n');
        builder.append("账单类型: ").append(value(item.getBillingType())).append('\n');
        builder.append("订阅类型: ").append(value(item.getSubscriptionType())).append('\n');
        builder.append("币种: ").append(value(item.getCurrency())).append('\n');
        builder.append("账单日金额: ").append(item.getPeriodPretaxAmount()).append('\n');
        builder.append("实例数量: ").append(item.getInstanceCount()).append('\n');
        builder.append("地域数量: ").append(item.getRegionCount()).append('\n');
        builder.append("样例实例: ").append(value(item.getSampleInstanceId())).append('\n');
        builder.append("样例地域: ").append(value(item.getSampleRegion())).append('\n');
        builder.append("样例用量: ").append(value(item.getSampleUsage())).append(' ').append(value(item.getSampleUsageUnit())).append('\n');
        builder.append("阿里云文档检索关键词: ")
            .append(value(item.getProductName()))
            .append(' ')
            .append(value(item.getBillingItem()))
            .append(' ')
            .append(value(item.getBillingItemCode()))
            .append('\n');
        builder.append("阿里云文档搜索链接: ").append(aliyunHelpSearchUrl(item)).append('\n');
        builder.append("控制台定位提示: 阿里云控制台 -> 搜索产品名称或产品代码 -> 资源/实例列表 -> 按资源 ID 或地域定位 -> 查看释放、删除、关闭或配置调整入口\n");
        builder.append("资源明细样例:\n");
        resources.stream()
            .limit(12)
            .forEach(resource -> builder.append("- resourceId=")
                .append(value(resource.getInstanceId()))
                .append(", name=")
                .append(value(resource.getInstanceName()))
                .append(", region=")
                .append(value(resource.getRegion()))
                .append(", resourceGroup=")
                .append(value(resource.getResourceGroup()))
                .append(", usage=")
                .append(resource.getUsageAmount())
                .append(' ')
                .append(value(resource.getUsageUnit()))
                .append(", amount=")
                .append(resource.getPretaxAmount())
                .append(' ')
                .append(value(resource.getCurrency()))
                .append(", billingType=")
                .append(value(resource.getBillingType()))
                .append('\n'));
        return builder.toString();
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String aliyunHelpSearchUrl(BillingAuditItemDO item) {
        String keyword = String.join(" ", value(item.getProductName()), value(item.getProductCode()), value(item.getBillingItem()), value(item.getBillingItemCode()))
            .replace("-", "")
            .trim();
        if (keyword.isBlank()) {
            keyword = "阿里云 计费项";
        }
        return "https://help.aliyun.com/search?keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8);
    }
}
