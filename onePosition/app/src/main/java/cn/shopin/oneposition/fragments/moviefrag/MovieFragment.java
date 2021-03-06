package cn.shopin.oneposition.fragments.moviefrag;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import cn.shopin.oneposition.R;
import cn.shopin.oneposition.ScaleInTransformer;
import cn.shopin.oneposition.adapter.ViewPagerAdapter;
import cn.shopin.oneposition.constants.Cans;
import cn.shopin.oneposition.customview.ItemView;
import cn.shopin.oneposition.customview.TabButton;
import cn.shopin.oneposition.entity.movie.BannerDetailEntity;
import cn.shopin.oneposition.fragments.BaseMvpFragment;
import cn.shopin.oneposition.fragments.moviefrag.collection.CollectionFrag;
import cn.shopin.oneposition.fragments.moviefrag.nostalgic.NostalgicFrag;
import cn.shopin.oneposition.fragments.moviefrag.piecerate.MoviePieceFrag;
import cn.shopin.oneposition.model.db.DBManager;
import cn.shopin.oneposition.util.EnumServerMap;

/**
 * Created by zcs on 2016/12/5.
 */
public class MovieFragment extends BaseMvpFragment<MoviePresenter> implements MovieContract.IMovieView, View.OnClickListener, ViewPager.OnPageChangeListener, View.OnTouchListener {
    /**
     * 这个方法中我们主要是通过布局填充器获取fragment布局.
     * Nullable 表示可以为空-即：可传空值
     */
    private ViewPager viewPager;
    private ViewPagerAdapter pagerAdapter;
    private List<ImageView> imgs;
    private SparseArray<Fragment> sparseArray;
    private CollectionFrag collectionFrag;
    private NostalgicFrag nostalgicFrag;
    private MoviePieceFrag moviePieceFrag;
    List<BannerDetailEntity> dataList;
    private TabButton tabButton;
    private FragmentManager fragManager;
    private ItemView item1;
    private ItemView item2;
    private ItemView item3;
    @BindView(R.id.swipe_refreshlayout)
    protected SwipeRefreshLayout swipeRefreshLayout;
    @BindView(R.id.nestedScrollview)
    protected NestedScrollView nestedScrollview;
    private static int selectedIndex = 1;
    @Inject
    protected DBManager dbManager;
    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            swipeRefreshLayout.setRefreshing(false);
        }
    };

    /**
     * fragment初始化的时候调用，我们通常在onCreate方法中使
     * 用getArguments获取activity传来的初始化fragment的参数。
     * 注意：这个方法中我们不能获取activity中的控件！
     * 因为，activity的onCreate还没有执行完，即activity还没有创建完，
     * 要想获取activity相关的资源应该在onActivityCreated中获取。
     * 从activity和fragment的生命周期对比图可以明显看出这一点。
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inidFrags();
    }

    @Override
    protected void initInject() {
        getFragmentComponent().inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.frag_movie;
    }

    @Override
    protected void initEventAndData() {
        initView();
        initListener();
        initViewPager();
        initData();
    }

    /**
     * 初始化Frags
     */
    private void inidFrags() {
        //切换时：点击时子Frag才加载--只执行一次onCreate方法，onCreateView方法会重新执行
        collectionFrag = new CollectionFrag();
        nostalgicFrag = new NostalgicFrag();
        moviePieceFrag = new MoviePieceFrag();
        sparseArray = new SparseArray<>();
        sparseArray.put(1, collectionFrag);
        sparseArray.put(2, nostalgicFrag);
        sparseArray.put(3, moviePieceFrag);
    }

    /**
     * viewPager 动态加载,刷新后判断得到的item数量是否==imgs.size
     */
    private void initViewPager() {
        imgs = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ImageView img = new ImageView(getActivity());
            imgs.add(img);
        }
        pagerAdapter = new ViewPagerAdapter(getActivity(), imgs);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setPageMargin(0);
        viewPager.setOffscreenPageLimit(3);
        viewPager.setPageTransformer(true, new ScaleInTransformer());
        nestedScrollview.setOnTouchListener(this);
        viewPager.setOnTouchListener(this);
        pagerAdapter.notifyDataSetChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void initView() {
        fragManager = getChildFragmentManager();
        viewPager = (ViewPager) mView.findViewById(R.id.viewpager);
        tabButton = (TabButton) mView.findViewById(R.id.bt_hovertab);
        swipeRefreshLayout = (SwipeRefreshLayout) mView.findViewById(R.id.swipe_refreshlayout);
        swipeRefreshLayout.setEnabled(false);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                handler.sendEmptyMessageDelayed(0, 2000);
            }
        });
        nestedScrollview = (NestedScrollView) mView.findViewById(R.id.nestedScrollview);
        // TODO: 2017/1/19 防止执行onCreateView时，item再次创建
        if (tabButton.getChildCount() == 0) {
            item1 = new ItemView(getActivity());
            item1.setFragment(sparseArray.get(1));
            item1.setTag(1);
            item2 = new ItemView(getActivity());
            item2.setFragment(sparseArray.get(2));
            item2.setTag(2);
            item3 = new ItemView(getActivity());
            item3.setFragment(sparseArray.get(3));
            item3.setTag(3);
            tabButton.addView(item1);
            tabButton.addView(item2);
            tabButton.addView(item3);
        }
        // TODO: 2017/1/19 首次进入|外层Frag切换
        if (!sparseArray.get(selectedIndex).isAdded()) {
            tabButton.resetSelectedIndex(selectedIndex);
            fragManager.beginTransaction().add(R.id.frag_container, sparseArray.get(selectedIndex)).commitAllowingStateLoss();
        }
    }

    public void initListener() {
        item1.setOnClickListener(this);
        item2.setOnClickListener(this);
        item3.setOnClickListener(this);
        viewPager.addOnPageChangeListener(this);
    }

    //进行数据加载
    public void initData() {
        dataList = new ArrayList<>();
        //得到banner数据
        mPresenter.getBanners();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onClick(View view) {
        if (Integer.valueOf(String.valueOf(view.getTag())).equals(selectedIndex)) {
            return;
        }
        int currIndex = (int) view.getTag();
        if (sparseArray.get(currIndex).isAdded()) {
            // TODO: 2017/3/30 因为：IllegalStateException: Can not perform this action after onSaveInstanceState，commit() 替换成 commitAllowingStateLoss() 然并O
            fragManager.beginTransaction().hide(sparseArray.get(selectedIndex)).show(sparseArray.get(currIndex)).commitAllowingStateLoss();
        } else {
            fragManager.beginTransaction().hide(sparseArray.get(selectedIndex)).add(R.id.frag_container, sparseArray.get(currIndex)).commitAllowingStateLoss();
        }
        tabButton.resetSelectedIndex(selectedIndex = currIndex);
    }

    /**
     * 得到banner数据
     *
     * @param lists
     */
    @Override
    public void getBannerData(List<BannerDetailEntity> lists) {
        dataList.clear();
        dataList.addAll(lists);
        dataList.add(0, dataList.get(dataList.size() - 1));
        dataList.add(dataList.size(), dataList.get(1));
        if (dataList.size() > imgs.size()) {
            for (int i = imgs.size(); i < dataList.size(); i++) {
                ImageView img = new ImageView(getActivity());
                imgs.add(img);
            }
        } else if (dataList.size() < imgs.size()) {
            for (int size = imgs.size(); size > dataList.size(); ) {
                imgs.remove(--size);
            }
        }
        pagerAdapter.notifyDataSetChanged();
        for (int i = 0; i < dataList.size(); i++) {
            Picasso.with(getActivity()).load(EnumServerMap.getBaseUrlByTag(Cans.TAG_MOVIE) + dataList.get(i).getPic()).placeholder(R.color.colorWhite).fit().centerCrop().into(imgs.get(i));
        }
        viewPager.setCurrentItem(xposition, false);
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
//        swipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void onPageSelected(int position) {
        xposition = position;
    }

    int xposition = 1;

    @Override
    public void onPageScrollStateChanged(int state) {
        if (state == ViewPager.SCROLL_STATE_IDLE) {
            if (xposition == 0) {
                xposition = dataList.size() - 2;
            } else if (xposition == dataList.size() - 1) {
                xposition = 1;
            }
            viewPager.setCurrentItem(xposition, false);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (v.getId()) {
            case R.id.viewpager:
            case R.id.nestedScrollview:
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        Rect rect = new Rect();
                        if (viewPager.getGlobalVisibleRect(rect)) {
                            if (rect.height() == viewPager.getHeight()) {
                                swipeRefreshLayout.setEnabled(true);
                                refresh(selectedIndex);
                            } else {
                                swipeRefreshLayout.setEnabled(false);
                            }
                        } else {
                            swipeRefreshLayout.setEnabled(false);
                        }
                        break;
                }
                break;
        }
        return false;
    }

    private void refresh(int selectedIndex) {
        Toast.makeText(getActivity(), "刷新 " + selectedIndex, Toast.LENGTH_SHORT).show();
    }
}
