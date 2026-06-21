package cc.wlizhi.eddieai.chat.service;

import cc.wlizhi.eddieai.chat.entity.request.AssistantCreateRequest;
import cc.wlizhi.eddieai.chat.entity.request.AssistantUpdateRequest;
import cc.wlizhi.eddieai.chat.entity.response.AssistantDetailVO;
import cc.wlizhi.eddieai.chat.entity.response.AssistantVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 助手列表业务接口
 */
public interface AssistantService {

    /**
     * 查询助手列表
     *
     * @param showAll true=查询全部, false=仅查询启用的
     */
    List<AssistantVO> list(boolean showAll);

    /**
     * 获取助手详情（配置回显）
     */
    AssistantDetailVO getDetail(Long id);

    /**
     * 新建助手
     */
    AssistantVO create(AssistantCreateRequest request);

    /**
     * 更新助手设置
     */
    AssistantVO update(Long id, AssistantUpdateRequest request);

    /**
     * 更新助手头像
     * <p>
     * 同时支持文字/emoji 和图片上传：
     * <ul>
     *   <li>传 file → 保存图片文件，avatar 设为图片 URL</li>
     *   <li>传 avatarText → avatar 设为文字/emoji</li>
     * </ul>
     * 自动删除旧头像文件（如果是图片路径）。
     *
     * @param id         助手 ID
     * @param avatarText 文字或 emoji（file 为空时生效）
     * @param file       上传的图片文件（非空时优先）
     * @return 更新后的助手
     */
    AssistantVO updateAvatar(Long id, String avatarText, MultipartFile file);

    /**
     * 删除助手
     */
    void delete(Long id);

    /**
     * 批量排序：按 ID 数组顺序，自动赋 sort_order = 1,2,3...
     */
    void batchSort(List<Long> ids);
}
