package com.cncoderx.launcher.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Filter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.cncoderx.recyclerviewhelper.adapter.DelegateAdapter;
import com.cncoderx.recyclerviewhelper.listener.OnItemClickListener;
import com.cncoderx.recyclerviewhelper.listener.OnItemLongClickListener;

import java.util.HashMap;

public class XRecyclerView extends RecyclerView {
    private DelegateAdapter mDelegateAdapter;
    private final HashMap<String, View> mHeaderViewCache = new HashMap<>();
    private final HashMap<String, View> mFooterViewCache = new HashMap<>();

    public XRecyclerView(@NonNull Context context) {
        this(context, null);
    }

    public XRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        if (adapter == null) {
            super.setAdapter(null);
        } else {
            mDelegateAdapter = new DelegateAdapter(adapter);
            super.setAdapter(mDelegateAdapter);
        }
    }

    public DelegateAdapter getDelegateAdapter() {
        return mDelegateAdapter;
    }

    @Nullable
    @Override
    public Adapter getAdapter() {
        return mDelegateAdapter != null ? mDelegateAdapter.getWrappedAdapter() : null;
    }

    public View getHeaderView(String tag) {
        return mHeaderViewCache.get(tag);
    }

    public void addHeaderView(View headerView, String tag) {
        mHeaderViewCache.put(tag, headerView);
        if (mDelegateAdapter != null) {
            mDelegateAdapter.addHeaderView(headerView);
        }
    }

    public void addHeaderView(View headerView, boolean isFullSpan, String tag) {
        mHeaderViewCache.put(tag, headerView);
        if (mDelegateAdapter != null) {
            mDelegateAdapter.addHeaderView(headerView, isFullSpan);
        }
    }

    public void removeHeaderView(String tag) {
        View headerView = mHeaderViewCache.remove(tag);
        if (headerView != null) {
            removeHeaderView(headerView);
        }
    }

    public void removeHeaderView(View headerView) {
        if (mDelegateAdapter != null) {
            mDelegateAdapter.removeHeaderView(headerView);
        }
    }

    public View getFooterView(String tag) {
        return mFooterViewCache.get(tag);
    }

    public void addFooterView(View footerView, String tag) {
        mFooterViewCache.put(tag, footerView);
        if (mDelegateAdapter != null) {
            mDelegateAdapter.addFooterView(footerView);
        }
    }

    public void addFooterView(View footerView, boolean isFullSpan, String tag) {
        mFooterViewCache.put(tag, footerView);
        if (mDelegateAdapter != null) {
            mDelegateAdapter.addFooterView(footerView, isFullSpan);
        }
    }

    public void removeFooterView(String tag) {
        View footerView = mFooterViewCache.remove(tag);
        if (footerView != null) {
            removeFooterView(footerView);
        }
    }

    public void removeFooterView(View footerView) {
        if (mDelegateAdapter != null) {
            mDelegateAdapter.removeFooterView(footerView);
        }
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        if (mDelegateAdapter != null) {
            mDelegateAdapter.setOnItemClickListener(listener);
        }
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        if (mDelegateAdapter != null) {
            mDelegateAdapter.setOnItemLongClickListener(listener);
        }
    }

    public boolean isEmpty() {
        return mDelegateAdapter != null && mDelegateAdapter.getItemCount() == 0;
    }

    public void setFilterText(String filterText) {
        if (mDelegateAdapter != null) {
            Filter filter = mDelegateAdapter.getFilter();
            if (filter != null) {
                filter.filter(filterText);
            }
        }
    }
}
