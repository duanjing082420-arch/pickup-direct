package cn.pickup.launcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从短信 / 通知 / 剪贴板文本中识别取件码、快递公司和驿站位置。
 */
final class CodeParser {
    static final class Result {
        String code = "";
        String carrier = "";
        String station = "";
    }

    // 必须出现这些关键词之一，才认为这条消息和取件有关
    private static final String[] CONTEXT_KEYWORDS = {
            "取件", "取货", "驿站", "代收", "包裹", "快递", "待取", "菜鸟", "自提", "货架"
    };

    // 取件码的常见写法：取件码：3-1-2040 / 取件码 6-4-0883 / 取件码A1024 / 凭 3-1-2040 取件
    private static final Pattern[] CODE_PATTERNS = {
            Pattern.compile("取件码(?:是)?\\s*[:：]?\\s*([0-9A-Za-z]{1,6}(?:[-－][0-9A-Za-z]{1,8}){0,3})"),
            Pattern.compile("取货号\\s*[:：]?\\s*([0-9A-Za-z]{1,6}(?:[-－][0-9A-Za-z]{1,8}){0,3})"),
            Pattern.compile("取货码\\s*[:：]?\\s*([0-9A-Za-z]{1,6}(?:[-－][0-9A-Za-z]{1,8}){0,3})"),
            Pattern.compile("凭\\s*([0-9A-Za-z]{1,6}(?:[-－][0-9A-Za-z]{1,8}){0,3})\\s*(?:取件|领取|取货)"),
            Pattern.compile("货位\\s*[:：]?\\s*([0-9A-Za-z]{1,6}(?:[-－][0-9A-Za-z]{1,8}){0,3})")
    };

    // 驿站 / 代收点名称
    private static final Pattern[] STATION_PATTERNS = {
            Pattern.compile("([^\\s，,。;；]{2,20}(?:驿站|代收点|服务中心|快递柜|自提点|菜鸟))"),
            Pattern.compile("到\\s*([^\\s，,。;；]{2,20}(?:驿站|代收点|门店))")
    };

    private static final String[] CARRIERS = {
            "菜鸟驿站", "菜鸟", "中通", "圆通", "申通", "韵达", "极兔", "京东",
            "顺丰", "邮政", "EMS", "丹鸟", "拼多多", "多多买菜", "淘宝"
    };

    private CodeParser() {
    }

    /** 解析一段文本，解析不到返回 null。 */
    static Result parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String value = text.replace('\n', ' ');
        if (!hasContext(value)) {
            return null;
        }
        String code = findCode(value);
        if (code == null) {
            return null;
        }
        Result result = new Result();
        result.code = code;
        result.carrier = findCarrier(value);
        result.station = findStation(value);
        return result;
    }

    /** 剪贴板专用：用户可能只复制了一串裸取件码，没有上下文。 */
    static String parseBareCode(String text) {
        if (text == null) {
            return null;
        }
        String value = text.trim();
        if (value.length() < 4 || value.length() > 24) {
            return null;
        }
        if (!value.matches("[0-9A-Za-z]+(?:[-－][0-9A-Za-z]+){0,3}")) {
            return null;
        }
        return value.replace('－', '-');
    }

    private static boolean hasContext(String value) {
        for (String keyword : CONTEXT_KEYWORDS) {
            if (value.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static String findCode(String value) {
        for (Pattern pattern : CODE_PATTERNS) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                String code = matcher.group(1).replace('－', '-');
                if (code.length() >= 3) {
                    return code;
                }
            }
        }
        return null;
    }

    private static String findCarrier(String value) {
        for (String carrier : CARRIERS) {
            if (value.contains(carrier)) {
                return carrier;
            }
        }
        return "";
    }

    private static String findStation(String value) {
        for (Pattern pattern : STATION_PATTERNS) {
            Matcher matcher = pattern.matcher(value);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return "";
    }
}
