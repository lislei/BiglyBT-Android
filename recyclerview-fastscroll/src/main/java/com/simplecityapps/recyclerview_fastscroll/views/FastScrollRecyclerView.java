/*
 * Copyright (c) 2016 Tim Malseed
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.simplecityapps.recyclerview_fastscroll.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Typeface;
import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import com.google.android.material.appbar.AppBarLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.simplecityapps.recyclerview_fastscroll.R;
import com.simplecityapps.recyclerview_fastscroll.interfaces.OnFastScrollStateChangeListener;
import com.simplecityapps.recyclerview_fastscroll.utils.Utils;


/**
 * @note https://github.com/timusus/RecyclerView-FastScroll/blob/master/recyclerview-fastscroll/src/main/java/com/simplecityapps/recyclerview_fastscroll/views/FastScrollRecyclerView.java
 * with enableFastScrolling and null checks and stuff
 */

public class FastScrollRecyclerView extends RecyclerView implements RecyclerView.OnItemTouchListener {



    private FastScroller mScrollbar;
    private AppBarLayout mAppBarLayout;
    int lastScrolledPosition = 0;

    int expandCount = 0;

    boolean enableFastScrolling = true;

    /**
     * The current scroll state of the recycler view.  We use this in onUpdateScrollbar()
     * and scrollToPositionAtProgress() to determine the scroll position of the recycler view so
     * that we can calculate what the scroll bar looks like, and where to jump to from the fast
     * scroller.
     */
    public static class ScrollPositionState {
        // The index of the first visible row
        public int rowIndex;
        // The offset of the first visible row
        public int rowTopOffset;
        // The height of a given row (they are currently all the same height)
        public int rowHeight;
    }

    private ScrollPositionState mScrollPosState = new ScrollPositionState();

    private int mDownX;
    private int mDownY;
    private int mLastY;

    private OnFastScrollStateChangeListener mStateChangeListener;

