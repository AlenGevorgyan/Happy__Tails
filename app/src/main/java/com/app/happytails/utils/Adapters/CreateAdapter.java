package com.app.happytails.utils.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.app.happytails.R;
import com.app.happytails.utils.model.GalleryImage;
import com.bumptech.glide.Glide;

import java.util.List;

public class CreateAdapter extends RecyclerView.Adapter<CreateAdapter.CreateHolder> {

    private List<GalleryImage> list;

    SendImage onImageSend;

    public CreateAdapter(List<GalleryImage> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public CreateHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.images_item, parent, false);
        return new CreateHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CreateHolder holder, final int position) {
        Glide.with(holder.imageView.getContext().getApplicationContext())
                        .load(list.get(position).getPicUrl())
                        .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            chooseImage(Uri.parse(list.get(position).getPicUrl()));
        });
    }

    private void chooseImage(Uri pictureUri){

        onImageSend.onSend(pictureUri);

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    class CreateHolder extends RecyclerView.ViewHolder{

        private ImageView imageView;

        public CreateHolder(@NonNull View itemView) {
            super(itemView);

            imageView = itemView.findViewById(R.id.imageView);
        }
    }

    public interface SendImage{
        void onSend(Uri picUri);
    }

    public void SendImage(SendImage sendImage) {
        this.onImageSend = sendImage;
    }
}
