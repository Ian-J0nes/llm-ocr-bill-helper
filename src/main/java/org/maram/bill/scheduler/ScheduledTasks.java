package org.maram.bill.scheduler;

import org.maram.bill.config.properties.SchedulerProperties;
import org.maram.bill.service.ExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final ExchangeRateService exchangeRateService;
    private final SchedulerProperties schedulerProperties;

    @Autowired
    public ScheduledTasks(ExchangeRateService exchangeRateService, SchedulerProperties schedulerProperties) {
        this.exchangeRateService = exchangeRateService;
        this.schedulerProperties = schedulerProperties;
    }

    // 使用配置文件中的 cron 表达式
    @Scheduled(cron = "${scheduler.update-exchange-rates-cron}") // 从配置文件读取cron表达式
    public void reportCurrentTime() {
        logger.info("执行定时任务：更新汇率数据。");
        try {
            exchangeRateService.fetchAndUpdateExchangeRates();
        } catch (Exception e) {
            logger.error("定时更新汇率任务执行失败。", e);
        }
        logger.info("定时任务：更新汇率数据 执行完毕。");
    }
}