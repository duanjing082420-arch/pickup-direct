package cn.pickup.launcher;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

/**
 * CodeParser 是纯函数（不依赖 Android 框架），适合做第一个本地单元测试。
 * 三类覆盖：正常命中、必须返回 null（防误判）、裸码；重点验证 2.2.1 修复的驿站正则 bug。
 */
public class CodeParserTest {

    // ===================== 一、正常命中 =====================

    @Test
    public void parseCainiaoWithBracket_returnsCodeAndCleanStation() {
        // 真实短信：只有签名【菜鸟驿站】在括号里，驿站名「北洋园驿站」在括号外
        String sms = "【菜鸟驿站】您有一个包裹已到达北洋园驿站，取件码：3-1-2040，请及时取件";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("3-1-2040", r.code);
        assertEquals("北洋园驿站", r.station);   // 关键：修复前会解析出「【菜鸟驿站」这种垃圾
        assertEquals("菜鸟驿站", r.carrier);
        assertEquals("", r.platform);             // 菜鸟不在平台规则里，留空由上层兜底
    }

    @Test
    public void parseStation_neverStartsWithBracket() {
        // 回归：2.2.0 前的 bug 会把短信签名【菜鸟驿站】整段吞成驿站名
        String sms = "【菜鸟驿站】您的快递已到达北洋园驿站，取件码 6-4-0883";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("北洋园驿站", r.station);
        assertFalse("驿站名不能再带【", r.station.startsWith("【"));
    }

    @Test
    public void parsePinduoduo_returnsPlatformPinduoduo() {
        String sms = "【拼多多】您的包裹已到达南区代收点，取件码：6-4-0883";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("6-4-0883", r.code);
        assertEquals("南区代收点", r.station);
        assertEquals("拼多多", r.carrier);
        assertEquals("pinduoduo", r.platform);
    }

    @Test
    public void parseZhongtong_withStation() {
        String sms = "【中通快递】您的快件已到西区驿站，取件码 5-2-3311，凭此码取件";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("5-2-3311", r.code);
        assertEquals("西区驿站", r.station);
        assertEquals("中通快递", r.carrier);
    }

    @Test
    public void parseNoSeparatorCode() {
        String sms = "【顺丰】您的快递已到达东区驿站，取件码A1024";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("A1024", r.code);
        assertEquals("东区驿站", r.station);
    }

    @Test
    public void parseHuoWeiPattern() {
        String sms = "您的包裹已到货位：2-3-1050，请到北洋园驿站取件";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("2-3-1050", r.code);
        assertEquals("北洋园驿站", r.station);
    }

    @Test
    public void parsePingPrefix() {
        String sms = "请到南区驿站凭8-1-2233领取您的快递";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("8-1-2233", r.code);
        assertEquals("南区驿站", r.station);
    }

    @Test
    public void parseFullWidthDash_normalizesToAscii() {
        String sms = "【菜鸟】取件码：3－1－2040";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("3-1-2040", r.code);   // 全角横杠必须规整成半角
    }

    @Test
    public void parseMultiLine_keepsNewline() {
        String sms = "【菜鸟驿站】您的快递已到达天津XX驿站，取件码 6-4-0883\n请及时领取";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("6-4-0883", r.code);
        assertEquals("天津XX驿站", r.station);
    }

    @Test
    public void parseStationWithBracketOnly() {
        String sms = "北洋园菜鸟驿站提醒您：取件码 4-1-5521";
        CodeParser.Result r = CodeParser.parse(sms);
        assertNotNull(r);
        assertEquals("4-1-5521", r.code);
        assertEquals("北洋园菜鸟驿站", r.station);
    }

    // ===================== 二、必须返回 null（防误判） =====================

    @Test
    public void parseVerificationCode_returnsNull() {
        assertNull(CodeParser.parse("您的验证码是123456，5分钟内有效"));
    }

    @Test
    public void parseBankSms_returnsNull() {
        assertNull(CodeParser.parse("【招商银行】您尾号1234的卡消费人民币30.00元"));
    }

    @Test
    public void parseShippingNoticeNoCode_returnsNull() {
        // 含「快递」关键词但没有任何取件码
        assertNull(CodeParser.parse("您的订单已发货，快递单号 1234567890"));
    }

    @Test
    public void parseEmpty_returnsNull() {
        assertNull(CodeParser.parse(""));
        assertNull(CodeParser.parse(null));
    }

    // ===================== 三、裸码 =====================

    @Test
    public void parseBareCode_valid() {
        assertEquals("3-1-2040", CodeParser.parseBareCode("3-1-2040"));
    }

    @Test
    public void parseBareCode_tooShort_returnsNull() {
        assertNull(CodeParser.parseBareCode("123"));   // 长度 < 4
    }

    @Test
    public void parseBareCode_notCode_returnsNull() {
        assertNull(CodeParser.parseBareCode("这是一段随手复制的普通文字"));
    }

    @Test
    public void parseBareCode_fullWidthDash_normalizes() {
        assertEquals("3-1-2040", CodeParser.parseBareCode("3－1－2040"));
    }
}
