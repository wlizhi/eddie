/**
 * @author Eddie
 * {@code @date} 2026-07-26
 */
package cc.wlizhi.eddie.app.controller;

import cc.wlizhi.eddie.common.config.EddieProperties;
import cc.wlizhi.eddie.common.dto.ApiResult;
import cc.wlizhi.eddie.common.util.SemanticVersion;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * 应用信息与版本升级接口。
 */
@RestController
@RequestMapping("/api/app")
@RequiredArgsConstructor
public class AppController {

    private static final Logger log = LoggerFactory.getLogger(AppController.class);

    /**
     * GitHub Releases 列表 API，默认按 created_at 降序排列，第一个即最新。
     * 相比 /releases/latest，此端点会返回 pre-release。
     */
    private static final String GITHUB_RELEASES_URL = "https://api.github.com/repos/wlizhi/eddie/releases?per_page=1";
    private static final String GITHUB_REPO_URL = "https://github.com/wlizhi/eddie";

    /**
     * GitHub Token，从 GITHUB_TOKEN 环境变量读取（可选）。
     * 配置后可提升 API 限流从 60 次/小时到 5000 次/小时。
     * 注意：不要将此 token 提交到代码仓库。
     */
    private static final String GITHUB_TOKEN;

    static {
        String token = System.getenv("GITHUB_TOKEN");
        if (token == null || token.isBlank()) {
            token = System.getenv("GH_TOKEN");
        }
        GITHUB_TOKEN = token;
        if (GITHUB_TOKEN != null) {
            log.info("GITHUB_TOKEN 已配置，GitHub API 限流将提升至 5000 次/小时");
        }
    }

    private final EddieProperties eddieProperties;
    private final RestClient restClient;

    /**
     * 获取当前版本信息。
     *
     * @return 当前版本号与仓库地址
     */
    @GetMapping("/version")
    public ApiResult<AppVersionVO> getVersion() {
        AppVersionVO vo = new AppVersionVO();
        vo.setCurrentVersion(eddieProperties.getVersion());
        vo.setRepository(GITHUB_REPO_URL);
        return ApiResult.success(vo);
    }

    /**
     * 检查 GitHub 上是否有新版本。
     * <p>
     * 调用 GitHub Releases 列表 API（?per_page=1），取第一个（最新的）Release 进行比较。
     * 使用列表 API 而非 /releases/latest，因为后者不返回 pre-release。
     *
     * @return 更新检查结果
     */
    @GetMapping("/check-update")
    public ApiResult<UpdateCheckResult> checkUpdate() {
        String currentVersion = eddieProperties.getVersion();
        UpdateCheckResult result = new UpdateCheckResult();
        result.setCurrentVersion(currentVersion);
        result.setRepository(GITHUB_REPO_URL);

        try {
            // 调用 GitHub Releases 列表 API，取第一条（最新）
            var request = restClient.get()
                    .uri(GITHUB_RELEASES_URL);
            // 如果有 GITHUB_TOKEN 环境变量，添加认证头以提升限流
            if (GITHUB_TOKEN != null) {
                request = request.header("Authorization", "Bearer " + GITHUB_TOKEN);
            }
            List<GitHubRelease> releases = request.retrieve()
                    .body(new ParameterizedTypeReference<List<GitHubRelease>>() {
                    });

            if (releases == null || releases.isEmpty()) {
                log.warn("GitHub Releases API returned empty list");
                result.setHasUpdate(false);
                result.setLatestVersion(null);
                result.setMessage("无法获取最新版本信息");
                return ApiResult.success(result);
            }

            GitHubRelease latest = releases.getFirst();
            String latestTag = latest.getTagName();
            if (latestTag == null || latestTag.isBlank()) {
                log.warn("GitHub release missing tag_name");
                result.setHasUpdate(false);
                result.setLatestVersion(null);
                result.setMessage("无法获取最新版本信息");
                return ApiResult.success(result);
            }

            // GitHub tag 格式为 v1.0.2-beta，去掉 v 前缀
            String latestVersion = latestTag.startsWith("v") || latestTag.startsWith("V")
                    ? latestTag.substring(1) : latestTag;
            result.setLatestVersion(latestVersion);
            result.setDownloadUrl(GITHUB_REPO_URL + "/releases/tag/" + latestTag);
            result.setReleaseNotesUrl(latest.getHtmlUrl());
            result.setReleaseNotes(latest.getBody());

            // 比较版本
            SemanticVersion currentVer = SemanticVersion.parse(currentVersion);
            SemanticVersion latestVer = SemanticVersion.parse(latestVersion);
            int cmp = currentVer.compareTo(latestVer);

            if (cmp < 0) {
                result.setHasUpdate(true);
                result.setMessage("发现新版本 " + latestVersion);
            } else {
                result.setHasUpdate(false);
                result.setMessage("已是最新版本");
            }
        } catch (Exception e) {
            log.warn("检查更新失败", e);
            result.setHasUpdate(false);
            result.setLatestVersion(null);
            result.setMessage("检查更新失败，请稍后重试");
        }

        return ApiResult.success(result);
    }

    // ===== DTO =====

    @Getter
    @Setter
    public static class AppVersionVO {
        private String currentVersion;
        private String repository;
    }

    @Getter
    @Setter
    public static class UpdateCheckResult {
        /** 是否有新版本 */
        private boolean hasUpdate;
        /** 当前版本号 */
        private String currentVersion;
        /** 最新版本号（无更新时为 null） */
        private String latestVersion;
        /** 下载页面 URL */
        private String downloadUrl;
        /** Release 备注页面 URL */
        private String releaseNotesUrl;
        /** Release 备注内容（Markdown） */
        private String releaseNotes;
        /** 提示消息 */
        private String message;
        /** 仓库地址 */
        private String repository;
    }

    /**
     * GitHub Releases API 响应结构（仅提取需要的字段）。
     */
    @Getter
    @Setter
    public static class GitHubRelease {
        @JsonProperty("tag_name")
        private String tagName;

        @JsonProperty("html_url")
        private String htmlUrl;

        private String body;
    }
}
