package cc.wlizhi.eddieai.common.dto;

import lombok.Data;

/**
 * 通用分页请求
 */
@Data
public class PageRequest {

    private int page = 1;
    private int size = 20;
}
