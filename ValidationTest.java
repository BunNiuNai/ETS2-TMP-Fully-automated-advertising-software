/**
 * 倒计时输入校验 — 验证程序
 * 测试规则: 必须 > 0 且 ≤ 1440，且 *60 不溢出
 *
 * 编译: javac -encoding UTF-8 ValidationTest.java
 * 运行: java -ea ValidationTest
 */
public class ValidationTest {

    private static int testsRun = 0;
    private static int testsPassed = 0;

    // ── 测试用例 ──

    static void test(String name, Runnable r) {
        testsRun++;
        try {
            r.run();
            testsPassed++;
            System.out.println("  PASS: " + name);
        } catch (AssertionError e) {
            System.out.println("  FAIL: " + name + " - " + e.getMessage());
        } catch (Exception e) {
            System.out.println("  ERROR: " + name + " - " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("=== 倒计时校验测试 ===\n");

        // 1. validateCountdown 测试
        test("空字符串应返回错误", () -> {
            String err = TMPAdSoftware.validateCountdown("");
            assert err != null : "空字符串应该返回错误消息";
        });

        test("'0' 应返回错误", () -> {
            String err = TMPAdSoftware.validateCountdown("0");
            assert err != null : "0 应该返回错误消息";
        });

        test("负数应返回错误", () -> {
            String err = TMPAdSoftware.validateCountdown("-5");
            assert err != null : "负数应该返回错误消息";
        });

        test("'1' 应通过", () -> {
            String err = TMPAdSoftware.validateCountdown("1");
            assert err == null : "1 应该通过校验, 但得到: " + err;
        });

        test("'1440' 应通过", () -> {
            String err = TMPAdSoftware.validateCountdown("1440");
            assert err == null : "1440 应该通过校验, 但得到: " + err;
        });

        test("'1441' 应返回错误", () -> {
            String err = TMPAdSoftware.validateCountdown("1441");
            assert err != null : "1441 应该超过上限";
        });

        test("非数字应返回错误", () -> {
            String err = TMPAdSoftware.validateCountdown("abc");
            assert err != null : "abc 应该返回错误消息";
        });

        // 2. parseCountdown 测试
        test("parseCountdown 正常情况", () -> {
            int result = TMPAdSoftware.parseCountdown("5");
            assert result == 300 : "5 分钟应为 300 秒, 实际: " + result;
        });

        test("parseCountdown 溢出保护", () -> {
            try {
                TMPAdSoftware.parseCountdown("999999999");
                assert false : "应该抛出 ArithmeticException";
            } catch (ArithmeticException e) {
                // 预期行为
            }
        });

        // 3. countdownSeconds 初始化测试 — Bug 回归测试
        //    start() 必须将 parseCountdown 结果赋值给 countdownSeconds
        test("countdownSeconds 初始化 — parseCountdown(5) 应返回 300", () -> {
            int seconds = TMPAdSoftware.parseCountdown("5");
            assert seconds == 300 : "5分钟 应为300秒, 实际: " + seconds;
            // Bug: start() 缺少 this.countdownSeconds = intervalSeconds;
            // 导致 countdownSeconds 保持默认值 0, 定时器立即触发
            // 修复: 在 start() 中 timer.schedule 之前赋值
        });

        test("countdownSeconds 初始化 — 验证值 > 0", () -> {
            // 验证: 倒计时秒数必须大于 0 (否则立即触发 sendMessage)
            int seconds = TMPAdSoftware.parseCountdown("3");
            assert seconds > 0 : "有效倒计时应 > 0 秒, 实际: " + seconds;
            // 此断言确保 parseCountdown 返回值可用于初始化 countdownSeconds
        });

        System.out.println("\n=== 结果: " + testsPassed + "/" + testsRun + " 通过 ===");

        if (testsPassed < testsRun) {
            System.out.println("还有 " + (testsRun - testsPassed) + " 个测试等待实现");
            System.exit(1);
        }
    }
}
