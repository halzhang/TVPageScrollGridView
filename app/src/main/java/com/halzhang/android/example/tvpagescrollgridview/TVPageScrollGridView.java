package com.halzhang.android.example.tvpagescrollgridview;

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.GridView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 分页滚动
 * 1、按照行数计算item 高度
 * 2、解决平滑滚动问题，支持dpad 操作
 * 3、解决焦点选中
 * Created by Hal on 15/1/10.
 */
public class TVPageScrollGridView extends GridView implements AbsListView.OnScrollListener {

    private static final String LOG_TAG = TVPageScrollGridView.class.getSimpleName();

    private static final int SCROLL_DURATION = 300;

    /**
     * 分页滚动监听
     */
    public interface OnPageScrollListener {
        /**
         * 滚动到现有数据的最后一页
         *
         * @param pageScrollGridView
         */
        public void onScrollLastPage(TVPageScrollGridView pageScrollGridView);
    }

    private int mRowNum = 2;
    private int mRowHeight;
    private int mCurrentSelectedPosition = 0;

    private OnItemSelectedListener mOuterOnItemSelectedListener;
    private OnScrollListener mOuterOnScrollListener;

    private OnPageScrollListener mOnPageScrollListener;

    public TVPageScrollGridView(Context context) {
        this(context, null);
    }

