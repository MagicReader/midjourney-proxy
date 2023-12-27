package com.github.novicezk.midjourney.support;

import cn.hutool.core.text.CharSequenceUtil;
import com.github.novicezk.midjourney.ProxyProperties;
import com.github.novicezk.midjourney.ReturnCode;
import com.github.novicezk.midjourney.enums.TaskStatus;
import com.github.novicezk.midjourney.result.Message;
import com.github.novicezk.midjourney.result.SubmitResultVO;
import com.github.novicezk.midjourney.service.NotifyService;
import com.github.novicezk.midjourney.service.TaskStoreService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
public class TaskQueueHelper {
    @Resource
    private TaskStoreService taskStoreService;
    @Resource
    private NotifyService notifyService;

    private final ProxyProperties properties;
    private final DiscordAccountConfigPool discordAccountConfigPool;
    private final ThreadPoolTaskExecutor taskExecutor;
    private final List<Task> runningTasks;
    private final Map<String, Future<?>> taskFutureMap;

    public TaskQueueHelper(ProxyProperties properties,DiscordAccountConfigPool discordAccountConfigPool) {
        this.properties = properties;
        this.discordAccountConfigPool = discordAccountConfigPool;
        this.runningTasks = new CopyOnWriteArrayList<>();
        this.taskFutureMap = new ConcurrentHashMap<>();
        this.taskExecutor = new ThreadPoolTaskExecutor();
        this.updateThreadPoolTaskExecutorConfig();
        this.taskExecutor.setThreadNamePrefix("TaskQueue-");
        this.taskExecutor.initialize();
    }

    public void updateThreadPoolTaskExecutorConfig(){
        List<DiscordAccountConfig> discordAccountOpenList = discordAccountConfigPool.getDiscordAccountConfigList()
                .stream()
                .filter(item-> BooleanUtils.isTrue(item.getOpenFlag()))
                .collect(Collectors.toList());
        int accountPoolSize = discordAccountOpenList.size()>0 ? discordAccountOpenList.size() : 1;
        ProxyProperties.TaskQueueConfig queueConfig = properties.getQueue();
        int corePoolSize = queueConfig.getCoreSize() * accountPoolSize;
        int queueCapacity = queueConfig.getQueueSize() * accountPoolSize;
        this.taskExecutor.setMaxPoolSize(corePoolSize);
        this.taskExecutor.setCorePoolSize(corePoolSize);
        this.taskExecutor.setQueueCapacity(queueCapacity);
    }

    public Set<String> getQueueTaskIds() {
        return this.taskFutureMap.keySet();
    }

    public Task getRunningTask(String id) {
        if (CharSequenceUtil.isBlank(id)) {
            return null;
        }
        return this.runningTasks.stream().filter(t -> id.equals(t.getId())).findFirst().orElse(null);
    }

    public Stream<Task> findRunningTask(Predicate<Task> condition) {
        return this.runningTasks.stream().filter(condition);
    }

    public Future<?> getRunningFuture(String taskId) {
        return this.taskFutureMap.get(taskId);
    }

    public SubmitResultVO submitTask(Task task, Callable<Message<Void>> discordSubmit) {
        this.taskStoreService.save(task);
        int size;
        SubmitResultVO submitResultVO;
        try {
            size = this.taskExecutor.getThreadPoolExecutor().getQueue().size();
            Future<?> future = this.taskExecutor.submit(() -> executeTask(task, discordSubmit));
            this.taskFutureMap.put(task.getId(), future);
            if (size == 0) {
                submitResultVO = SubmitResultVO.of(ReturnCode.SUCCESS, "提交成功", task.getId());
            } else {
                submitResultVO = SubmitResultVO.of(ReturnCode.IN_QUEUE, "排队中，前面还有" + size + "个任务", task.getId())
                        .setProperty("numberOfQueues", size);
            }
        } catch (RejectedExecutionException e) {
            this.taskStoreService.delete(task.getId());
            submitResultVO = SubmitResultVO.fail(ReturnCode.QUEUE_REJECTED, "队列已满，请稍后尝试");
        } catch (Exception e) {
            log.error("submit task error", e);
            submitResultVO = SubmitResultVO.fail(ReturnCode.FAILURE, "提交失败，系统异常");
        }
        submitResultVO.setGuildId(task.getGuildId());
        submitResultVO.setChannelId(task.getChannelId());
        return submitResultVO;
    }

    private void executeTask(Task task, Callable<Message<Void>> discordSubmit) {
        this.runningTasks.add(task);
        try {
            task.start();
            Message<Void> result = discordSubmit.call();
            if (result.getCode() != ReturnCode.SUCCESS) {
                task.fail(result.getDescription());
                changeStatusAndNotify(task, TaskStatus.FAILURE);
                return;
            }
            changeStatusAndNotify(task, TaskStatus.SUBMITTED);
            do {
                task.sleep();
                // 任务完成后，立即从运行中的任务列表中删除，防止相同提示词的任务重复匹配到某个已完成的任务上
                if(task.getStatus() == TaskStatus.SUCCESS){
                    this.runningTasks.remove(task);
                }
                changeStatusAndNotify(task, task.getStatus());
            } while (task.getStatus() == TaskStatus.IN_PROGRESS);
            log.debug("task finished, id: {}, status: {}", task.getId(), task.getStatus());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            log.error("task execute error", e);
            task.fail("执行错误，系统异常");
            changeStatusAndNotify(task, TaskStatus.FAILURE);
        } finally {
            this.runningTasks.remove(task);
            this.taskFutureMap.remove(task.getId());
        }
    }

    public void changeStatusAndNotify(Task task, TaskStatus status) {
        task.setStatus(status);
        this.taskStoreService.save(task);
        this.notifyService.notifyTaskChange(task);
    }
}
