package cc.wlizhi.eddie.common.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 通用分页结果
 */
@Data
public class PageResult<T> {

    private int page;
    private int size;
    private long total;
    private List<T> records;

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.page = 1;
        result.size = 20;
        result.total = 0;
        result.records = Collections.emptyList();
        return result;
    }

    public static <T> PageResult<T> of(int page, int size, long total, List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.page = page;
        result.size = size;
        result.total = total;
        result.records = records;
        return result;
    }
}
