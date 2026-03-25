package io.github.atengk.basic;

import java.util.List;

/**
 * switch 表达式示例（JDK14+）
 */
public class SwitchExpressionExample {

    /**
     * 核心方法：switch 表达式使用
     */
    public static void switchUsage() {

        // =========================
        // 1. 基础写法（返回值🔥）
        // =========================
        int day = 3;

        String result = switch (day) {
            case 1 -> "周一";
            case 2 -> "周二";
            case 3 -> "周三";
            default -> "未知";
        };

        System.out.println("结果：" + result);


        // =========================
        // 2. 多 case 合并
        // =========================
        String type = switch (day) {
            case 1, 2, 3, 4, 5 -> "工作日";
            case 6, 7 -> "周末";
            default -> "非法";
        };

        System.out.println("类型：" + type);


        // =========================
        // 3. 代码块（yield 返回值）
        // =========================
        int score = 85;

        String level = switch (score / 10) {
            case 10, 9 -> {
                System.out.println("优秀");
                yield "A";
            }
            case 8 -> {
                System.out.println("良好");
                yield "B";
            }
            case 7 -> "C";
            default -> "D";
        };

        System.out.println("等级：" + level);


        // =========================
        // 4. 替代 if-else（推荐🔥）
        // =========================
        String role = "ADMIN";

        String permission = switch (role) {
            case "ADMIN" -> "全部权限";
            case "USER" -> "普通权限";
            default -> "游客权限";
        };

        System.out.println("权限：" + permission);


        // =========================
        // 5. 枚举（最佳实践🔥）
        // =========================
        Status status = Status.SUCCESS;

        String msg = switch (status) {
            case SUCCESS -> "成功";
            case FAIL -> "失败";
            case PROCESSING -> "处理中";
        };

        System.out.println("状态：" + msg);


        // =========================
        // 6. 避免 fall-through（更安全）
        // =========================
        // ❗ 不再需要 break，避免遗漏


        // =========================
        // 7. 结合方法封装（项目推荐）
        // =========================
        System.out.println("计算结果：" + calc(10, 5, "+"));


        // =========================
        // 8. 结合 Stream 使用
        // =========================
        List<String> roles = List.of("ADMIN", "USER", "GUEST");

        List<String> perms = roles.stream()
                .map(r -> switch (r) {
                    case "ADMIN" -> "ALL";
                    case "USER" -> "NORMAL";
                    default -> "NONE";
                })
                .toList();

        System.out.println("权限列表：" + perms);


        // =========================
        // 9. null 处理（注意⚠️）
        // =========================
        String input = null;

        try {
            String res = switch (input) {
                case "A" -> "1";
                default -> "0";
            };
        } catch (NullPointerException e) {
            System.out.println("switch 不支持 null");
        }


        // =========================
        // 10. 项目实战（状态转换🔥）
        // =========================
        int code = 200;

        String desc = getStatusDesc(code);

        System.out.println("HTTP状态：" + desc);
    }

    /**
     * 示例枚举
     */
    enum Status {
        SUCCESS, FAIL, PROCESSING
    }

    /**
     * 封装方法（项目常用）
     */
    public static String calc(int a, int b, String op) {
        return switch (op) {
            case "+" -> String.valueOf(a + b);
            case "-" -> String.valueOf(a - b);
            case "*" -> String.valueOf(a * b);
            case "/" -> b != 0 ? String.valueOf(a / b) : "除0错误";
            default -> "非法操作";
        };
    }

    /**
     * 状态码映射
     */
    public static String getStatusDesc(int code) {
        return switch (code) {
            case 200 -> "OK";
            case 404 -> "Not Found";
            case 500 -> "Server Error";
            default -> "Unknown";
        };
    }

    public static void main(String[] args) {
        switchUsage();
    }
}
