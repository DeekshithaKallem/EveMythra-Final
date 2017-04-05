package org.bvrit.evemythra.adapters;

import android.content.Context;
import android.databinding.DataBindingUtil;
import android.databinding.ViewDataBinding;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;


import org.bvrit.evemythra.BR;
import org.bvrit.evemythra.R;

import java.util.List;

/**
 * Created by Deekshitha on 09-03-2017.
 */

public class GenericRcyclerViewAdapter extends RecyclerView.Adapter {

    Context mContext;
    List<?> mList;
    int viewType;
    int viewVal;
    int timeline = 0;

    public GenericRcyclerViewAdapter(Context context, List<?> list, int viewType){
        this.mContext = context;
        this.mList = list;
        this.viewType = viewType;
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        RecyclerView.ViewHolder vh;
        View view;
        if(viewVal == timeline){
            view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.timeline_item, viewGroup, false);
            vh = new BindingHolder(view);
        } else {
            view = LayoutInflater.from(viewGroup.getContext()).inflate(
                    R.layout.layout_loading_item, viewGroup, false);

            vh = new BindingHolder(view);
        }
        return vh;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        ((BindingHolder) holder).binding.setVariable(BR.bindingValue, mList.get(position));
        ((BindingHolder) holder).binding.executePendingBindings();
    }

    @Override
    public int getItemCount() {
        return (null != mList ?
                mList.size() : 0);    }


    @Override
    public int getItemViewType(int position){
        if(viewType ==0){
            viewVal = viewType;
            return  viewVal;
        } else {
            return position;

        }

    }
    private static class BindingHolder extends RecyclerView.ViewHolder {
        ViewDataBinding binding;

        public BindingHolder(View itemView) {
            super(itemView);
            binding = DataBindingUtil.bind(itemView);

        }
    }
}
