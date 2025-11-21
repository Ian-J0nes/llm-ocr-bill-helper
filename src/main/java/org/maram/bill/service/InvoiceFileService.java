package org.maram.bill.service;

import org.maram.bill.entity.InvoiceFile;

public interface InvoiceFileService {
    /**
     * 上传发票文件
     * @param fileData 文件数据
     * @param fileName 文件名
     * @param userId 上传用户ID
     * @return 文件记录的ID
     */
    String uploadInvoiceFile(byte[] fileData, String fileName, Long userId); 

    /**
     * 通过微信openid上传发票文件
     * @param fileData 文件数据
     * @param fileName 文件名
     * @param openid 微信openid
     * @return 文件记录的ID
     */
    String saveInvoiceFile(byte[] fileData, String fileName, String openid);

    /**
     * 通过微信openid上传发票文件
     * @param fileData 文件数据
     * @param fileName 文件名
     * @param openid 微信openid
     * @return 文件记录的ID
     */
    String uploadInvoiceFileByOpenid(byte[] fileData, String fileName, String openid);

    /**
     * 删除发票文件
     * @param fileId 文件ID
     * @return 是否成功
     */
    boolean deleteInvoiceFile(String fileId);

    /**
     * 获取发票文件信息
     * @param fileId 文件ID
     * @return 文件信息的JSON字符串
     */
    String getInvoiceFileInfo(String fileId);

    /**
     * 检查发票文件是否存在
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean existsInvoiceFile(String fileId);

    /**
     * 根据文件ID获取发票文件实体
     * @param fileId 文件ID
     * @return InvoiceFile 实体，如果未找到则返回null
     */
    InvoiceFile getInvoiceFileEntity(String fileId);
}
