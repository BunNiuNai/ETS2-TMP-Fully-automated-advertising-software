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

        System.out.println("\n=== 结果: " + testsPassed + "/" + testsRun + " 通过 ===");

        if (testsPassed < testsRun) {
            System.out.println("还有 " + (testsRun - testsPassed) + " 个测试等待实现");
            System.exit(1);
        }
    }
}
