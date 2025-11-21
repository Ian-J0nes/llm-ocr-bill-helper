package org.maram.bill.common.service;

import org.maram.bill.common.constants.PromptTemplates;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用于管理和格式化来自 {@link PromptTemplates} 常量类的AI提示词的服务。
 * 直接从Java类获取提示词可以避免因文件编码问题导致的乱码。
 */
@Service
public class PromptService {

    public PromptService() {
        // 构造函数现在为空，因为我们不再注入配置
    }

    /**
     * 获取AI助手“小咩”的系统提示词。
     *
     * @param categories 一个包含可用账单分类的列表，将包含在提示词中。
     * @return 格式化后的系统提示词。
     */
    public String getSystemPrompt(List<String> categories) {
        return String.format(PromptTemplates.System.XIAOMIE_TEMPLATE, String.join(", ", categories));
    }

    /**
     * 使用当前日期、可用分类和用户消息来格式化用户提示词。
     *
     * @param currentDate   当前日期的字符串。
     * @param categories    一个包含可用账单分类的列表。
     * @param userMessage   用户的消息。
     * @return 格式化后的用户提示词。
     */
    public String formatUserPromptWithCategories(String currentDate, List<String> categories, String userMessage) {
        return String.format(PromptTemplates.User.WITH_CATEGORIES, currentDate, String.join(", ", categories), userMessage);
    }

    /**
     * 为纯图片提交格式化用户提示词。
     *
     * @param currentDate 当前日期的字符串。
     * @param categories  一个包含可用账单分类的列表。
     * @param fileId      上传图片的File ID。
     * @return 格式化后的用户提示词。
     */
    public String formatUserPromptForImageOnly(String currentDate, List<String> categories, String fileId) {
        return String.format(PromptTemplates.User.IMAGE_ONLY_WITH_CATEGORIES_AND_FILE_ID, currentDate, String.join(", ", categories), fileId);
    }

    /**
     * 为包含图片和文本的提交格式化用户提示词。
     *
     * @param currentDate   当前日期的字符串。
     * @param categories    一个包含可用账单分类的列表。
     * @param userMessage   用户的文本消息。
     * @param fileId        上传图片的File ID。
     * @return 格式化后的用户提示词。
     */
    public String formatUserPromptForImageAndText(String currentDate, List<String> categories, String userMessage, String fileId) {
        return String.format(PromptTemplates.User.IMAGE_AND_TEXT_WITH_CATEGORIES_AND_FILE_ID, currentDate, String.join(", ", categories), userMessage, fileId);
    }
}
