package cc.wlizhi.eddie.memory.context;

import cc.wlizhi.eddie.common.cache.InitScheduler;
import cc.wlizhi.eddie.common.config.BuiltInPrompts;
import cc.wlizhi.eddie.common.config.EddieProperties;
import cc.wlizhi.eddie.common.util.PromptVariableResolver;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.PropertyPlaceholderHelper;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 全局提示词模板上下文。<p>
 * 从 {@code application.yml} 的 {@code eddie.prompts.*} 读取 classpath 路径，
 * 在初始化时加载文件实际内容并缓存，供各业务模块直接使用。
 *
 * @author Eddie
 */
@Slf4j
@Component
public class BuiltInPromptsContext {

    /**
     * ${...} 占位符解析器，未匹配的占位符保留原样
     */
    private static final PropertyPlaceholderHelper PLACEHOLDER_HELPER =
            new PropertyPlaceholderHelper("${", "}", null, null, true);
    @Resource
    private EddieProperties eddieProperties;
    @Resource
    private InitScheduler initScheduler;

    /**
     * 标题生成 prompt 模板内容
     * -- GETTER --
     * 获取标题生成 prompt 模板内容
     */
    @Getter
    private String sessionTitlePrompts;
    @Getter
    private String agentChatPrompts;
    @Getter
    private String agentTaskPlanPrompts;

    @Resource
    private ResourceLoader resourceLoader;
    @Resource
    private PromptVariableResolver promptVariableResolver;

    @PostConstruct
    void doInit() {
        BuiltInPrompts prompts = eddieProperties.getPrompts();
        initScheduler.addTask(this.getClass().getSimpleName(), 10, () -> this.doInit(prompts));
    }

    /**
     * 初始化加载所有 prompt 模板文件内容。
     *
     * @param prompts 包含各 prompt 文件 classpath 路径的配置 record
     */
    public void doInit(BuiltInPrompts prompts) {
        if (prompts == null) {
            log.warn("Prompts configuration is null, skip initialization");
            return;
        }
        this.sessionTitlePrompts = loadContent(prompts.getSessionTitlePrompts());
        this.agentChatPrompts = loadContent(prompts.getAgentChatPrompts());
        this.agentTaskPlanPrompts = loadContent(prompts.getAgentTaskPlanPrompts());
        log.info("Global prompts initialized: sessionTitlePrompts loaded={}", sessionTitlePrompts != null);
    }

    /**
     * 从 classpath 加载单个 prompt 文件内容
     *
     * @param classpath classpath 路径（不含 classpath: 前缀）
     * @return 文件内容，加载失败返回 null
     */
    private String loadContent(String classpath) {
        if (classpath == null || classpath.isBlank()) {
            log.warn("Prompt path [{}] is empty, skip", "sessionTitlePrompts");
            return null;
        }
        try {
            org.springframework.core.io.Resource resource = resourceLoader.getResource("classpath:" + classpath);
            if (!resource.exists()) {
                log.warn("Prompt resource [{}] not found: {}", "sessionTitlePrompts", classpath);
                return null;
            }
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.warn("Failed to load prompt [{}]: {}", "sessionTitlePrompts", classpath, e);
            return null;
        }
    }

    /**
     * 使用 {@code ${...}} 占位符语法解析 prompt 模板，将模板中的占位符替换为实际值。<p>
     * 未匹配到变量的占位符会保留原样（不会抛异常），适合提示词中可能包含 {@code ${...}} 字面量的场景。
     *
     * @param template  prompt 模板文本
     * @param variables 变量名 → 变量值的映射
     * @return 替换后的文本，template 为 null 时返回 null
     */
    public String resolvePrompt(String template, Map<String, String> variables) {
        return promptVariableResolver.resolve(template, variables);
    }
}
