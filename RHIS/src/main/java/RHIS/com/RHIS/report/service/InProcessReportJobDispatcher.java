package RHIS.com.RHIS.report.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

@Component
public class InProcessReportJobDispatcher implements ReportJobDispatcher {
    private final TaskExecutor executor;
    private final ReportGenerationWorker generationWorker;
    private final ReportExportWorker exportWorker;
    private final ReportJobStateService stateService;
    private final Map<UUID, Future<?>> generationTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Future<?>> exportTasks = new ConcurrentHashMap<>();

    public InProcessReportJobDispatcher(
            @Qualifier("reportJobExecutor") TaskExecutor executor,
            ReportGenerationWorker generationWorker,
            ReportExportWorker exportWorker,
            ReportJobStateService stateService
    ) {
        this.executor = executor;
        this.generationWorker = generationWorker;
        this.exportWorker = exportWorker;
        this.stateService = stateService;
    }

    @Override
    public void dispatchGeneration(UUID generationId) {
        submit(generationId, generationTasks, () -> generationWorker.run(generationId));
    }

    @Override
    public void dispatchExport(UUID exportId) {
        submit(exportId, exportTasks, () -> exportWorker.run(exportId));
    }

    @Override
    public void cancelGeneration(UUID generationId) {
        Future<?> generation = generationTasks.remove(generationId);
        if (generation != null) {
            generation.cancel(true);
        }
    }

    @Override
    public void cancelExport(UUID exportId) {
        Future<?> export = exportTasks.remove(exportId);
        if (export != null) {
            export.cancel(true);
        }
    }

    private void submit(UUID id, Map<UUID, Future<?>> tasks, Runnable work) {
        FutureTask<Void> task = new FutureTask<>(() -> {
            try {
                work.run();
            } finally {
                tasks.remove(id);
            }
            return null;
        });
        tasks.put(id, task);
        try {
            executor.execute(task);
        } catch (RuntimeException exception) {
            tasks.remove(id);
            if (tasks == generationTasks) {
                stateService.failGeneration(id, "JOB_CAPACITY_EXCEEDED");
            } else {
                stateService.failExport(id, "JOB_CAPACITY_EXCEEDED");
            }
        }
    }
}
