/**
 * @author Eddie
 * {@code @date} 2026-07-04
 */

package cc.wlizhi.eddie.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shell 工具权限配置。<p>
 * 存储在对应 MCP Server 的 {@code source_config} 字段中。
 * 默认 SMART 模式：只读命令自动放行，写入命令需确认，高危命令直接拒绝。
 *
 * @author Eddie
 */
@Getter
@Setter
public class ShellToolConfig {

    /** 权限模式 */
    private String mode = "SMART";

    /** 黑名单命令前缀（CUSTOM 模式生效，命中直接拒绝） */
    private List<String> blacklist = List.of(
            "rm", "dd", "mkfs", "fdisk", "sudo", "chmod", "chown", ">"
    );

    /** 白名单命令前缀（CUSTOM 模式生效，仅允许这些命令） */
    private List<String> whitelist = List.of(
            "ls", "cat", "echo", "pwd", "cp", "mv", "head", "tail",
            "grep", "find", "ps", "df", "du", "uname", "whoami",
            "date", "which", "wc", "sort", "uniq", "diff", "tree",
            "file", "stat", "env", "printenv", "locale", "uptime",
            "hostname", "id", "who", "w", "last", "lsof", "netstat",
            "ifconfig", "lscpu", "lsblk", "lspci", "lsusb", "ping",
            "traceroute", "nslookup", "dig", "curl", "wget", "mkdir",
            "touch", "ln", "tee"
    );

    // ===== 内置只读命令字典（SMART 模式使用，不可配置） =====

    /**
     * 只读命令前缀集合。<p>
     * SMART 模式下，以这些前缀开头的命令自动放行，无需确认。
     */
    public static final Set<String> READONLY_PREFIXES = Set.of(
            "ls", "cat", "echo", "pwd", "which", "type", "whereis",
            "head", "tail", "less", "more", "grep", "find", "locate",
            "wc", "sort", "uniq", "diff", "cmp", "cut", "tr", "tree",
            "file", "stat", "du", "df",
            "uname", "hostname", "uptime", "date", "cal", "whoami",
            "id", "who", "w", "last", "ps", "top", "htop",
            "lsof", "netstat", "ss", "ifconfig", "ip a", "ip addr",
            "lscpu", "lsblk", "lspci", "lsusb",
            "env", "printenv", "getconf", "locale",
            "ping", "traceroute", "nslookup", "dig",
            "curl", "wget", "git status", "git log", "git diff",
            "npm ls", "npm list", "pip list", "brew list",
            "systemctl status", "service --status-all",
            "sysctl -a", "ulimit -a", "timedatectl"
    );

    /**
     * 高危命令前缀集合。<p>
     * SMART 模式下，以这些前缀开头的命令直接拒绝，不询问用户。
     */
    public static final Set<String> DANGEROUS_PREFIXES = Set.of(
            "rm -rf /", "rm -rf --no-preserve-root",
            "dd if=", "mkfs", "fdisk", "parted",
            ":(){ :|:& };:", "> /dev/sd", "> /dev/nvme",
            "chmod -R 777 /", "chown -R 0:0 /",
            "sudo rm", "sudo dd", "sudo mkfs",
            "mv / ", "cp / ", "wget -O /",
            "curl -o /", "bash <(curl ", "sh -c \"$(curl "
    );

    // ===== 内置写入命令集合（SMART 模式使用，需确认） =====

    /**
     * 写入操作命令前缀集合。<p>
     * SMART 模式下，以这些前缀开头的命令属于写入操作，需用户确认后才执行。
     */
    public static final Set<String> WRITE_PREFIXES = Set.of(
            "touch", "mkdir", "rmdir", "cp", "mv", "ln",
            "rm -",  // 排除 rm -rf / 这种高危
            "chmod", "chown", "chattr",
            "kill", "killall", "pkill", "renice",
            "sudo", "su",
            "mount", "umount", "dd ",
            "iptables", "ufw", "nmcli",
            "systemctl start", "systemctl stop", "systemctl restart",
            "systemctl enable", "systemctl disable",
            "service start", "service stop", "service restart",
            "launchctl",
            "brew install", "brew uninstall", "brew update", "brew upgrade",
            "apt install", "apt remove", "apt update", "apt upgrade",
            "yum install", "yum remove", "yum update",
            "npm install", "npm uninstall", "npm run",
            "pip install", "pip uninstall",
            "git add", "git commit", "git push", "git pull", "git merge",
            "git reset", "git checkout", "git branch -d",
            "echo >", "echo >>", "cat >", "cat >>",
            "tee",  "sed -i", "awk -i",
            "> ", ">> ",
            "nohup", "bg", "fg",
            "crontab", "at",
            "passwd", "useradd", "usermod", "userdel", "groupadd",
            "shutdown", "reboot", "halt", "poweroff",
            "docker run", "docker start", "docker stop", "docker rm",
            "docker build", "docker push",
            "kubectl apply", "kubectl delete", "kubectl create"
    );

    /**
     * 判断命令是否为只读操作（无需用户确认）。
     *
     * @param command 完整 shell 命令
     * @return true 表示只读操作
     */
    public static boolean isReadOnly(String command) {
        String trimmed = command.trim();
        return READONLY_PREFIXES.stream().anyMatch(trimmed::startsWith);
    }

    /**
     * 判断命令是否为高危操作（直接拒绝）。
     *
     * @param command 完整 shell 命令
     * @return true 表示高危操作
     */
    public static boolean isDangerous(String command) {
        String trimmed = command.trim();
        return DANGEROUS_PREFIXES.stream().anyMatch(trimmed::startsWith);
    }

    /**
     * 判断命令是否为写入操作（需要用户确认）。
     *
     * @param command 完整 shell 命令
     * @return true 表示写入操作
     */
    public static boolean isWriteOperation(String command) {
        String trimmed = command.trim();
        return WRITE_PREFIXES.stream().anyMatch(trimmed::startsWith);
    }
}
