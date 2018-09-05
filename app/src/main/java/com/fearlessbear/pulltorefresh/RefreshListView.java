package com.fearlessbear.pulltorefresh;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class RefreshListView extends ListView implements AbsListView.OnScrollListener {

    // 下拉刷新状态
    protected static final int REFRESH_STATE_CLOSED = 0;
    protected static final int REFRESH_STATE_PULL = 1;
    protected static final int REFRESH_STATE_OVER_PULL = 2;
    protected static final int REFRESH_STATE_RELEASE_TO_REFRESH = 3;
    protected static final int REFRESH_STATE_REFRESHING = 4;
    protected static final int REFRESH_STATE_CLOSING = 5;

    // 上拉加载更多状态
    protected static final int LOAD_STATE_CLOSED = 0;
    protected static final int LOAD_STATE_LOADING = 1;
    protected static final int LOAD_STATE_NO_MORE = 2;

    public static final int DEFAULT_OPEN_REFRESH_DURATION = 500;
    public static final int DEFAULT_CLOSE_REFRESH_DURATION = 200;
    public static final int DEFAULT_RELEASE_TO_REFRESH_DURATION = 200;

    // TODO: 04/09/2018 移动到strings
    String LOADING = "正在加载更";
    String NO_MORE = "没有更多了";

    protected int mRefreshState = REFRESH_STATE_CLOSED;
    protected int mLoadState = LOAD_STATE_CLOSED;

    protected View mRefreshView;
    protected ProgressBar mRefreshBar;
    protected int mRefreshViewOriginHeight;

    private ValueAnimator mOpenRefreshAnimation;
    private ValueAnimator mCloseRefreshAnimation;
    private ValueAnimator mReleaseToRefreshAnimation;
    private ValueAnimator.AnimatorUpdateListener mUpdatePaddingListener;
    private LinearInterpolator mLinearInterpolator;
    protected boolean mIsAnimating;

    protected View mLoadingView;
    protected ProgressBar mLoadBar;
    protected TextView mLoadText;

    protected OnRefreshListener mOnRefreshListener;
    protected OnLoadMoreListener mOnLoadMoreListener;
    protected OnRefreshStateChangeListener mOnRefreshStateChangeListener;

    protected int mLastDownY;

    public RefreshListView(Context context) {
        this(context, null);
    }

    public RefreshListView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RefreshListView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initHeaderView();
        initFooterView();
    }

    protected void initHeaderView() {
        View search = View.inflate(getContext(), R.layout.load_more_footer_layout, null);
        addHeaderView(search);

        mRefreshView = View.inflate(getContext(), R.layout.refresh_header_layout, null);
        mRefreshBar = mRefreshView.findViewById(R.id.pb_refresh);

        mRefreshView.measure(0, 0);
        mRefreshViewOriginHeight = mRefreshView.getMeasuredHeight();
        mRefreshView.setPadding(0, -mRefreshViewOriginHeight, 0, 0);
        addHeaderView(mRefreshView);

        mLinearInterpolator = new LinearInterpolator();
        mUpdatePaddingListener = new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mRefreshView.setPadding(0, ((Integer) animation.getAnimatedValue()), 0, 0);
            }
        };

        mOpenRefreshAnimation = ValueAnimator.ofInt(-mRefreshViewOriginHeight, 0);
        mOpenRefreshAnimation.setDuration(DEFAULT_OPEN_REFRESH_DURATION);
        mOpenRefreshAnimation.setInterpolator(mLinearInterpolator);
        mOpenRefreshAnimation.addUpdateListener(mUpdatePaddingListener);
        mOpenRefreshAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setRefreshState(REFRESH_STATE_PULL);
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setRefreshState(REFRESH_STATE_REFRESHING);
                mIsAnimating = false;

                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mRefreshView.setPadding(0, -mRefreshViewOriginHeight, 0, 0);
                setRefreshState(REFRESH_STATE_CLOSED);
                mIsAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mCloseRefreshAnimation = ValueAnimator.ofInt(0, -mRefreshViewOriginHeight);
        mCloseRefreshAnimation.setDuration(DEFAULT_CLOSE_REFRESH_DURATION);
        mCloseRefreshAnimation.setInterpolator(mLinearInterpolator);
        mCloseRefreshAnimation.addUpdateListener(mUpdatePaddingListener);
        mCloseRefreshAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setRefreshState(REFRESH_STATE_CLOSING);
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setRefreshState(REFRESH_STATE_CLOSED);
                mIsAnimating = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mRefreshView.setPadding(0, -mRefreshViewOriginHeight, 0, 0);
                setRefreshState(REFRESH_STATE_CLOSED);
                mIsAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        mReleaseToRefreshAnimation = ValueAnimator.ofInt(0, mRefreshViewOriginHeight);
        mReleaseToRefreshAnimation.setInterpolator(mLinearInterpolator);
        mReleaseToRefreshAnimation.addUpdateListener(mUpdatePaddingListener);
        mReleaseToRefreshAnimation.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                setRefreshState(REFRESH_STATE_RELEASE_TO_REFRESH);
                mIsAnimating = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                setRefreshState(REFRESH_STATE_REFRESHING);
                mIsAnimating = false;

                if (mOnRefreshListener != null) {
                    mOnRefreshListener.onRefresh();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                mRefreshView.setPadding(0, 0, 0, 0);
                setRefreshState(REFRESH_STATE_REFRESHING);
                mIsAnimating = false;
            }

            @Override
            public void onAnimationRepeat(Animator animation) {
            }
        });

        setOnRefreshStateChangeListener(new OnRefreshStateChangeListener() {
            @Override
            public void onStateChange(int state) {
                refreshRefreshView(state);
            }
        });

    }

    protected void initFooterView() {
        mLoadingView = View.inflate(getContext(), R.layout.load_more_footer_layout, null);
        mLoadBar = mLoadingView.findViewById(R.id.pb_load_more);
        mLoadText = mLoadingView.findViewById(R.id.tv_load_text);
        mLoadingView.setVisibility(GONE);
        addFooterView(mLoadingView);
    }

    public boolean stopRefresh() {
        if (mRefreshView == null || mCloseRefreshAnimation == null
                || mRefreshState != REFRESH_STATE_REFRESHING) {
            return false;
        }

        mCloseRefreshAnimation.setIntValues(0, -mRefreshViewOriginHeight);
        mCloseRefreshAnimation.start();
        return true;
    }

    public boolean startRefresh() {
        if (mLoadState == LOAD_STATE_LOADING || mRefreshView == null
                || mOpenRefreshAnimation == null
                || mRefreshState != REFRESH_STATE_CLOSED) {
            return false;
        }

        mOpenRefreshAnimation.start();
        return true;
    }

    public boolean stopLoadMore() {
        if (mLoadingView == null || mLoadState == LOAD_STATE_CLOSED) {
            return false;
        }

        mLoadingView.setVisibility(GONE);
        mLoadState = LOAD_STATE_CLOSED;
        return true;
    }

    public boolean startLoadMore() {
        if (mRefreshState != REFRESH_STATE_CLOSING || mLoadingView == null
                || mLoadState == LOAD_STATE_LOADING) {
            return false;
        }

        mLoadBar.setVisibility(VISIBLE);
        mLoadText.setVisibility(VISIBLE);
        mLoadText.setText(LOADING);
        mLoadingView.setVisibility(VISIBLE);
        mLoadState = LOAD_STATE_LOADING;

        if (mOnLoadMoreListener != null) {
            mOnLoadMoreListener.onLoadMore();
        }
        return true;
    }

    public boolean setLoadNoMore() {
        if (mLoadingView == null || mLoadState == LOAD_STATE_NO_MORE) {
            return false;
        }

        mLoadBar.setVisibility(GONE);
        mLoadText.setVisibility(VISIBLE);
        mLoadText.setText(NO_MORE);
        mLoadingView.setVisibility(VISIBLE);
        mLoadState = LOAD_STATE_NO_MORE;
        return true;
    }

    private void setRefreshState(int state) {
        if (mRefreshState == state) {
            return;
        }

        mRefreshState = state;
        if (mOnRefreshStateChangeListener != null) {
            mOnRefreshStateChangeListener.onStateChange(state);
        }
    }

    protected boolean mHandleRefresh;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastDownY = (int) ev.getY();
                break;
            case MotionEvent.ACTION_MOVE:
                int firstVisiblePosition = getFirstVisiblePosition();
                View firstChild = getChildAt(0);
                int deltaY = (int) (ev.getY() - mLastDownY);

                if (firstChild == null
                        || firstChild.getTop() < 0
                        || firstVisiblePosition != 0
                        || mLoadState == LOAD_STATE_LOADING
                        || mIsAnimating
                        || mRefreshState == REFRESH_STATE_REFRESHING
                        || (!mHandleRefresh && mRefreshState == REFRESH_STATE_CLOSED && deltaY < 0)) {
                    break;
                }

                mHandleRefresh = true;

                if (mRefreshState == REFRESH_STATE_CLOSED && deltaY < 0) {
                    mLastDownY = (int) ev.getY();
                    break;
                }

                int paddingTop = Math.max(-mRefreshViewOriginHeight, -mRefreshViewOriginHeight + deltaY);
                mRefreshView.setPadding(0, paddingTop, 0, 0);

                if (mRefreshState == REFRESH_STATE_CLOSED && deltaY > 0) {
                    setRefreshState(REFRESH_STATE_PULL);
                } else if (mRefreshState == REFRESH_STATE_PULL && paddingTop > 0) {
                    setRefreshState(REFRESH_STATE_OVER_PULL);
                } else if (mRefreshState == REFRESH_STATE_OVER_PULL && paddingTop < 0) {
                    setRefreshState(REFRESH_STATE_PULL);
                } else if (mRefreshState == REFRESH_STATE_PULL && deltaY <= 0) {
                    setRefreshState(REFRESH_STATE_CLOSED);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                switch (mRefreshState) {
                    case REFRESH_STATE_OVER_PULL:
                        mReleaseToRefreshAnimation.setIntValues(mRefreshView.getPaddingTop(), 0);
                        mReleaseToRefreshAnimation.setDuration((1000));
                        mReleaseToRefreshAnimation.start();
                        break;
                    case REFRESH_STATE_PULL:
                        mCloseRefreshAnimation.setIntValues(mRefreshView.getPaddingTop(), -mRefreshViewOriginHeight);
                        mCloseRefreshAnimation.start();
                        break;
                }
                mHandleRefresh = false;
                break;
        }

        return mHandleRefresh || super.onTouchEvent(ev);
    }

    private void refreshRefreshView(int state) {
        // TODO: 04/09/2018 更改refreshView的状态
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
    }


    @Override
    public void onScroll(AbsListView view, int firstVisibleItem,
                         int visibleItemCount, int totalItemCount) {

    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mOpenRefreshAnimation != null) {
            mOpenRefreshAnimation.cancel();
        }
        if (mCloseRefreshAnimation != null) {
            mCloseRefreshAnimation.cancel();
        }
        if (mReleaseToRefreshAnimation != null) {
            mReleaseToRefreshAnimation.cancel();
        }
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        mOnRefreshListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        mOnLoadMoreListener = listener;
    }

    public void setOnRefreshStateChangeListener(OnRefreshStateChangeListener listener) {
        mOnRefreshStateChangeListener = listener;
    }

    public interface OnRefreshListener {
        void onRefresh();
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }

    public interface OnRefreshStateChangeListener {
        void onStateChange(int state);
    }
}