    public FastScrollRecyclerView(Context context) {
        this(context, null);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FastScrollRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
            attrs, R.styleable.FastScrollRecyclerView, 0, 0);
        try {
            enableFastScrolling = typedArray.getBoolean(R.styleable.FastScrollRecyclerView_fastScrollerEnabled, true);
        } finally {
            typedArray.recycle();
        }
        if (enableFastScrolling) {
            mScrollbar = new FastScroller(context, this, attrs);
        }
    }

    //Todo: move
    // move where and why?
    public int getScrollBarWidth() {
        return mScrollbar == null ? 0 : mScrollbar.getWidth();
    }

    public int getScrollBarThumbHeight() {
        return mScrollbar == null ? 0 : mScrollbar.getThumbHeight();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (enableFastScrolling) {
            addOnItemTouchListener(this);
        }
    }

    /**
     * We intercept the touch handling only to support fast scrolling when initiated from the
     * scroll bar.  Otherwise, we fall back to the default RecyclerView touch handling.
     */
    @Override
    public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent ev) {
        return handleTouchEvent(ev);
    }

    @Override
    public void onTouchEvent(RecyclerView rv, MotionEvent ev) {
        handleTouchEvent(ev);
    }

    /**
     * Handles the touch event and determines whether to show the fast scroller (or updates it if
     * it is already showing).
     */
    private boolean handleTouchEvent(MotionEvent ev) {
        if (mScrollbar == null) {
            return false;
        }
        int action = ev.getAction();
        int x = (int) ev.getX();
        int y = (int) ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // Keep track of the down positions
                mDownX = x;
                mDownY = mLastY = y;
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener);
                break;
            case MotionEvent.ACTION_MOVE:
                mLastY = y;
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener);
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mScrollbar.handleTouchEvent(ev, mDownX, mDownY, mLastY, mStateChangeListener);
                break;
        }
        return mScrollbar.isDragging();
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

    }

    /**
     * Returns the available scroll height:
     * AvailableScrollHeight = Total height of the all items - last page height
     * <p>
     * This assumes that all rows are the same height.
     *
     * @param yOffset the offset from the top of the recycler view to start tracking.
     */
    protected int getAvailableScrollHeight(int rowCount, int rowHeight, int yOffset) {
        int visibleHeight = getHeight();
        int scrollHeight = getPaddingTop() + yOffset + rowCount * rowHeight + getPaddingBottom();
        int availableScrollHeight = scrollHeight - visibleHeight;
        return availableScrollHeight;
    }

    /**
     * Returns the available scroll bar height:
     * AvailableScrollBarHeight = Total height of the visible view - thumb height
     */
    protected int getAvailableScrollBarHeight() {
        if (mScrollbar == null) {
            return 0;
        }
        int visibleHeight = getHeight();
        int availableScrollBarHeight = visibleHeight - mScrollbar.getThumbHeight();
        return availableScrollBarHeight;
    }

    /* Supposed to fix ItemDecoration drawing on top, but this call doesn't
     * draw at all for me
     *
    @Override
    public void draw(Canvas c) {
        super.draw(c);
        if (enableFastScrolling) {
            onUpdateScrollbar();
            mScrollbar.draw(c);
        }
    }
     */

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (enableFastScrolling) {
            onUpdateScrollbar();
            mScrollbar.draw(canvas);
        }
    }

    /**
     * Updates the scrollbar thumb offset to match the visible scroll of the recycler view.  It does
     * this by mapping the available scroll area of the recycler view to the available space for the
     * scroll bar.
     *
     * @param scrollPosState the current scroll position
     * @param rowCount       the number of rows, used to calculate the total scroll height (assumes that
     *                       all rows are the same height)
     * @param yOffset        the offset to start tracking in the recycler view (only used for all apps)
     */
    protected void synchronizeScrollBarThumbOffsetToViewScroll(ScrollPositionState scrollPosState, int rowCount, int yOffset) {
        if (mScrollbar == null) {
            return;
        }
        int availableScrollHeight = getAvailableScrollHeight(rowCount, scrollPosState.rowHeight, yOffset);
        int availableScrollBarHeight = getAvailableScrollBarHeight();

        // Only show the scrollbar if there is height to be scrolled
        if (availableScrollHeight <= 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        // Calculate the current scroll position, the scrollY of the recycler view accounts for the
        // view padding, while the scrollBarY is drawn right up to the background padding (ignoring
        // padding)
        int scrollY = getPaddingTop() + yOffset + (scrollPosState.rowIndex * scrollPosState.rowHeight) - scrollPosState.rowTopOffset;
        int scrollBarY = (int) (((float) scrollY / availableScrollHeight) * availableScrollBarHeight);

        // Calculate the position and size of the scroll bar
        int scrollBarX;
        if (Utils.isRtl(getResources())) {
            scrollBarX = 0;
        } else {
            scrollBarX = getWidth() - mScrollbar.getWidth();
        }
        mScrollbar.setThumbPosition(scrollBarX, scrollBarY);
    }

    /**
     * Maps the touch (from 0..1) to the adapter position that should be visible.
     */
    public String scrollToPositionAtProgress(float touchFraction) {
        int itemCount = getAdapter().getItemCount();
        if (itemCount == 0) {
            return "";
        }
        int spanCount = 1;
        int rowCount = itemCount;
        if (getLayoutManager() instanceof GridLayoutManager) {
            spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
            rowCount = (int) Math.ceil((double) rowCount / spanCount);
        }

        // Stop the scroller if it is scrolling
        stopScroll();

        getCurScrollState(mScrollPosState);

        float itemPos = itemCount * touchFraction;

        int availableScrollHeight = getAvailableScrollHeight(rowCount, mScrollPosState.rowHeight, 0);

        //The exact position of our desired item
        int exactItemPos = (int) (availableScrollHeight * touchFraction);

        //Scroll to the desired item. The offset used here is kind of hard to explain.
        //If the position we wish to scroll to is, say, position 10.5, we scroll to position 10,
        //and then offset by 0.5 * rowHeight. This is how we achieve smooth scrolling.
        LinearLayoutManager layoutManager = ((LinearLayoutManager) getLayoutManager());
        int tempLastScrolledPositon;
        layoutManager.scrollToPositionWithOffset(tempLastScrolledPositon = spanCount * exactItemPos / mScrollPosState.rowHeight,
                -(exactItemPos % mScrollPosState.rowHeight));
        if (mAppBarLayout != null) {
            if (tempLastScrolledPositon >= lastScrolledPosition && tempLastScrolledPositon != 0) {
                --expandCount;
            } else {
                ++expandCount;
            }

            if (expandCount == 3) {
                mAppBarLayout.setExpanded(true, true);
                expandCount = 0;
            } else if (expandCount == -3) {
                mAppBarLayout.setExpanded(false, true);
                expandCount = 0;
            }
        }
        lastScrolledPosition = tempLastScrolledPositon;
        if (!(getAdapter() instanceof SectionedAdapter)) {
            return "";
        }

        int posInt = (int) ((touchFraction == 1) ? itemPos - 1 : itemPos);

        SectionedAdapter sectionedAdapter = (SectionedAdapter) getAdapter();
        return sectionedAdapter.getSectionName(posInt);
    }

    /**
     * Updates the bounds for the scrollbar.
     */
    public void onUpdateScrollbar() {
        if (mScrollbar == null) {
            return;
        }
        Adapter adapter = getAdapter();
        if (adapter == null) {
            return;
        }
        int rowCount = adapter.getItemCount();
        if (getLayoutManager() instanceof GridLayoutManager) {
            int spanCount = ((GridLayoutManager) getLayoutManager()).getSpanCount();
            rowCount = (int) Math.ceil((double) rowCount / spanCount);
        }
        // Skip early if, there are no items.
        if (rowCount == 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        // Skip early if, there no child laid out in the container.
        getCurScrollState(mScrollPosState);
        if (mScrollPosState.rowIndex < 0) {
            mScrollbar.setThumbPosition(-1, -1);
            return;
        }

        synchronizeScrollBarThumbOffsetToViewScroll(mScrollPosState, rowCount, 0);
    }

    /**
     * Returns the current scroll state of the apps rows.
     */
    private void getCurScrollState(ScrollPositionState stateOut) {
        stateOut.rowIndex = -1;
        stateOut.rowTopOffset = -1;
        stateOut.rowHeight = -1;

        int itemCount = getAdapter().getItemCount();

        // Return early if there are no items, or no children.
        if (itemCount == 0 || getChildCount() == 0) {
            return;
        }

        View child = getChildAt(0);
        if (child == null) {
            return;
        }

        stateOut.rowIndex = getChildAdapterPosition(child);
        if (getLayoutManager() instanceof GridLayoutManager) {
            stateOut.rowIndex = stateOut.rowIndex / ((GridLayoutManager) getLayoutManager()).getSpanCount();
        }
        if (child != null) {
            stateOut.rowTopOffset = getLayoutManager().getDecoratedTop(child);
            stateOut.rowHeight = child.getHeight();
        }
    }

    public void setThumbColor(@ColorInt int color) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setThumbColor(color);
    }

    public void setTrackColor(@ColorInt int color) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setTrackColor(color);
    }

    public void setPopupBgColor(@ColorInt int color) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setPopupBgColor(color);
    }

    public void setPopupTextColor(@ColorInt int color) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setPopupTextColor(color);
    }

    public void setPopupTextSize(int textSize) {
        mScrollbar.setPopupTextSize(textSize);
    }

    public void setPopUpTypeface(Typeface typeface) {
        mScrollbar.setPopupTypeface(typeface);
    }

    public void setAutoHideDelay(int hideDelay) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setAutoHideDelay(hideDelay);
    }

    public void setAutoHideEnabled(boolean autoHideEnabled) {
        if (mScrollbar == null) {
            return;
        }
        mScrollbar.setAutoHideEnabled(autoHideEnabled);
    }

    public void setStateChangeListener(OnFastScrollStateChangeListener stateChangeListener) {
        mStateChangeListener = stateChangeListener;
    }

    public interface SectionedAdapter {
        @NonNull
        String getSectionName(int position);
    }

    public void attachAppBarLayout(AppBarLayout appBarLayout) {
        this.mAppBarLayout = appBarLayout;
    }

    public void setScrollPopUpTypeface(Typeface typeface) {
        mScrollbar.setPopupTypeface(typeface);
    }

    public void setEnableFastScrolling(boolean enable) {
        this.enableFastScrolling = enable;
        invalidate();
    }

}
