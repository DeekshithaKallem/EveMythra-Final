package org.bvrit.evemythra.adapters;

import android.databinding.BindingAdapter;

import com.facebook.drawee.view.SimpleDraweeView;

/**
 * Created by Deekshitha on 09-03-2017.
 */

public class CustomBindingAdapter {


    @BindingAdapter("imageUrl")
   public static void loadImage(SimpleDraweeView imageView, String url){
       // imageView.setImageURI(url);
       // if(url!=null){
         //   Picasso.with(imageView.getContext()).load(url).placeholder(android.R.drawable.editbox_background).into(imageView);

      //  }
        imageView.setImageURI(url);
   }
}
