package cc.wlizhi.eddie.common.cache;

import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 项目启动初始化任务编排器。
 * <p>
 * 随着项目代码量和初始化内容的增加，部分初始化步骤之间存在依赖关系，必须按特定顺序加载才能正常工作。
 * 此类将所有初始化任务集中注册、统一调度，通过 {@link #addTask(String, int, Runnable)} 注册
 * 带优先级的任务，在 {@link ApplicationReadyEvent} 事件触发后异步执行。所有初始化任务按
 * {@link TaskDefinition#order} 升序执行，任一任务异常将触发应用退出（{@link System#exit(int)}）。
 * <p>
 * <b>设计目标：</b>
 * <ul>
 *   <li><b>启动速度固定可控</b> — 将初始化与主流程解耦，启动时间不会随业务代码迭代线性增加</li>
 *   <li><b>集中管理，方便排查</b> — 所有初始化步骤集中编排，出现问题可快速定位</li>
 *   <li><b>尽早提供服务</b> — 采取"边启动边初始化"策略，核心服务先就绪，非关键初始化后台完成</li>
 * </ul>
 * <p>
 * <b>得失权衡：</b>
 * 异步初始化在性能较差的机器上可能存在极端情况：启动后立即访问页面时，部分数据尚未初始化完成，
 * 导致部分功能短暂不可用。这类似于现代操作系统（如 Windows/macOS）的启动体验——桌面加载完成后，
 * 部分软件并非立即完全可用。整体上，尽早提供可用服务对用户更为友好。
 *
 * @author Eddie
 * {@code @date} 2026-06-29
 */
@Slf4j
@Component
public class InitScheduler {
    @Resource
    private ApplicationContext applicationContext;

    private final List<TaskDefinition> tasks = new ArrayList<>();

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        CompletableFuture.runAsync(this::doInit);
    }

    private void doInit() {
        long doInitStartTime = System.currentTimeMillis();
        Collections.sort(tasks);
        log.info("==================初始化任务列表===================");
        for (TaskDefinition task : tasks) {
            log.info("初始化任务-{}：{}", task.getOrder(), task.getName());
        }
        log.info("==================初始化任务开始执行===================");
        for (TaskDefinition taskDefinition : tasks) {
            long startTime = System.currentTimeMillis();
            log.info("初始化任务-{}：{} 执行中...", taskDefinition.getOrder(), taskDefinition.getName());
            try {
                taskDefinition.task.run();
                log.info("初始化任务-{}：{} 执行完毕，耗时：{}ms", taskDefinition.getOrder(), taskDefinition.getName(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.error("初始化任务-{}：{} 执行异常，耗时：{}ms，应用即将退出", taskDefinition.getOrder(), taskDefinition.getName(), System.currentTimeMillis() - startTime, e);
                int exitCode = SpringApplication.exit(applicationContext, () -> -1);
                System.exit(exitCode);
            }
        }
        log.info("初始化任务数量：{}，总耗时：{}ms", tasks.size(), System.currentTimeMillis() - doInitStartTime);
        log.info("==================初始化任务执行完毕===================");
        tasks.clear();
    }

    public void addTask(String name, int order, Runnable task) {
        tasks.add(new TaskDefinition(name, order, task));
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    static class TaskDefinition implements Comparable<TaskDefinition> {
        private String name;
        private Integer order;
        private Runnable task;


        @Override
        public int compareTo(@NonNull TaskDefinition td) {
            return Integer.compare(order, td.getOrder());
        }
    }
}
