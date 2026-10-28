package cn.pickup.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public final class MainActivity extends Activity {
    static final String ACTION_OPEN = "cn.pickup.launcher.OPEN";
    static final String EXTRA_DESTINATION = "destination";

    // ===== 待办清单主题（对应 v6 原型） =====
    private static final int PAGE_BACKGROUND = Color.rgb(237, 241, 247);
    private static final int CARD_WHITE = Color.WHITE;
    private static final int TEXT_PRIMARY = Color.rgb(42, 52, 70);
    private static final int TEXT_SECONDARY = Color.rgb(138, 148, 166);
    private static final int LINE_COLOR = Color.rgb(228, 233, 241);
    private static final int ACCENT_WATER = Color.rgb(52, 97, 143);
    private static final int NAV_IDLE = Color.rgb(154, 166, 157);
    private static final int CHECK_IDLE = Color.rgb(198, 207, 221);
    private static final int DONE_TEXT = Color.rgb(160, 172, 178);

    // ===== 平台品牌色（渐变起止） =====
    private static final int[] CAINIAO_COLORS = {Color.rgb(63, 179, 212), Color.rgb(31, 122, 158)};
    private static final int[] TAOBAO_COLORS = {Color.rgb(255, 144, 70), Color.rgb(232, 115, 44)};
    private static final int[] PDD_COLORS = {Color.rgb(232, 80, 106), Color.rgb(214, 58, 82)};
    private static final int[] JD_COLORS = {Color.rgb(224, 74, 50), Color.rgb(196, 52, 31)};
    private static final int[] XHS_COLORS = {Color.rgb(232, 98, 156), Color.rgb(216, 67, 124)};
    private static final int[] DOUYIN_COLORS = {Color.rgb(43, 43, 51), Color.rgb(15, 15, 20)};

    // 顶部两个分段：取件（默认） / 身份码
    private static final int TAB_PICKUP = 0;
    private static final int TAB_CHECKOUT = 1;

    private final View[] segmentButtons = new View[2];
    private View addWrapView;
    private ScrollView pickupPage;
    private ScrollView checkoutPage;
    private int currentTab = TAB_PICKUP;
    private TextView summaryTitle;
    private TextView summaryHint;
    private ProgressRingView summaryRing;
    private LinearLayout packageListContainer;
    private TextView smsStatusView;
    private TextView notifyStatusView;
    private TextView historyCountView;
    private LinearLayout historyDialogContainer;
    private final List<String> justPickedCodes = new ArrayList<>();

    private static final int REQUEST_SMS_PERMISSION = 1001;
    private static final int SWIPE_ACTION_WIDTH = 88;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Destination destination = destinationFromIntent();
        if (destination != null) {
            DeepLinkLauncher.open(this, destination);
            finish();
            return;
        }

        getWindow().setStatusBarColor(PAGE_BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildContent());
    }

    private Destination destinationFromIntent() {
        String action = getIntent().getAction();
        if (ACTION_OPEN.equals(action)) {
            return Destination.fromKey(getIntent().getStringExtra(EXTRA_DESTINATION));
        }
        if ("cn.pickup.launcher.OPEN_CAINIAO".equals(action)) {
            return Destination.CAINIAO;
        }
        if ("cn.pickup.launcher.OPEN_TAOBAO".equals(action)) {
            return Destination.TAOBAO;
        }
        if ("cn.pickup.launcher.OPEN_PINDUODUO".equals(action)) {
            return Destination.PINDUODUO;
        }
        if ("cn.pickup.launcher.OPEN_TAOBAO_PENDING".equals(action)) {
            return Destination.TAOBAO_PENDING;
        }
        if ("cn.pickup.launcher.OPEN_PINDUODUO_PENDING".equals(action)) {
            return Destination.PINDUODUO_PENDING;
        }
        if ("cn.pickup.launcher.OPEN_JD".equals(action)) {
            return Destination.JD;
        }
        if ("cn.pickup.launcher.OPEN_XHS".equals(action)) {
            return Destination.XHS;
        }
        return null;
    }

    // =====================================================================
    // 页面骨架：内容区（三个可切换页面） + 底部导航
    // =====================================================================

    private View buildContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(PAGE_BACKGROUND);

        // targetSdk 35 强制 edge-to-edge：内容会画到状态栏/手势条底下，必须避让
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top;
            int bottom;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                top = insets.getInsets(android.view.WindowInsets.Type.statusBars()).top;
                bottom = insets.getInsets(android.view.WindowInsets.Type.navigationBars()).bottom;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
            }
            if (top > 0) {
                v.setPadding(0, top, 0, 0);
            }
            if (addWrapView != null && bottom > 0) {
                addWrapView.setPadding(0, dp(10), 0, bottom);
            }
            return insets;
        });

        root.addView(buildHeader());
        root.addView(buildSegmentBar());

        FrameLayout content = new FrameLayout(this);
        pickupPage = buildPickupPage();
        checkoutPage = buildCheckoutPage();
        checkoutPage.setVisibility(View.GONE);
        content.addView(pickupPage, frameFillParams());
        content.addView(checkoutPage, frameFillParams());
        root.addView(content, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        addWrapView = buildAddButtonWrap();
        root.addView(addWrapView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        return root;
    }

    /** 顶部标题栏：我的包裹 + 右上角多功能入口（历史 / 查待取 / 自动获取 / 添加到桌面）。 */
    private View buildHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(20), dp(10), dp(14), dp(4));

        header.addView(text("我的包裹", 21, TEXT_PRIMARY, Typeface.BOLD),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView menu = text("≡", 18, TEXT_PRIMARY, Typeface.BOLD);
        menu.setGravity(Gravity.CENTER);
        menu.setBackground(rounded(CARD_WHITE, 999));
        menu.setClickable(true);
        menu.setFocusable(true);
        menu.setContentDescription("更多功能：历史取件码、查待取快递、自动获取设置、添加到桌面");
        menu.setOnClickListener(v -> showMenuDialog());
        header.addView(menu, new LinearLayout.LayoutParams(dp(36), dp(36)));
        return header;
    }

    /** 顶部「取件 / 身份码」分段切换，替代原来的底部导航。 */
    private View buildSegmentBar() {
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackground(rounded(Color.rgb(226, 232, 241), 999));
        bar.setPadding(dp(4), dp(4), dp(4), dp(4));
        LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        barParams.setMargins(dp(20), dp(8), dp(20), dp(6));
        bar.setLayoutParams(barParams);

        segmentButtons[TAB_PICKUP] = segmentButton("取件", TAB_PICKUP);
        segmentButtons[TAB_CHECKOUT] = segmentButton("身份码", TAB_CHECKOUT);
        bar.addView(segmentButtons[TAB_PICKUP], horizontalWeightParams(0));
        bar.addView(segmentButtons[TAB_CHECKOUT], horizontalWeightParams(dp(4)));
        restyleSegmentBar();
        return bar;
    }

    private LinearLayout segmentButton(String label, int tab) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription("切换到" + label);
        item.setOnClickListener(v -> switchTab(tab));
        item.addView(text(label, 14, TEXT_SECONDARY, Typeface.BOLD));
        return item;
    }

    private void restyleSegmentBar() {
        for (int i = 0; i < segmentButtons.length; i++) {
            LinearLayout item = (LinearLayout) segmentButtons[i];
            boolean active = i == currentTab;
            item.setBackground(rounded(active ? CARD_WHITE : Color.TRANSPARENT, 999));
            item.setPadding(dp(10), dp(9), dp(10), dp(9));
            TextView label = (TextView) item.getChildAt(0);
            label.setTextColor(active ? ACCENT_WATER : TEXT_SECONDARY);
        }
    }

    private void switchTab(int tab) {
        currentTab = tab;
        pickupPage.setVisibility(tab == TAB_PICKUP ? View.VISIBLE : View.GONE);
        checkoutPage.setVisibility(tab == TAB_CHECKOUT ? View.VISIBLE : View.GONE);
        restyleSegmentBar();
    }

    /** 底部常驻「＋ 添加包裹」按钮，单手拇指的自然落点。 */
    private View buildAddButtonWrap() {
        FrameLayout wrap = new FrameLayout(this);
        wrap.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.TRANSPARENT, PAGE_BACKGROUND}));
        wrap.setPadding(0, dp(14), 0, dp(12));

        TextView add = text("＋ 添加包裹", 16, Color.WHITE, Typeface.BOLD);
        add.setGravity(Gravity.CENTER);
        add.setBackground(gradient(new int[]{Color.rgb(72, 120, 168), ACCENT_WATER}, 999));
        add.setClickable(true);
        add.setFocusable(true);
        add.setContentDescription("添加包裹");
        add.setOnClickListener(v -> showAddDialog(add));
        FrameLayout.LayoutParams addParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(52)
        );
        addParams.setMargins(dp(20), 0, dp(20), 0);
        wrap.addView(add, addParams);
        return wrap;
    }

    private FrameLayout.LayoutParams frameFillParams() {
        return new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    // =====================================================================
    // 右上菜单：历史 / 查待取 / 自动获取设置 / 添加到桌面
    // =====================================================================

    private void showMenuDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(10), dp(4), dp(10), dp(4));
        box.addView(menuItem("🕘", "历史取件码", v -> showHistoryDialog()));
        box.addView(menuItem("🚚", "查待取快递", v -> showPendingDialog()));
        box.addView(menuItem("🔔", "自动获取设置", v -> showAutoSettingsDialog()));
        box.addView(menuItem("📌", "添加到桌面", v -> showPinDialog()));
        new AlertDialog.Builder(this)
                .setTitle("更多")
                .setView(box)
                .show();
    }

    private View menuItem(String emoji, String label, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(13), dp(14), dp(13));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);

        TextView emojiView = text(emoji, 16, TEXT_PRIMARY, Typeface.NORMAL);
        emojiView.setGravity(Gravity.CENTER);
        row.addView(emojiView, new LinearLayout.LayoutParams(dp(30), dp(30)));

        TextView labelView = text(label, 15, TEXT_PRIMARY, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        labelParams.leftMargin = dp(12);
        row.addView(labelView, labelParams);

        row.addView(text("›", 22, TEXT_SECONDARY, Typeface.NORMAL));
        return row;
    }

    // =====================================================================
    // 待取（弹窗：跳外部 App 看别人家的包裹）
    // =====================================================================

    private void showPendingDialog() {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(buildPendingContent());
        new AlertDialog.Builder(this)
                .setTitle("查待取快递")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private LinearLayout buildPendingContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(4), dp(18), dp(8));

        root.addView(buildHero("五个平台各查各的", "末端系统数据不通，要一个个看"));

        // ---- 五个待取入口 ----
        LinearLayout grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setWeightSum(5f);
        grid.setPadding(0, dp(4), 0, dp(4));
        grid.addView(quickEntry(Destination.TAOBAO_PENDING, "淘", "淘宝待取", TAOBAO_COLORS), horizontalWeightParams(0));
        grid.addView(quickEntry(Destination.PINDUODUO_PENDING, "拼", "拼多多待取", PDD_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.JD, "京", "京东待取", JD_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.XHS, "书", "小红书待取", XHS_COLORS), horizontalWeightParams(dp(5)));
        grid.addView(quickEntry(Destination.DOUYIN, "抖", "抖音待取", DOUYIN_COLORS), horizontalWeightParams(dp(5)));
        LinearLayout.LayoutParams gridParams = verticalParams(dp(12));
        grid.setLayoutParams(gridParams);
        root.addView(grid);

        root.addView(hintCard("🔀",
                "菜鸟驿站和多多驿站是两套独立系统，数据不互通。"
                        + "拼多多的件常常在淘宝里查不到，两个都要看。"));

        return root;
    }

    // =====================================================================
    // 添加到桌面（弹窗）
    // =====================================================================

    private void showPinDialog() {
        ScrollView scroll = new ScrollView(this);
        scroll.addView(buildPinContent());
        new AlertDialog.Builder(this)
                .setTitle("添加到桌面")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private LinearLayout buildPinContent() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(4), dp(18), dp(8));

        root.addView(hintCard("📌", "添加后不必进入本应用，桌面点击一次即可跳转。"));

        LinearLayout pinRow1 = new LinearLayout(this);
        pinRow1.setOrientation(LinearLayout.HORIZONTAL);
        pinRow1.setWeightSum(4f);
        pinRow1.setLayoutParams(verticalParams(dp(12)));
        addPinButton(pinRow1, Destination.TAOBAO_PENDING, "淘宝待取");
        addPinButton(pinRow1, Destination.PINDUODUO_PENDING, "拼多多待取");
        addPinButton(pinRow1, Destination.JD, "京东待取");
        addPinButton(pinRow1, Destination.XHS, "小红书待取");
        root.addView(pinRow1);

        LinearLayout pinRow2 = new LinearLayout(this);
        pinRow2.setOrientation(LinearLayout.HORIZONTAL);
        pinRow2.setWeightSum(4f);
        pinRow2.setLayoutParams(verticalParams(dp(8)));
        addPinButton(pinRow2, Destination.DOUYIN, "抖音待取");
        addPinButton(pinRow2, Destination.CAINIAO, "菜鸟码");
        addPinButton(pinRow2, Destination.TAOBAO, "淘宝码");
        addPinButton(pinRow2, Destination.PINDUODUO, "拼多多码");
        root.addView(pinRow2);

        return root;
    }

    // =====================================================================
    // 自动获取设置（弹窗：短信权限、通知使用权）
    // =====================================================================

    private void showAutoSettingsDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(18), dp(4), dp(18), dp(8));

        box.addView(hintCard("✨", "开启后，取件短信和菜鸟/淘宝/拼多多通知里的取件码会自动保存，不用手动录入。"));

        smsStatusView = text("", 11, TEXT_SECONDARY, Typeface.NORMAL);
        box.addView(settingsRow("📩", "短信自动识别", smsStatusView, v -> requestSmsPermission()));

        notifyStatusView = text("", 11, TEXT_SECONDARY, Typeface.NORMAL);
        box.addView(settingsRow("🔔", "通知自动识别", notifyStatusView, v -> openNotificationListenerSettings()));

        box.addView(hintCard("💡", "在菜鸟 / 淘宝复制取件码后切回本 App，会自动识别并询问是否保存。"));
        scroll.addView(box);

        updateAutoStatus();
        new AlertDialog.Builder(this)
                .setTitle("自动获取设置")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private View buildHero(String title, String subtitle) {
        LinearLayout hero = new LinearLayout(this);
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setLayoutParams(verticalParams(0));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = text(title, 24, TEXT_PRIMARY, Typeface.BOLD);
        titles.addView(titleView);

        TextView subtitleView = text(subtitle, 13, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(4);
        subtitleView.setLayoutParams(subtitleParams);
        titles.addView(subtitleView);

        hero.addView(titles, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        return hero;
    }

    private View quickEntry(Destination destination, String mark, String label, int[] colors) {
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER_HORIZONTAL);
        item.setClickable(true);
        item.setFocusable(true);
        item.setContentDescription("打开" + destination.title);
        item.setOnClickListener(v -> DeepLinkLauncher.open(this, destination));

        TextView icon = text(mark, 18, Color.WHITE, Typeface.BOLD);
        icon.setGravity(Gravity.CENTER);
        icon.setBackground(gradient(colors, 17));
        item.addView(icon, new LinearLayout.LayoutParams(dp(50), dp(50)));

        TextView labelView = text(label, 11, TEXT_SECONDARY, Typeface.NORMAL);
        labelView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(7);
        labelView.setLayoutParams(labelParams);
        item.addView(labelView);
        return item;
    }

    private View pendingCard(
            Destination destination,
            String title,
            String subtitle,
            int[] colors
    ) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(15), dp(14), dp(15));
        card.setMinimumHeight(dp(78));
        card.setBackground(rounded(CARD_WHITE, 18));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("打开" + destination.title);
        card.setOnClickListener(v -> DeepLinkLauncher.open(this, destination));

        LinearLayout.LayoutParams cardParams = verticalParams(dp(12));
        card.setLayoutParams(cardParams);

        TextView mark = text(destination.mark, 16, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(colors, 12));
        card.addView(mark, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, dp(10), 0);

        TextView titleView = text(title, 16, TEXT_PRIMARY, Typeface.BOLD);
        labels.addView(titleView);

        TextView subtitleView = text(subtitle, 12, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subtitleParams.topMargin = dp(3);
        subtitleView.setLayoutParams(subtitleParams);
        labels.addView(subtitleView);

        card.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 26, colors[1], Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        card.addView(arrow, new LinearLayout.LayoutParams(dp(28), dp(42)));

        return card;
    }

    // =====================================================================
    // 页面一：取件（自己的取件码卡片，默认进入）
    // =====================================================================

    private ScrollView buildPickupPage() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAGE_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(8), dp(18), dp(16));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        // ---- 汇总卡：进度环 + 待取数量 ----
        root.addView(buildSummaryCard());

        packageListContainer = new LinearLayout(this);
        packageListContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(packageListContainer);
        refreshPackageList();

        return scrollView;
    }

    /** 汇总卡：进度环（已取/总数）+「还有 N 个待取」。 */
    private View buildSummaryCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setBackground(rounded(CARD_WHITE, 18));
        LinearLayout.LayoutParams cardParams = verticalParams(dp(10));
        card.setLayoutParams(cardParams);

        summaryRing = new ProgressRingView(this,
                LINE_COLOR, ACCENT_WATER, TEXT_PRIMARY, TEXT_SECONDARY);
        card.addView(summaryRing, new LinearLayout.LayoutParams(dp(52), dp(52)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, dp(8), 0);

        summaryTitle = text("", 15, TEXT_PRIMARY, Typeface.BOLD);
        labels.addView(summaryTitle);

        summaryHint = text("", 12, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        hintParams.topMargin = dp(3);
        summaryHint.setLayoutParams(hintParams);
        labels.addView(summaryHint);

        card.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    /** taken/total：分母 = 列表里的取件码总数（已取 + 未取）。 */
    private void refreshSummary(int taken, int total) {
        if (summaryRing != null) {
            summaryRing.setProgress(taken, total, true);
        }
        if (summaryTitle != null) {
            int left = total - taken;
            summaryTitle.setText(total == 0
                    ? "还没有包裹"
                    : left == 0 ? "全部取完，出库走起 🎉" : "还有 " + left + " 个待取");
        }
        if (summaryHint != null) {
            summaryHint.setText(total == 0
                    ? "点下方「＋ 添加包裹」录入取件码"
                    : "点圆圈完成，变灰沉到底部");
        }
    }

    // =====================================================================
    // 页面二：出库（亮身份码，最高频动作，放在导航栏中间）
    // =====================================================================

    private ScrollView buildCheckoutPage() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(PAGE_BACKGROUND);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(14), dp(18), dp(20));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.WRAP_CONTENT
        ));

        // ---- 身份码平铺：一张卡一个平台，点一次直接亮码 ----
        root.addView(identityEntry(Destination.TAOBAO));
        root.addView(identityEntry(Destination.PINDUODUO));

        root.addView(hintCard("🛡️",
                "取件码是包裹的，身份码是你的。出库只有淘宝和拼多多两套，菜鸟归淘宝。"
                        + "两套末端系统数据不互通，淘宝里常常查不到拼多多的件。"));

        return scrollView;
    }

    /** 平铺身份码入口卡：点整张卡直接打开对应平台身份码。 */
    private View identityEntry(Destination destination) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(18), dp(18), dp(16), dp(18));
        card.setBackground(rounded(CARD_WHITE, 18));
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("打开" + destination.identityName() + "身份码");
        card.setOnClickListener(v -> DeepLinkLauncher.open(this, destination));
        card.setLayoutParams(verticalParams(dp(12)));

        TextView mark = text(destination.mark, 20, Color.WHITE, Typeface.BOLD);
        mark.setGravity(Gravity.CENTER);
        mark.setBackground(gradient(colorsFor(destination), 14));
        card.addView(mark, new LinearLayout.LayoutParams(dp(48), dp(48)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, dp(8), 0);

        labels.addView(text(destination.identityName() + "身份码", 16, TEXT_PRIMARY, Typeface.BOLD));

        TextView sub = text(destination == Destination.PINDUODUO
                        ? "多多代收点 · 校园取件"
                        : "菜鸟驿站 · 校园取件",
                12, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        subParams.topMargin = dp(3);
        sub.setLayoutParams(subParams);
        labels.addView(sub);

        card.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView go = text("亮码 ›", 14, ACCENT_WATER, Typeface.BOLD);
        go.setGravity(Gravity.CENTER);
        go.setBackground(rounded(Color.rgb(232, 240, 249), 999));
        go.setPadding(dp(14), dp(9), dp(14), dp(9));
        card.addView(go);
        return card;
    }

    private int[] colorsFor(Destination destination) {
        switch (destination) {
            case CAINIAO:
                return CAINIAO_COLORS;
            case PINDUODUO:
            case PINDUODUO_PENDING:
                return PDD_COLORS;
            case JD:
                return JD_COLORS;
            case XHS:
                return XHS_COLORS;
            case DOUYIN:
                return DOUYIN_COLORS;
            case TAOBAO:
            case TAOBAO_PENDING:
            default:
                return TAOBAO_COLORS;
        }
    }

    // =====================================================================
    // 我的包裹：手动录入 + 本地保存 + 大号取件码卡片
    // =====================================================================

    @Override
    protected void onResume() {
        super.onResume();
        // 关键：不要在 onResume 里清空 justPickedCodes。清空只在冷启动（新 Activity 实例）
        // 时自然发生——这样本会话内完成的卡片会保持灰态并沉到列表底部，切到出库页再切回来
        // 也不会消失；只有杀进程重新打开，已完成的卡片才真正归档进历史记录。
        refreshPackageList();
        refreshHistory();
        updateAutoStatus();
        checkClipboard();
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            updateAutoStatus();
            if (hasSmsPermission()) {
                Toast.makeText(this, "已开启短信识别", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "未授权，短信里的取件码不会自动保存", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 标记完成 / 恢复待取。
     * 完成时先在原地播放一笔画对勾并变灰（透明度动画），动画结束再重建列表让它沉底；
     * 恢复时直接重建即可。justPickedCodes 保证会话内沉底的卡片可见，重启才真正归档。
     */
    private void togglePicked(PackageStore.Item item, View cardView) {
        if (item.picked) {
            PackageStore.markPicked(this, item, false);
            justPickedCodes.remove(item.code);
            Toast.makeText(this, "已恢复为待取件", Toast.LENGTH_SHORT).show();
            refreshPackageList();
            refreshHistory();
            return;
        }
        PackageStore.markPicked(this, item, true);
        justPickedCodes.add(item.code);
        Toast.makeText(this, "已取件，可在「历史取件码」里查看", Toast.LENGTH_SHORT).show();
        if (cardView != null) {
            cardView.animate().alpha(0.45f).setDuration(380).start();
            cardView.postDelayed(() -> {
                refreshPackageList();
                refreshHistory();
            }, 420);
        } else {
            refreshPackageList();
            refreshHistory();
        }
    }

    private List<PackageStore.Item> loadHistory() {
        List<PackageStore.Item> history = new ArrayList<>();
        for (PackageStore.Item item : PackageStore.load(this)) {
            if (item.picked) {
                history.add(item);
            }
        }
        return history;
    }

    private View buildClearHistoryButton(int size) {
        TextView clear = text("清空历史记录", 12, TEXT_SECONDARY, Typeface.BOLD);
        clear.setGravity(Gravity.CENTER);
        clear.setBackground(rounded(CARD_WHITE, 999));
        clear.setPadding(dp(16), dp(9), dp(16), dp(9));
        clear.setClickable(true);
        clear.setFocusable(true);
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空历史记录")
                .setMessage("将删除 " + size + " 条已取件记录，无法恢复。")
                .setPositiveButton("清空", (dialog, which) -> {
                    PackageStore.clearHistory(this);
                    justPickedCodes.clear();
                    refreshPackageList();
                    refreshHistory();
                })
                .setNegativeButton("取消", null)
                .show());
        LinearLayout.LayoutParams clearParams = verticalParams(dp(12));
        clear.setLayoutParams(clearParams);
        return clear;
    }

    private void showHistoryDialog() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(14), dp(22), dp(6));
        scroll.addView(box);

        historyDialogContainer = box;
        refreshHistory();

        new AlertDialog.Builder(this)
                .setTitle("历史记录（已取件）")
                .setView(scroll)
                .setPositiveButton("关闭", null)
                .show();
    }

    private void deleteHistoryItem(PackageStore.Item item) {
        List<PackageStore.Item> items = PackageStore.load(this);
        List<PackageStore.Item> remaining = new ArrayList<>();
        for (PackageStore.Item current : items) {
            if (current.createdAt != item.createdAt || !current.code.equals(item.code)) {
                remaining.add(current);
            }
        }
        PackageStore.save(this, remaining);
        justPickedCodes.remove(item.code);
        refreshPackageList();
        refreshHistory();
    }

    private void refreshHistory() {
        if (historyCountView != null) {
            int count = loadHistory().size();
            historyCountView.setText(count == 0
                    ? "暂无已取件记录"
                    : count + " 条已取件记录");
        }
        if (historyDialogContainer == null) {
            return;
        }
        historyDialogContainer.removeAllViews();
        List<PackageStore.Item> history = loadHistory();
        if (history.isEmpty()) {
            historyDialogContainer.addView(hintCard("🗂️",
                    "还没有取件记录。在上方包裹卡片左侧点圆圈打勾，就会归档到这里。"));
            return;
        }
        for (PackageStore.Item item : history) {
            historyDialogContainer.addView(historyRow(item));
        }
        historyDialogContainer.addView(buildClearHistoryButton(history.size()));
    }

    private View historyRow(PackageStore.Item item) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(14), dp(12), dp(12), dp(12));
        row.setBackground(rounded(Color.rgb(246, 249, 250), 16));
        row.setLayoutParams(verticalParams(dp(10)));

        TextView check = text("✓", 12, Color.rgb(63, 179, 212), Typeface.BOLD);
        check.setGravity(Gravity.CENTER);
        row.addView(check, new LinearLayout.LayoutParams(dp(26), dp(26)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(10), 0, dp(8), 0);

        String title = (item.carrier == null || item.carrier.isEmpty())
                ? item.code
                : item.carrier + " · " + item.code;
        TextView titleView = text(title, 14, TEXT_SECONDARY, Typeface.BOLD);
        titleView.setPaintFlags(titleView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        labels.addView(titleView);

        String detail = formatTime(item.pickedAt);
        if (item.station != null && !item.station.isEmpty()) {
            detail = item.station + " · " + detail;
        }
        TextView detailView = text(detail, 11, Color.rgb(160, 172, 178), Typeface.NORMAL);
        LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        detailParams.topMargin = dp(2);
        detailView.setLayoutParams(detailParams);
        labels.addView(detailView);

        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView delete = text("✕", 13, Color.rgb(160, 172, 178), Typeface.NORMAL);
        delete.setGravity(Gravity.CENTER);
        delete.setClickable(true);
        delete.setFocusable(true);
        delete.setContentDescription("删除这条记录");
        delete.setOnClickListener(v -> deleteHistoryItem(item));
        row.addView(delete, new LinearLayout.LayoutParams(dp(28), dp(32)));
        return row;
    }

    private String formatTime(long timeMillis) {
        if (timeMillis <= 0) {
            return "已取件";
        }
        return new java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.CHINA)
                .format(new java.util.Date(timeMillis));
    }

    private void refreshPackageList() {
        if (packageListContainer == null) {
            return;
        }
        packageListContainer.removeAllViews();
        List<PackageStore.Item> items = PackageStore.load(this);

        List<PackageStore.Item> active = new ArrayList<>();
        List<PackageStore.Item> done = new ArrayList<>();
        for (PackageStore.Item item : items) {
            if (item.picked) {
                if (justPickedCodes.contains(item.code)) {
                    done.add(item);
                }
            } else {
                active.add(item);
            }
        }

        if (active.isEmpty() && done.isEmpty()) {
            packageListContainer.addView(hintCard("📭",
                    "还没有待取的包裹。点下方「＋ 添加包裹」录入取件码，它就会一直显示在这里。"));
            refreshSummary(0, 0);
            return;
        }

        for (PackageStore.Item item : active) {
            packageListContainer.addView(packageCard(item, false));
        }
        for (PackageStore.Item item : done) {
            packageListContainer.addView(packageCard(item, true));
        }
        refreshSummary(done.size(), active.size() + done.size());
    }

    /**
     * 待办清单风格卡片（对应 v6 原型）：
     * 左侧大号勾选圈（一笔画对勾）→ 取件码 + 驿站/平台说明 → 平台亮码胶囊。
     * 点卡片空白处复制取件码；左滑露出删除。
     */
    private View packageCard(PackageStore.Item item, boolean done) {
        // 出库平台是说明行的一部分：快递公司在校园场景里没用，不再展示
        Destination identity = Destination.identityFor(item.platform);
        String station = item.station == null ? "" : item.station.trim();
        boolean hasStation = !station.isEmpty();

        // SwipeRow（HorizontalScrollView）子 View 需要-wrap：按屏宽减页边距定宽
        int cardWidth = getResources().getDisplayMetrics().widthPixels - dp(36);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(14), dp(14));
        card.setBackground(rounded(done ? Color.rgb(246, 249, 250) : CARD_WHITE, 16));
        card.setLayoutParams(new LinearLayout.LayoutParams(cardWidth,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // ---- 左：大号勾选圈 ----
        CheckCircleView check = new CheckCircleView(this, CHECK_IDLE, ACCENT_WATER);
        check.setCheckedState(done, false);
        check.setContentDescription(done ? "取消已取件标记" : "标记为已取件");
        check.setOnClickListener(v -> togglePicked(item, card));
        card.addView(check, new LinearLayout.LayoutParams(dp(46), dp(46)));

        // ---- 中：取件码 + 驿站/平台说明 ----
        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(14), 0, dp(8), 0);

        TextView codeView = text("取件码：" + item.code, 16,
                done ? DONE_TEXT : TEXT_PRIMARY, Typeface.BOLD);
        if (done) {
            codeView.setPaintFlags(codeView.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        }
        labels.addView(codeView);

        TextView metaView = text((hasStation ? station + "驿站" : "未标注驿站")
                        + " · " + identity.identityName() + "出库",
                12, TEXT_SECONDARY, Typeface.NORMAL);
        LinearLayout.LayoutParams metaParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        metaParams.topMargin = dp(3);
        metaView.setLayoutParams(metaParams);
        labels.addView(metaView);

        card.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        // ---- 右：平台亮码胶囊（按钮本身就是平台标记） ----
        TextView checkout = text("亮码", 13, Color.WHITE, Typeface.BOLD);
        checkout.setGravity(Gravity.CENTER);
        checkout.setBackground(rounded(done ? Color.rgb(186, 200, 206) : colorsFor(identity)[1], 999));
        checkout.setPadding(dp(13), dp(8), dp(13), dp(8));
        checkout.setClickable(true);
        checkout.setFocusable(true);
        checkout.setContentDescription("打开" + identity.title + "身份码");
        checkout.setOnClickListener(v -> DeepLinkLauncher.open(this, identity));
        card.addView(checkout);

        // 点卡片空白处复制取件码
        card.setClickable(true);
        card.setFocusable(true);
        card.setContentDescription("复制取件码 " + item.code);
        card.setOnClickListener(v -> copyCode(item.code));

        // ---- 删除只走左滑：卡片左滑露出垃圾桶 ----
        SwipeRow swipe = new SwipeRow();
        swipe.setLayoutParams(verticalParams(dp(10)));

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
        row.addView(card);
        row.addView(buildSwipeDeleteAction(item, swipe));
        swipe.addView(row);
        return swipe;
    }

    private final class SwipeRow extends HorizontalScrollView {
        SwipeRow() {
            super(MainActivity.this);
            setHorizontalScrollBarEnabled(false);
            setOverScrollMode(View.OVER_SCROLL_NEVER);
            setOnTouchListener((v, event) -> {
                int action = event.getActionMasked();
                if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                    int actionWidth = dp(SWIPE_ACTION_WIDTH);
                    final int target = getScrollX() > actionWidth / 2 ? actionWidth : 0;
                    post(() -> smoothScrollTo(target, 0));
                }
                return false;
            });
        }

        @Override
        public void fling(int velocityX) {
            // 关掉惯性甩动，避免一下滑过头
        }
    }

    private View buildSwipeDeleteAction(PackageStore.Item item, final SwipeRow swipe) {
        LinearLayout action = new LinearLayout(this);
        action.setOrientation(LinearLayout.VERTICAL);
        action.setGravity(Gravity.CENTER);
        action.setBackground(gradient(new int[]{Color.rgb(236, 100, 94), Color.rgb(204, 60, 56)}, 18));
        action.setMinimumHeight(dp(96));
        action.setPadding(dp(8), dp(16), dp(8), dp(16));
        action.setClickable(true);
        action.setFocusable(true);
        action.setContentDescription("删除包裹");
        action.setLayoutParams(new LinearLayout.LayoutParams(
                dp(SWIPE_ACTION_WIDTH), LinearLayout.LayoutParams.WRAP_CONTENT));
        action.setOnClickListener(v -> {
            swipe.smoothScrollTo(0, 0);
            confirmDelete(item);
        });

        ImageView icon = new ImageView(this);
        icon.setImageResource(android.R.drawable.ic_menu_delete);
        icon.setColorFilter(Color.WHITE);
        action.addView(icon, new LinearLayout.LayoutParams(dp(28), dp(28)));

        TextView label = text("删除", 12, Color.WHITE, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        labelParams.topMargin = dp(4);
        label.setLayoutParams(labelParams);
        action.addView(label);
        return action;
    }

    private void showAddDialog(View anchor) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), 0);

        EditText carrierInput = inputField("快递公司 / 平台（如：中通、菜鸟驿站）");
        box.addView(carrierInput);

        EditText codeInput = inputField("取件码（如：3-1-2040）");
        LinearLayout.LayoutParams codeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        codeParams.topMargin = dp(10);
        codeInput.setLayoutParams(codeParams);
        box.addView(codeInput);

        EditText stationInput = inputField("驿站位置（可选，如：南区菜鸟驿站）");
        LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stationParams.topMargin = dp(10);
        stationInput.setLayoutParams(stationParams);
        box.addView(stationInput);

        box.addView(platformSectionTitle());
        final PlatformHolder holder = new PlatformHolder();
        box.addView(buildPlatformPicker(holder, Destination.TAOBAO));

        // 智能粘贴：把整段取件短信粘进来，自动拆出取件码 / 驿站 / 平台，
        // 不让整段文字堆在「取件码」那一格里占地方
        TextView pasteBtn = text("📋 粘贴短信自动识别", 13, ACCENT_WATER, Typeface.BOLD);
        pasteBtn.setGravity(Gravity.CENTER);
        GradientDrawable pasteBg = rounded(CARD_WHITE, 999);
        pasteBg.setStroke(dp(1), LINE_COLOR);
        pasteBtn.setBackground(pasteBg);
        pasteBtn.setPadding(dp(14), dp(9), dp(14), dp(9));
        pasteBtn.setClickable(true);
        pasteBtn.setFocusable(true);
        pasteBtn.setContentDescription("粘贴短信自动识别");
        LinearLayout.LayoutParams pasteParams = verticalParams(dp(14));
        pasteParams.topMargin = dp(12);
        pasteBtn.setLayoutParams(pasteParams);
        pasteBtn.setOnClickListener(v -> pasteAndParse(carrierInput, codeInput, stationInput, holder));
        box.addView(pasteBtn);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("添加包裹")
                .setView(box)
                .setPositiveButton("保存", (d, which) -> {
                    String rawCode = codeInput.getText().toString().trim();
                    // 兜底：用户可能直接把整段短信粘进了取件码框，保存时再拆一次
                    CodeParser.Result parsed = CodeParser.parse(rawCode);
                    String code;
                    String carrier;
                    String station;
                    String platform;
                    if (parsed != null && !parsed.code.isEmpty()) {
                        code = parsed.code;
                        carrier = parsed.carrier.isEmpty()
                                ? carrierInput.getText().toString().trim() : parsed.carrier;
                        station = parsed.station.isEmpty()
                                ? stationInput.getText().toString().trim() : parsed.station;
                        platform = parsed.platform.isEmpty()
                                ? holder.value.key : Destination.identityFor(parsed.platform).key;
                    } else {
                        code = rawCode;
                        carrier = carrierInput.getText().toString().trim();
                        station = stationInput.getText().toString().trim();
                        platform = holder.value.key;
                    }
                    if (code.isEmpty()) {
                        Toast.makeText(this, "没有填写取件码，未保存", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    PackageStore.Item item = new PackageStore.Item();
                    item.carrier = carrier;
                    item.code = code;
                    item.station = station;
                    item.platform = platform;
                    item.createdAt = System.currentTimeMillis();
                    List<PackageStore.Item> items = PackageStore.load(this);
                    items.add(0, item);
                    PackageStore.save(this, items);
                    refreshPackageList();
                    Toast.makeText(this, "已添加包裹", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
        playCircularReveal(dialog, anchor);
    }

    /**
     * 从「添加包裹」按钮位置圆形扩散打开弹窗（API 21+，minSdk 23 恒可用）。
     * 先把 decor 藏起来避免闪烁，布局完成后再从按钮中心扩散展开。
     */
    private void playCircularReveal(AlertDialog dialog, View anchor) {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
            return;
        }
        android.view.Window window = dialog.getWindow();
        if (window == null) {
            return;
        }
        final View decor = window.getDecorView();
        decor.setVisibility(View.INVISIBLE);
        decor.post(() -> {
            int[] anchorLoc = new int[2];
            anchor.getLocationOnScreen(anchorLoc);
            int[] decorLoc = new int[2];
            decor.getLocationOnScreen(decorLoc);
            int cx = anchorLoc[0] + anchor.getWidth() / 2 - decorLoc[0];
            int cy = anchorLoc[1] + anchor.getHeight() / 2 - decorLoc[1];
            float maxRadius = (float) Math.hypot(Math.max(cx, decor.getWidth() - cx),
                    Math.max(cy, decor.getHeight() - cy));
            android.animation.Animator reveal = android.view.ViewAnimationUtils
                    .createCircularReveal(decor, cx, cy, 0f, maxRadius);
            reveal.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(android.animation.Animator animation) {
                    decor.setVisibility(View.VISIBLE);
                }
            });
            reveal.setDuration(320);
            reveal.start();
        });
    }

    /** 读取剪贴板 → CodeParser 解析 → 回填到录入表单的三个字段和平台选择。 */
    private void pasteAndParse(
            EditText carrierInput,
            EditText codeInput,
            EditText stationInput,
            PlatformHolder holder
    ) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) {
            Toast.makeText(this, "剪贴板为空，无法识别", Toast.LENGTH_SHORT).show();
            return;
        }
        CharSequence raw = manager.getPrimaryClip().getItemAt(0).getText();
        if (raw == null) {
            Toast.makeText(this, "剪贴板里没有文字", Toast.LENGTH_SHORT).show();
            return;
        }
        CodeParser.Result result = CodeParser.parse(raw.toString());
        if (result == null || result.code.isEmpty()) {
            Toast.makeText(this, "没从这串文字里找到取件码", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!result.carrier.isEmpty()) {
            carrierInput.setText(result.carrier);
        }
        codeInput.setText(result.code);
        if (!result.station.isEmpty()) {
            stationInput.setText(result.station);
        }
        if (!result.platform.isEmpty()) {
            holder.value = Destination.identityFor(result.platform);
            restylePlatformPicker(holder.row, holder.value);
        }
        Toast.makeText(this, "已识别取件码 " + result.code, Toast.LENGTH_SHORT).show();
    }

    private void confirmDelete(PackageStore.Item target) {
        new AlertDialog.Builder(this)
                .setTitle("删除包裹")
                .setMessage("确定删除取件码 " + target.code + " 吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    List<PackageStore.Item> items = PackageStore.load(this);
                    List<PackageStore.Item> remaining = new ArrayList<>();
                    for (PackageStore.Item item : items) {
                        boolean same = item.createdAt == target.createdAt
                                && item.code.equals(target.code);
                        if (!same) {
                            remaining.add(item);
                        }
                    }
                    PackageStore.save(this, remaining);
                    refreshPackageList();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void copyCode(String code) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager != null) {
            manager.setPrimaryClip(ClipData.newPlainText("取件码", code));
        }
        Toast.makeText(this, "已复制 " + code, Toast.LENGTH_SHORT).show();
    }

    // =====================================================================
    // 自动获取：短信权限、通知使用权、剪贴板捕获
    // =====================================================================

    private boolean hasSmsPermission() {
        return android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M
                || checkSelfPermission(android.Manifest.permission.RECEIVE_SMS)
                == android.content.pm.PackageManager.PERMISSION_GRANTED;
    }

    private void requestSmsPermission() {
        if (hasSmsPermission()) {
            Toast.makeText(this, "短信权限已开启，收到取件短信会自动保存", Toast.LENGTH_SHORT).show();
            return;
        }
        requestPermissions(new String[]{android.Manifest.permission.RECEIVE_SMS},
                REQUEST_SMS_PERMISSION);
    }

    private void openNotificationListenerSettings() {
        try {
            startActivity(new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        } catch (Exception ignored) {
            Toast.makeText(this, "无法打开设置，请手动在系统设置里开启通知使用权",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void updateAutoStatus() {
        if (smsStatusView != null) {
            smsStatusView.setText(hasSmsPermission()
                    ? "已开启 · 收到取件短信自动保存"
                    : "未授权 · 点击授权后自动识别取件短信");
        }
        if (notifyStatusView != null) {
            notifyStatusView.setText(PickupNotificationListener.isEnabled(this)
                    ? "已开启 · 菜鸟/淘宝/拼多多通知里的取件码自动保存"
                    : "未开启 · 点击前往系统设置开启通知使用权");
        }
    }

    private void checkClipboard() {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null || !manager.hasPrimaryClip()) {
            return;
        }
        ClipData clip = manager.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) {
            return;
        }
        CharSequence raw = clip.getItemAt(0).getText();
        if (raw == null) {
            return;
        }
        String value = raw.toString().trim();
        if (value.isEmpty() || PackageStore.isClipHandled(this, value)) {
            return;
        }

        CodeParser.Result parsed = CodeParser.parse(value);
        String code = parsed != null ? parsed.code : CodeParser.parseBareCode(value);
        if (code == null || code.isEmpty()) {
            return;
        }
        PackageStore.markClipHandled(this, value);

        List<PackageStore.Item> existing = PackageStore.load(this);
        for (PackageStore.Item item : existing) {
            if (code.equals(item.code)) {
                return;
            }
        }

        String carrier = parsed != null ? parsed.carrier : "";
        String station = parsed != null ? parsed.station : "";
        String platform = parsed != null ? parsed.platform : "";
        showClipDialog(code, carrier, station, platform);
    }

    private void showClipDialog(String code, String carrier, String station, String platform) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(22), dp(18), dp(22), 0);

        TextView tip = text("检测到取件码：" + code, 13, ACCENT_WATER, Typeface.BOLD);
        box.addView(tip);

        EditText carrierInput = inputField("快递公司 / 平台（可留空）");
        carrierInput.setText(carrier);
        LinearLayout.LayoutParams carrierParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        carrierParams.topMargin = dp(12);
        carrierInput.setLayoutParams(carrierParams);
        box.addView(carrierInput);

        EditText stationInput = inputField("驿站位置（可留空）");
        stationInput.setText(station);
        LinearLayout.LayoutParams stationParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        stationParams.topMargin = dp(10);
        stationInput.setLayoutParams(stationParams);
        box.addView(stationInput);

        box.addView(platformSectionTitle());
        final PlatformHolder holder = new PlatformHolder();
        box.addView(buildPlatformPicker(holder, Destination.identityFor(platform)));

        new AlertDialog.Builder(this)
                .setTitle("保存这个取件码？")
                .setView(box)
                .setPositiveButton("保存", (dialog, which) -> {
                    PackageStore.addIfAbsent(
                            this,
                            carrierInput.getText().toString().trim(),
                            code,
                            stationInput.getText().toString().trim(),
                            holder.value.key
                    );
                    refreshPackageList();
                })
                .setNegativeButton("忽略", null)
                .show();
    }

    private View settingsRow(
            String emoji,
            String title,
            TextView subtitleView,
            View.OnClickListener listener
    ) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(14), dp(14));
        row.setBackground(rounded(CARD_WHITE, 18));
        row.setClickable(true);
        row.setFocusable(true);
        row.setOnClickListener(listener);
        row.setLayoutParams(verticalParams(dp(12)));

        TextView emojiView = text(emoji, 18, TEXT_PRIMARY, Typeface.NORMAL);
        row.addView(emojiView, new LinearLayout.LayoutParams(dp(34), dp(34)));

        LinearLayout labels = new LinearLayout(this);
        labels.setOrientation(LinearLayout.VERTICAL);
        labels.setPadding(dp(10), 0, dp(8), 0);

        labels.addView(text(title, 15, TEXT_PRIMARY, Typeface.BOLD));
        subtitleView.setLineSpacing(0, 1.4f);
        labels.addView(subtitleView);

        row.addView(labels, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView arrow = text("›", 24, ACCENT_WATER, Typeface.NORMAL);
        arrow.setGravity(Gravity.CENTER);
        row.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(34)));
        return row;
    }

    private EditText inputField(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setTextSize(14);
        input.setTextColor(TEXT_PRIMARY);
        input.setHintTextColor(TEXT_SECONDARY);
        GradientDrawable background = rounded(Color.rgb(242, 246, 249), 12);
        background.setStroke(dp(1), LINE_COLOR);
        input.setBackground(background);
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setMaxLines(2);
        return input;
    }

    /** 弹窗里的平台选择结果持有者：lambda 里要改，所以不能直接拿局部变量。 */
    private static final class PlatformHolder {
        Destination value = Destination.TAOBAO;
        LinearLayout row;   // 供智能粘贴后回写选中态
    }

    private TextView platformSectionTitle() {
        TextView title = text("去哪个平台出库", 12, TEXT_SECONDARY, Typeface.BOLD);
        LinearLayout.LayoutParams params = verticalParams(dp(16));
        params.bottomMargin = dp(8);
        title.setLayoutParams(params);
        return title;
    }

    /** 三选一平台选择器，选中的结果写进 holder.value。 */
    private View buildPlatformPicker(PlatformHolder holder, Destination initial) {
        holder.value = initial;
        LinearLayout row = new LinearLayout(this);
        holder.row = row;
        row.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < Destination.IDENTITY_DESTINATIONS.length; i++) {
            Destination destination = Destination.IDENTITY_DESTINATIONS[i];
            TextView button = text(destination.identityName(), 13, TEXT_PRIMARY, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setClickable(true);
            button.setFocusable(true);
            button.setTag(destination);
            button.setContentDescription("选择" + destination.identityName());
            button.setOnClickListener(v -> {
                holder.value = (Destination) v.getTag();
                restylePlatformPicker(row, holder.value);
            });
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(42), 1f);
            if (i > 0) {
                params.leftMargin = dp(8);
            }
            row.addView(button, params);
        }
        restylePlatformPicker(row, holder.value);
        return row;
    }

    private void restylePlatformPicker(LinearLayout row, Destination selected) {
        for (int i = 0; i < row.getChildCount(); i++) {
            View child = row.getChildAt(i);
            if (!(child instanceof TextView) || !(child.getTag() instanceof Destination)) {
                continue;
            }
            Destination destination = (Destination) child.getTag();
            boolean picked = destination == selected;
            int[] colors = colorsFor(destination);
            GradientDrawable background = rounded(picked ? colors[1] : CARD_WHITE, 10);
            background.setStroke(dp(1), picked ? colors[1] : LINE_COLOR);
            child.setBackground(background);
            ((TextView) child).setTextColor(picked ? Color.WHITE : TEXT_SECONDARY);
        }
    }

    // =====================================================================
    // 通用小组件
    // =====================================================================

    private View hintCard(String emoji, String message) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        card.setLayoutParams(verticalParams(dp(12)));

        GradientDrawable background = rounded(Color.rgb(242, 249, 252), 18);
        background.setStroke(dp(1), Color.rgb(211, 230, 239));
        card.setBackground(background);

        TextView emojiView = text(emoji, 20, TEXT_PRIMARY, Typeface.NORMAL);
        LinearLayout.LayoutParams emojiParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        emojiParams.rightMargin = dp(10);
        emojiView.setLayoutParams(emojiParams);
        card.addView(emojiView);

        TextView messageView = text(message, 12, Color.rgb(61, 92, 110), Typeface.NORMAL);
        messageView.setLineSpacing(0, 1.5f);
        card.addView(messageView, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        return card;
    }

    private void addPinButton(
            LinearLayout parent,
            Destination destination,
            String label
    ) {
        TextView button = text(label, 13, TEXT_PRIMARY, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        GradientDrawable background = rounded(CARD_WHITE, 10);
        background.setStroke(dp(1), LINE_COLOR);
        button.setBackground(background);
        button.setClickable(true);
        button.setFocusable(true);
        button.setContentDescription("将" + destination.title + "添加到桌面");
        button.setOnClickListener(view -> ShortcutPinning.request(this, destination));

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1f);
        if (parent.getChildCount() > 0) {
            params.leftMargin = dp(8);
        }
        parent.addView(button, params);
    }

    private TextView text(String value, float sizeSp, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private LinearLayout.LayoutParams verticalParams(int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.topMargin = dp(topMarginDp);
        return params;
    }

    private LinearLayout.LayoutParams horizontalWeightParams(int leftMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        params.leftMargin = dp(leftMarginDp);
        return params;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private GradientDrawable gradient(int[] colors, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                colors
        );
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
