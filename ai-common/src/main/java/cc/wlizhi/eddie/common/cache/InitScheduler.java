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
        tasks.forEach(taskDefinition -> {
            long startTime = System.currentTimeMillis();
            log.info("初始化任务-{}：{} 执行中...", taskDefinition.getOrder(), taskDefinition.getName());
            try {
                taskDefinition.task.run();
                log.info("初始化任务-{}：{} 执行完毕，耗时：{}ms", taskDefinition.getOrder(), taskDefinition.getName(), System.currentTimeMillis() - startTime);
            } catch (Exception e) {
                log.info("初始化任务-{}：{} 执行异常，耗时：{}ms", taskDefinition.getOrder(), taskDefinition.getName(), System.currentTimeMillis() - startTime);
                log.error(e.getMessage(), e);
                SpringApplication.exit(applicationContext, () -> -1);
            }
        });
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
