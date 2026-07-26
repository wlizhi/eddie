/**
 * @author Eddie
 * {@code @date} 2026-07-26
 */
package cc.wlizhi.eddie.common.util;

import lombok.Getter;

/**
 * 语义化版本号解析与比较工具。
 * <p>
 * 支持格式：{@code [v]major.minor.patch[-preRelease]}，例如：{@code 1.0.2-beta}、{@code v2.0.0}。
 * 版本号不重复且单调递增，比较时仅比较 major.minor.patch 数字部分，忽略 pre-release 标签。
 */
@Getter
public class SemanticVersion implements Comparable<SemanticVersion> {

    private final int major;
    private final int minor;
    private final int patch;
    private final String preRelease;

    private SemanticVersion(int major, int minor, int patch, String preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }

    /**
     * 解析版本字符串。
     *
     * @param version 版本字符串，如 "1.0.2-beta" 或 "v1.0.2-beta"
     * @return 解析后的版本对象
     * @throws IllegalArgumentException 格式不合法时抛出
     */
    public static SemanticVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("version must not be blank");
        }
        String raw = version.trim();
        // 去掉可选的 'v' 前缀
        if (raw.startsWith("v") || raw.startsWith("V")) {
            raw = raw.substring(1);
        }
        // 分离 pre-release 标签
        String numericPart = raw;
        String preRelease = null;
        int dashIdx = raw.indexOf('-');
        if (dashIdx != -1) {
            numericPart = raw.substring(0, dashIdx);
            preRelease = raw.substring(dashIdx + 1);
        }
        String[] parts = numericPart.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid version format: " + version
                    + ". Expected major.minor.patch[-preRelease]");
        }
        try {
            int major = Integer.parseInt(parts[0]);
            int minor = Integer.parseInt(parts[1]);
            int patch = Integer.parseInt(parts[2]);
            return new SemanticVersion(major, minor, patch, preRelease);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid version number in: " + version, e);
        }
    }

    @Override
    public int compareTo(SemanticVersion other) {
        int cmp = Integer.compare(this.major, other.major);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.minor, other.minor);
        if (cmp != 0) return cmp;
        cmp = Integer.compare(this.patch, other.patch);
        if (cmp != 0) return cmp;
        // major.minor.patch 相同：有 pre-release 的版本 < 无 pre-release 的版本
        if (this.preRelease != null && other.preRelease == null) return -1;
        if (this.preRelease == null && other.preRelease != null) return 1;
        return 0;
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch + (preRelease != null ? "-" + preRelease : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SemanticVersion that)) return false;
        return major == that.major && minor == that.minor && patch == that.patch;
    }

    @Override
    public int hashCode() {
        return 31 * (31 * major + minor) + patch;
    }
}
