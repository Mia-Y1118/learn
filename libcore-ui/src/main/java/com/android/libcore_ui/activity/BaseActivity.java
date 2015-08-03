package com.android.libcore_ui.activity;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.android.libcore.activity.RootActivity;
import com.android.libcore.log.L;
import com.android.libcore.utils.CommonUtils;
import com.android.libcore_ui.R;
import com.android.libcore_ui.widget.BottomBarGroupLinearLayout;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Description: 继承自{@link RootActivity}的基础activity，在这里进行页面界面的统一<br/><br/>
 *
 *
 * 应用整体样式现在有status bar颜色修改和底部navigation bar透明两种样式<br/>
 * 应用的top bar样式有自定义ViewGroup和toolbar两种样式<br/><br/>
 *
 *
 * <ol>
 * <li>{@linkplain #receiver}用来在组件之间进行广播的接收</li>
 * <li>{@linkplain #initView()}用来初始化该activity的view，第一步调用{@link #setContentViewSrc(Object)}
 * 进行设置布局，参数为layout的id或者view，在里面进行{@link #findViewById(int)}等等操作</li>
 * <li>{@linkplain #initData()}用来初始化该activity的data</li>
 * <li>{@linkplain #setTitle(String)}用来设置页面标题</li>
 * <li>{@linkplain #addOptionsMenuView(View)}用来在页面的右侧添加一个按钮</li>
 * <li>{@linkplain #addNavigationOnBottom(ViewGroup)}将一个和NavigationBar的高度一样的空白的view添加到viewGroup中</li>
 * <li>{@linkplain #onHandleMessageFromFragment(Message)}用来处理fragment传递过来的消息</li>
 * </ol>
 *
 * 自定义底部弹出框:
 * <ul>
 * <li>{@link #addItemToBottomPopWindow(int, int, String)}方法用来在底部弹出的框内加上按钮选项，有组id，元素id,
 * 和元素名称来标识,显示的上下顺序将会按照添加时候的顺序显示，如果需要在中间插入一个组元素，则初始化添加的时候调用
 * {@link #addItemToBottomPopWindow(int, int, String)}函数的时候，groupId传一个新值，itemId传递一个小于0
 * 的数值即可，代表先占一个空位，方便以后来对该groupId位置的元素进行操作</li>
 * <li>{@link #removeItemFromBottomPopWindow(int, int)}方法用来删除在底部添加的按钮选项</li>
 * <li>{@link #showBottomPopWindow()}方法用来显示底部popwindow，调用之前请先调用
 * {@link #addItemToBottomPopWindow(int, int, String)}方法</li>
 * <li>{@link #onItemClickCallback(int, int)}方法由子类继承用来处理底部弹出框的点击回调</li>
 * </ul>
 *
 * <strong>{@linkplain #initView()}和{@linkplain #initData()}需要子类实现</strong>
 *
 * @author zzp(zhao_zepeng@hotmail.com)
 * @since 2015-07-08
 */
public abstract class BaseActivity extends RootActivity{

    /** 填充19版本以上SDK　status bar */
    protected View v_status_bar;
    /** 填充19版本以上SDK　navigation bar */
    protected View v_navigation_bar;
    /** 头部top bar容器 */
    protected FrameLayout fl_top_bar;
    /** 头部top bar */
    public ViewGroup top_bar;
    /** 内容区域 */
    protected FrameLayout base_content;
    /** 全屏的半透明显示 */
    protected View ll_full_screen;
    /** 底部popWindow */
    protected ScrollView sv_bottom_content;
    protected LinearLayout ll_bottom_content;
    /** 底部弹出框数据集合 */
    protected LinkedHashMap<Integer, ArrayList<ItemHolder>> bottomItems;

    protected LayoutInflater inflater;
    protected ObjectAnimator popAnimation;
    protected ObjectAnimator reverseAnimation;

    /** 底部弹出框的默认高度 */
    protected int scrollViewMeasureHeight;

    /** 底部navigation是否透明,如果应用使用的是Activity_translucent_navigation_bar风格，
     * navigation bar透明的风格，则下面这个变量会变成true,并且一定不要忘记调用addBlankOnBottom(View view)
     * 函数，将一个空白的view添加进去即可 */
    private boolean isUsingNavigation = false;
    /** 设置整个应用主题样式是否使用toolbar还是使用自定义view */
    private boolean useToolbar = true;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        //如果系统的主题为Activity_translucent_navigation_bar，但是手机没有navigation bar，则将其设置回status bar主题，
        // setTheme设置主题一定要在onCreate()之前
        if (!CommonUtils.hasNavigationBar()
                && getApplicationInfo().theme==R.style.Activity_translucent_navigation_bar) {
            setTheme(R.style.Activity_translucent_status_bar);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base_layout);
        fl_top_bar = (FrameLayout) findViewById(R.id.fl_top_bar);
        base_content = (FrameLayout) findViewById(R.id.base_content);
        sv_bottom_content = (ScrollView) findViewById(R.id.sv_bottom_content);
        ll_bottom_content = (LinearLayout) findViewById(R.id.ll_bottom_content);
        ll_full_screen = findViewById(R.id.ll_full_screen);
        ll_full_screen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doReverseAnimation();
            }
        });

        //SDK19版本以上，仿material风格
        if (Build.VERSION.SDK_INT >= 19 &&
                (getApplicationInfo().theme==R.style.Activity_translucent_status_bar
                        || getApplicationInfo().theme==R.style.Activity_translucent_navigation_bar)){
            v_status_bar = findViewById(R.id.v_status_bar);
            v_navigation_bar = findViewById(R.id.v_navigation_bar);
            v_status_bar.setVisibility(View.VISIBLE);
            v_navigation_bar.setVisibility(View.VISIBLE);

            int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
            v_status_bar.getLayoutParams().height = getResources().getDimensionPixelOffset(id);
            //如果手机无navigation bar，则直接关闭该功能
            if (CommonUtils.hasNavigationBar()){
                isUsingNavigation = (getApplicationInfo().theme == R.style.Activity_translucent_navigation_bar);
                //仿bilibili的底部navigation bar透明风格
                if (isUsingNavigation) {
                    id = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
                    v_navigation_bar.getLayoutParams().height = getResources().getDimensionPixelOffset(id);
                }
            }
        }

        //添加top bar,现在有两种样式的top bar可以使用：
        //一种是自定的viewGroup，比如QQ
        if (!useToolbar) {
            top_bar = (ViewGroup) View.inflate(this, R.layout.activity_top_bar_layout, null);
            View rl_back = top_bar.findViewById(R.id.rl_back);
            rl_back.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            TextView tv_title = (TextView) top_bar.findViewById(R.id.tv_title);
            // 通过 android::label 设置的标题
            if (!TextUtils.isEmpty(getTitle()))
                tv_title.setText(getTitle());
        }
        //一种是使用系统控件toolbar，比如微信
        else{
            top_bar = (ViewGroup) View.inflate(this, R.layout.activity_top_toolbar_layout, null);
            Toolbar toolbar = (Toolbar) top_bar;
            setSupportActionBar(toolbar);
            ((Toolbar) top_bar).setNavigationIcon(getDrawable(R.mipmap.ic_arrow_back));
            ((Toolbar) top_bar).setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });
            // 通过 android::label 设置的标题
            if (!TextUtils.isEmpty(getTitle()))
                toolbar.setTitle(getTitle());
        }

        fl_top_bar.addView(top_bar);

        bottomItems = new LinkedHashMap<>();
        inflater = LayoutInflater.from(this);

        initView();
        initData();
    }

    /** <Strong><font color=red>第一步</font></Strong>调用{@link #setContentViewSrc(Object)}进行布局的设置 */
    protected abstract void initView();
    protected abstract void initData();

    /**
     * 设置标题
     */
    protected void setTitle(String title){
        if (!useToolbar) {
            ((TextView) top_bar.findViewById(R.id.tv_title)).setText(title);
        }else{
            ((Toolbar) top_bar).setTitle(title);
        }
    }

    /**
     * 将view添加进top bar右侧的相关区域中
     */
    protected void addOptionsMenuView(View view){
        if (!useToolbar){
            ((ViewGroup) top_bar.findViewById(R.id.rl_top_extra_content)).addView(view);
        }else{
            top_bar.addView(view);
            //TODO toolbar的添加menu有问题
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!useToolbar) {
            L.e("该样式无法使用optionsMenu，请将view单独addView到top bar的相关区域即可");
            return false;
        }
        else{
            menu.clear();
            return super.onCreateOptionsMenu(menu);
        }
    }

    /**
     * 请使用该函数来设置该页面需要显示的内容，不包括topbar
     * @param object resource id 或者 view
     */
    protected void setContentViewSrc(Object object){
        if (object instanceof Integer){
            LayoutInflater inflater = LayoutInflater.from(this);
            View v = inflater.inflate((Integer)object, null);
            base_content.addView(v);
        }else if(object instanceof View){
            base_content.addView((View)object);
        }else{
            L.e("参数只能为id或者view");
        }
    }

    /**
     * 底部item的数据集合
     */
    private class ItemHolder{
        private int itemId;
        private String name;
    }

    /**
     * 通过添加item到底部bar来创建一系列的选项
     * @param groupId 该item的组id，不同的组id在不同的区域内,请使用大于0的数字来表示
     * @param itemId 该item的item id，用来标示该item，组内的两个item不能有相同的item id,要不然回调无法识别
     * @param name 用来显示该item的名字
     */
    protected void addItemToBottomPopWindow(int groupId, int itemId, String name){
        ArrayList<ItemHolder> temp = null;
        if (bottomItems.containsKey(groupId)) {
            if (itemId < 0){
                throw new IllegalArgumentException("groupId can be found,so itemId must bigger than 0 or equal 0");
            }
            temp = bottomItems.get(groupId);
            ItemHolder holder = new ItemHolder();
            holder.itemId = itemId;
            holder.name = name;
            temp.add(holder);
        }
        else {
            temp = new ArrayList<>();
            if (itemId >= 0) {
                ItemHolder holder = new ItemHolder();
                holder.itemId = itemId;
                holder.name = name;
                temp.add(holder);
            }
            bottomItems.put(groupId, temp);
        }
        buildBottomPopWindow();
    }

    /**
     * 将item从底部bar中删除
     * @param groupId
     * @param itemId
     */
    protected void removeItemFromBottomPopWindow(int groupId, int itemId){
        if (bottomItems.containsKey(groupId)){
            ArrayList<ItemHolder> temp = bottomItems.get(groupId);
            for (ItemHolder holder : temp){
                if (holder.itemId == itemId){
                    temp.remove(holder);
                    buildBottomPopWindow();
                    return;
                }
            }
            throw new IllegalArgumentException("can't find this itemId in this groupId");
        }else{
            throw new IllegalArgumentException("can't find this groupId");
        }
    }

    /**
     * 通过{@link #bottomItems}建立底部弹出框
     */
    private void buildBottomPopWindow(){
        if (bottomItems.size() <= 0)
            return;
        //现将底部弹出框的所有选项去除
        ll_bottom_content.removeAllViews();
        popAnimation = null;
        reverseAnimation = null;
        final Iterator iterator = bottomItems.entrySet().iterator();
        while (iterator.hasNext()){
            Map.Entry<Integer, ArrayList<ItemHolder>> entry = (Map.Entry<Integer, ArrayList<ItemHolder>>) iterator.next();
            Integer groupId = entry.getKey();
            ArrayList<ItemHolder> holder = entry.getValue();
            //如果该groupId的items不为0，代表需要将该group显示出来
            if (holder.size() >= 0){
                BottomBarGroupLinearLayout group = (BottomBarGroupLinearLayout) inflater.inflate(R.layout.bottom_group_layout, null);
                group.setGroupId(groupId);
                group.setCallback(new BottomBarGroupLinearLayout.GroupItemClickCallback() {
                    @Override
                    public void callback(int groupId, int itemId) {
                        onItemClickCallback(groupId, itemId);
                    }
                });
                for (ItemHolder temp : holder){
                    group.addItemToGroup(temp.itemId, temp.name);
                }
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams
                        (ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                int margin = CommonUtils.dp2px(10);
                params.setMargins(margin, 0, margin, margin);
                ll_bottom_content.addView(group, params);
            }
        }
        //每次组建完底部弹出框之后，就开始计算他的高度
        int width = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        sv_bottom_content.measure(width, width);
        scrollViewMeasureHeight = sv_bottom_content.getMeasuredHeight();
    }

    @Override
    public void onBackPressed() {
        if (sv_bottom_content.getVisibility() == View.VISIBLE){
            doReverseAnimation();
        }else {
            super.onBackPressed();
        }
    }

    /**
     * 点击底部弹出框的回调
     */
    protected void onItemClickCallback(int groupId, int itemId){
        doReverseAnimation();
    }

    /**
     * 执行反向动画将其隐藏
     */
    private void doReverseAnimation(){
        if (Build.VERSION.SDK_INT < 11) {
            sv_bottom_content.setVisibility(View.GONE);
            ll_full_screen.setVisibility(View.GONE);
        }else{
            //如果弹出动画还在执行，则直接将弹出动画的值置为最终值，代表该动画结束，接着直接进行收进动画
            popAnimation.end();
            //避免用户连续快速点击造成短时间内执行两次收进动画，此处进行判断
            if (reverseAnimation != null && reverseAnimation.isRunning()){
                return;
            }
            if (reverseAnimation == null) {
                reverseAnimation = ObjectAnimator.ofInt(sv_bottom_content, "bottomMargin", 0, -scrollViewMeasureHeight);
                reverseAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                        params.bottomMargin = value;
                        sv_bottom_content.setLayoutParams(params);
                        ((View) (sv_bottom_content.getParent())).invalidate();
                        if (value <= -scrollViewMeasureHeight){
                            sv_bottom_content.setVisibility(View.GONE);
                        }

                        ll_full_screen.setAlpha((float) (((scrollViewMeasureHeight + value) * 1.0) / (scrollViewMeasureHeight * 1.0)));
                        if (ll_full_screen.getAlpha()<=0){
                            ll_full_screen.setVisibility(View.GONE);
                        }
                    }
                });
                reverseAnimation.setDuration(500);
            }
            reverseAnimation.start();
        }
    }

    /**
     * 用来显示该popwindow，保证在调用该方法之前已经调用{@link #addItemToBottomPopWindow(int, int, String)}方法
     */
    protected void showBottomPopWindow(){
        if (Build.VERSION.SDK_INT >= 11) {
            //如果上次的动画还在执行，直接停止
            if (reverseAnimation != null){
                reverseAnimation.end();
            }
            sv_bottom_content.setVisibility(View.VISIBLE);
            ll_full_screen.setVisibility(View.VISIBLE);
            //需要滚动到顶部
            sv_bottom_content.scrollTo(0, 0);
            if (popAnimation == null) {
                popAnimation = ObjectAnimator.ofInt(sv_bottom_content, "bottomMargin", -scrollViewMeasureHeight, 0);
                popAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        int value = (Integer) animation.getAnimatedValue();
                        RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) sv_bottom_content.getLayoutParams();
                        params.bottomMargin = value;
                        sv_bottom_content.setLayoutParams(params);
                        ((View) (sv_bottom_content.getParent())).invalidate();

                        ll_full_screen.setAlpha((float) (((scrollViewMeasureHeight + value) * 1.0) / (scrollViewMeasureHeight * 1.0)));
                    }
                });
                popAnimation.setDuration(500);
            }
            popAnimation.start();
        }else{
            ll_full_screen.setVisibility(View.VISIBLE);
            sv_bottom_content.setVisibility(View.VISIBLE);
            //需要滚动到顶部
            sv_bottom_content.scrollTo(0, 0);
        }
    }

    /**
     * 将一个空白的代替navigation bar的view添加进传入的参数view中，记住该函数只是简单的将view添加进
     * 底部，所以传进来的view一定要在正确的位置
     */
    public void addNavigationOnBottom(ViewGroup view){
        if (CommonUtils.hasNavigationBar() && isUsingNavigation) {
            View navigationView = new View(this);
            ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, v_navigation_bar.getLayoutParams().height);
            navigationView.setLayoutParams(params);
            navigationView.setBackgroundColor(getResources().getColor(R.color.transparent));
            view.addView(navigationView);
        }
    }

    /**
     * 处理来自fragment的消息
     */
    protected void onHandleMessageFromFragment(Message msg){}
}