    public TVPageScrollGridView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TVPageScrollGridView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setOnItemSelectedListener(onItemSelectedListener);
        super.setOnScrollListener(this);
        setFocusableInTouchMode(true);
    }

    @Override
    public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
        this.mOuterOnItemSelectedListener = onItemSelectedListener;
    }

    @Override
    public void setOnScrollListener(OnScrollListener l) {
        this.mOuterOnScrollListener = l;
    }

    public void setOnPageScrollListener(OnPageScrollListener onPageScrollListener) {
        mOnPageScrollListener = onPageScrollListener;
    }

    /**
     * 计算每行高度
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void computeRowHeight() {
        int height = getHeight();
        mRowHeight = (height - getPaddingTop() - getPaddingBottom() - (getVerticalSpacing() * (mRowNum - 1))) / mRowNum;
    }

    public int getRowHeight() {
        return mRowHeight;
    }


    public void setRowNum(int rowNum) {
        mRowNum = rowNum;
    }

    /**
     * 是否是当前页面的第二行
     *
     * @return
     * @deprecated 当数据有奇数行的时候，此方法有问题
     */
    private boolean isSecondRowInPage() {
        int selectedItemPosition = getSelectedItemPosition();
        int pageSize = getNumColumns() * mRowNum;
        int mod = ((selectedItemPosition + 1) % pageSize);
        return mod > getNumColumns() || mod == 0;
    }

    /**
     * 第一行
     */
    private boolean isFirstRowInPage() {
        int position = getSelectedItemPosition();
        int firstVisiblePosition = getFirstVisiblePosition();
        return position >= firstVisiblePosition && position < firstVisiblePosition + getNumColumns();
    }

    /**
     * 第一列
     *
     * @return
     */
    private boolean isFirstColumn() {
        int position = getSelectedItemPosition() + 1;
        return position % getNumColumns() == 1;
    }

    /**
     * 最后一列
     *
     * @return
     */
    private boolean isLastColumn() {
        int position = getSelectedItemPosition() + 1;
        return position % getNumColumns() == 0;
    }

    /**
     * 当前位置是否处于最后一页
     *
     * @return
     */
    private boolean isLastPage(int position) {
        int pageSize = mRowNum * getNumColumns();
        int count = getCount();
        int pageCount = count % pageSize == 0 ? count / pageSize : count / pageSize + 1;
        return position >= (pageCount - 1) * pageSize && position < count;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    @Override
    protected void layoutChildren() {
        super.layoutChildren();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int measuredHeight = getMeasuredHeight();
        int height = getHeight();
        Log.i(LOG_TAG, "MeasuredHeight: " + measuredHeight + " Height: " + height);
        computeRowHeight();
        Log.d(LOG_TAG, "RowHeight: " + mRowHeight);
        //根据行数，处理 item 高度
        final int childrenCount = getChildCount();
        for (int i = 0; i < childrenCount; i++) {
            View view = getChildAt(i);
            LayoutParams lp = (LayoutParams) view.getLayoutParams();
            if (lp == null) {
                lp = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mRowHeight);
            } else {
                lp.height = mRowHeight;
            }
            view.setLayoutParams(lp);
        }
        requestLayout();

    }

    /**
     * 滚动前处理，子类根据需要实现
     */
    protected void onScrollPage() {

    }


    /**
     * 上一页
     */
    private void scrollPrev() {
        mNextPosition = -1;
        mNextPosition = Math.max(0, getSelectedItemPosition() - getNumColumns());
        Log.i(LOG_TAG, "NextPage position: " + mNextPosition);
        if (getSelectedItemPosition() == mNextPosition) {
            return;
        }
        if (mNextPosition == 0) {
            callSetSelectionInt(0);
        } else if (mNextPosition > 0) {
            int scrollHeight = getPageScrollHeight();
            onScrollPage();
            smoothScrollBy((0 - scrollHeight) / 2, SCROLL_DURATION);
        }
    }

    private int getPageScrollHeight() {
        return getHeight() - getPaddingBottom() - getPaddingTop() + dip2px(getContext(), 50);
    }

    private int mNextPosition = -1;

    /**
     * 下一页
     */
    private void scrollNext() {
        mNextPosition = -1;
        mNextPosition = Math.min(getCount() - 1, getSelectedItemPosition() + getNumColumns());
        Log.i(LOG_TAG, "NextPage position: " + mNextPosition);
        if (getSelectedItemPosition() == mNextPosition) {
            return;
        }
        if (mNextPosition == getAdapter().getCount() - 1) {
            callSetSelectionInt(mNextPosition);
            return;
        }
        if (mNextPosition >= 0) {
            onScrollPage();
            smoothScrollBy(getPageScrollHeight() / 2, SCROLL_DURATION);
        }
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {
        Log.d(LOG_TAG, "ScrollState: " + scrollState);
        switch (scrollState) {
            case SCROLL_STATE_IDLE:
                if (mNextPosition >= 0) {
                    //滚动结束后，显示选中项
                    //TODO 处理遥控快速操作后的并发问题
//                    callSetSelectionInt(mNextPosition);
                    setSelection(mNextPosition);
                    mCurrentSelectedPosition = mNextPosition;
                    if (mOnPageScrollListener != null) {
                        if (isLastPage(mNextPosition)) {
                            mOnPageScrollListener.onScrollLastPage(this);
                        }
                    }
                    mNextPosition = -1;
                }
                break;
            default:
                break;

        }

        if (mOuterOnScrollListener != null) {
            mOuterOnScrollListener.onScrollStateChanged(view, scrollState);
        }

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
        if (mOuterOnScrollListener != null) {
            mOuterOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    private OnItemSelectedListener onItemSelectedListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            Log.i(LOG_TAG, "onItemSelected Position: " + position);
            if (mOuterOnItemSelectedListener != null) {
                mOuterOnItemSelectedListener.onItemSelected(parent, view, position, id);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
            if (mOuterOnItemSelectedListener != null) {
                mOuterOnItemSelectedListener.onNothingSelected(parent);
            }
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        final int action = event.getAction();
        final int keycode = event.getKeyCode();
        Log.d(LOG_TAG, "dispatchKeyEvent - action: " + action + " keycode: " + keycode);
        if (action == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_DPAD_DOWN) {
            if (!isFirstRowInPage()) {
                scrollNext();
                Log.i(LOG_TAG, "call scrollNext");
                return true;
            }
        } else if (action == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_DPAD_UP) {
            if (isFirstRowInPage()) {
                scrollPrev();
                Log.i(LOG_TAG, "call scrollPrev");
                return true;
            }
        } else if (action == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_DPAD_LEFT) {
            if (getSelectedItemPosition() != 0 && isFirstColumn()) {
                int position = getSelectedItemPosition() - 1;
                if (isFirstRowInPage()) {
                    //第一行的时候，要翻页
                    mNextPosition = position;
                    onScrollPage();
                    smoothScrollBy((0 - getPageScrollHeight()) / 2, SCROLL_DURATION);
                } else {
                    setSelection(position);
                }
                return true;
            }
        } else if (action == KeyEvent.ACTION_DOWN && keycode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if (getSelectedItemPosition() != getCount() - 1 && isLastColumn()) {
                int position = getSelectedItemPosition() + 1;
                if (!isFirstRowInPage()) {
                    //第二行的时候，要翻页
                    mNextPosition = position;
                    onScrollPage();
                    smoothScrollBy(getPageScrollHeight() / 2, SCROLL_DURATION);
                } else {
                    setSelection(position);
                }
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private static int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

    public void callPageScroll(int direction) {
        try {
            Class c = Class.forName(GridView.class.getName());
            Method pageScroll = c.getDeclaredMethod("pageScroll", int.class);
            pageScroll.setAccessible(true);
            pageScroll.invoke(this, direction);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void callSetSelectionInt(int position) {
        try {
            Class c = Class.forName(GridView.class.getName());
            Method setSelectionInt = c.getDeclaredMethod("setSelectionInt", int.class);
            setSelectionInt.setAccessible(true);
            setSelectionInt.invoke(this, position);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void callSetNextSelectedPositionInt(int position) {
        try {
            Class c = Class.forName(AdapterView.class.getName());
            Method setNextSelectedPositionInt = c.getDeclaredMethod("setNextSelectedPositionInt", int.class);
            setNextSelectedPositionInt.setAccessible(true);
            setNextSelectedPositionInt.invoke(this, position);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void callSetSelectedPositionInt(int position) {
        try {
            Class c = Class.forName(AdapterView.class.getName());
            Method setSelectedPositionInt = c.getDeclaredMethod("setSelectedPositionInt", int.class);
            setSelectedPositionInt.setAccessible(true);
            setSelectedPositionInt.invoke(this, position);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public void callFireOnSelected() {
        try {
            Class c = Class.forName(AdapterView.class.getName());
            Method fireOnSelected = c.getDeclaredMethod("fireOnSelected");
            fireOnSelected.setAccessible(true);
            fireOnSelected.invoke(this);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
